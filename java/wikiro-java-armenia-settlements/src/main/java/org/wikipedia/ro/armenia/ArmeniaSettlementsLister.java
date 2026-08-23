package org.wikipedia.ro.armenia;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.security.auth.login.LoginException;

import org.wikibase.WikibaseException;
import org.wikibase.data.Claim;
import org.wikibase.data.Entity;
import org.wikibase.data.Item;
import org.wikibase.data.Property;
import org.wikibase.data.Sitelink;
import org.wikipedia.ro.translit.IcuTransliterationService;
import org.wikipedia.ro.utility.AbstractExecutable;
import org.wikipedia.ro.utils.LinkUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ArmeniaSettlementsLister extends AbstractExecutable {

    private static final String TRANSLIT_MODULE_URL =
            "https://ro.wikipedia.org/w/index.php?title=Modul:Transliteration/langdata&action=raw";

    private static final String NEW_VILLAGE_TEMPLATE_RESOURCE = "/new_village.template";
    private static final String NEW_COMMUNE_TEMPLATE_RESOURCE = "/new_commune.template";
    private static final String NEW_PROVINCE_TEMPLATE_RESOURCE = "/new_province.template";

    private static final String TOWN_QID = "Q20724701";
    private static final String VILLAGE_QID = "Q28328984";

    private static final String MUNICIPALITY_TYPE_QID = "Q3685430";
    private static final String PROVINCE_TYPE_QID = "Q514860";

    private static final Property P131 = new Property("P131");
    private static final Property P31 = new Property("P31");
    private static final Property P582 = new Property("P582");
    private static final Property P36 = new Property("P36");
    private static final Property P138 = new Property("P138");

    // Transliterated Armenian province labels are often suffixed with the word for
    // "province" itself (մարզ -> "marz"), e.g. "Aragacotni marz". That word is
    // redundant in the generated Romanian text, so it is stripped out.
    private static final Pattern PROVINCE_SUFFIX_PATTERN = Pattern.compile("(?i)\\bmarz\\b\\.?");

    // A P138 ("named after") value's own Romanian label sometimes already reads
    // "Provincia Foo" -- that prefix is redundant since the generated title/text
    // prepends its own "Provincia ", so it is stripped out.
    private static final Pattern PROVINCE_PREFIX_PATTERN = Pattern.compile("(?i)^\\s*provincia\\s+");

    // Similarly, transliterated Armenian commune labels sometimes literally include the
    // word for "community"/"commune" itself (համայնք -> "hamaink"), e.g. "Foo hamaink"
    // or "Foo (hamaink)". That word is redundant in the generated Romanian text (which
    // already says "comuna Foo"), so it -- and any enclosing brackets -- are stripped out.
    private static final Pattern COMMUNE_HAMAINK_PATTERN =
            Pattern.compile("(?i)\\(\\s*hamaink\\s*\\)|\\bhamaink\\b\\.?");

    private static final String ROWIKI_SITE_ID = "rowiki";
    private static final String EDIT_SUMMARY = "Articol nou despre o localitate din Armenia";
    private static final String NEW_COMMUNE_SUMMARY = "Articol nou despre o comună din Armenia";
    private static final String RENAME_SUMMARY = "Redenumit conform [[WP:TLOC]] și [[Wikipedia:Transliterare]]";

    /**
     * SPARQL query to retrieve towns and villages of Armenia from Wikidata,
     * together with their Romanian Wikipedia article title, if one already exists.
     *
     * Q399    = Armenia
     * P17     = country
     * P31     = instance of
     * Q486972 = human settlement
     * Q515    = city
     * Q3957   = town
     * Q532    = village
     *
     * NOTE: We intentionally use a fixed VALUES list of concrete settlement types
     * instead of the transitive path "wdt:P31/wdt:P279* wd:Q486972". The transitive
     * subclass-of closure of "human settlement" spans many thousands of items across
     * every country, and the property-path traversal it requires reliably times out
     * on the public Wikidata Query Service. Restricting to the handful of concrete
     * types actually used for Armenian settlements keeps the query well under a second.
     *
     * Labels are fetched directly via rdfs:label to avoid the SERVICE wikibase:label
     * fan-out and to get both the Armenian (hy) and Romanian (ro) labels as plain bindings.
     * Rather than excluding items that already have a rowiki article (FILTER NOT EXISTS),
     * the existing article title is fetched via an OPTIONAL, so the caller can decide what
     * to do with settlements that already have an article.
     *
     * NOTE: The administrative parent chain (P131) is deliberately NOT fetched here.
     * Many P131 statements on Armenian settlement items are inconsistently ranked, so
     * relying on SPARQL (which would need to pick a "best" value, or would otherwise
     * return duplicate rows for the same item) is unreliable. Instead, P131 is resolved
     * per-settlement via direct Wikibase API calls in {@link #findAdminParent}, which
     * filters out statements with a P582 ("end time") qualifier and checks the P31 type
     * of each candidate value.
     */
    private static final String ARMENIA_SETTLEMENTS_SPARQL = """
            SELECT DISTINCT ?item ?settlementType ?hyLabel ?roLabel ?roArticleName WHERE {
              VALUES ?settlementType { wd:Q20724701 wd:Q28328984}
              ?item wdt:P17 wd:Q399 ;
                    wdt:P31 ?settlementType .
              OPTIONAL { ?item rdfs:label ?hyLabel . FILTER(LANG(?hyLabel) = "hy") }
              OPTIONAL { ?item rdfs:label ?roLabel . FILTER(LANG(?roLabel) = "ro") }
              OPTIONAL {
                ?roArticle schema:about ?item ;
                           schema:isPartOf <https://ro.wikipedia.org/> ;
                           schema:name ?roArticleName .
              }
            }
            ORDER BY ?item
            """;

    /**
     * SPARQL query to retrieve provinces (Q514860) of Armenia, together with their
     * Romanian Wikipedia article title, if one already exists.
     */
    private static final String ARMENIA_PROVINCES_SPARQL = """
            SELECT DISTINCT ?item ?hyLabel ?roLabel ?roArticleName WHERE {
              ?item wdt:P17 wd:Q399 ;
                    wdt:P31 wd:Q514860 .
              OPTIONAL { ?item rdfs:label ?hyLabel . FILTER(LANG(?hyLabel) = "hy") }
              OPTIONAL { ?item rdfs:label ?roLabel . FILTER(LANG(?roLabel) = "ro") }
              OPTIONAL {
                ?roArticle schema:about ?item ;
                           schema:isPartOf <https://ro.wikipedia.org/> ;
                           schema:name ?roArticleName .
              }
            }
            ORDER BY ?item
            """;

    private IcuTransliterationService transliterationService;
    private String newVillageTemplate;
    private String newCommuneTemplate;
    private String newProvinceTemplate;

    // Caches the resolved display name (see #resolveProvinceDisplayName) for each
    // province QID, since the same province is looked up repeatedly -- once per
    // village/town located in it, plus once for the province's own article -- and its
    // resolution (P138 "named after" lookups included) requires extra Wikibase calls.
    private final Map<String, String> provinceDisplayNameCache = new HashMap<>();

    @Override
    protected void execute() throws IOException, WikibaseException, LoginException {
        transliterationService = IcuTransliterationService.fromLuaUrl(TRANSLIT_MODULE_URL);
        newVillageTemplate = loadTemplate(NEW_VILLAGE_TEMPLATE_RESOURCE);
        newCommuneTemplate = loadTemplate(NEW_COMMUNE_TEMPLATE_RESOURCE);
        newProvinceTemplate = loadTemplate(NEW_PROVINCE_TEMPLATE_RESOURCE);

        log.debug("Querying Wikidata for Armenia settlements...");
        List<Map<String, Object>> results = dwiki.query(ARMENIA_SETTLEMENTS_SPARQL);
        log.debug("Found " + results.size() + " settlements.");
        log.debug("");

        int existingCount = 0;
        int renamedCount = 0;
        int noLabelCount = 0;
        int skippedGenerationCount = 0;
        int toCreateCount = 0;

        // Settlements (villages/towns) grouped by the commune (municipality) they belong
        // to, used afterwards to build each commune's village enumeration. This is built
        // for ALL settlements, whether or not they already have their own ro.wp article,
        // since a commune's enumeration needs to list all of its member settlements.
        Map<String, List<SettlementRef>> communeMembers = new LinkedHashMap<>();

        // Settlements awaiting a (re-)computed title, collected here instead of being
        // finalized immediately, so that (a) duplicate (name, province) pairs can be
        // detected across ALL of them, and (b) settlements that already have an article
        // can have their existing title compared against the computed one, and renamed
        // if they differ -- see the disambiguation/rename pass below.
        List<PendingSettlement> pendingSettlements = new ArrayList<>();

        for (Map<String, Object> row : results) {
            Object itemObj = row.get("item");
            if (!(itemObj instanceof Item item)) {
                continue;
            }

            String qid = item.getEnt().getId();
            String hyLabel = (String) row.get("hyLabel");
            String roLabel = (String) row.get("roLabel");
            String roArticleName = (String) row.get("roArticleName");
            String settlementTypeQid = asQid(row.get("settlementType"));

            String translit = transliterateArmenian(hyLabel);
            String displayName = roLabel != null ? roLabel
                    : (translit != null ? translit : hyLabel);

            if (displayName == null) {
                // No Armenian or Romanian label at all -- nothing to name the article after.
                log.debug(qid + "\tskipped: no hy/ro label found");
                noLabelCount++;
                continue;
            }

            AdminParent commune = findAdminParent(qid, MUNICIPALITY_TYPE_QID);

            log.debug(qid + "\t" + hyLabel + "\tresolving: " + displayName
                    + (roArticleName != null ? " (existing article: " + roArticleName + ")" : " (new)"));

            LocationInfo locationInfo = resolveLocationInfo(qid, settlementTypeQid, displayName, commune);
            if (locationInfo == null) {
                skippedGenerationCount++;
                // Location info couldn't be fully resolved -- still register the
                // settlement in its commune's enumeration, using its existing title (if
                // any) or its plain name as a best-effort fallback (no province
                // disambiguation available, and no way to verify/rename an existing title).
                addCommuneMember(communeMembers, commune, qid, displayName, settlementTypeQid,
                        roArticleName != null ? roArticleName : displayName);
                continue;
            }

            pendingSettlements.add(new PendingSettlement(qid, displayName, settlementTypeQid, commune, locationInfo,
                    roArticleName));
        }

        // Villages sharing the same (name, province) pair would otherwise get identical
        // titles -- e.g. two different villages both named "Foo" in the same province.
        // Count occurrences per pair first, so any duplicates can be disambiguated below
        // by including their commune's name in the title too.
        Map<String, Integer> nameProvinceCounts = new HashMap<>();
        for (PendingSettlement pending : pendingSettlements) {
            String key = nameProvinceKey(pending);
            if (key != null) {
                nameProvinceCounts.merge(key, 1, Integer::sum);
            }
        }

        for (PendingSettlement pending : pendingSettlements) {
            String qid = pending.qid();
            String displayName = pending.displayName();
            String settlementTypeQid = pending.settlementTypeQid();
            LocationInfo locationInfo = pending.locationInfo();

            // Villages get a disambiguated title "<VillageName>, <ProvinceName>", since
            // village names are frequently reused across different provinces of Armenia.
            // If that title collides with another village in the same province, the
            // commune name is included too: "<VillageName> (<CommuneName>), <ProvinceName>".
            String articleTitle;
            if (VILLAGE_QID.equals(settlementTypeQid) && locationInfo.provinceName() != null) {
                String key = nameProvinceKey(pending);
                boolean duplicate = key != null && nameProvinceCounts.getOrDefault(key, 0) > 1;
                if (duplicate && pending.commune() != null) {
                    String communeName = transliterateArmenian(pending.commune().hyLabel());
                    articleTitle = displayName + " (" + communeName + "), " + locationInfo.provinceName();
                } else {
                    articleTitle = displayName + ", " + locationInfo.provinceName();
                }
            } else {
                articleTitle = displayName;
            }
            String existingTitle = pending.existingArticleTitle();
            String finalArticleTitle = articleTitle;
            if (existingTitle != null) {
                existingCount++;
                if (existingTitle.equals(articleTitle)) {
                    log.debug(qid + "\texists, title correct: " + articleTitle);
                } else {
                    finalArticleTitle = resolveTitleCollision(qid, articleTitle, existingTitle);
                    log.debug(qid + "\trename needed: \"" + existingTitle + "\" -> \"" + finalArticleTitle + "\"");
                    renameArticleAndRelink(qid, existingTitle, finalArticleTitle);
                    renamedCount++;
                }
                addCommuneMember(communeMembers, pending.commune(), qid, displayName, settlementTypeQid, finalArticleTitle);
                continue;
            }

            finalArticleTitle = resolveTitleCollision(qid, articleTitle, null);
            addCommuneMember(communeMembers, pending.commune(), qid, displayName, settlementTypeQid, finalArticleTitle);

            String articleContent = buildNewVillageArticle(displayName, settlementTypeQid, locationInfo.locationText(),
                    locationInfo.provinceName());
            log.debug("---- article content for " + qid + " ----");
            log.debug(articleContent);
            log.debug("");

            createArticleAndLink(qid, finalArticleTitle, articleContent, EDIT_SUMMARY);
            toCreateCount++;
        }

        log.debug("");
        log.info("Summary:");
        log.info("  Settlements found:               " + results.size());
        log.info("  Already have a ro.wp article:    " + existingCount);
        log.info("  Renamed (title mismatch):        " + renamedCount);
        log.info("  Skipped (no hy/ro label):        " + noLabelCount);
        log.info("  Skipped (missing admin/type data): " + skippedGenerationCount);
        log.info("  Pages to create:                 " + toCreateCount);

        Map<String, AdminParent> provincesEncountered = new LinkedHashMap<>();
        generateCommuneArticles(communeMembers, provincesEncountered);
        generateProvinceArticles(provincesEncountered);
    }

    /**
     * Generates (dry-run only) commune articles for every commune found while grouping
     * settlements in the main loop. Skips communes that already have a ro.wp article, or
     * whose own label/province could not be resolved. Communes whose province was
     * resolved successfully are added to {@code provincesEncountered}, keyed by QID, for
     * the subsequent province-article generation pass.
     *
     * Commune details (labels, existing ro.wp sitelink, P36 capital) are fetched directly
     * via {@link org.wikibase.Wikibase#getWikibaseItemById(String)} rather than SPARQL,
     * for the same reliability reasons as {@link #findAdminParent}.
     */
    private void generateCommuneArticles(Map<String, List<SettlementRef>> communeMembers,
            Map<String, AdminParent> provincesEncountered) throws IOException, WikibaseException {
        log.debug("");
        log.debug("Resolving commune articles...");

        int existingCount = 0;
        int renamedCount = 0;
        int skippedCount = 0;
        int toCreateCount = 0;

        for (Map.Entry<String, List<SettlementRef>> entry : communeMembers.entrySet()) {
            String communeQid = entry.getKey();
            List<SettlementRef> members = entry.getValue();

            Entity communeEntity = dwiki.getWikibaseItemById(communeQid);
            if (communeEntity == null) {
                log.debug(communeQid + "\tskipped commune article generation: commune entity not found");
                skippedCount++;
                continue;
            }

            String communeHyLabel = communeEntity.getLabels().get("hy");
            String communeRoLabel = communeEntity.getLabels().get("ro");
            String communeTranslit = transliterateArmenian(communeHyLabel);
            String communeDisplayName = stripHamaink(communeRoLabel != null ? communeRoLabel
                    : (communeTranslit != null ? communeTranslit : communeHyLabel));

            if (communeDisplayName == null) {
                log.debug(communeQid + "\tskipped commune article generation: no hy/ro label found");
                skippedCount++;
                continue;
            }

            Sitelink communeRoSitelink = communeEntity.getSitelinks().get(ROWIKI_SITE_ID);

            AdminParent province = findAdminParent(communeQid, PROVINCE_TYPE_QID);
            if (province == null || province.hyLabel() == null) {
                if (communeRoSitelink != null) {
                    log.debug(communeQid + "\t" + communeHyLabel + "\tcommune exists: "
                            + communeRoSitelink.getPageName() + " (title not verified: no valid province (P131, type "
                            + PROVINCE_TYPE_QID + ") found)");
                    existingCount++;
                } else {
                    log.debug(communeQid + "\tskipped commune article generation: no valid province (P131, type "
                            + PROVINCE_TYPE_QID + ") found");
                    skippedCount++;
                }
                continue;
            }
            provincesEncountered.putIfAbsent(province.qid(), province);

            String provinceName = resolveProvinceDisplayName(province.qid(), province.hyLabel());
            String articleTitle = "Comuna " + communeDisplayName + ", " + provinceName;

            if (communeRoSitelink != null) {
                existingCount++;
                String existingTitle = communeRoSitelink.getPageName();
                if (existingTitle.equals(articleTitle)) {
                    log.debug(communeQid + "\t" + communeHyLabel + "\tcommune exists, title correct: " + articleTitle);
                } else {
                    String finalArticleTitle = resolveTitleCollision(communeQid, articleTitle, existingTitle);
                    log.debug(communeQid + "\trename needed: \"" + existingTitle + "\" -> \"" + finalArticleTitle + "\"");
                    renameArticleAndRelink(communeQid, existingTitle, finalArticleTitle);
                    renamedCount++;
                }
                continue;
            }

            MemberComposition composition = classifyMembers(members);
            SettlementRef onlyMember = composition.towns().isEmpty() && composition.villages().size() == 1
                    ? composition.villages().get(0)
                    : composition.villages().isEmpty() && composition.towns().size() == 1
                            ? composition.towns().get(0)
                            : null;
            if (onlyMember != null) {
                // The commune consists of a single settlement (village or town) --
                // redirect to that settlement's own article instead of duplicating its
                // content in a separate page.
                String redirectTarget = onlyMember.articleTitle();
                String redirectContent = "#REDIRECT [[" + redirectTarget + "]]";
                String finalArticleTitle = resolveTitleCollision(communeQid, articleTitle, null);

                log.debug("---- commune redirect for " + communeQid + " (\"" + finalArticleTitle
                        + "\" -> \"" + redirectTarget + "\") ----");
                createArticleAndLink(communeQid, finalArticleTitle, redirectContent, NEW_COMMUNE_SUMMARY);
                toCreateCount++;
                continue;
            }

            String capitalQid = findCapitalQid(communeEntity);
            String villageEnumeration = buildVillageEnumeration(members, communeDisplayName, capitalQid);
            String articleContent = buildNewCommuneArticle(communeDisplayName, province.qid(), provinceName, villageEnumeration);
            String finalArticleTitle = resolveTitleCollision(communeQid, articleTitle, null);

            log.debug("---- commune article content for " + communeQid + " (" + finalArticleTitle + ") ----");
            log.debug(articleContent);
            log.debug("");

            createArticleAndLink(communeQid, finalArticleTitle, articleContent, NEW_COMMUNE_SUMMARY);
            toCreateCount++;
        }

        log.debug("");
        log.info("Commune summary:");
        log.info("  Communes found:                  " + communeMembers.size());
        log.info("  Already have a ro.wp article:    " + existingCount);
        log.info("  Renamed (title mismatch):        " + renamedCount);
        log.info("  Skipped (missing label/province): " + skippedCount);
        log.info("  Pages to create:                 " + toCreateCount);
    }

    /**
     * Generates (dry-run only) province articles for every province encountered while
     * resolving communes' provinces. Skips provinces that already have a ro.wp article,
     * or whose own label could not be resolved.
     *
     * Province details (labels, existing ro.wp sitelink) are fetched directly via
     * {@link org.wikibase.Wikibase#getWikibaseItemById(String)} rather than SPARQL, for
     * the same reliability reasons as {@link #findAdminParent}.
     */
    private void generateProvinceArticles(Map<String, AdminParent> provincesEncountered) throws IOException, WikibaseException {
        log.debug("");
        log.debug("Resolving province articles...");

        int existingCount = 0;
        int renamedCount = 0;
        int skippedCount = 0;
        int toCreateCount = 0;

        for (String provinceQid : provincesEncountered.keySet()) {
            Entity provinceEntity = dwiki.getWikibaseItemById(provinceQid);
            if (provinceEntity == null) {
                log.debug(provinceQid + "\tskipped province article generation: province entity not found");
                skippedCount++;
                continue;
            }

            String provinceHyLabel = provinceEntity.getLabels().get("hy");
            String provinceDisplayName = resolveProvinceDisplayName(provinceQid, provinceEntity, provinceHyLabel);

            if (provinceDisplayName == null) {
                log.debug(provinceQid + "\tskipped province article generation: no hy/ro label found");
                skippedCount++;
                continue;
            }

            String articleTitle = "Provincia " + provinceDisplayName;

            Sitelink provinceRoSitelink = provinceEntity.getSitelinks().get(ROWIKI_SITE_ID);
            if (provinceRoSitelink != null) {
                existingCount++;
                String existingTitle = provinceRoSitelink.getPageName();
                if (existingTitle.equals(articleTitle)) {
                    log.debug(provinceQid + "\t" + provinceHyLabel + "\tprovince exists, title correct: " + articleTitle);
                } else {
                    String finalArticleTitle = resolveTitleCollision(provinceQid, articleTitle, existingTitle);
                    log.debug(provinceQid + "\trename needed: \"" + existingTitle + "\" -> \"" + finalArticleTitle + "\"");
                    renameArticleAndRelink(provinceQid, existingTitle, finalArticleTitle);
                    renamedCount++;
                }
                continue;
            }

            String articleContent = buildNewProvinceArticle(provinceDisplayName);
            String finalArticleTitle = resolveTitleCollision(provinceQid, articleTitle, null);

            log.debug("---- province article content for " + provinceQid + " (" + finalArticleTitle + ") ----");
            log.debug(articleContent);
            log.debug("");

            createArticleAndLink(provinceQid, finalArticleTitle, articleContent, EDIT_SUMMARY);
            toCreateCount++;
        }

        log.debug("");
        log.info("Province summary:");
        log.info("  Provinces found:                 " + provincesEncountered.size());
        log.info("  Already have a ro.wp article:    " + existingCount);
        log.info("  Renamed (title mismatch):        " + renamedCount);
        log.info("  Skipped (missing label):         " + skippedCount);
        log.info("  Pages to create:                 " + toCreateCount);
    }

    /**
     * Resolves the name to use for a province's Romanian article title/text, honoring
     * a P138 ("named after") claim if present -- e.g. Armenian provinces are
     * conventionally named in Romanian after their eponym rather than a
     * transliteration of their own native name. For each P138 value (in claim order):
     * <ul>
     * <li>if it has a Romanian ("ro") label, that label is used directly, with any
     * leading "provincia " prefix stripped (see {@link #PROVINCE_PREFIX_PATTERN}),
     * since the caller prepends its own "Provincia ";</li>
     * <li>otherwise, if it has an Armenian ("hy") label, that label is
     * transliterated;</li>
     * <li>otherwise, the next P138 value (if any) is tried.</li>
     * </ul>
     * Falls back to transliterating {@code ownHyLabel} if there is no P138 claim, or
     * none of its values could be resolved to a usable (ro or hy) label.
     */
    private String resolveProvinceNamingName(String provinceQid, Entity provinceEntity, String ownHyLabel)
            throws IOException, WikibaseException {
        Set<Claim> namedAfterClaims = provinceEntity.getClaims(P138);
        if (namedAfterClaims != null) {
            for (Claim claim : namedAfterClaims) {
                if (!(claim.getValue() instanceof Item namedAfterItem)) {
                    continue;
                }
                String namedAfterQid = normalizeQid(namedAfterItem.getEnt().getId());
                Entity namedAfterEntity = dwiki.getWikibaseItemById(namedAfterQid);
                if (namedAfterEntity == null) {
                    log.debug(provinceQid + "\tresolveProvinceNamingName: P138 value " + namedAfterQid + " not found");
                    continue;
                }

                String namedAfterRoLabel = namedAfterEntity.getLabels().get("ro");
                if (namedAfterRoLabel != null) {
                    String strippedRoLabel = stripProvincePrefix(namedAfterRoLabel);
                    log.debug(provinceQid + "\tusing P138 (named after) value " + namedAfterQid
                            + "'s Romanian label \"" + namedAfterRoLabel + "\" (\"" + strippedRoLabel + "\")");
                    return strippedRoLabel;
                }

                String namedAfterHyLabel = namedAfterEntity.getLabels().get("hy");
                if (namedAfterHyLabel != null) {
                    log.debug(provinceQid + "\tusing P138 (named after) value " + namedAfterQid
                            + "'s Armenian label \"" + namedAfterHyLabel + "\" for transliteration");
                    return stripProvinceSuffix(transliterateArmenian(namedAfterHyLabel));
                }

                log.debug(provinceQid + "\tresolveProvinceNamingName: P138 value " + namedAfterQid
                        + " has no Romanian (ro) or Armenian (hy) label");
            }
        }
        return stripProvinceSuffix(transliterateArmenian(ownHyLabel));
    }

    /**
     * Resolves the display name to use for a province wherever it is referenced --
     * its own article title, a village/town's location clause, or a category name --
     * so that all of these stay consistent. Results are cached per province QID (see
     * {@link #provinceDisplayNameCache}), since this is called once per settlement
     * located in the province in addition to once for the province's own article.
     *
     * Mirrors the label-priority logic used when generating the province's own
     * article: a Romanian ("ro") label on the province item itself is used if present
     * (with any redundant leading "provincia " prefix stripped, since callers prepend
     * their own "Provincia "/"provincia "); otherwise {@link #resolveProvinceNamingName}
     * is used, which honors a P138 ("named after") claim before falling back to
     * transliterating the province's own Armenian label.
     *
     * @param provinceEntity the already-fetched province entity, or null if it could
     *      not be fetched, in which case {@code hyLabelFallback} is transliterated
     *      directly as a last resort.
     * @param hyLabelFallback the province's Armenian label, used as a fallback when
     *      {@code provinceEntity} is null and as the input to
     *      {@link #resolveProvinceNamingName}'s own fallback otherwise.
     */
    private String resolveProvinceDisplayName(String provinceQid, Entity provinceEntity, String hyLabelFallback)
            throws IOException, WikibaseException {
        String cached = provinceDisplayNameCache.get(provinceQid);
        if (cached != null) {
            return cached;
        }

        String resolved;
        if (provinceEntity == null) {
            resolved = hyLabelFallback != null ? stripProvinceSuffix(transliterateArmenian(hyLabelFallback)) : null;
        } else {
            String provinceRoLabel = provinceEntity.getLabels().get("ro");
            if (provinceRoLabel != null) {
                resolved = stripProvincePrefix(provinceRoLabel);
            } else {
                String provinceNamingName = resolveProvinceNamingName(provinceQid, provinceEntity, hyLabelFallback);
                resolved = provinceNamingName != null ? provinceNamingName : hyLabelFallback;
            }
        }

        if (resolved != null) {
            provinceDisplayNameCache.put(provinceQid, resolved);
        }
        return resolved;
    }

    /**
     * Same as {@link #resolveProvinceDisplayName(String, Entity, String)}, but fetches
     * the province entity itself, for callers (such as village/town location
     * resolution) that only have the province's QID/Armenian label at hand (see
     * {@link AdminParent}).
     */
    private String resolveProvinceDisplayName(String provinceQid, String hyLabelFallback)
            throws IOException, WikibaseException {
        String cached = provinceDisplayNameCache.get(provinceQid);
        if (cached != null) {
            return cached;
        }
        Entity provinceEntity = dwiki.getWikibaseItemById(provinceQid);
        return resolveProvinceDisplayName(provinceQid, provinceEntity, hyLabelFallback);
    }

    /**
     * Logs what would be done to create the new article on ro.wikipedia and link it
     * back to the Wikidata item {@code qid}, without actually performing either action.
     * The actual creation/linking code is left commented out below so it can be
     * re-enabled once the generated content has been reviewed.
     */
    private void createArticleAndLink(String qid, String articleTitle, String articleContent, String summary) {
        log.debug(qid + "\twould create article \"" + articleTitle + "\" (summary: \""
                + summary + "\") and link it to the Wikidata item via \"" + ROWIKI_SITE_ID + "\" sitelink.");

        // try {
        //     wikiEditWithRetry(articleTitle, articleContent, summary);
        // } catch (TimeoutException e) {
        //     log.debug(qid + "\tfailed to create article \"" + articleTitle + "\": " + e.getMessage());
        //     return;
        // }
        //
        // dwiki.setSitelink(qid, ROWIKI_SITE_ID, articleTitle);
        // log.debug(qid + "\tlinked article \"" + articleTitle + "\" to the Wikidata item.");
    }

    /**
     * Logs what would be done to rename an existing article on ro.wikipedia (because
     * its current title doesn't match the title we compute) and update its Wikidata
     * sitelink to the new title, without actually performing either action. The actual
     * move/relink code is left commented out below, per the same dry-run policy as
     * {@link #createArticleAndLink}.
     */
    private void renameArticleAndRelink(String qid, String oldTitle, String newTitle) {
        log.debug(qid + "\twould rename article \"" + oldTitle + "\" to \"" + newTitle + "\" (summary: \""
                + RENAME_SUMMARY + "\") and update its \"" + ROWIKI_SITE_ID + "\" sitelink accordingly.");

        // try {
        //     wiki.move(oldTitle, newTitle, RENAME_SUMMARY);
        // } catch (IOException | LoginException e) {
        //     log.debug(qid + "\tfailed to rename article \"" + oldTitle + "\" to \"" + newTitle + "\": " + e.getMessage());
        //     return;
        // }
        //
        // dwiki.setSitelink(qid, ROWIKI_SITE_ID, newTitle);
        // log.debug(qid + "\trelinked article \"" + newTitle + "\" to the Wikidata item.");
    }

    /**
     * Checks whether {@code candidateTitle} already exists as a ro.wikipedia page. Any
     * article that legitimately belongs to the current Wikidata item is detected and
     * handled separately, via its own recorded sitelink (see the {@code existingTitle}
     * handling in the callers above) -- so if a page with this exact title exists
     * regardless, it is either an unrelated article, or a redirect that already points
     * to this same item's current article ({@code ownTitle}).
     *
     * In the latter case -- {@code candidateTitle} is a redirect to {@code ownTitle} --
     * there is nothing to disambiguate: renaming/moving over a redirect that already
     * points back to the very article being renamed is safe, so {@code candidateTitle}
     * is returned as-is. Otherwise, ", Armenia" is appended to {@code candidateTitle} to
     * disambiguate the new/renamed title from that unrelated article, instead of
     * overwriting or colliding with it.
     *
     * @param ownTitle the ro.wp title currently linked (via sitelink) to this item, or
     *      null if the item has no recorded sitelink yet (a brand-new article), in which
     *      case any page found at {@code candidateTitle} is necessarily unrelated.
     */
    private String resolveTitleCollision(String qid, String candidateTitle, String ownTitle) throws IOException {
        boolean[] exists = wiki.exists(List.of(candidateTitle));
        if (exists.length > 0 && exists[0]) {
            if (ownTitle != null) {
                String redirectTarget = wiki.resolveRedirects(List.of(candidateTitle)).get(0);
                if (ownTitle.equals(redirectTarget)) {
                    log.debug(qid + "\ttitle \"" + candidateTitle
                            + "\" already exists, but only as a redirect to \"" + ownTitle
                            + "\" -- renaming over it with no qualms");
                    return candidateTitle;
                }
            }
            String disambiguated = candidateTitle + ", Armenia";
            log.debug(qid + "\ttitle \"" + candidateTitle
                    + "\" already exists as an unrelated ro.wp article -- using \"" + disambiguated + "\" instead");
            return disambiguated;
        }
        return candidateTitle;
    }

    /**
     * Extracts the QID string from a query result binding, which is an {@link Item}
     * wrapping an {@link org.wikibase.data.Entity} for any wikidata entity URI, or
     * null if the binding is absent/not an entity reference.
     */
    private static String asQid(Object binding) {
        return binding instanceof Item item ? item.getEnt().getId() : null;
    }

    private String loadTemplate(String resourceName) throws IOException {
        try (InputStream in = getClass().getResourceAsStream(resourceName)) {
            if (in == null) {
                throw new IOException("Template resource not found: " + resourceName);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * A settlement (village or town) as a member of a commune, used to build the
     * commune's village enumeration. {@code articleTitle} is the settlement's actual or
     * intended ro.wp article title (existing sitelink title, computed disambiguated
     * title, or -- as a last resort, if location info couldn't be resolved -- the plain
     * display name), used e.g. as a redirect target for single-village communes.
     */
    private record SettlementRef(String qid, String displayName, String settlementTypeQid, String articleTitle) {
    }

    /**
     * A settlement awaiting a brand-new article, collected during the main loop of
     * {@link #execute()} so that its final title can be decided only after every
     * pending settlement has been seen -- see {@link #nameProvinceKey}.
     */
    private record PendingSettlement(String qid, String displayName, String settlementTypeQid, AdminParent commune,
            LocationInfo locationInfo, String existingArticleTitle) {
    }

    /**
     * Returns a case-insensitive key combining a pending village's display name and
     * province name, used to detect villages that would otherwise end up with the same
     * title (same name, same province, different commune). Returns null for anything
     * that isn't a province-disambiguated village (towns, or villages with no resolved
     * province), since those aren't subject to this collision.
     */
    private static String nameProvinceKey(PendingSettlement pending) {
        if (!VILLAGE_QID.equals(pending.settlementTypeQid()) || pending.locationInfo().provinceName() == null) {
            return null;
        }
        return pending.displayName().trim().toLowerCase() + "\u0000" + pending.locationInfo().provinceName().trim().toLowerCase();
    }

    /**
     * Registers {@code qid} as a member of {@code commune}'s enumeration, if
     * {@code commune} was successfully resolved. No-op otherwise.
     */
    private static void addCommuneMember(Map<String, List<SettlementRef>> communeMembers, AdminParent commune,
            String qid, String displayName, String settlementTypeQid, String articleTitle) {
        if (commune == null) {
            return;
        }
        communeMembers.computeIfAbsent(commune.qid(), k -> new ArrayList<>())
                .add(new SettlementRef(qid, displayName, settlementTypeQid, articleTitle));
    }

    /**
     * The resolved administrative location clause ($3 in the template) together with
     * the (already "marz"-stripped) province name, if it could be resolved. Used by the
     * caller to build a disambiguated article title for villages (towns are not
     * disambiguated by province), and by both villages and towns to build the
     * generated article's category. May be null if the settlement's province could not
     * be resolved (see {@link #resolveLocationInfo}).
     */
    private record LocationInfo(String locationText, String provinceName) {
    }

    /**
     * Resolves the administrative location clause ($3 in the template) for the given
     * settlement, using direct Wikibase API lookups (see {@link #findAdminParent})
     * instead of the SPARQL P131 chain, since many P131 statements on Armenian
     * settlement items are inconsistently ranked.
     *
     * For towns, this is a single {{Ill-wd|QID}} reference to the settlement's P131
     * (administrative parent), with no type filtering beyond excluding statements with
     * a P582 ("end time") qualifier.
     *
     * For villages, this references both the municipality (the settlement's P131,
     * filtered to a value of type {@link #MUNICIPALITY_TYPE_QID}) and the province
     * (the municipality's own P131, filtered to a value of type
     * {@link #PROVINCE_TYPE_QID}), each with an explicit display name obtained by
     * transliterating its Armenian label, e.g.:
     * {{Ill-wd|Q123|3=comuna Foo}}, {{Ill-wd|Q456|3=provincia Bar}}
     *
     * If the settlement's own display name is the same (case-insensitively) as its
     * commune's name, the commune reference's label reads "comuna cu același nume"
     * instead of repeating the name. The same applies to a town whose P131 parent turns
     * out to be of the commune type.
     *
     * Returns null (and logs a warning) if the settlement type is not recognized, or
     * if the administrative parents required to build the location clause could not
     * be resolved.
     *
     * @param displayName the settlement's own display name, used only to detect the
     *      "same name as its commune" case described above.
     * @param precomputedMunicipality the settlement's commune, already resolved by the
     *      caller (via {@link #findAdminParent}) while grouping settlements by commune;
     *      reused here for villages to avoid a duplicate API lookup. Ignored for towns,
     *      whose own location clause uses the settlement's unfiltered P131 parent.
     */
    private LocationInfo resolveLocationInfo(String qid, String settlementTypeQid, String displayName,
            AdminParent precomputedMunicipality) throws IOException, WikibaseException {
        if (VILLAGE_QID.equals(settlementTypeQid)) {
            AdminParent municipality = precomputedMunicipality;
            if (municipality == null) {
                log.debug(qid + "\tskipped article generation: no valid municipality (P131, type "
                        + MUNICIPALITY_TYPE_QID + ") found (see reasons above)");
                return null;
            }
            if (municipality.hyLabel() == null) {
                log.debug(qid + "\tskipped article generation: municipality " + municipality.qid()
                        + " was found but has no Armenian (hy) label");
                return null;
            }
            AdminParent province = findAdminParent(municipality.qid(), PROVINCE_TYPE_QID);
            if (province == null) {
                log.debug(qid + "\tskipped article generation: no valid province (P131, type "
                        + PROVINCE_TYPE_QID + ") found for municipality " + municipality.qid()
                        + " (see reasons above)");
                return null;
            }
            if (province.hyLabel() == null) {
                log.debug(qid + "\tskipped article generation: province " + province.qid()
                        + " (for municipality " + municipality.qid() + ") was found but has no Armenian (hy) label");
                return null;
            }
            String provinceName = resolveProvinceDisplayName(province.qid(), province.hyLabel());
            String locationText = formatCommuneReference(municipality, displayName) + ", "
                    + "{{Ill-wd|" + province.qid() + "|3=provincia " + provinceName + "}}";
            return new LocationInfo(locationText, provinceName);
        } else if (TOWN_QID.equals(settlementTypeQid)) {
            AdminParent parent = findAdminParent(qid, null);
            if (parent == null) {
                log.debug(qid + "\tskipped article generation: no P131 (administrative parent) found");
                return null;
            }
            Entity parentEntity = dwiki.getWikibaseItemById(parent.qid());
            if (parentEntity != null && parent.hyLabel() != null && hasType(parentEntity, MUNICIPALITY_TYPE_QID)) {
                AdminParent province = findAdminParent(parent.qid(), PROVINCE_TYPE_QID);
                String provinceName = province != null && province.hyLabel() != null
                        ? resolveProvinceDisplayName(province.qid(), province.hyLabel())
                        : null;
                return new LocationInfo(formatCommuneReference(parent, displayName), provinceName);
            }
            String provinceName = parentEntity != null && parent.hyLabel() != null
                    && hasType(parentEntity, PROVINCE_TYPE_QID)
                            ? resolveProvinceDisplayName(parent.qid(), parentEntity, parent.hyLabel())
                            : null;
            return new LocationInfo("{{Ill-wd|" + parent.qid() + "}}", provinceName);
        } else {
            log.debug(qid + "\tskipped article generation: unrecognized settlement type " + settlementTypeQid);
            return null;
        }
    }

    /**
     * Formats an {{Ill-wd|...}} reference to a commune, using "comuna cu același nume"
     * as the display label instead of repeating the commune's name when it is the same
     * (case-insensitively, after trimming) as {@code settlementDisplayName} -- the
     * settlement located in that commune.
     */
    private String formatCommuneReference(AdminParent commune, String settlementDisplayName) {
        String communeName = stripHamaink(transliterateArmenian(commune.hyLabel()));
        String label = communeName != null && settlementDisplayName != null
                && settlementDisplayName.trim().equalsIgnoreCase(communeName.trim())
                ? "comuna cu același nume"
                : "comuna " + communeName;
        return "{{Ill-wd|" + commune.qid() + "|3=" + label + "}}";
    }

    /**
     * A resolved P131 target: its QID and its Armenian ("hy") label.
     */
    private record AdminParent(String qid, String hyLabel) {
    }

    /**
     * Finds the administrative parent of {@code qid} via its P131 statements, fetched
     * directly from the Wikibase API (not SPARQL). Candidates with a P582 ("end time")
     * qualifier are excluded, since that qualifier marks a historical/no-longer-valid
     * value. If {@code expectedTypeQid} is non-null, candidates are further filtered to
     * those whose value has a P31 claim equal to {@code expectedTypeQid}.
     *
     * If more than one candidate remains after filtering, the first one found is used
     * and a warning is logged, since the ambiguity should be investigated manually.
     *
     * If no candidate matches, the reason each P131 candidate was rejected is logged
     * (e.g. excluded via P582, or a P31 type mismatch showing the candidate's actual
     * types), so the underlying data issue can be diagnosed.
     */
    private AdminParent findAdminParent(String qid, String expectedTypeQid) throws IOException, WikibaseException {
        Entity entity = dwiki.getWikibaseItemById(qid);
        if (entity == null) {
            log.debug(qid + "\tfindAdminParent: entity not found");
            return null;
        }
        Set<Claim> claims = entity.getClaims(P131);
        if (claims == null || claims.isEmpty()) {
            log.debug(qid + "\tfindAdminParent: no P131 claims found");
            return null;
        }

        AdminParent match = null;
        int matchCount = 0;
        List<String> rejectionReasons = new ArrayList<>();
        for (Claim claim : claims) {
            if (claim.getQualifiers().containsKey(P582)) {
                Object value = claim.getValue();
                String candidateQid = value instanceof Item item ? normalizeQid(item.getEnt().getId()) : String.valueOf(value);
                rejectionReasons.add(candidateQid + ": has a P582 (end time) qualifier -- treated as historical");
                continue; // has an end-time qualifier -- no longer the current value
            }
            if (!(claim.getValue() instanceof Item candidateItem)) {
                rejectionReasons.add("P131 value is not an item reference: " + claim.getValue());
                continue;
            }

            String candidateQid = normalizeQid(candidateItem.getEnt().getId());
            Entity candidateEntity = dwiki.getWikibaseItemById(candidateQid);
            if (candidateEntity == null) {
                rejectionReasons.add(candidateQid + ": could not fetch entity");
                continue;
            }
            if (expectedTypeQid != null && !hasType(candidateEntity, expectedTypeQid)) {
                rejectionReasons.add(candidateQid + ": P31 type(s) [" + describeTypes(candidateEntity)
                        + "] do not include expected type " + expectedTypeQid);
                continue;
            }

            matchCount++;
            if (match == null) {
                match = new AdminParent(candidateQid, candidateEntity.getLabels().get("hy"));
            }
        }

        if (match == null) {
            log.debug(qid + "\tfindAdminParent: no valid P131 candidate found (expected type "
                    + expectedTypeQid + "):");
            for (String reason : rejectionReasons) {
                log.debug(qid + "\t  - " + reason);
            }
        } else if (matchCount > 1) {
            log.debug(qid + "\tmultiple matching P131 candidates found, using the first one: "
                    + match.qid());
        }
        return match;
    }

    /**
     * Checks whether {@code entity} has a P31 claim whose value is {@code expectedTypeQid}.
     */
    private static boolean hasType(Entity entity, String expectedTypeQid) {
        Set<Claim> typeClaims = entity.getClaims(P31);
        if (typeClaims == null) {
            return false;
        }
        for (Claim claim : typeClaims) {
            if (claim.getValue() instanceof Item item && expectedTypeQid.equals(normalizeQid(item.getEnt().getId()))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a human-readable, comma-separated list of the QIDs of {@code entity}'s
     * P31 (instance of) values, or "none" if it has no P31 claims.
     */
    private static String describeTypes(Entity entity) {
        Set<Claim> typeClaims = entity.getClaims(P31);
        if (typeClaims == null || typeClaims.isEmpty()) {
            return "none";
        }
        StringBuilder sb = new StringBuilder();
        for (Claim claim : typeClaims) {
            if (claim.getValue() instanceof Item item) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(normalizeQid(item.getEnt().getId()));
            }
        }
        return sb.length() > 0 ? sb.toString() : "none";
    }

    /**
     * Works around a quirk of the underlying Wikibase API client: item-valued claims
     * parsed from the API's XML response (e.g. P131/P31 targets) carry a bare numeric
     * ID (e.g. "3685430") instead of the full QID ("Q3685430"). This normalizes such
     * an ID to its full "Q..." form, leaving already-prefixed IDs untouched.
     */
    private static String normalizeQid(String id) {
        if (id == null || id.isEmpty() || Character.toLowerCase(id.charAt(0)) == 'q') {
            return id;
        }
        return "Q" + id;
    }

    /**
     * Returns the QID of the first P36 (capital/seat) claim value of {@code entity}, or
     * null if it has none.
     */
    private static String findCapitalQid(Entity entity) {
        Set<Claim> capitalClaims = entity.getClaims(P36);
        if (capitalClaims == null) {
            return null;
        }
        for (Claim claim : capitalClaims) {
            if (claim.getValue() instanceof Item item) {
                return normalizeQid(item.getEnt().getId());
            }
        }
        return null;
    }

    /**
     * Builds the wikitext for a new settlement article from {@code new_village.template},
     * substituting:
     * $1 - the Romanian name of the settlement
     * $2 - "sat" or "oraș", depending on the settlement's P31 value
     * $3 - the administrative location clause, as resolved by {@link #resolveLocationInfo}
     * $4 - the category ("Sate în provincia &lt;ProvinceName&gt;" or "Orașe în provincia
     * &lt;ProvinceName&gt;"), falling back to "Sate în Armenia"/"Orașe în Armenia" if
     * {@code provinceName} could not be resolved
     */
    private String buildNewVillageArticle(String roName, String settlementTypeQid, String locationText,
            String provinceName) {
        String typeLabel = VILLAGE_QID.equals(settlementTypeQid) ? "sat" : "oraș";
        String typeCategoryLabel = VILLAGE_QID.equals(settlementTypeQid) ? "Sate" : "Orașe";
        String category = provinceName != null ? typeCategoryLabel + " în provincia " + provinceName
                : typeCategoryLabel + " în Armenia";

        return newVillageTemplate
                .replace("$1", roName)
                .replace("$2", typeLabel)
                .replace("$3", locationText)
                .replace("$4", category);
    }

    /**
     * Builds the wikitext for a new commune article from {@code new_commune.template},
     * substituting:
     * $1 - the Romanian name of the commune
     * $2 - the province clause, {{Ill-wd|<provinceQid>|3=<provinceName>}}
     * $3 - the village enumeration, as built by {@link #buildVillageEnumeration}
     * $4 - the category ("Comune în Armenia")
     */
    private String buildNewCommuneArticle(String communeName, String provinceQid, String provinceName, String villageEnumeration) {
        return newCommuneTemplate
                .replace("$1", communeName)
                .replace("$2", "{{Ill-wd|" + provinceQid + "|3=provincia " + provinceName + "}}")
                .replace("$3", villageEnumeration)
                .replace("$4", "Comune în Armenia");
    }

    /**
     * Builds the wikitext for a new province article from {@code new_province.template},
     * substituting:
     * $1 - the Romanian name of the province
     * $2 - the category ("Provincii în Armenia")
     */
    private String buildNewProvinceArticle(String provinceName) {
        return newProvinceTemplate
                .replace("$1", provinceName)
                .replace("$2", "Provincii în Armenia");
    }

    /**
     * Splits {@code members} into villages and towns, per their settlement type QID.
     */
    private static MemberComposition classifyMembers(List<SettlementRef> members) {
        List<SettlementRef> villages = new ArrayList<>();
        List<SettlementRef> towns = new ArrayList<>();
        for (SettlementRef ref : members) {
            if (VILLAGE_QID.equals(ref.settlementTypeQid())) {
                villages.add(ref);
            } else if (TOWN_QID.equals(ref.settlementTypeQid())) {
                towns.add(ref);
            }
        }
        return new MemberComposition(villages, towns);
    }

    /**
     * The villages and towns among a commune's members, as split out by
     * {@link #classifyMembers}.
     */
    private record MemberComposition(List<SettlementRef> villages, List<SettlementRef> towns) {
    }

    /**
     * Builds the "formată din ..." village/town enumeration clause for a commune's
     * article, following these rules:
     * <ul>
     * <li>Only one village, with the same name as the commune: "satul de reședință cu
     * același nume"</li>
     * <li>Only one village, with a different name: "satul &lt;Village&gt;"</li>
     * <li>Only one town (no villages): "orașul &lt;Town&gt;"</li>
     * <li>One town and one village: "orașul &lt;Town&gt; și satul &lt;Village&gt;"</li>
     * <li>One town and several villages: "orașul &lt;Town&gt; și satele
     * &lt;Village1&gt;, ..., &lt;VillageN&gt;"</li>
     * <li>Only villages (2 or more): "satele &lt;Village1&gt;, ..., &lt;VillageN&gt;"</li>
     * <li>Several towns (2 or more), no villages: "orașele &lt;Town1&gt;, ...,
     * &lt;TownN&gt;"</li>
     * <li>Several towns and one village: "orașele &lt;Town1&gt;, ..., &lt;TownN&gt; și
     * satul &lt;Village&gt;"</li>
     * <li>Several towns and several villages: "orașele &lt;Town1&gt;, ...,
     * &lt;TownN&gt; și satele &lt;Village1&gt;, ..., &lt;VillageN&gt;"</li>
     * </ul>
     * Whichever member matches {@code capitalQid} (the commune's P36 capital/seat) is
     * suffixed with " (reședința)". The only combination not covered above (no
     * settlements resolved at all) falls back to a generic listing and is logged so it
     * can be reviewed manually.
     */
    private String buildVillageEnumeration(List<SettlementRef> members, String communeDisplayName, String capitalQid) {
        MemberComposition composition = classifyMembers(members);
        List<SettlementRef> villages = composition.villages();
        List<SettlementRef> towns = composition.towns();

        if (towns.isEmpty() && villages.isEmpty()) {
            // Not covered by the rules above (no settlements resolved at all) -- flag it
            // for manual review.
            log.debug("(commune enumeration) no towns or villages resolved for this commune");
            return "(nicio localitate identificată)";
        }

        String townsClause = towns.isEmpty() ? null : enumerationClause(towns, "orașul ", "orașele ", capitalQid);

        String villagesClause;
        if (villages.isEmpty()) {
            villagesClause = null;
        } else if (towns.isEmpty() && villages.size() == 1
                && villages.get(0).displayName().trim().equalsIgnoreCase(communeDisplayName.trim())) {
            villagesClause = "satul de reședință cu același nume";
        } else {
            villagesClause = enumerationClause(villages, "satul ", "satele ", capitalQid);
        }

        if (townsClause == null) {
            return villagesClause;
        }
        if (villagesClause == null) {
            return townsClause;
        }
        return townsClause + " și " + villagesClause;
    }

    /**
     * Builds an enumeration clause for a non-empty list of same-kind members (all
     * towns, or all villages), e.g. "orașul Foo" or "satele Foo, Bar și Baz", using
     * {@code singularPrefix} for a single member and {@code pluralPrefix} for several.
     */
    private static String enumerationClause(List<SettlementRef> refs, String singularPrefix, String pluralPrefix,
            String capitalQid) {
        String prefix = refs.size() == 1 ? singularPrefix : pluralPrefix;
        return prefix + joinWithConjunction(refs, capitalQid);
    }

    /**
     * Formats a single enumeration member as a wikilink to its article, appending
     * " (reședința)" if it is the commune's capital/seat.
     */
    private static String formatMember(SettlementRef ref, String capitalQid) {
        String link = LinkUtils.createLink(ref.articleTitle(), ref.displayName());
        return ref.qid().equals(capitalQid) ? link + " (reședința)" : link;
    }

    /**
     * Joins the display names of {@code refs} into a natural Romanian list, e.g.
     * "A, B și C".
     */
    private static String joinWithConjunction(List<SettlementRef> refs, String capitalQid) {
        List<String> names = new ArrayList<>();
        for (SettlementRef ref : refs) {
            names.add(formatMember(ref, capitalQid));
        }
        if (names.size() == 1) {
            return names.get(0);
        }
        return String.join(", ", names.subList(0, names.size() - 1)) + " și " + names.get(names.size() - 1);
    }

    private String transliterateArmenian(String name) {
        if (name == null) {
            return null;
        }
        if (transliterationService.isTransliterationSupported("hy")) {
            return transliterationService.transliterate(name, "hy");
        }
        // Fallback: return original name if Armenian transliteration is not loaded
        return name;
    }

    /**
     * Removes the (transliterated) word "marz" -- Armenian for "province" -- from a
     * province name, e.g. "Aragacotni marz" becomes "Aragacotni".
     */
    private static String stripProvinceSuffix(String name) {
        if (name == null) {
            return null;
        }
        return PROVINCE_SUFFIX_PATTERN.matcher(name).replaceAll("").replaceAll("\\s+", " ").trim();
    }

    /**
     * Removes a leading "provincia " prefix from a name, e.g. "Provincia Foo" becomes
     * "Foo", since the caller prepends its own "Provincia " when building the title/text.
     */
    private static String stripProvincePrefix(String name) {
        if (name == null) {
            return null;
        }
        return PROVINCE_PREFIX_PATTERN.matcher(name).replaceFirst("").trim();
    }

    /**
     * Removes the (transliterated) word "hamaink" -- Armenian for "community"/"commune"
     * -- from a commune name, along with any enclosing brackets, e.g. "Foo (hamaink)" or
     * "Foo hamaink" both become "Foo".
     */
    private static String stripHamaink(String name) {
        if (name == null) {
            return null;
        }
        return COMMUNE_HAMAINK_PATTERN.matcher(name).replaceAll("").replaceAll("\\s+", " ").trim();
    }
}
