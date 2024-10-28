package org.wikipedia.ro.java.elections;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.wikipedia.ro.java.elections.PartyConstants.PARTIES;
import static org.wikipedia.ro.java.elections.PartyConstants.PARTIES_LONG;
import static org.wikipedia.ro.java.elections.PartyConstants.PARTIES_REGEX_ID;
import static org.wikipedia.ro.java.elections.PartyConstants.PARTIES_SHORT;
import static org.wikipedia.ro.java.elections.PartyConstants.PARTIES_SHORT_ID;

import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.security.auth.login.FailedLoginException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.wikibase.Wikibase;
import org.wikibase.WikibaseException;
import org.wikibase.WikibasePropertyFactory;
import org.wikibase.data.Claim;
import org.wikibase.data.Entity;
import org.wikibase.data.LanguageString;
import org.wikipedia.Wiki;
import org.wikipedia.ro.cache.WikidataEntitiesCache;
import org.wikipedia.ro.model.WikiTemplate;
import org.wikipedia.ro.utils.Credentials;
import org.wikipedia.ro.utils.TextUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class LocalCouncilUpdater
{
    private static final String COUNTY_QUERY = "SELECT DISTINCT ?county ?countyLabel ?lpc WHERE {\n" + "  ?county wdt:P31 wd:Q1776764.\n"
        + "  OPTIONAL { ?county wdt:P395 ?lpc. }\n" + "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"ro\". }\n" + "}\n" + "ORDER BY (?countyLabel)";

    private static final String PV_URL_TEMPLATE = "https://prezenta.roaep.ro/locale09062024/data/json/sicpv/pv/pv_%s_final.json";

    private static Pattern NAMESPLITTER_PATTERN = Pattern.compile("^(\\S+?)\\s(.*)$");

    private static Object deepMapGet(Map<String, Object> map, String... pathElem)
    {
        Object crtElem = map;
        for (String eachPathElem : pathElem)
        {
            if (!(crtElem instanceof Map))
            {
                return null;
            }
            crtElem = ((Map<String, Object>) crtElem).get(eachPathElem);
            if (null == crtElem)
            {
                return null;
            }
        }
        return crtElem;
    }

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
        MongoClient client = MongoClients.create("mongodb://localhost:57017");
        MongoDatabase database = client.getDatabase("elections2024");

        ObjectMapper objectMapper = new ObjectMapper();

        try
        {
            Credentials wdCredentials = identifyCredentials("WIKI_WDUSERNAME", "WIKI_WDPASSWORD", "Wikidata");
            dwiki.login(wdCredentials.username, wdCredentials.password);
            //            Property licensePlateCode = WikibasePropertyFactory.getWikibaseProperty("P395");
            List<Map<String, Object>> resultSet = dwiki.query(COUNTY_QUERY);
            WikidataEntitiesCache wdEntCache = new WikidataEntitiesCache(dwiki);
            for (Map<String, Object> eachResult : resultSet)
            {
                String lpc = (String) eachResult.get("lpc");
                System.out.printf("Processing county %s, LPC: %s ", eachResult.get("countyLabel"), lpc);

                processCountyFile(database, objectMapper, wdEntCache, lpc);
            }
            processCountyFile(database, objectMapper, wdEntCache, "b");
        }
        catch (IOException | WikibaseException | FailedLoginException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        finally
        {
            client.close();
        }
    }

    private static void processCountyFile(MongoDatabase database, ObjectMapper objectMapper, WikidataEntitiesCache wdEntCache, String lpc)
        throws IOException, MalformedURLException, JsonProcessingException, JsonMappingException, WikibaseException
    {
        String pvurl = String.format(PV_URL_TEMPLATE, lpc.toLowerCase());

        System.out.printf("Reading from %s%n", pvurl);
        URLConnection conn = new URL(pvurl).openConnection();
        String jsonString;
        try (InputStream in = conn.getInputStream())
        {
            jsonString = IOUtils.toString(in, conn.getContentEncoding());
        }

        if (null == jsonString)
        {
            return;
        }

        Map<String, Object> countyMap = objectMapper.readValue(jsonString, new TypeReference<Map<String, Object>>()
        {
        });

        readCountyCouncils(database, countyMap, wdEntCache);
        readLocalCouncils(database, countyMap, wdEntCache);
        readMayors(database, countyMap, wdEntCache);
    }

    private static void readMayors(MongoDatabase database, Map<String, Object> countyMap, WikidataEntitiesCache wdEntCache)
    {
        if (!database.listCollectionNames().into(new ArrayList<>()).contains("mayor"))
        {
            database.createCollection("mayor");
        }
        MongoCollection<Document> cjCollection = database.getCollection("mayor");
        Map<String, Object> mayorMap = (Map<String, Object>) deepMapGet(countyMap, "stages", "FINAL", "scopes", "UAT", "categories", "P");
        final Map<String, Object> mayorMapTable = (Map<String, Object>) deepMapGet(mayorMap, "table");
        mayorMapTable.entrySet().stream().filter(e -> e.getKey().matches("\\d+")).map(Map.Entry::getValue).map(Map.class::cast)
            .map(v -> {
                try
                {
                    return readMayorFromCommune(database, v, wdEntCache);
                }
                catch (IOException e1)
                {
                    e1.printStackTrace();
                    return null;
                }
                catch (WikibaseException e1)
                {
                    e1.printStackTrace();
                    return null;
                }
            }).forEach(cjCollection::insertOne);
    }

    private static Document readMayorFromCommune(MongoDatabase database, Map<String, Object> communeMap, WikidataEntitiesCache wdEntCache) throws IOException, WikibaseException
    {
        List<Map<String, Object>> votesList = (List<Map<String, Object>>) deepMapGet(communeMap, "votes");
        Optional<Map<String, Object>> winner = votesList.stream()
            .sorted((o1, o2) -> Integer.parseInt(deepMapGet(o2, "votes").toString()) - Integer.parseInt(deepMapGet(o1, "votes").toString())).findFirst();

        if (winner.isPresent())
        {
            Map<String, Object> actualWinner = winner.get();
            System.out.printf("UAT: %s  ---> %s (%s) with %s votes%n", deepMapGet(communeMap, "uat_name"), deepMapGet(actualWinner, "candidate"), deepMapGet(actualWinner, "party"),
                deepMapGet(actualWinner, "votes"));

            String uatName = TextUtils.capitalizeName(sanitizeName(StringUtils.removeStart(StringUtils.removeStart(deepMapGet(communeMap, "uat_name").toString(), "MUNICIPIUL "), "ORAŞ ")));
            int uatSiruta = Integer.parseInt(deepMapGet(communeMap, "uat_siruta").toString());
            String countyCode = deepMapGet(communeMap, "county_code").toString();
            String mayorFullName = deepMapGet(actualWinner, "candidate").toString();
            Matcher mayorSplitter = NAMESPLITTER_PATTERN.matcher(mayorFullName);
            mayorSplitter.find();
            String mayorLastName = TextUtils.capitalizeName(sanitizeName(mayorSplitter.group(1)));
            String mayorFirstName = TextUtils.capitalizeName(sanitizeName(mayorSplitter.group(2)));
            final String rawParty = Optional.ofNullable(deepMapGet(actualWinner, "party")).map(Objects::toString).map(LocalCouncilUpdater::sanitizeName).orElse("INDEPENDENT");
            PartyData party = identifyParty(rawParty, wdEntCache);

            Document d = new Document();
            d.put("siruta", uatSiruta);
            d.put("uat", uatName);
            d.put("county", countyCode);
            d.put("firstName", mayorFirstName);
            d.put("lastName", mayorLastName);
            d.put("partyQId", party.qId);
            d.put("partyName", party.longName);

            return d;
        }
        return null;
    }

    private static class PartyData
    {
        public String qId;
        public String shortName;
        public String longName;
    }

    private static PartyData identifyParty(final String rawParty, WikidataEntitiesCache wdEntCache) throws IOException, WikibaseException
    {
        PartyData out = new PartyData();
        out.qId = PARTIES.get(rawParty);
        if (null == out.qId && PARTIES_SHORT.containsKey(rawParty))
        {
            out.shortName = PARTIES_SHORT.get(rawParty);
            out.qId = PARTIES_SHORT_ID.get(out.shortName);
            out.longName = StringUtils.defaultString(PARTIES_LONG.get(rawParty), TextUtils.capitalizeName(rawParty));
        }
        if (null == out.qId)
        {
            Optional<String> firstMatching = PARTIES_REGEX_ID.entrySet().stream().filter(e -> {
                Pattern p = Pattern.compile(e.getKey());
                Matcher m = p.matcher(rawParty);
                return m.matches();
            }).map(Map.Entry::getValue).findFirst();
            if (firstMatching.isPresent())
            {
                out.qId = firstMatching.get();
                out.longName = TextUtils.capitalizeName(rawParty);
            }
        }
        if (null != out.qId)
        {
            Entity partyEntity = wdEntCache.get(out.qId);
            if (null == out.longName)
            {
                out.longName = Optional.ofNullable(partyEntity.getLabels().get("ro")).orElse(null);
            }
            if (null == out.shortName)
            {
                Set<Claim> shortNameClaims = partyEntity.getBestClaims(WikibasePropertyFactory.getWikibaseProperty("P1813"));
                if (null != shortNameClaims)
                {
                    out.shortName = shortNameClaims.stream().filter(x -> null != x && null != x.getValue()).map(Claim::getValue).filter(x -> x instanceof LanguageString)
                        .map(x -> LanguageString.class.cast(x)).filter(ls -> "ro".equals(ls.getLanguage())).findFirst().map(LanguageString::getText)
                        .orElse(null);
                }
            }
        }
        if (null == out.longName)
        {
            out.longName = StringUtils.defaultString(PARTIES_LONG.get(rawParty), TextUtils.capitalizeName(rawParty));
        }
        if (null == out.shortName)
        {
            out.shortName = StringUtils.defaultString(PARTIES_SHORT.get(rawParty), TextUtils.capitalizeName(rawParty));
        }

        return out;
    }

    private static void readCountyCouncils(MongoDatabase database, Map<String, Object> countyMap, WikidataEntitiesCache wdEntCache) throws IOException, WikibaseException
    {
        if (!database.listCollectionNames().into(new ArrayList<>()).contains("cj"))
        {
            database.createCollection("cj");
        }

        MongoCollection<Document> cjCollection = database.getCollection("cj");
        Map<String, Object> cjMap = (Map<String, Object>) deepMapGet(countyMap, "stages", "FINAL", "scopes", "CNTY", "categories", "CJ");
        Map<String, Object> cjMapTable = (Map<String, Object>) deepMapGet(cjMap, "table");
        String ctyKey = cjMapTable.keySet().stream().filter(s -> s.matches("\\d+")).findFirst().orElse(null);

        if (null == ctyKey)
        {
            return;
        }

        List<Map<String, Object>> cjVotes = (List<Map<String, Object>>) deepMapGet(cjMapTable, ctyKey, "votes");
        for (Map<String, Object> partyResult : cjVotes)
        {
            Map<String, Object> party = parsePartyForCouncil(partyResult, wdEntCache);
            party.put("county", TextUtils.capitalizeName((String) deepMapGet(cjMapTable, ctyKey, "county_name")).replace('ş', 'ș').replace('ţ', 'ț'));
            if (null == party || 0 == ((Integer) party.get("seats")))
            {
                continue;
            }
            Document d = party.entrySet().stream().collect(Document::new, (doc, entry) -> {
                doc.put(entry.getKey(), entry.getValue());
            }, Document::putAll);
            cjCollection.insertOne(d);
        }
    }

    private static void readLocalCouncils(MongoDatabase database, Map<String, Object> countyMap, WikidataEntitiesCache wdEntCache) throws IOException, WikibaseException
    {
        if (!database.listCollectionNames().into(new ArrayList<>()).contains("cl"))
        {
            database.createCollection("cl");
        }

        MongoCollection<Document> clCollection = database.getCollection("cl");

        Map<String, Map<String, Object>> clList = (Map<String, Map<String, Object>>) deepMapGet(countyMap, "stages", "FINAL", "scopes", "UAT", "categories", "CL", "table");

        for (Map<String, Object> eachCl : clList.values())
        {
            List<Map<String, Object>> clVotes = (List<Map<String, Object>>) deepMapGet(eachCl, "votes");
            for (Map<String, Object> partyResult : clVotes)
            {
                String siruta = (String) deepMapGet(eachCl, "uat_siruta");
                Map<String, Object> party = parsePartyForCouncil(partyResult, wdEntCache);
                party.put("siruta", siruta);
                if (null == party || 0 == ((Integer) party.get("seats")))
                {
                    continue;
                }
                Document d = party.entrySet().stream().collect(Document::new, (doc, entry) -> {
                    doc.put(entry.getKey(), entry.getValue());
                }, Document::putAll);
                clCollection.insertOne(d);
            }
        }
    }

    private static String generateTemplate(List<Map<String, String>> councilMembership)
    {
        WikiTemplate res = new WikiTemplate();
        res.setTemplateTitle("Componență politică");
        res.setParam("eticheta_compoziție", "Componența consiliului");
        res.setParam("eticheta_mandate", "consilieri");
        res.setSingleLine(false);
        for (int idx = 0; idx < councilMembership.size(); idx++)
        {
            Map<String, String> cParty = councilMembership.get(idx);
            res.setParam("nume_scurt" + (1 + idx), cParty.get("shortName"));
            res.setParam("nume_complet" + (1 + idx), cParty.get("fullName"));
            res.setParam("mandate" + (1 + idx), cParty.get("seats"));
        }
        return res.toString();
    }

    private static String generateCountyCouncilPhrase(List<Map<String, String>> countyCouncilMembership, String county)
    {
        return String.format(
            "Județul %s este administrat de un consiliu județean compus din %d consilieri. Începând cu alegerile locale din 2020, consiliul județean are următoarea componență pe partide politice:",
            county, countyCouncilMembership.stream().map(ccm -> ccm.get("seats")).map(Integer::valueOf).reduce(0, Integer::sum));
    }

    private static Map<String, Object> parsePartyForCouncil(Map<String, Object> partyResult, WikidataEntitiesCache wdEntCache) throws IOException, WikibaseException
    {
        Map<String, Object> res = new HashMap<>();
        String partyName = sanitizeName((String) deepMapGet(partyResult, "candidate"));
        PartyData party = identifyParty(partyName, wdEntCache);
        res.put("fullName", party.longName);
        res.put("shortName", StringUtils.defaultString(party.shortName, party.longName));

        int mandate = Stream.of(deepMapGet(partyResult, "mandates1"), deepMapGet(partyResult, "mandates2"), deepMapGet(partyResult, "mandates_g8")).filter(Objects::nonNull)
            .map(x -> {
                if (x instanceof String)
                {
                    return Integer.parseInt((String) x);
                }
                else if (x instanceof Integer)
                    return (Integer) x;
                return 0;
            }).reduce(0, Integer::sum);
        res.put("seats", Integer.valueOf(mandate));

        return res;
    }

    private static Pattern QUOTED_STRING_PATTERN = Pattern.compile("^\"(.*)\"$");

    private static String sanitizeName(String s)
    {
        s = s.replaceAll("Ş", "Ș").replaceAll("Ţ", "Ț");
        Matcher matcher = QUOTED_STRING_PATTERN.matcher(s);
        if (matcher.matches())
        {
            return matcher.group(1);
        }
        return s;
    }

}
