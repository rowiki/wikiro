package org.wikipedia.ro.java.elections;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.Console;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.security.auth.login.LoginException;

import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.wikibase.Wikibase;
import org.wikibase.WikibaseException;
import org.wikibase.data.Entity;
import org.wikibase.data.Item;
import org.wikibase.data.Sitelink;
import org.wikipedia.Wiki;
import org.wikipedia.ro.cache.WikidataEntitiesCache;
import org.wikipedia.ro.model.WikiPart;
import org.wikipedia.ro.model.WikiTemplate;
import org.wikipedia.ro.parser.AggregatingParser;
import org.wikipedia.ro.parser.ParseResult;
import org.wikipedia.ro.utils.Credentials;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

public class CountyPolitics
{
    private static final String COUNTY_QUERY = "SELECT DISTINCT ?county ?countyLabel ?lpc WHERE {\n" + "  ?county wdt:P31 wd:Q1776764.\n"
        + "  OPTIONAL { ?county wdt:P395 ?lpc. }\n" + "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"ro\". }\n" + "}\n" + "ORDER BY (?countyLabel)";

    private static final Pattern ALREADY_GENERATED_SECTION_PATTERN = Pattern
        .compile("\\s?<!--\\s*secțiune administrație\\s*-->.*?<!--\\s*sfârșit secțiune administrație\\s*-->", Pattern.DOTALL);

    public static Credentials identifyCredentials(String userVarName, String passVarName, String target)
    {
        Credentials credentials = new Credentials();

        credentials.username = defaultString(System.getenv(userVarName), System.getProperty(userVarName));

        Console c = System.console();
        if (isEmpty(credentials.username))
        {
            credentials.username = c.readLine(String.format("%s user name: ", target));
            System.setProperty(userVarName, credentials.username);
        }

        String password = defaultString(System.getenv(passVarName), System.getProperty(passVarName));
        if (isEmpty(password))
        {
            c.printf("%s password for user %s: ", target, credentials.username);
            credentials.password = c.readPassword();
            System.setProperty(passVarName, new String(credentials.password));
        }
        else
        {
            credentials.password = password.toCharArray();
        }
        return credentials;
    }

    public static void main(String[] args)
    {
        Wiki rowiki = Wiki.newSession("ro.wikipedia.org");

        Wikibase dwiki = new Wikibase();

        try
        {

            Credentials wdCredentials = identifyCredentials("WD_USER", "WD_PASS", "Wikidata");
            dwiki.login(wdCredentials.username, wdCredentials.password);
            Credentials wpCredentials = identifyCredentials("WP_USER", "WP_PASS", "ro.wikipedia");
            rowiki.login(wpCredentials.username, wpCredentials.password);
            rowiki.setMarkBot(true);
            List<Map<String, Object>> resultSet = dwiki.query(COUNTY_QUERY);
            WikidataEntitiesCache wdEntCache = new WikidataEntitiesCache(dwiki);
            for (Map<String, Object> eachResult : resultSet)
            {
                Item countyItem = (Item) eachResult.get("county");
                String lpc = (String) eachResult.get("lpc");
                Entity countyEnt = dwiki.getWikibaseItemById(countyItem.getEnt());
                if (null != countyEnt.getSitelinks())
                {
                    Sitelink rositelink = countyEnt.getSitelinks().get("rowiki");

                    String countyText = rowiki.getPageText(List.of(rositelink.getPageName())).stream().findFirst().orElse("");
                    int templateIdx = countyText.indexOf("{{Infocaseta Județe");
                    AggregatingParser parser = new AggregatingParser();
                    List<ParseResult<WikiPart>> parsedElems = parser.parse(countyText);
                    parsedElems.stream().map(ParseResult::getIdentifiedPart).filter(p -> p instanceof WikiTemplate).map(WikiTemplate.class::cast)
                        .filter(t -> "Infocaseta Județe".equalsIgnoreCase(t.getTemplateTitle())).findFirst()
                        .ifPresent(infoboxTemplate -> infoboxTemplate.removeParam("presedinte"));
                    String newText = parsedElems.stream().map(ParseResult::getIdentifiedPart).map(Object::toString).collect(Collectors.joining());
                    LinkedHashMap<String, String> sectionMap = rowiki.getSectionMap(rositelink.getPageName());

                    Pattern sectionToModify = sectionMap.values().stream().filter(x -> StringUtils.startsWithAny(x, "Politic", "Administra")).findFirst()
                        .map(x -> Pattern.compile("^==\\s*" + x + "\\s*==\\s*$", Pattern.MULTILINE)).orElse(null);
                    Pattern sectionToAddBefore = sectionMap.values().stream()
                        .filter(x -> StringUtils.startsWithAny(x, "Diviziuni", "Orașe", "Comune", "Împărțirea", "Note", "Bibliografie", "Vezi și", "Legături")).findFirst()
                        .map(x -> Pattern.compile("^==\\s*" + x + "\\s*==\\s*$", Pattern.MULTILINE)).orElse(null);

                    MongoClient mongoClient = MongoClients.create("mongodb://localhost:57017");
                    MongoDatabase electionsDb = mongoClient.getDatabase("elections2020");
                    MongoCollection<Document> electionsColl = electionsDb.getCollection("cj");
                    FindIterable<Document> electionResultsItrble = electionsColl.find(Filters.eq("county", countyEnt.getLabels().get("ro")));
                    electionResultsItrble.sort(new BasicDBObject("seats", -1));

                    final List<Object[]> electionResults = new ArrayList<Object[]>();
                    electionResultsItrble.forEach(t ->
                        {
                            Object[] crtResult = new Object[3];
                            crtResult[0] = t.getString("shortName");
                            crtResult[1] = t.getString("fullName");
                            crtResult[2] = t.get("seats");
                            electionResults.add(crtResult);
                        });

                    mongoClient.close();

                    WikiTemplate councillorsTemplate = new WikiTemplate();
                    councillorsTemplate.setSingleLine(false);
                    councillorsTemplate.setTemplateTitle("Componență politică");
                    councillorsTemplate.setParam("eticheta_compoziție", "Componența Consiliului");
                    councillorsTemplate.setParam("eticheta_mandate", "Consilieri");
                    int mandatesCount = 0;
                    for (int i = 0; i < electionResults.size(); i++)
                    {
                        Object[] eachElectionResult = electionResults.get(i);
                        mandatesCount += (Integer) eachElectionResult[2];
                        councillorsTemplate.setParam("nume_scurt" + String.valueOf(1 + i), eachElectionResult[0].toString());
                        councillorsTemplate.setParam("nume_complet" + String.valueOf(1 + i), eachElectionResult[1].toString());
                        councillorsTemplate.setParam("mandate" + String.valueOf(1 + i), eachElectionResult[2].toString());
                    }

                    String sectionText = String.format(
                        "\n<!--secțiune administrație-->Județul %s este administrat de un consiliu județean format din %d consilieri. În urma [[Alegeri locale în România, 2020|alegerilor locale din 2020]], consiliul este prezidat de {{Date înlănțuite de la Wikidata|P194|P2388|P1308}} de la {{Partid|q={{Date înlănțuite de la Wikidata|P194|P2388|P1308|P102|raw}} }}, iar componența politică a Consiliului este următoarea:<ref>{{Citat web|url=https://prezenta.roaep.ro/locale27092020/data/json/sicpv/pv/pv_%s_final.json|format=Json|titlu=Rezultatele finale ale alegerilor locale din 2020 |publisher=Autoritatea Electorală Permanentă|accessdate=2020-11-02}}</ref>%n%s<!-- sfârșit secțiune administrație-->",
                        countyEnt.getLabels().get("ro"), mandatesCount, lpc.toLowerCase(), councillorsTemplate);
                    Matcher alreadyGeneratedSectionMatcher = ALREADY_GENERATED_SECTION_PATTERN.matcher(newText);
                    if (alreadyGeneratedSectionMatcher.find())
                    {
                        StringBuffer sbuf = new StringBuffer();
                        alreadyGeneratedSectionMatcher.appendReplacement(sbuf, sectionText);
                        alreadyGeneratedSectionMatcher.appendTail(sbuf);
                        newText = sbuf.toString();
                    }
                    else if (null != sectionToModify)
                    {
                        Matcher sectionToModifyMatcher = sectionToModify.matcher(newText);
                        StringBuffer changedNewText = new StringBuffer();
                        while (sectionToModifyMatcher.find())
                        {
                            sectionToModifyMatcher.appendReplacement(changedNewText, "$0\n" + sectionText + "\n");
                        }
                        sectionToModifyMatcher.appendTail(changedNewText);
                        newText = changedNewText.toString();
                    }
                    else if (null != sectionToAddBefore)
                    {
                        Matcher sectionToAddBeforeMatcher = sectionToAddBefore.matcher(newText);
                        StringBuffer changedNewText = new StringBuffer();
                        while (sectionToAddBeforeMatcher.find())
                        {
                            sectionToAddBeforeMatcher.appendReplacement(changedNewText, "== Politică și administrație ==\n" + sectionText + "\n$0");
                        }
                        sectionToAddBeforeMatcher.appendTail(changedNewText);
                        newText = changedNewText.toString();
                    }
                    //System.out.println(newText);
                    if (!countyText.equals(newText))
                    {
                        rowiki.edit(countyEnt.getSitelinks().get("rowiki").getPageName(), newText, "Robot: adăugat administrație; preluat președinte CJ de la Wikidata");
                        try
                        {
                            Thread.currentThread().sleep(30000l);
                        }
                        catch (InterruptedException e)
                        {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }
        }
        catch (IOException | WikibaseException | LoginException e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (null != dwiki)
            {
                dwiki.logout();
            }
            rowiki.logout();
        }

    }

}
