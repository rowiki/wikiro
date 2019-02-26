package org.wikipedia.ro.java.lister;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.substring;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.apache.commons.lang3.StringUtils.trim;

import java.io.Console;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.login.LoginException;

import org.apache.commons.lang3.StringUtils;
import org.wikibase.Wikibase;
import org.wikibase.WikibaseException;
import org.wikibase.data.Entity;
import org.wikipedia.Wiki;
import org.wikipedia.ro.java.lister.generators.DefaultListGenerator;
import org.wikipedia.ro.java.lister.generators.FootballTeamListGenerator;
import org.wikipedia.ro.java.lister.generators.WikidataListGenerator;
import org.wikipedia.ro.java.lister.util.WikidataEntitiesCache;
import org.wikipedia.ro.utils.Credentials;

/**
 * Hello world!
 *
 */
public class App {

    public static Credentials identifyCredentials() {
        Credentials credentials = new Credentials();

        credentials.username =
            defaultString(System.getenv("WIKI_LISTER_USERNAME"), System.getProperty("WIKI_LISTER_USERNAME"));

        Console c = System.console();
        if (isEmpty(credentials.username)) {
            credentials.username = c.readLine("User name: ");
            System.setProperty("WIKI_LISTER_USERNAME", credentials.username);
        }

        String password = defaultString(System.getenv("WIKI_LISTER_PASSWORD"), System.getProperty("WIKI_LISTER_PASSWORD"));
        if (isEmpty(password)) {
            c.printf("Password for user %s: ", credentials.username);
            credentials.password = c.readPassword();
            System.setProperty("WIKI_LISTER_PASSWORD", new String(credentials.password));
        } else {
            credentials.password = password.toCharArray();
        }
        return credentials;
    }

    private static Pattern LIST_START_PATTERN =
        Pattern.compile("\\{\\{\\s*[Uu]tilizator:Andrebot/Listă de la Wikidata((?:\\|[^=]+=[^\\|]*?)*?)?\\}\\}");
    private static Pattern LIST_END_PATTERN =
        Pattern.compile("\\{\\{\\s*[Uu]tilizator:Andrebot/Listă de la Wikidata/sfârșit\\s*\\}\\}");
    private static Pattern UPDATED_TEMPLATE_PATTERN =
        Pattern.compile("\\{\\{[Uu]pdated\\s*\\|\\{\\{[Dd]ată\\s*(\\|[\\d]+)+?\\}{2}\\s*\\}{2}");
    private static Map<String, WikidataListGenerator> LIST_GENERATORS = new HashMap<String, WikidataListGenerator>();

    public static void main(String[] args) {
        Wiki wiki = Wiki.createInstance("ro.wikipedia.org");
        Wikibase wikibase = new Wikibase();
        WikidataEntitiesCache wikidataEntitiesCache = new WikidataEntitiesCache(wikibase);
        LIST_GENERATORS.put("lot-fotbal", new FootballTeamListGenerator(wikidataEntitiesCache));
        LIST_GENERATORS.put("default", new DefaultListGenerator());

        Credentials credentials = identifyCredentials();

        try {
            wiki.login(credentials.username, credentials.password);
            
            wiki.setMarkBot(true);

            String[] listMarkersPresence = wiki.whatTranscludesHere("Utilizator:Andrebot/Listă de la Wikidata");

            for (String eachTransclusion : listMarkersPresence) {
                String pageText = wiki.getPageText(eachTransclusion);

                Matcher listStartMatcher = LIST_START_PATTERN.matcher(pageText);
                if (listStartMatcher.find()) {
                    String qId = null;
                    String predefinedType = "default";
                    int insertPosition = 1 + listStartMatcher.end();

                    // --- parse params
                    String paramString = listStartMatcher.group(1);
                    String[] paramGroups = paramString.split("\\|");
                    for (String eachParamGroup : paramGroups) {
                        String paramName = trim(substringBefore(eachParamGroup, "="));
                        String paramValue = trim(substringAfter(eachParamGroup, "="));

                        switch (paramName) {
                        case "qid":
                            qId = paramValue;
                        case "tip_predefinit":
                            predefinedType = paramValue;
                        }
                    }

                    Matcher listEndMatcher = LIST_END_PATTERN.matcher(pageText);
                    if (listEndMatcher.find()) {
                        int endReplacePosition = listEndMatcher.start();

                        Entity wdEntity = null;
                        if (null == qId) {
                            wdEntity = wikibase.getWikibaseItemBySiteAndTitle("rowiki", eachTransclusion);
                        } else {
                            wdEntity = wikibase.getWikibaseItemById(qId);
                        }
                        if (null == wdEntity) {
                            continue;
                        }

                        WikidataListGenerator listGen = LIST_GENERATORS.get(predefinedType);
                        String generatedListContent = listGen.generateListContent(wdEntity);

                        String oldListContent = substring(pageText, insertPosition, endReplacePosition);
                        String newPageText = substring(pageText, 0, insertPosition) + generatedListContent
                            + substring(pageText, endReplacePosition);

                        Matcher oldListUpdatedMatcher = UPDATED_TEMPLATE_PATTERN.matcher(oldListContent);
                        StringBuffer oldListUpdatedBuilder = new StringBuffer();
                        while (oldListUpdatedMatcher.find()) {
                            oldListUpdatedMatcher.appendReplacement(oldListUpdatedBuilder, "");
                        }
                        oldListUpdatedMatcher.appendTail(oldListUpdatedBuilder);

                        Matcher newListUpdatedMatcher = UPDATED_TEMPLATE_PATTERN.matcher(generatedListContent);
                        StringBuffer newListUpdatedBuilder = new StringBuffer();
                        while (newListUpdatedMatcher.find()) {
                            newListUpdatedMatcher.appendReplacement(newListUpdatedBuilder, "");
                        }
                        newListUpdatedMatcher.appendTail(newListUpdatedBuilder);

                        if (!StringUtils.equals(trim(newListUpdatedBuilder.toString()),
                            trim(oldListUpdatedBuilder.toString()))) {

                            wiki.edit(eachTransclusion, newPageText, "Robot: actualizat listă");
                        }
                    }
                }
            }

            wiki.setMarkBot(true);
        } catch (IOException | WikibaseException | LoginException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            wiki.logout();
        }
    }
}
