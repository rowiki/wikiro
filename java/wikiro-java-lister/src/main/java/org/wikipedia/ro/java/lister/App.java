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
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.login.LoginException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.wikibase.Wikibase;
import org.wikibase.WikibaseException;
import org.wikibase.data.Entity;
import org.wikipedia.Wiki;
import org.wikipedia.ro.cache.WikidataEntitiesCache;
import org.wikipedia.ro.java.lister.generators.DefaultListGenerator;
import org.wikipedia.ro.java.lister.generators.FootballTeamListGenerator;
import org.wikipedia.ro.java.lister.generators.SettlementListsGenerator;
import org.wikipedia.ro.java.lister.generators.WikidataListGenerator;
import org.wikipedia.ro.utils.Credentials;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;

/**
 * Hello world!
 *
 */
public class App {
    
    public static Credentials identifyCredentials(String userVarName, String passVarName, String target) {
        Credentials credentials = new Credentials();

        credentials.username =
            defaultString(System.getenv("WIKI_LISTER_USERNAME"), System.getProperty("WIKI_LISTER_USERNAME"));

        Console c = System.console();
        if (isEmpty(credentials.username)) {
            credentials.username = c.readLine(String.format("%s user name: ", target));
            System.setProperty("WIKI_LISTER_USERNAME", credentials.username);
        }

        String password = defaultString(System.getenv("WIKI_LISTER_PASSWORD"), System.getProperty("WIKI_LISTER_PASSWORD"));
        if (isEmpty(password)) {
            c.printf("%s password for user %s: ", target, credentials.username);
            credentials.password = c.readPassword();
            System.setProperty("WIKI_LISTER_PASSWORD", new String(credentials.password));
        } else {
            credentials.password = password.toCharArray();
        }
        return credentials;
    }

    private static Pattern LIST_START_PATTERN =
        Pattern.compile("\\{\\{\\s*(?:[Ff]ormat:)?Listă populată din Wikidata((?:\\|[^=]+=[^\\|]*?)*?)?\\}\\}");
    private static Pattern LIST_END_PATTERN =
        Pattern.compile("\\{\\{\\s*(?:[Ff]ormat:)?Listă populată din Wikidata/sfârșit\\s*\\}\\}");
    private static Pattern UPDATED_TEMPLATE_PATTERN =
        Pattern.compile("\\{\\{[Uu]pdated\\s*\\|\\{\\{[Dd]ată\\s*(\\|[\\d]+)+?\\}{2}\\s*\\}{2}");
    private static Map<String, WikidataListGenerator> LIST_GENERATORS = new HashMap<String, WikidataListGenerator>();
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        Wiki wiki = Wiki.newSession("ro.wikipedia.org");
        Wikibase wikibase = new Wikibase();
        wikibase.setThrottle(70000);
        WikidataEntitiesCache wikidataEntitiesCache = new WikidataEntitiesCache(wikibase);
        LIST_GENERATORS.put("lot-fotbal", new FootballTeamListGenerator(wikidataEntitiesCache));
        LIST_GENERATORS.put("comune-sate", new SettlementListsGenerator(wikidataEntitiesCache));
        LIST_GENERATORS.put("default", new DefaultListGenerator());

        Credentials credentials = identifyCredentials("WIKI_LISTER_USERNAME", "WIKI_LISTER_PASSWORD", "Wikipedia");
        Credentials wdCredentials = identifyCredentials("WIKI_LISTER_WDUSERNAME", "WIKI_LISTER_WDPASSWORD", "Wikidata");

        try {
            wiki.login(credentials.username, credentials.password);
            wikibase.login(wdCredentials.username, wdCredentials.password);

            wiki.setMarkBot(true);
            wikibase.setMarkBot(true);
            
            List<String> listMarkersPresence = wiki.whatTranscludesHere(List.of("Format:Listă populată din Wikidata")).stream().findFirst().orElse(List.of());

            for (String eachTransclusion : listMarkersPresence) {
                LOG.info("Found template on page {}", eachTransclusion);
                String pageText = wiki.getPageText(List.of(eachTransclusion)).stream().findFirst().orElse("");

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
                        if (isEmpty(generatedListContent)) {
                            continue;
                        }

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

                            wiki.setMarkBot(true);
                            wiki.edit(eachTransclusion, newPageText, "Robot: actualizat listă în conformitate cu [[Wikipedia:Wikidata|Wikidata]]");
                        }
                    }
                }
            }

        } catch (IOException | WikibaseException | LoginException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            wiki.logout();
        }
    }
    
    private static void initLogging()
    {
        LoggerContext logbackContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        
        PatternLayoutEncoder ple = new PatternLayoutEncoder();

        ple.setPattern("%date %level %logger{10} [%file:%line] %msg%n");
        ple.setContext(logbackContext);
        ple.start();

        ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
        appender.setContext(logbackContext);
        appender.setName("console");
        appender.setEncoder(ple);
        appender.start();
        
        Logger roWikiLog = logbackContext.getLogger("org.wikipedia.ro");
        roWikiLog.setAdditive(false);
        roWikiLog.setLevel(Level.INFO);
        roWikiLog.addAppender(appender);
        
        Logger wikiLog =  logbackContext.getLogger("wiki");
        wikiLog.setAdditive(false);
        wikiLog.setLevel(Level.WARN);
        wikiLog.addAppender(appender);
        
        logbackContext.setPackagingDataEnabled(true);

        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }
}
