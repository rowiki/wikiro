package org.wikipedia.ro.ukraine;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.apache.commons.lang3.StringUtils.trimToNull;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.login.LoginException;

import org.wikibase.WikibaseException;
import org.wikibase.WikibasePropertyFactory;
import org.wikibase.data.Claim;
import org.wikibase.data.Entity;
import org.wikibase.data.Item;
import org.wikibase.data.Property;
import org.wikibase.data.Sitelink;
import org.wikipedia.Wiki;
import org.wikipedia.ro.model.WikiTemplate;
import org.wikipedia.ro.parser.ParseResult;
import org.wikipedia.ro.parser.WikiTemplateParser;
import org.wikipedia.ro.translit.IcuTransliterationService;
import org.wikipedia.ro.utility.AbstractExecutable;
import org.wikipedia.ro.utils.LinkUtils;

public class UkraineSettlementsUpdater extends AbstractExecutable {

    private static final String UKRAINE_REGIONS_CATEGORY = "Categorie:Regiuni ale Ucrainei";

    private static final String REGION_CATEGORY_PREFIX = "Categorie:Regiunea ";
    private static final String[] SETTLEMENT_CATEGORY_PATTERNS = {
        "Categorie:Sate din regiunea %s",
        "Categorie:Așezări de tip urban din regiunea %s",
        "Categorie:Orașe raionale din regiunea %s",
        "Categorie:Orașe regionale din regiunea %s"
    };

    /** P131 = "located in administrative territorial entity" */
    private static final Property P131 = WikibasePropertyFactory.getWikibaseProperty("P131");
    /** P31 = "instance of" */
    private static final Property P31 = WikibasePropertyFactory.getWikibaseProperty("P31");
    /** P36 = "capital" */
    private static final Property P36 = WikibasePropertyFactory.getWikibaseProperty("P36");
    /** P138 = "named after" */
    private static final Property P138 = WikibasePropertyFactory.getWikibaseProperty("P138");

    /** Q19356062 = "hromada of Ukraine" */
    private static final String HROMADA_QID = "Q19356062";

    /** Regions where the settlement's existing Romanian name/title predates the 2020 reform
     *  and must be kept as-is, instead of being replaced/renamed using the new raion/hromada. */
    private static final Set<String> SPECIAL_RO_NAME_REGIONS =
            Set.of("Transcarpatia", "Odesa", "Cernăuți");

    /** Pattern to extract old commune wikilink from article intro */
    private static final Pattern OLD_COMMUNE_LINK_PATTERN =
            Pattern.compile("(\\[\\[Comuna\\s+[^\\]]+\\]\\])");

    /** Pattern to extract old raion wikilink from article intro */
    private static final Pattern OLD_RAION_LINK_PATTERN =
            Pattern.compile("(\\[\\[Raionul\\s+[^\\]]+\\]\\])");

    /** Detect if the settlement was a commune capital from the intro text, regardless of tense/verb
     *  or phrasing (e.g. "este reședința", "a fost reședința", "localitatea de reședință"),
     *  and regardless of ș/ş and ț/ţ diacritic encoding */
    private static final Pattern RESIDENCE_PATTERN =
            Pattern.compile("re[șş]edin[țţ]");

    /** Pattern to extract the hromada wikilink from the Ukrainian Wikipedia infobox */
    private static final Pattern UK_INFOBOX_HROMADA_PATTERN =
            Pattern.compile("\\|\\s*громада\\s*=\\s*\\[\\[([^|\\]]+)");

    /** Infobox parameters removed from the kept infobox: geographical coordinates and the image */
    private static final String[] INFOBOX_PARAMS_TO_REMOVE =
            {"latd", "latm", "lats", "latNS", "longd", "longm", "longs", "longEV", "imagine"};

    private static final String INTRODUCERE_ASEZARE_TEMPLATE = "{{Introducere așezare}}";

    private static final String TRANSLIT_MODULE_URL =
            "https://ro.wikipedia.org/w/index.php?title=Modul:Transliteration/langdata&action=raw";

    private static final int MEMORY_LOG_INTERVAL = 50;

    private IcuTransliterationService transliterationService;
    private Wiki ukWiki;
    private PrintWriter renamingsReport;
    private PrintWriter adminLinksReport;
    private PrintWriter hromadaMismatchReport;
    private PrintWriter textChangesReport;
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

    @Override
    protected void execute() throws IOException, WikibaseException, LoginException {
        transliterationService = IcuTransliterationService.fromLuaUrl(TRANSLIT_MODULE_URL);
        ukWiki = Wiki.newSession("uk.wikipedia.org");

        renamingsReport = new PrintWriter(new FileWriter("report-renamings.txt"), true);
        adminLinksReport = new PrintWriter(new FileWriter("report-admin-links.txt"), true);
        hromadaMismatchReport = new PrintWriter(new FileWriter("report-hromada-mismatch.txt"), true);
        textChangesReport = new PrintWriter(new FileWriter("report-text-changes.txt"), true);

        try {
            List<String> regionCategories = wiki.getCategoryMembers(
                    UKRAINE_REGIONS_CATEGORY, Wiki.CATEGORY_NAMESPACE);

            int processedCount = 0;
            logMemoryUsage("Start");

            for (String regionCategory : regionCategories) {
                String regionName = extractRegionName(regionCategory);
                if (regionName == null || regionName.isEmpty()) {
                    continue;
                }

                for (String pattern : SETTLEMENT_CATEGORY_PATTERNS) {
                    String settlementCategory = String.format(pattern, regionName);
                    String settlementType = settlementTypeFromPattern(pattern);
                    try {
                        List<String> settlements = wiki.getCategoryMembers(
                                settlementCategory, Wiki.MAIN_NAMESPACE);
                        for (String settlement : settlements) {
                            processSettlement(settlement, regionName, settlementType);
                            processedCount++;
                            if (processedCount % MEMORY_LOG_INTERVAL == 0) {
                                logMemoryUsage("After " + processedCount + " settlements");
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("Could not get members of " + settlementCategory
                                + ": " + e.getMessage());
                    }
                }
            }
            logMemoryUsage("End (" + processedCount + " settlements total)");
        } finally {
            renamingsReport.close();
            adminLinksReport.close();
            hromadaMismatchReport.close();
            textChangesReport.close();
        }
    }

    private String extractRegionName(String categoryName) {
        if (categoryName.startsWith(REGION_CATEGORY_PREFIX)) {
            return categoryName.substring(REGION_CATEGORY_PREFIX.length()).trim();
        }
        // Fallback: strip the "Categorie:" namespace prefix
        return categoryName.replaceFirst("^Categorie:", "").trim();
    }

    private static String settlementTypeFromPattern(String pattern) {
        if (pattern.startsWith("Categorie:Sate")) {
            return "un sat";
        } else if (pattern.startsWith("Categorie:Așezări de tip urban")) {
            return "o așezare de tip urban";
        } else {
            return "un oraș";
        }
    }

    private void processSettlement(String pageName, String regionName, String settlementType)
            throws IOException, LoginException {
        Entity wdEntity = null;
        Entity hromadaEntity = null;
        Entity raionEntity = null;
        boolean isHromadaCapital = false;

        try {
            wdEntity = dwiki.getWikibaseItemBySiteAndTitle("rowiki", pageName);
            if (wdEntity != null) {
                hromadaEntity = findHromada(wdEntity);
                if (hromadaEntity == null) {
                    hromadaEntity = findHromadaFromUkWiki(wdEntity);
                }
                raionEntity = findAncestorEntityByLabel(wdEntity, "raion", 3);
                if (hromadaEntity != null) {
                    isHromadaCapital = isCapitalOf(hromadaEntity, wdEntity.getId());
                }
            }
        } catch (WikibaseException e) {
            System.err.println("  Could not fetch Wikidata item for " + pageName
                    + ": " + e.getMessage());
        }

        // Transcarpatia/Odesa/Cernăuți keep their pre-existing Romanian name/title;
        // elsewhere the settlement is (re)named after a transliteration of its Ukrainian name.
        boolean useExistingRoName = SPECIAL_RO_NAME_REGIONS.contains(regionName);
        String settlementName = resolveSettlementName(pageName, wdEntity, useExistingRoName);

        // Hromada QID and name
        String hromadaQid = hromadaEntity != null ? hromadaEntity.getId() : null;
        String hromadaName = computeHromadaLabel(hromadaEntity, regionName);

        // New raion QID and name
        String raionQid = raionEntity != null ? raionEntity.getId() : null;
        String raionName = extractCleanName(raionEntity, "raion");

        // --- Renamings report ---
        if (!useExistingRoName && raionName != null) {
            String proposedTitle = settlementName + ", " + raionName;
            if (!proposedTitle.equals(pageName)) {
                // Check if proposed title already exists (another settlement with same name in same raion)
                // A target that merely redirects to the current page is safe to rename over.
                boolean[] titleExists = wiki.exists(Collections.singletonList(proposedTitle));
                if (titleExists[0] && !redirectsTo(proposedTitle, pageName)) {
                    // Use hromada capital name as disambiguator
                    String hromadaCapitalName = findHromadaCapitalName(hromadaEntity, regionName);
                    if (hromadaCapitalName != null) {
                        proposedTitle = settlementName + " (" + hromadaCapitalName + "), " + raionName;
                    }
                }
            }
            if (!proposedTitle.equals(pageName)) {
                StringBuilder renamingLine = new StringBuilder();
                renamingLine.append(pageName).append(" --> ").append(proposedTitle);
                // Check if the final proposed title already exists
                boolean[] finalExists = wiki.exists(Collections.singletonList(proposedTitle));
                if (finalExists[0]) {
                    // If the existing page is a redirect back to the current page, rename is OK
                    if (!redirectsTo(proposedTitle, pageName)) {
                        renamingLine.append(" EXISTS");
                    }
                }
                renamingsReport.println(renamingLine);
            }
        }

        // --- Admin links report ---
        if (wdEntity != null) {
            String raionLink = buildAdminLink(raionEntity, "Raionul", raionName);
            String hromadaLink = buildAdminLink(hromadaEntity, null, hromadaName);
            adminLinksReport.println(wdEntity.getId() + ": " + pageName
                    + " -- raion " + raionLink + " -- hromada " + hromadaLink);
        }

        // Get article text and extract old admin info from the intro
        List<String> texts = wiki.getPageText(Collections.singletonList(pageName));
        String pageText = defaultString(texts != null && !texts.isEmpty() ? texts.get(0) : null);
        String intro = extractIntro(pageText);

        String oldCommuneLink = extractMatch(OLD_COMMUNE_LINK_PATTERN, intro);
        String oldRaionLink = extractMatch(OLD_RAION_LINK_PATTERN, intro);
        boolean wasOldResidence = RESIDENCE_PATTERN.matcher(intro).find();

        // Compose the new intro
        String newIntro = composeNewIntro(
                settlementName, settlementType, hromadaQid, hromadaName,
                raionQid, raionName, regionName, oldCommuneLink, oldRaionLink, wasOldResidence);

        WikiTemplate infobox = parseLeadingInfobox(intro);
        String restOfArticle = extractRest(pageText);
        String newArticleText = composeNewArticleText(infobox, newIntro, restOfArticle);

        System.out.println("=== " + pageName + " ===");
        System.out.println(newArticleText);
        System.out.println();

        textChangesReport.println("-------- " + pageName + " -------");
        textChangesReport.println("FROM:");
        textChangesReport.println(pageText);
        textChangesReport.println("TO:");
        textChangesReport.println(newArticleText);
        textChangesReport.println();
    }

    /** True if the existing page {@code title} resolves (via redirect) to {@code targetPage}. */
    private boolean redirectsTo(String title, String targetPage) throws IOException {
        List<String> resolved = wiki.resolveRedirects(Collections.singletonList(title));
        return resolved != null && !resolved.isEmpty() && targetPage.equals(resolved.get(0));
    }

    /**
     * Parses the infobox template at the start of the article's intro, if any.
     */
    private WikiTemplate parseLeadingInfobox(String intro) {
        int templateStart = intro.indexOf("{{");
        if (templateStart < 0) {
            return null;
        }
        ParseResult<WikiTemplate> parseResult =
                new WikiTemplateParser().parse(intro.substring(templateStart));
        return parseResult != null ? parseResult.getIdentifiedPart() : null;
    }

    /**
     * Assembles the new article text: the kept infobox (minus geographical coordinates
     * and the image), the new intro sentence, {{Introducere așezare}}, then any text the
     * original article had beyond its intro (sections, categories, etc.), left untouched.
     */
    private String composeNewArticleText(WikiTemplate infobox, String newIntro, String restOfArticle) {
        StringBuilder sb = new StringBuilder();
        if (infobox != null) {
            for (String param : INFOBOX_PARAMS_TO_REMOVE) {
                infobox.removeParam(param);
            }
            sb.append(infobox).append("\n\n");
        }
        sb.append(newIntro).append("\n\n").append(INTRODUCERE_ASEZARE_TEMPLATE);
        if (!restOfArticle.isEmpty()) {
            sb.append('\n').append(restOfArticle);
        }
        return sb.toString();
    }

    /**
     * Builds a link string for the admin links report.
     * Returns [[Article title]] if a rowiki article exists, or {{ill-wd|QID}} if not.
     */
    private String buildAdminLink(Entity entity, String prefix, String cleanName) {
        if (entity == null || cleanName == null) {
            return "?";
        }
        Sitelink roSitelink = entity.getSitelinks().get("rowiki");
        String displayName = prefix != null ? prefix + " " + cleanName : cleanName;
        if (roSitelink != null) {
            return LinkUtils.createLink(roSitelink.getPageName(), displayName);
        }
        return "{{Ill-wd|" + entity.getId() + "|3=" + displayName + "}}";
    }

    private String composeNewIntro(String settlementName, String settlementType,
            String hromadaQid, String hromadaName,
            String raionQid, String raionName,
            String regionName,
            String oldCommuneLink, String oldRaionLink, boolean wasOldResidence) {

        StringBuilder sb = new StringBuilder();
        sb.append("'''").append(settlementName).append("'''");
        sb.append(" ({{în limbi locale}})");
        sb.append(" este ").append(settlementType).append(" din ");

        // Hromada
        if (hromadaQid != null && hromadaName != null) {
            sb.append("{{Ill-wd|").append(hromadaQid).append("|").append(hromadaName).append("}}");
        } else {
            sb.append("<!-- hromada necunoscută -->");
        }
        sb.append(", ");

        // New raion
        if (raionQid != null && raionName != null) {
            sb.append("{{Ill-wd|").append(raionQid).append("|raionul ").append(raionName).append("}}");
        } else {
            sb.append("<!-- raion necunoscut -->");
        }
        sb.append(", ");

        // Region and country
        sb.append("[[regiunea ").append(regionName).append("]], [[Ucraina]].");

        // Old admin sentence
        if (oldCommuneLink != null || oldRaionLink != null) {
            if (wasOldResidence) {
                sb.append(" Până în 2017, a fost reședința ");
            } else {
                sb.append(" Până în 2017, a făcut parte din ");
            }
            if (oldCommuneLink != null) {
                sb.append(oldCommuneLink);
                if (oldRaionLink != null) {
                    sb.append(", ").append(oldRaionLink);
                }
            } else if (oldRaionLink != null) {
                sb.append(oldRaionLink);
            }
            sb.append(".");
        }

        return sb.toString();
    }

    /**
     * Resolves the settlement's display name/title. In {@link #SPECIAL_RO_NAME_REGIONS},
     * the existing Romanian Wikidata label (i.e. the pre-2020-reform name) is kept;
     * elsewhere the Ukrainian label is transliterated instead, per the new naming convention.
     */
    private String resolveSettlementName(String pageName, Entity wdEntity, boolean useExistingRoName) {
        String fallback = substringBefore(pageName, ",").trim();
        if (wdEntity == null) {
            return fallback;
        }
        if (useExistingRoName) {
            String roLabel = wdEntity.getLabels().get("ro");
            return roLabel != null ? roLabel : fallback;
        }
        String ukLabel = wdEntity.getLabels().get("uk");
        if (ukLabel != null) {
            return transliterationService.transliterate(substringBefore(ukLabel, ",").trim(), "uk");
        }
        String roLabel = wdEntity.getLabels().get("ro");
        return roLabel != null ? roLabel : fallback;
    }

    /**
     * Extracts a clean name from an entity label by removing a keyword prefix/suffix.
     * E.g., "Hromada Ivanivka" -> "Ivanivka", "Raionul Berezivka" -> "Berezivka".
     */
    private String extractCleanName(Entity entity, String keyword) {
        if (entity == null) {
            return null;
        }
        String label = entity.getLabels().get("ro");
        boolean needsTranslit = false;
        if (label == null) {
            label = entity.getLabels().get("uk");
            needsTranslit = true;
        }
        if (label == null) {
            return null;
        }
        // Remove keyword prefix (e.g. "Raionul ", "Hromada ")
        String cleaned = label.replaceFirst("(?i)^\\S*" + keyword + "\\S*\\s+", "");
        // Remove keyword suffix (e.g. " hromada")
        cleaned = cleaned.replaceFirst("(?i)\\s+\\S*" + keyword + "\\S*$", "");
        // Strip Wikidata disambiguation suffix (e.g. ", Cerkasî")
        cleaned = substringBefore(cleaned, ",");
        if (needsTranslit) {
            cleaned = transliterationService.transliterate(cleaned, "uk");
        }
        return trimToNull(cleaned);
    }

    /**
     * Extracts a clean name from a hromada entity's own label, stripping any leading
     * "Hromada"/"Comuna"/"Comunitatea" keyword together with an "urbană"/"rurală" type
     * word that may follow it, in either order (e.g. "Hromada urbană Cernăuți",
     * "Comunitatea rurală X" both become just the remaining name).
     */
    private String extractHromadaCleanName(Entity entity) {
        if (entity == null) {
            return null;
        }
        String label = entity.getLabels().get("ro");
        boolean needsTranslit = false;
        if (label == null) {
            label = entity.getLabels().get("uk");
            needsTranslit = true;
        }
        if (label == null) {
            return null;
        }
        String cleaned = substringBefore(label, ",").trim();
        if (!needsTranslit) {
            cleaned = stripHromadaPrefixWords(cleaned);
        }
        if (needsTranslit) {
            cleaned = transliterationService.transliterate(cleaned, "uk");
        }
        return trimToNull(cleaned);
    }

    /**
     * Strips up to two leading words from a hromada Romanian label that are just the
     * "hromada"/"comuna"/"comunitatea" keyword and/or an "urbană"/"rurală" type word.
     */
    private static String stripHromadaPrefixWords(String label) {
        String[] words = label.trim().split("\\s+", -1);
        int idx = 0;
        while (idx < words.length && idx < 2 && isHromadaPrefixWord(words[idx])) {
            idx++;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = idx; i < words.length; i++) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(words[i]);
        }
        return sb.toString();
    }

    private static boolean isHromadaPrefixWord(String word) {
        String lower = word.toLowerCase();
        return lower.startsWith("hromad") || lower.startsWith("comun")
                || lower.equals("urbană") || lower.equals("urbana")
                || lower.equals("rurală") || lower.equals("rurala");
    }

    /**
     * Resolves the display name to use for a naming-source entity (a hromada capital
     * or a P138 "named after" value): in {@link #SPECIAL_RO_NAME_REGIONS} its Romanian
     * label is preferred, elsewhere its Ukrainian label is transliterated instead (its
     * Romanian label, if any, is ignored outside those regions).
     */
    private String resolveEntityDisplayName(Entity entity, String regionName) {
        if (entity == null) {
            return null;
        }
        if (SPECIAL_RO_NAME_REGIONS.contains(regionName)) {
            String roLabel = entity.getLabels().get("ro");
            if (roLabel != null) {
                return substringBefore(roLabel, ",").trim();
            }
        }
        String ukLabel = entity.getLabels().get("uk");
        if (ukLabel != null) {
            return transliterationService.transliterate(substringBefore(ukLabel, ",").trim(), "uk");
        }
        String roLabel = entity.getLabels().get("ro");
        return roLabel != null ? substringBefore(roLabel, ",").trim() : null;
    }

    /**
     * Computes the hromada label as "hromada <type> <namingName>".
     * The type is determined from the Ukrainian name: селищна -> "de așezare", міська -> "urbană", сільська -> "rurală".
     * The name is derived from a P138 ("named after") claim if present, else the hromada capital (P36).
     * Also writes to the mismatch report if the first 3 chars of the hromada name differ from the naming name.
     */
    private String computeHromadaLabel(Entity hromadaEntity, String regionName) {
        if (hromadaEntity == null) {
            return null;
        }

        // Determine hromada type from Ukrainian label
        String hromadaType = determineHromadaType(hromadaEntity);

        // Find the naming source: P138 ("named after") first, then the capital (P36)
        String namingName = findHromadaNamingName(hromadaEntity, regionName);

        if (namingName == null) {
            // Fallback to extracting name from the hromada label itself
            namingName = extractHromadaCleanName(hromadaEntity);
        }

        if (namingName == null) {
            return null;
        }

        // Mismatch report: compare first 3 letters of hromada name vs naming name
        String hromadaCleanName = extractHromadaCleanName(hromadaEntity);
        if (hromadaCleanName != null && namingName.length() >= 3 && hromadaCleanName.length() >= 3) {
            String hromadaPrefix = hromadaCleanName.substring(0, 3).toLowerCase();
            String namingPrefix = namingName.substring(0, 3).toLowerCase();
            if (!hromadaPrefix.equals(namingPrefix)) {
                hromadaMismatchReport.println(hromadaEntity.getId()
                        + " (" + regionName + "): hromada name=\"" + hromadaCleanName
                        + "\" vs naming name=\"" + namingName + "\"");
            }
        }

        if (hromadaType != null) {
            return "hromada " + hromadaType + " " + namingName;
        }
        return "hromada " + namingName;
    }

    /**
     * Determines the type of hromada from its Ukrainian label.
     */
    private static String determineHromadaType(Entity hromadaEntity) {
        String ukLabel = hromadaEntity.getLabels().get("uk");
        if (ukLabel == null) {
            return null;
        }
        String lower = ukLabel.toLowerCase();
        if (lower.contains("селищна")) {
            return "de așezare";
        } else if (lower.contains("міська")) {
            return "urbană";
        } else if (lower.contains("сільська")) {
            return "rurală";
        }
        return "";
    }

    /**
     * Finds the name to use for a hromada by first checking its P138 ("named after")
     * claim, falling back to its P36 (capital) claim if no usable name is found there.
     */
    private String findHromadaNamingName(Entity hromadaEntity, String regionName) {
        if (hromadaEntity == null) {
            return null;
        }
        Set<Claim> namedAfterClaims = hromadaEntity.getBestClaims(P138);
        if (namedAfterClaims != null) {
            for (Claim claim : namedAfterClaims) {
                if (!(claim.getValue() instanceof Item namedAfterItem)) {
                    continue;
                }
                try {
                    Entity namedAfterEntity = dwiki.getWikibaseItemById(namedAfterItem.getEnt());
                    String resolved = resolveEntityDisplayName(namedAfterEntity, regionName);
                    if (resolved != null) {
                        return resolved;
                    }
                } catch (Exception e) {
                    System.err.println("  Could not fetch P138 (named after) value for hromada "
                            + hromadaEntity.getId() + ": " + e.getMessage());
                }
            }
        }
        return findHromadaCapitalName(hromadaEntity, regionName);
    }

    /**
     * Finds the hromada capital's name by looking up the P36 (capital) claim.
     * Returns the Romanian label of the capital in {@link #SPECIAL_RO_NAME_REGIONS},
     * or a transliterated Ukrainian label otherwise.
     */
    private String findHromadaCapitalName(Entity hromadaEntity, String regionName) {
        if (hromadaEntity == null) {
            return null;
        }
        Set<Claim> p36Claims = hromadaEntity.getBestClaims(P36);
        if (p36Claims == null || p36Claims.isEmpty()) {
            return null;
        }
        for (Claim claim : p36Claims) {
            if (!(claim.getValue() instanceof Item capitalItem)) {
                continue;
            }
            try {
                Entity capitalEntity = dwiki.getWikibaseItemById(capitalItem.getEnt());
                String resolved = resolveEntityDisplayName(capitalEntity, regionName);
                if (resolved != null) {
                    return resolved;
                }
            } catch (Exception e) {
                System.err.println("  Could not fetch hromada capital for "
                        + hromadaEntity.getId() + ": " + e.getMessage());
            }
        }
        return null;
    }

    private static String extractIntro(String pageText) {
        if (pageText == null || pageText.isEmpty()) {
            return "";
        }
        int sectionStart = pageText.indexOf("\n==");
        return sectionStart > 0 ? pageText.substring(0, sectionStart) : pageText;
    }

    /**
     * Returns whatever follows the intro (sections, categories, etc.), or "" if none.
     */
    private static String extractRest(String pageText) {
        if (pageText == null || pageText.isEmpty()) {
            return "";
        }
        int sectionStart = pageText.indexOf("\n==");
        return sectionStart > 0 ? pageText.substring(sectionStart) : "";
    }

    private static String extractMatch(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        return m.find() ? m.group(1) : null;
    }

    /**
     * Walks up the P131 chain to find an ancestor Entity whose ro label contains the given keyword.
     */
    private Entity findAncestorEntityByLabel(Entity entity, String keyword, int maxDepth)
            throws IOException, WikibaseException {
        if (maxDepth <= 0) {
            return null;
        }
        Set<Claim> p131Claims = entity.getBestClaims(P131);
        if (p131Claims == null || p131Claims.isEmpty()) {
            return null;
        }

        for (Claim claim : p131Claims) {
            if (!(claim.getValue() instanceof Item itemValue)) {
                continue;
            }
            Entity adminEntity = dwiki.getWikibaseItemById(itemValue.getEnt());
            if (adminEntity == null) {
                continue;
            }
            String roLabel = adminEntity.getLabels().get("ro");
            if (roLabel != null && roLabel.toLowerCase().contains(keyword)) {
                return adminEntity;
            }
            // recurse up
            Entity found = findAncestorEntityByLabel(adminEntity, keyword, maxDepth - 1);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * Finds the hromada this settlement belongs to by walking the P131 chain
     * and checking P31 (instance of) for Q19356062 or the label for "hromad".
     */
    private Entity findHromada(Entity entity) throws IOException, WikibaseException {
        Set<Claim> p131Claims = entity.getBestClaims(P131);
        if (p131Claims == null || p131Claims.isEmpty()) {
            return null;
        }

        for (Claim claim : p131Claims) {
            if (!(claim.getValue() instanceof Item itemValue)) {
                continue;
            }
            Entity adminEntity = dwiki.getWikibaseItemById(itemValue.getEnt());
            if (adminEntity == null) {
                continue;
            }
            if (isHromada(adminEntity)) {
                return adminEntity;
            }
            // Try one level up (settlement -> village council -> hromada)
            Set<Claim> parentClaims = adminEntity.getBestClaims(P131);
            if (parentClaims != null) {
                for (Claim parentClaim : parentClaims) {
                    if (!(parentClaim.getValue() instanceof Item parentItem)) {
                        continue;
                    }
                    Entity parentEntity = dwiki.getWikibaseItemById(parentItem.getEnt());
                    if (parentEntity != null && isHromada(parentEntity)) {
                        return parentEntity;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Fallback: extracts the hromada from the Ukrainian Wikipedia article infobox.
     * Gets the ukwiki page title from the Wikidata sitelinks, fetches the article,
     * parses the "| громада = [[...]]" field, then looks up its Wikidata entity.
     */
    private Entity findHromadaFromUkWiki(Entity wdEntity) throws IOException {
        Sitelink ukSitelink = wdEntity.getSitelinks().get("ukwiki");
        if (ukSitelink == null) {
            return null;
        }
        String ukTitle = ukSitelink.getPageName();
        List<String> ukTexts = ukWiki.getPageText(Collections.singletonList(ukTitle));
        if (ukTexts == null || ukTexts.isEmpty() || ukTexts.get(0) == null) {
            return null;
        }
        String ukText = ukTexts.get(0);
        Matcher m = UK_INFOBOX_HROMADA_PATTERN.matcher(ukText);
        if (!m.find()) {
            return null;
        }
        String hromadaUkTitle = m.group(1).trim();
        try {
            Entity hromadaEntity = dwiki.getWikibaseItemBySiteAndTitle("ukwiki", hromadaUkTitle);
            if (hromadaEntity != null) {
                return hromadaEntity;
            }
        } catch (WikibaseException e) {
            System.err.println("  Could not look up hromada from ukwiki title '"
                    + hromadaUkTitle + "': " + e.getMessage());
        }
        return null;
    }

    private boolean isHromada(Entity entity) {
        Set<Claim> p31Claims = entity.getBestClaims(P31);
        if (p31Claims != null) {
            for (Claim claim : p31Claims) {
                if (claim.getValue() instanceof Item item
                        && HROMADA_QID.equals(item.getEnt().getId())) {
                    return true;
                }
            }
        }
        String roLabel = entity.getLabels().get("ro");
        if (roLabel != null && roLabel.toLowerCase().contains("hromad")) {
            return true;
        }
        String ukLabel = entity.getLabels().get("uk");
        return ukLabel != null && ukLabel.toLowerCase().contains("громад");
    }

    private boolean isCapitalOf(Entity hromadaEntity, String settlementQid) {
        Set<Claim> p36Claims = hromadaEntity.getBestClaims(P36);
        if (p36Claims == null || settlementQid == null) {
            return false;
        }
        for (Claim claim : p36Claims) {
            if (claim.getValue() instanceof Item item
                    && settlementQid.equals(item.getEnt().getId())) {
                return true;
            }
        }
        return false;
    }

    private void logMemoryUsage(String label) {
        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        long usedMB = heap.getUsed() / (1024 * 1024);
        long maxMB = heap.getMax() / (1024 * 1024);
        System.out.printf("[Memory] %s: %d MB used / %d MB max (%.0f%%)%n",
                label, usedMB, maxMB, (100.0 * heap.getUsed() / heap.getMax()));
        if (usedMB > maxMB * 0.85) {
            System.err.println("[Memory] WARNING: heap usage above 85%");
        }
    }
}
