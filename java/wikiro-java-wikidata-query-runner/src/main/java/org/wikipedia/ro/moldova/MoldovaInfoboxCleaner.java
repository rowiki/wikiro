package org.wikipedia.ro.moldova;

import static org.apache.commons.lang3.StringUtils.substring;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.wikibase.WikibaseException;
import org.wikibase.data.Item;
import org.wikipedia.ro.model.WikiTemplate;
import org.wikipedia.ro.parser.ParseResult;
import org.wikipedia.ro.parser.WikiTemplateParser;
import org.wikipedia.ro.wikidata.WikidataQueryProcessor;

import lombok.extern.slf4j.Slf4j;

/**
 * Visits every settlement of Moldova (Q217) that has a ro.wp article and removes the
 * population/density parameters from its settlement infobox. Runs in dry-run mode unless
 * started with {@code -DdryRun=false}; see {@link WikidataQueryProcessor}.
 */
@Slf4j
public class MoldovaInfoboxCleaner extends WikidataQueryProcessor {

    private static final String EDIT_SUMMARY = "Robot: elimin parametrii de populație și densitate din infocaseta localității";

    private static final Pattern INFOBOX_PATTERN = Pattern.compile("\\{\\{\\s*[Ii]nfocaseta\\s+A[șş]ezare");

    private static final List<String> PARAMS_TO_REMOVE =
            List.of("populație", "populatie", "populația", "populatia", "recensamant", "recensământ", "populație_note_subsol", "densitate", "populație note subsol");

    // Concrete types instead of a transitive P279* closure, which times out on the public WDQS.
    // Q532 village, Q4413925 seat village, Q4229812 commune of Moldova, Q515 city, Q3957 town.
    private static final String MOLDOVA_SETTLEMENTS_SPARQL = """
            SELECT DISTINCT ?item ?roArticleName WHERE {
              VALUES ?settlementType { wd:Q532 wd:Q4413925 wd:Q4229812 wd:Q515 wd:Q3957 }
              ?item wdt:P17 wd:Q217 ;
                    wdt:P31 ?settlementType .
              ?roArticle schema:about ?item ;
                         schema:isPartOf <https://ro.wikipedia.org/> ;
                         schema:name ?roArticleName .
            }
            ORDER BY ?item
            """;

    private int changedCount = 0;
    private int unchangedCount = 0;
    private int noInfoboxCount = 0;

    @Override
    protected String getQuery() {
        return MOLDOVA_SETTLEMENTS_SPARQL;
    }

    @Override
    protected void processResult(Map<String, Object> row) throws IOException, WikibaseException {
        if (!(row.get("item") instanceof Item item)) {
            return;
        }
        String qid = item.getEnt().getId();
        String title = (String) row.get("roArticleName");

        String articleText = wiki.getPageText(List.of(title)).stream().findFirst().orElse("");
        Matcher infoboxMatcher = INFOBOX_PATTERN.matcher(articleText);
        if (!infoboxMatcher.find()) {
            log.debug(qid + "\t" + title + "\tno settlement infobox found");
            noInfoboxCount++;
            return;
        }
        int infoboxLocation = infoboxMatcher.start();

        ParseResult<WikiTemplate> parseResult = new WikiTemplateParser().parse(substring(articleText, infoboxLocation));
        WikiTemplate infobox = parseResult.getIdentifiedPart();

        // Skip pages with none of the parameters, so the infobox isn't needlessly reformatted.
        if (PARAMS_TO_REMOVE.stream().noneMatch(infobox.getParamNames()::contains)) {
            log.debug(qid + "\t" + title + "\tnothing to remove");
            unchangedCount++;
            return;
        }

        PARAMS_TO_REMOVE.forEach(infobox::removeParam);

        String newText = new StringBuilder(articleText)
                .replace(infoboxLocation, infoboxLocation + infobox.getTemplateLength(), infobox.toString())
                .toString();
        if (newText.equals(articleText)) {
            unchangedCount++;
            return;
        }

        log.debug(qid + "\t" + title + "\tremoving population/density parameters");
        savePage(title, articleText, newText, EDIT_SUMMARY);
        changedCount++;
    }

    @Override
    protected void afterProcessing(List<Map<String, Object>> results) {
        log.info("Summary (" + (isDryRun() ? "dry run" : "edits performed") + "):");
        log.info("  Settlements found:       " + results.size());
        log.info("  Changed:                 " + changedCount);
        log.info("  Nothing to remove:       " + unchangedCount);
        log.info("  No settlement infobox:   " + noInfoboxCount);
    }

    public static void main(String[] args) {
        new MoldovaInfoboxCleaner().doExecution();
    }
}
