package org.wikipedia.ro.armenia;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.LoginException;

import org.wikibase.WikibaseException;
import org.wikibase.data.Item;
import org.wikipedia.ro.translit.IcuTransliterationService;
import org.wikipedia.ro.utility.AbstractExecutable;

public class ArmeniaSettlementsLister extends AbstractExecutable {

    private static final String TRANSLIT_MODULE_URL =
            "https://ro.wikipedia.org/w/index.php?title=Modul:Transliteration/langdata&action=raw";

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
     */
    private static final String ARMENIA_SETTLEMENTS_SPARQL = """
            SELECT DISTINCT ?item ?hyLabel ?roLabel ?roArticleName WHERE {
              VALUES ?settlementType { wd:Q486972 wd:Q515 wd:Q3957 wd:Q532 wd:Q20724701 wd:Q28328984}
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

    @Override
    protected void execute() throws IOException, WikibaseException, LoginException {
        transliterationService = IcuTransliterationService.fromLuaUrl(TRANSLIT_MODULE_URL);

        System.out.println("Querying Wikidata for Armenia settlements...");
        List<Map<String, Object>> results = dwiki.query(ARMENIA_SETTLEMENTS_SPARQL);
        System.out.println("Found " + results.size() + " settlements.");
        System.out.println();

        for (Map<String, Object> row : results) {
            Object itemObj = row.get("item");
            if (!(itemObj instanceof Item item)) {
                continue;
            }

            String qid = item.getEnt().getId();
            String hyLabel = (String) row.get("hyLabel");
            String roLabel = (String) row.get("roLabel");
            String roArticleName = (String) row.get("roArticleName");

            if (roArticleName != null) {
                // A Romanian Wikipedia article already exists for this settlement.
                System.out.println(qid + "\t" + hyLabel + "\texists: " + roArticleName);
                continue;
            }

            // No Romanian Wikipedia article yet -- transliterate the Armenian name.
            String translit = transliterateArmenian(hyLabel);
            String displayName = roLabel != null ? roLabel
                    : (translit != null ? translit : hyLabel);

            System.out.println(qid + "\t" + hyLabel + "\tnew: " + displayName);
        }
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
}
