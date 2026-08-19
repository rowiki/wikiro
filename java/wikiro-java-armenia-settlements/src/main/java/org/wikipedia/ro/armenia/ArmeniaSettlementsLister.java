package org.wikipedia.ro.armenia;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
import org.wikipedia.ro.translit.IcuTransliterationService;
import org.wikipedia.ro.utility.AbstractExecutable;

public class ArmeniaSettlementsLister extends AbstractExecutable {

    private static final String TRANSLIT_MODULE_URL =
            "https://ro.wikipedia.org/w/index.php?title=Modul:Transliteration/langdata&action=raw";

    private static final String NEW_VILLAGE_TEMPLATE_RESOURCE = "/new_village.template";

    private static final String TOWN_QID = "Q20724701";
    private static final String VILLAGE_QID = "Q28328984";

    private static final String MUNICIPALITY_TYPE_QID = "Q3685430";
    private static final String PROVINCE_TYPE_QID = "Q514860";

    private static final Property P131 = new Property("P131");
    private static final Property P31 = new Property("P31");
    private static final Property P582 = new Property("P582");

    // Transliterated Armenian province labels are often suffixed with the word for
    // "province" itself (մարզ -> "marz"), e.g. "Aragacotni marz". That word is
    // redundant in the generated Romanian text, so it is stripped out.
    private static final Pattern PROVINCE_SUFFIX_PATTERN = Pattern.compile("(?i)\\bmarz\\b\\.?");

    private static final String ROWIKI_SITE_ID = "rowiki";
    private static final String EDIT_SUMMARY = "Articol nou despre o localitate din Armenia";

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

    private IcuTransliterationService transliterationService;
    private String newVillageTemplate;

    @Override
    protected void execute() throws IOException, WikibaseException, LoginException {
        transliterationService = IcuTransliterationService.fromLuaUrl(TRANSLIT_MODULE_URL);
        newVillageTemplate = loadNewVillageTemplate();

        System.out.println("Querying Wikidata for Armenia settlements...");
        List<Map<String, Object>> results = dwiki.query(ARMENIA_SETTLEMENTS_SPARQL);
        System.out.println("Found " + results.size() + " settlements.");
        System.out.println();

        int existingCount = 0;
        int noLabelCount = 0;
        int skippedGenerationCount = 0;
        int toCreateCount = 0;

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

            if (roArticleName != null) {
                // A Romanian Wikipedia article already exists for this settlement.
                System.out.println(qid + "\t" + hyLabel + "\texists: " + roArticleName);
                existingCount++;
                continue;
            }

            // No Romanian Wikipedia article yet -- transliterate the Armenian name.
            String translit = transliterateArmenian(hyLabel);
            String displayName = roLabel != null ? roLabel
                    : (translit != null ? translit : hyLabel);

            if (displayName == null) {
                // No Armenian or Romanian label at all -- nothing to name the article after.
                System.out.println(qid + "\tskipped: no hy/ro label found");
                noLabelCount++;
                continue;
            }

            System.out.println(qid + "\t" + hyLabel + "\tnew: " + displayName);

            String locationText = resolveLocationText(qid, settlementTypeQid);
            if (locationText == null) {
                skippedGenerationCount++;
                continue;
            }

            String articleContent = buildNewVillageArticle(displayName, settlementTypeQid, locationText);
            System.out.println("---- article content for " + qid + " ----");
            System.out.println(articleContent);
            System.out.println();

            createArticleAndLink(qid, displayName, articleContent);
            toCreateCount++;
        }

        System.out.println();
        System.out.println("Summary:");
        System.out.println("  Settlements found:               " + results.size());
        System.out.println("  Already have a ro.wp article:    " + existingCount);
        System.out.println("  Skipped (no hy/ro label):        " + noLabelCount);
        System.out.println("  Skipped (missing admin/type data): " + skippedGenerationCount);
        System.out.println("  Pages to create:                 " + toCreateCount);
    }

    /**
     * Logs what would be done to create the new article on ro.wikipedia and link it
     * back to the Wikidata item {@code qid}, without actually performing either action.
     * The actual creation/linking code is left commented out below so it can be
     * re-enabled once the generated content has been reviewed.
     */
    private void createArticleAndLink(String qid, String articleTitle, String articleContent) {
        System.out.println(qid + "\twould create article \"" + articleTitle + "\" (summary: \""
                + EDIT_SUMMARY + "\") and link it to the Wikidata item via \"" + ROWIKI_SITE_ID + "\" sitelink.");

        // try {
        //     wikiEditWithRetry(articleTitle, articleContent, EDIT_SUMMARY);
        // } catch (TimeoutException e) {
        //     System.out.println(qid + "\tfailed to create article \"" + articleTitle + "\": " + e.getMessage());
        //     return;
        // }
        //
        // dwiki.setSitelink(qid, ROWIKI_SITE_ID, articleTitle);
        // System.out.println(qid + "\tlinked article \"" + articleTitle + "\" to the Wikidata item.");
    }

    /**
     * Extracts the QID string from a query result binding, which is an {@link Item}
     * wrapping an {@link org.wikibase.data.Entity} for any wikidata entity URI, or
     * null if the binding is absent/not an entity reference.
     */
    private static String asQid(Object binding) {
        return binding instanceof Item item ? item.getEnt().getId() : null;
    }

    private String loadNewVillageTemplate() throws IOException {
        try (InputStream in = getClass().getResourceAsStream(NEW_VILLAGE_TEMPLATE_RESOURCE)) {
            if (in == null) {
                throw new IOException("Template resource not found: " + NEW_VILLAGE_TEMPLATE_RESOURCE);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
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
     * {{Ill-wd|Q123|3=comuna Foo}}, {{Ill-wd|Q456|3=Bar}}
     *
     * Returns null (and logs a warning) if the settlement type is not recognized, or
     * if the administrative parents required to build the location clause could not
     * be resolved.
     */
    private String resolveLocationText(String qid, String settlementTypeQid) throws IOException, WikibaseException {
        if (VILLAGE_QID.equals(settlementTypeQid)) {
            AdminParent municipality = findAdminParent(qid, MUNICIPALITY_TYPE_QID);
            if (municipality == null) {
                System.out.println(qid + "\tskipped article generation: no valid municipality (P131, type "
                        + MUNICIPALITY_TYPE_QID + ") found (see reasons above)");
                return null;
            }
            if (municipality.hyLabel() == null) {
                System.out.println(qid + "\tskipped article generation: municipality " + municipality.qid()
                        + " was found but has no Armenian (hy) label");
                return null;
            }
            AdminParent province = findAdminParent(municipality.qid(), PROVINCE_TYPE_QID);
            if (province == null) {
                System.out.println(qid + "\tskipped article generation: no valid province (P131, type "
                        + PROVINCE_TYPE_QID + ") found for municipality " + municipality.qid()
                        + " (see reasons above)");
                return null;
            }
            if (province.hyLabel() == null) {
                System.out.println(qid + "\tskipped article generation: province " + province.qid()
                        + " (for municipality " + municipality.qid() + ") was found but has no Armenian (hy) label");
                return null;
            }
            String municipalityName = transliterateArmenian(municipality.hyLabel());
            String provinceName = stripProvinceSuffix(transliterateArmenian(province.hyLabel()));
            return "{{Ill-wd|" + municipality.qid() + "|3=comuna " + municipalityName + "}}, "
                    + "{{Ill-wd|" + province.qid() + "|3=" + provinceName + "}}";
        } else if (TOWN_QID.equals(settlementTypeQid)) {
            AdminParent parent = findAdminParent(qid, null);
            if (parent == null) {
                System.out.println(qid + "\tskipped article generation: no P131 (administrative parent) found");
                return null;
            }
            return "{{Ill-wd|" + parent.qid() + "}}";
        } else {
            System.out.println(qid + "\tskipped article generation: unrecognized settlement type " + settlementTypeQid);
            return null;
        }
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
            System.out.println(qid + "\tfindAdminParent: entity not found");
            return null;
        }
        Set<Claim> claims = entity.getClaims(P131);
        if (claims == null || claims.isEmpty()) {
            System.out.println(qid + "\tfindAdminParent: no P131 claims found");
            return null;
        }

        AdminParent match = null;
        int matchCount = 0;
        List<String> rejectionReasons = new ArrayList<>();
        for (Claim claim : claims) {
            if (claim.getQualifiers().containsKey(P582)) {
                Object value = claim.getValue();
                String candidateQid = value instanceof Item item ? item.getEnt().getId() : String.valueOf(value);
                rejectionReasons.add(candidateQid + ": has a P582 (end time) qualifier -- treated as historical");
                continue; // has an end-time qualifier -- no longer the current value
            }
            if (!(claim.getValue() instanceof Item candidateItem)) {
                rejectionReasons.add("P131 value is not an item reference: " + claim.getValue());
                continue;
            }

            String candidateQid = candidateItem.getEnt().getId();
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
            System.out.println(qid + "\tfindAdminParent: no valid P131 candidate found (expected type "
                    + expectedTypeQid + "):");
            for (String reason : rejectionReasons) {
                System.out.println(qid + "\t  - " + reason);
            }
        } else if (matchCount > 1) {
            System.out.println(qid + "\tmultiple matching P131 candidates found, using the first one: "
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
            if (claim.getValue() instanceof Item item && expectedTypeQid.equals(item.getEnt().getId())) {
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
                sb.append(item.getEnt().getId());
            }
        }
        return sb.length() > 0 ? sb.toString() : "none";
    }

    /**
     * Builds the wikitext for a new settlement article from {@code new_village.template},
     * substituting:
     * $1 - the Romanian name of the settlement
     * $2 - "sat" or "oraș", depending on the settlement's P31 value
     * $3 - the administrative location clause, as resolved by {@link #resolveLocationText}
     * $4 - the category ("Sate în Armenia" or "Orașe în Armenia")
     */
    private String buildNewVillageArticle(String roName, String settlementTypeQid, String locationText) {
        String typeLabel = VILLAGE_QID.equals(settlementTypeQid) ? "sat" : "oraș";
        String category = VILLAGE_QID.equals(settlementTypeQid) ? "Sate în Armenia" : "Orașe în Armenia";

        return newVillageTemplate
                .replace("$1", roName)
                .replace("$2", typeLabel)
                .replace("$3", locationText)
                .replace("$4", category);
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
}
