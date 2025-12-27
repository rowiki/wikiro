package org.wikipedia.ro.java.elections;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.Console;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.security.auth.login.FailedLoginException;

import org.apache.commons.lang3.StringUtils;
import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.mongojack.JacksonMongoCollection;
import org.mongojack.ObjectMapperConfigurer;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.wikibase.Wikibase;
import org.wikibase.WikibaseException;
import org.wikibase.WikibasePropertyFactory;
import org.wikibase.data.Claim;
import org.wikibase.data.Entity;
import org.wikibase.data.Item;
import org.wikibase.data.LanguageString;
import org.wikibase.data.Property;
import org.wikibase.data.Rank;
import org.wikibase.data.Snak;
import org.wikibase.data.StringData;
import org.wikibase.data.Time;
import org.wikibase.data.URLData;
import org.wikipedia.ro.cache.WikidataEntitiesCache;
import org.wikipedia.ro.utils.Credentials;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;

public class WikidataMayorUpdater
{
    private static final Property WD_PROP_START_TIME = WikibasePropertyFactory.getWikibaseProperty("P580");
    private static final Property WD_PROP_POLITICAL_PARTY = WikibasePropertyFactory.getWikibaseProperty("P102");
    private static final Property WD_PROP_HEAD_OF_GOVT_POSITION = WikibasePropertyFactory.getWikibaseProperty("P1313");
    private static final Property WD_PROP_REPLACES = WikibasePropertyFactory.getWikibaseProperty("P1365");
    private static final Property WD_PROP_OFFICEHOLDER = WikibasePropertyFactory.getWikibaseProperty("P1308");
    private static final Property WD_PROP_POSITION_HELD = WikibasePropertyFactory.getWikibaseProperty("P39");
    private static final Property WD_PROP_UAT = WikibasePropertyFactory.getWikibaseProperty("P131");
    private static final Property WD_PROP_HEAD_OF_GOVT = WikibasePropertyFactory.getWikibaseProperty("P6");
    private static final Property WD_PROP_END_TIME = WikibasePropertyFactory.getWikibaseProperty("P582");
    private static final Property WD_PROP_REPLACED_BY = WikibasePropertyFactory.getWikibaseProperty("P1366");
    private static final Property WD_PROP_ELECTED_IN = WikibasePropertyFactory.getWikibaseProperty("P2715");
    private static final Property WD_PROP_GENDER = WikibasePropertyFactory.getWikibaseProperty("P21");
    private static final Property WD_PROP_GIVEN_NAME = WikibasePropertyFactory.getWikibaseProperty("P735");

    private static final String WD_ENT_ID_2020_ELECTIONS = "Q96251607";
    private static final String WD_ENT_ID_2024_ELECTIONS = "Q105494567";
    private static final String WD_ENT_ID_PERM_ELECT_AUTH = "Q28726168";
    private static final String WD_ENT_ID_MALE = "Q6581097";
    private static final String WD_ENT_ID_FEMALE = "Q6581072";

    private static List<Name> NAMES = new ArrayList<>();

    private static Time FIRST_NOV_2024;

    private static final String WD_QUERY_GET_COMMUNE_BY_SIRUTA = "select ?item " + "where {" + "    ?item wdt:P843 \"131069\"." + "  }";
    private static final String WD_QUERY_ALL_COUNTIES = "SELECT DISTINCT ?county ?countyLabel ?lpc WHERE {\n" + "  ?county wdt:P31 wd:Q1776764.\n"
        + "  OPTIONAL { ?county wdt:P395 ?lpc. }\n" + "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"ro\". }\n" + "}\n" + "ORDER BY (?countyLabel)";
    private static final String WD_QUERY_NAMES = "SELECT DISTINCT ?name ?isa ?rolabel\n" + "WHERE {\n" + "  ?name wdt:P31 ?isa;\n" + "        wdt:P407 ?lang;\n"
        + "        rdfs:label ?rolabel filter (lang(?rolabel) = \"ro\").\n" + "  FILTER (?isa in (wd:Q11879590,wd:Q12308941))\n" + "  FILTER (?lang in (wd:Q7913, wd:Q9067))\n"
        + "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"ro\". }\n" + "}\n" + "ORDER BY (?roLabel)";

    private static WikidataEntitiesCache WD_ENT_CACHE;
    private static Wikibase DWIKI;

    private static Claim HUMAN_CLAIM;

    private static Claim POLITICIAN_CLAIM;

    private static Entity HUMAN_ENT;

    private static Claim ROMANIAN_CLAIM;

    private static Claim ELECTION_CLAIM;
    private static String crtMayorPartyClaim;

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

    public static void init()
    {
        initLogging();
        
        DWIKI = new Wikibase();
        HUMAN_ENT = new Entity("Q5");

        HUMAN_CLAIM = new Claim();
        HUMAN_CLAIM.setProperty(WikibasePropertyFactory.getWikibaseProperty("P31"));
        HUMAN_CLAIM.setValue(new Item(HUMAN_ENT));

        POLITICIAN_CLAIM = new Claim();
        POLITICIAN_CLAIM.setProperty(WikibasePropertyFactory.getWikibaseProperty("P106"));
        POLITICIAN_CLAIM.setValue(new Item(new Entity("Q82955")));

        ROMANIAN_CLAIM = new Claim();
        ROMANIAN_CLAIM.setProperty(WikibasePropertyFactory.getWikibaseProperty("P27"));
        ROMANIAN_CLAIM.setValue(new Item(new Entity("Q218")));

        ELECTION_CLAIM = new Claim();
        ELECTION_CLAIM.setProperty(WikibasePropertyFactory.getWikibaseProperty("P2715"));
        ELECTION_CLAIM.setValue(new Item(new Entity("Q105494567")));

        FIRST_NOV_2024 = new Time();
        FIRST_NOV_2024.setDate(LocalDate.of(2024, 11, 1));
        FIRST_NOV_2024.setPrecision(11);
    }

    public static void main(String[] args)
    {
        // TODO Auto-generated method stub
        init();
        ConnectionString mongoConnStr = new ConnectionString("mongodb://localhost:57017");
        CodecRegistry pojoCodecRegistry = CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build());
        CodecRegistry codecRegistry = CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), pojoCodecRegistry);
        MongoClientSettings clientSettings = MongoClientSettings.builder().applyConnectionString(mongoConnStr).codecRegistry(codecRegistry).build();

        MongoClient client = MongoClients.create(clientSettings);
        MongoDatabase database = client.getDatabase("elections2024");
        
        JsonMapper jsonMapper = JsonMapper.builder()
            .visibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();
        ObjectMapperConfigurer.configureObjectMapper(jsonMapper);
        JacksonMongoCollection<Mayor> mayorCollection = JacksonMongoCollection.builder()
            .withObjectMapper(jsonMapper)
            .build(database, "mayor", Mayor.class, UuidRepresentation.STANDARD);
        
        try
        {
            Credentials wdCredentials = identifyCredentials("WIKI_WDUSERNAME", "WIKI_WDPASSWORD", "Wikidata");
            DWIKI.login(wdCredentials.username, wdCredentials.password);
            //            Property licensePlateCode = WikibasePropertyFactory.getWikibaseProperty("P395");
            WD_ENT_CACHE = new WikidataEntitiesCache(DWIKI);

            System.out.println("Populating names....");
            List<Map<String, Object>> namesResultset = DWIKI.query(WD_QUERY_NAMES);
            NAMES = namesResultset.stream().map(e -> {
                Name ret = new Name();
                ret.gender = "Q11879590".equals(((Item) e.get("isa")).getEnt().getId()) ? Name.Gender.FEMALE : Name.Gender.MALE;
                ret.label = (String) e.get("rolabel");
                ret.qId = ((Item) e.get("name")).getEnt().getId();
                return ret;
            }).collect(Collectors.toList());
            System.out.printf("Names list has %d names listed%n", NAMES.size());

            System.out.println("Querying counties....");
            List<Map<String, Object>> resultSet = DWIKI.query(WD_QUERY_ALL_COUNTIES);
            System.out.println("Counties retrieved.");
            Optional<String> onSwitch = Arrays.stream(args).findFirst();
            boolean on = !onSwitch.isPresent();
            for (Map<String, Object> eachResult : resultSet)
            {
                Item countyItem = (Item) eachResult.get("county");
                Entity county = WD_ENT_CACHE.get(countyItem.getEnt());
                if (!county.getId().equals("Q45868")) {
                    continue;
                }
                System.out.printf("----------------- %s ----------------%n", county.getLabels().get("ro"));
                if (on || onSwitch.get().equalsIgnoreCase(county.getLabels().get("ro")))
                {
                    on = true;
                }
                if (!on)
                {
                    continue;
                }
                Set<Claim> claims = county.getClaims(WikibasePropertyFactory.getWikibaseProperty("P150"));
                claims.stream().map(Claim::getMainsnak).map(Snak::getData).map(Item.class::cast).map(Item::getEnt)
                    .forEach(e -> updateMayor(e, mayorCollection));

            }
            Entity bucharestEnt = WD_ENT_CACHE.get(new Entity("Q19660"));
            Set<Claim> sectorClaims = bucharestEnt.getClaims(WikibasePropertyFactory.getWikibaseProperty("P1383"));
            System.out.printf("----------------- București sectoare ----------------%n");
            //sectorClaims.stream().map(Claim::getMainsnak).map(Snak::getData).map(Item.class::cast).map(Item::getEnt).forEach(e -> updateMayor(e, mayorCollection));
            
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

    private static void updateMayor(Entity communeEnt, MongoCollection<Mayor> mayorCollection)
    {
        System.out.printf("Processing commune %s (%s)%n", communeEnt.getLabels().get("ro"), communeEnt.getId());
        if(!StringUtils.substringBefore(communeEnt.getId(), "$").equals("12148992")) {
            return;
        }
        try
        {
            communeEnt = WD_ENT_CACHE.get(communeEnt);
            Property sirutaProp = WikibasePropertyFactory.getWikibaseProperty("P843");
            Optional<String> sirutaOpt = communeEnt.getBestClaims(sirutaProp).stream().findFirst().map(Claim::getMainsnak).map(Snak::getData).map(StringData.class::cast)
                .map(StringData::getValue);
            if (!sirutaOpt.isPresent())
            {
                System.out.printf("No siruta found for commune %s%n", communeEnt.getId());
                return;
            }
            String siruta = sirutaOpt.get();

            Set<Claim> crtHeadOfGovtClaims = communeEnt.getBestClaims(WD_PROP_HEAD_OF_GOVT);
            Optional<Claim> crtHeadOfGovtOptClaim = crtHeadOfGovtClaims.stream().filter(c -> !c.getQualifiers().containsKey(WikibasePropertyFactory.getWikibaseProperty("P582")))
                .findFirst();
            Entity crtMayorEnt = null;
            String crtMayorName = null;
            if (crtHeadOfGovtOptClaim.isPresent())
            {
                Claim crtHeadOfGovtClaim = crtHeadOfGovtOptClaim.get();
                if ("value".equals(crtHeadOfGovtClaim.getMainsnak().getSnaktype()))
                {
                    crtMayorEnt = WD_ENT_CACHE.get(((Item) crtHeadOfGovtClaim.getValue()).getEnt());
                    crtMayorName = crtMayorEnt.getLabels().get("ro");
                }
            }

            FindIterable<Mayor> communeMayorDTOs = mayorCollection.find(Filters.eq("siruta", Integer.parseInt(siruta)));

            for (Mayor electedMayor : communeMayorDTOs)
            {
                String electedMayorFullName = String.format("%s %s", electedMayor.getFirstName(), electedMayor.getLastName());
                if (nameEquals(electedMayorFullName, crtMayorName))
                {
                    System.out.printf("Mayor for %s reelected: %s%n", communeEnt.getLabels().get("ro"), electedMayorFullName);
                    updateMayorReelected(electedMayor, communeEnt, crtHeadOfGovtOptClaim);
                }
                else
                {
                    System.out.printf("Mayor for %s: %s replaced by %s%n", communeEnt.getLabels().get("ro"), crtMayorName, electedMayorFullName);
                    updateMayorNew(electedMayor, communeEnt, crtHeadOfGovtOptClaim);
                }
                break;
            }

        }
        catch (IOException | WikibaseException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void updateMayorNew(Mayor electedMayor, Entity communeEnt, Optional<Claim> crtHeadOfGovtOptClaim) throws WikibaseException, IOException
    {
        Entity newMayor = null;
        final String communeLabel = communeEnt.getLabels().get("ro");
        Optional<Entity> countyEntOpt = communeEnt.getBestClaims(WD_PROP_UAT).stream().findFirst().map(Claim::getValue).map(Item.class::cast).map(Item::getEnt);
        countyEntOpt = countyEntOpt.isPresent() ? Optional.of(WD_ENT_CACHE.get(countyEntOpt.get())) : Optional.empty();
        String countyAbbr = countyEntOpt.isPresent()
            ? countyEntOpt.get().getBestClaims(WikibasePropertyFactory.getWikibaseProperty("P395")).stream().findFirst().map(Claim::getValue).map(StringData.class::cast)
                .map(StringData::getValue).get()
            : null;
        String countyLabel = countyEntOpt.map(Entity::getLabels).map(m -> m.get("ro")).orElse(null);

        String expectedMayorName = String.format("%s %s", electedMayor.getFirstName(), electedMayor.getLastName());
        List<Entity> possibleExistingMayorEnts = DWIKI.searchWikibase(expectedMayorName, "ro");
        List<Entity> peopleWithMayorsName = possibleExistingMayorEnts.stream().filter(e -> Objects.equals(expectedMayorName, e.getLabels().get("ro"))).toList();
        Optional<Entity> alreadyExistingMayorOpt = Optional.empty();
            
        for (Entity personWithMayorsName: peopleWithMayorsName)
        {
            Map<String, String> personWithMayorsNameDescriptions = personWithMayorsName.getDescriptions();
            if (null != personWithMayorsNameDescriptions)
            {
                String personWithMayorsNameDescription = personWithMayorsNameDescriptions.get("ro");
                if (null != personWithMayorsNameDescription)
                {
                    List<String> expectedDescriptions = List.of(
                        String.format("politician român din %s, județul %s", communeLabel, countyLabel),
                        String.format("Romanian politician from %s, %s", communeLabel, countyLabel),
                        String.format("Romanian politician from %s, %s County", communeLabel, countyLabel));
                    if (expectedDescriptions.stream().anyMatch(personWithMayorsNameDescription::equals))
                    {
                        alreadyExistingMayorOpt = Optional.of(personWithMayorsName);
                        break;
                    }
                }
            }
        }
        if (alreadyExistingMayorOpt.isPresent())
        {
            System.out.printf("\tNew mayor %s of %s created already!%n", expectedMayorName, communeLabel);
            newMayor = WD_ENT_CACHE.get(alreadyExistingMayorOpt.get());
        }
        else
        {
            newMayor = createNewMayorPersonInWd(electedMayor, communeEnt, crtHeadOfGovtOptClaim, countyEntOpt);
        }
        List<Snak> ref = createRef(countyAbbr);
        updatePartyForMayor(electedMayor, newMayor, ref);
        setPositionHeldForMayor(communeEnt, newMayor);

        Claim mayorClaim = new Claim();
        mayorClaim.setProperty(WD_PROP_HEAD_OF_GOVT);
        mayorClaim.setValue(new Item(newMayor));
        final String claimId = DWIKI.addClaim(communeEnt.getId(), mayorClaim);

        if (null != claimId)
        {
            // mayorClaim.addQualifier(startTimeProp, startTime);
            DWIKI.addQualifier(claimId, WD_PROP_START_TIME.getId(), FIRST_NOV_2024);
            DWIKI.addQualifier(claimId, WD_PROP_ELECTED_IN.getId(), ELECTION_CLAIM.getValue());
            DWIKI.addReference(claimId, ref);
        }
        communeEnt = setClaimRankTo(communeEnt, WD_PROP_HEAD_OF_GOVT, claimId, Rank.PREFERRED);

        Entity positionEnt = communeEnt.getBestClaims(WD_PROP_HEAD_OF_GOVT_POSITION).stream().findFirst().map(Claim::getValue).map(Item.class::cast).map(Item::getEnt).orElse(null);
        positionEnt = WD_ENT_CACHE.get(positionEnt);

        if (crtHeadOfGovtOptClaim.isPresent())
        {
            newMayor = WD_ENT_CACHE.refresh(newMayor);
            Claim crtHeadOfGovtClaim = crtHeadOfGovtOptClaim.get();
            communeEnt = setClaimRankTo(communeEnt, WD_PROP_HEAD_OF_GOVT, crtHeadOfGovtClaim.getId(), Rank.NORMAL);

            setReplacedByAndTimeForFormerMayor(newMayor, crtHeadOfGovtClaim);

            setReplacesForNewMayor(newMayor, crtHeadOfGovtClaim);
        }

        Set<Claim> bestOhClaims = positionEnt.getBestClaims(WD_PROP_OFFICEHOLDER);
        if (null != bestOhClaims)
        {
            List<Claim> ohs = bestOhClaims.stream().filter(oh -> !oh.getQualifiers().containsKey(WD_PROP_END_TIME)).collect(Collectors.toList());
            for (Claim oh : ohs)
            {
                oh.setRank(Rank.NORMAL);
                DWIKI.editClaim(oh);
                DWIKI.addQualifier(oh.getId(), WD_PROP_END_TIME.getId(), FIRST_NOV_2024);
            }
        }

        Claim newOfficeholderClaim = new Claim();
        newOfficeholderClaim.setProperty(WD_PROP_OFFICEHOLDER);
        newOfficeholderClaim.setValue(new Item(newMayor));
        String newOfficeholderClaimId = DWIKI.addClaim(positionEnt.getId(), newOfficeholderClaim);

        setClaimRankTo(positionEnt, WD_PROP_OFFICEHOLDER, newOfficeholderClaimId, Rank.PREFERRED);

        DWIKI.addQualifier(newOfficeholderClaimId, WD_PROP_START_TIME.getId(), FIRST_NOV_2024);
        DWIKI.addReference(newOfficeholderClaimId, createRef(countyAbbr));
    }

    private static Entity setClaimRankTo(Entity parentEnt, final Property prop, final String claimId, Rank rank) throws IOException, WikibaseException
    {
        Claim editedClaim;

        parentEnt = WD_ENT_CACHE.refresh(parentEnt);
        Optional<Claim> savedClaimOpt = parentEnt.getClaims(prop).stream().filter(c -> Objects.equals(claimId, c.getId())).findFirst();
        if (savedClaimOpt.isPresent())
        {
            editedClaim = savedClaimOpt.get();
            if (editedClaim.getRank() != rank)
            {
                editedClaim.setRank(rank);
                DWIKI.editClaim(editedClaim);
            }
        }
        return parentEnt;
    }

    private static void setReplacesForNewMayor(Entity newMayor, Claim crtHeadOfGovtClaim) throws WikibaseException, IOException
    {
        Set<Claim> bestP39Claims = newMayor.getBestClaims(WD_PROP_POSITION_HELD);
        if (null != bestP39Claims)
        {
            Optional<Claim> mayorP39Opt = bestP39Claims.stream().filter(c -> c.getQualifiers().get(WD_PROP_END_TIME) == null).findFirst();
            if (mayorP39Opt.isPresent())
            {
                Claim mayorP39 = mayorP39Opt.get();
                if (null == mayorP39.getQualifiers() || !mayorP39.getQualifiers().containsKey(WD_PROP_REPLACES))
                {
                    DWIKI.addQualifier(mayorP39.getId(), WD_PROP_REPLACES.getId(), crtHeadOfGovtClaim.getValue());
                }
            }
        }
    }

    private static void setReplacedByAndTimeForFormerMayor(Entity newMayor, Claim crtHeadOfGovtClaim) throws WikibaseException, IOException
    {
        Entity crtHeadOfGovtEnt = WD_ENT_CACHE.get(((Item) crtHeadOfGovtClaim.getValue()).getEnt());
        Set<Claim> bestFormerMayorPosnHeldClaims = crtHeadOfGovtEnt.getBestClaims(WD_PROP_POSITION_HELD);
        if (null != bestFormerMayorPosnHeldClaims)
        {
            for (Claim bestFormerMayorPosnHeldClaim : bestFormerMayorPosnHeldClaims)
            {
                Set<Snak> endTimeQuals = bestFormerMayorPosnHeldClaim.getQualifiers().get(WD_PROP_END_TIME);
                if (null != endTimeQuals && !endTimeQuals.isEmpty() && endTimeQuals.stream().anyMatch(s -> ((Time) s.getData()).getYear() < 2024))
                {
                    continue;
                }
                Set<Snak> replacedByQuals = bestFormerMayorPosnHeldClaim.getQualifiers().get(WD_PROP_REPLACED_BY);
                if (null == replacedByQuals || replacedByQuals.isEmpty())
                {
                    DWIKI.addQualifier(bestFormerMayorPosnHeldClaim.getId(), WD_PROP_REPLACED_BY.getId(), new Item(newMayor));
                }
                if (null == endTimeQuals || endTimeQuals.isEmpty())
                {
                    DWIKI.addQualifier(bestFormerMayorPosnHeldClaim.getId(), WD_PROP_END_TIME.getId(), FIRST_NOV_2024);
                }
                setClaimRankTo(crtHeadOfGovtEnt, WD_PROP_POSITION_HELD, bestFormerMayorPosnHeldClaim.getId(), Rank.NORMAL);
            }
        }

        Set<Snak> endTimeClaims = crtHeadOfGovtClaim.getQualifiers().get(WD_PROP_END_TIME);
        if (null == endTimeClaims || endTimeClaims.isEmpty())
        {
            DWIKI.addQualifier(crtHeadOfGovtClaim.getId(), WD_PROP_END_TIME.getId(), FIRST_NOV_2024);
        }
    }

    private static Entity createNewMayorPersonInWd(Mayor electedMayor, Entity communeEnt, Optional<Claim> crtHeadOfGovtOptClaim, Optional<Entity> countyEntOpt)
    {
        System.out.printf("\tCreating new mayor in Wikidata: %s %s of %s%n", electedMayor.getFirstName(), electedMayor.getLastName(), communeEnt.getLabels().get("ro"));
        try
        {
            communeEnt = WD_ENT_CACHE.get(communeEnt);
            String roLabel = communeEnt.getLabels().get("ro");
            String county = countyEntOpt.isPresent() ? WD_ENT_CACHE.get(countyEntOpt.get()).getLabels().get("ro") : null;

            Entity newMayor = new Entity(null);
            String mayorFullName = String.format("%s %s", electedMayor.getFirstName(), electedMayor.getLastName());
            newMayor.addLabel("ro", mayorFullName);
            newMayor.addLabel("en", mayorFullName);
            newMayor.addDescription("en", "Romanian politician from " + roLabel + (StringUtils.equals(roLabel, county) ? "" : (", " + county + " County")));
            newMayor.addDescription("ro", "politician român din " + roLabel + (StringUtils.equals(roLabel, county) ? "" : (", județul " + county)));

            String itemId = DWIKI.createItem(newMayor);
            if (null == itemId)
            {
                System.out.printf("Could not make new mayor for %s %s of %s, %s%n", electedMayor.getFirstName(), electedMayor.getLastName(), electedMayor.getUat(),
                    electedMayor.getCounty());
                return null;
            }
            newMayor = DWIKI.getWikibaseItemById(itemId);

            DWIKI.addClaim(newMayor.getId(), HUMAN_CLAIM);
            // newMayor.addClaim(instanceofProp, humanClaim);
            DWIKI.addClaim(newMayor.getId(), POLITICIAN_CLAIM);
            // newMayor.addClaim(occupationProp, politicianClaim);
            DWIKI.addClaim(newMayor.getId(), ROMANIAN_CLAIM);

            String[] firstNames = electedMayor.getFirstName().split("[\\s\\-]");
            boolean genderFound = false;
            for (String fn : firstNames)
            {
                Optional<Name> foundNameOpt = NAMES.stream().filter(n -> fn.equalsIgnoreCase(n.label)).findAny();
                if (foundNameOpt.isPresent())
                {
                    Name foundName = foundNameOpt.get();
                    if (!genderFound)
                    {
                        DWIKI.addClaim(newMayor.getId(), new Claim(WD_PROP_GENDER, new Item(new Entity(foundName.gender == Name.Gender.MALE ? WD_ENT_ID_MALE : WD_ENT_ID_FEMALE))));
                        genderFound = true;
                    }
                    DWIKI.addClaim(newMayor.getId(), new Claim(WD_PROP_GIVEN_NAME, new Item(new Entity(foundName.qId))));
                }
            }

            return WD_ENT_CACHE.get(newMayor);
        }
        catch (WikibaseException | IOException e)
        {
            System.err.printf("Could not make new mayor for %s %s of %s, %s%n", electedMayor.getFirstName(), electedMayor.getLastName(), electedMayor.getUat(),
                electedMayor.getCounty());
            e.printStackTrace();
            return null;
        }
    }

    private static void setPositionHeldForMayor(Entity communeEnt, Entity newMayor) throws WikibaseException, IOException
    {
        Entity positionEnt = communeEnt.getBestClaims(WD_PROP_HEAD_OF_GOVT_POSITION).stream().findFirst().map(Claim::getValue).map(Item.class::cast).map(Item::getEnt).orElse(null);
        Claim positionHeldClaim = new Claim();
        positionHeldClaim.setProperty(WD_PROP_POSITION_HELD);
        positionHeldClaim.setValue(new Item(positionEnt));
        String posHeldClaimId = DWIKI.addClaim(newMayor.getId(), positionHeldClaim);
        DWIKI.addQualifier(posHeldClaimId, "P580", FIRST_NOV_2024);
        DWIKI.addQualifier(posHeldClaimId, "P2715", ELECTION_CLAIM.getValue());
    }

    private static List<Snak> createRef(String countyAbbr)
    {
        List<Snak> retRef = new ArrayList<>();
        try
        {
            Snak urlSnak = new Snak(new URLData(new URI(String.format("https://prezenta.roaep.ro/locale09062024/data/json/sicpv/pv/pv_%s_final.json", countyAbbr.toLowerCase()))),
                new Property("P854"));
            urlSnak.setDatatype("url");
            retRef.add(urlSnak);
            Snak titleSnak = new Snak(new LanguageString("ro", "Rezultatele alegerilor locale din 2024"), new Property("P1476"));
            titleSnak.setDatatype("monolingualtext");
            retRef.add(titleSnak);
            Snak becSnak = new Snak(new Item(new Entity(WD_ENT_ID_PERM_ELECT_AUTH)), new Property("P123"));
            becSnak.setDatatype("wikibase-item");
            retRef.add(becSnak);
        }
        catch (URISyntaxException e)
        {
            e.printStackTrace();
        }
        return retRef;
    }

    private static void updateMayorReelected(Mayor electedMayor, Entity communeEnt, Optional<Claim> crtHeadOfGovtOptClaim) throws IOException, WikibaseException
    {
        if (!crtHeadOfGovtOptClaim.isPresent())
        {
            System.out.println("   crt head of government not found !!! Not possible!!!!");
            return;
        }
        Claim crtHeadOfGovtClaim = crtHeadOfGovtOptClaim.get();
        Set<Snak> startTimeQuals = crtHeadOfGovtClaim.getQualifiers().get(WD_PROP_START_TIME);
        if (startTimeQuals.stream().map(s -> s.getData()).map(Time.class::cast).anyMatch(t -> t.getYear() == 2024l))
        {
            System.out.printf("   mayor already updated for %s!%n", communeEnt.getLabels().get("ro"));
            return;
        }
        Entity crtMayorEnt = WD_ENT_CACHE.get(((Item) crtHeadOfGovtClaim.getValue()).getEnt());
        Optional<Entity> countyEntOpt = communeEnt.getBestClaims(WD_PROP_UAT).stream().findFirst().map(Claim::getValue).map(Item.class::cast).map(Item::getEnt);
        String countyAbbr = countyEntOpt.isPresent()
            ? WD_ENT_CACHE.get(countyEntOpt.get()).getBestClaims(WikibasePropertyFactory.getWikibaseProperty("P395")).stream().findFirst().map(Claim::getValue)
                .map(StringData.class::cast).map(StringData::getValue).get()
                : null;
        List<Snak> ref = createRef(countyAbbr);
        crtMayorEnt = updatePartyForMayor(electedMayor, crtMayorEnt, ref);

        //add new reference to old mayor; add "elected in" (P2715) if not already present
        Entity positionEnt = communeEnt.getBestClaims(WD_PROP_HEAD_OF_GOVT_POSITION).stream().findFirst().map(Claim::getValue).map(Item.class::cast).map(Item::getEnt).orElse(null);
        Optional<Claim> latestMayorPositionOpt = crtMayorEnt.getClaims(WD_PROP_POSITION_HELD).stream()
            .filter(c -> ((Item) c.getValue()).getEnt().getId().equals(positionEnt.getId())).filter(c -> !c.getQualifiers().containsKey(WD_PROP_END_TIME))
            .filter(c -> c.getQualifiers().containsKey(WD_PROP_START_TIME))
            .filter(c -> c.getQualifiers().get(WD_PROP_START_TIME).stream().anyMatch(q -> ((Time) q.getData()).getYear() < 2024)).findFirst();
        if (latestMayorPositionOpt.isPresent())
        {
            Claim latestMayorPosition = latestMayorPositionOpt.get();
            latestMayorPosition.addQualifier(WD_PROP_END_TIME, FIRST_NOV_2024);
            latestMayorPosition.setRank(Rank.NORMAL);
            DWIKI.editClaim(latestMayorPosition);
        }
        Optional<Claim> alreadySetupNewPositionOpt = crtMayorEnt.getClaims(WD_PROP_POSITION_HELD).stream()
            .filter(c -> ((Item) c.getValue()).getEnt().getId().equals(positionEnt.getId())).filter(c -> !c.getQualifiers().containsKey(WD_PROP_END_TIME))
            .filter(c -> c.getQualifiers().containsKey(WD_PROP_START_TIME))
            .filter(c -> c.getQualifiers().get(WD_PROP_START_TIME).stream().anyMatch(q -> ((Time) q.getData()).getYear() == 2024)).findFirst();
        if (!alreadySetupNewPositionOpt.isPresent())
        {
            Claim newMayorPosition = new Claim();
            newMayorPosition.setProperty(WD_PROP_POSITION_HELD);
            newMayorPosition.setValue(new Item(positionEnt));
            String newMayorPosnClaimId = DWIKI.addClaim(crtMayorEnt.getId(), newMayorPosition);
            newMayorPosition.setId(newMayorPosnClaimId);
            DWIKI.addQualifier(newMayorPosnClaimId, WD_PROP_START_TIME.getId(), FIRST_NOV_2024);
            DWIKI.addQualifier(newMayorPosnClaimId, WD_PROP_ELECTED_IN.getId(), new Item(new Entity(WD_ENT_ID_2024_ELECTIONS)));
            DWIKI.addReference(newMayorPosnClaimId, ref);
            setClaimRankTo(crtMayorEnt, WD_PROP_POSITION_HELD, newMayorPosnClaimId, Rank.PREFERRED);
        }
    }

    private static Entity updatePartyForMayor(Mayor electedMayor, Entity crtMayorEnt, List<Snak> ref) throws IOException, WikibaseException
    {
        Optional<Claim> knownMayorPartyOpt = Optional.ofNullable(crtMayorEnt.getBestClaims(WD_PROP_POLITICAL_PARTY)).map(Collection::stream).orElseGet(Stream::empty)
            .filter(c -> "value".equals(c.getMainsnak().getSnaktype())).findFirst();
        if (knownMayorPartyOpt.isPresent())
        {
            Claim knownMayorParty = knownMayorPartyOpt.get();
            Entity oldParty = WD_ENT_CACHE.get(knownMayorPartyOpt.map(Claim::getValue).map(Item.class::cast).map(Item::getEnt).get());
            String dbPartyId = oldParty.getId();
            if (!Objects.equals(dbPartyId, electedMayor.getPartyQId()))
            {
                System.out.printf("\tswitched party from %s to %s%n", oldParty.getLabels().get("ro"), electedMayor.getPartyName());
                //old party claim: set end time, set rank to normal
                crtMayorEnt = setClaimRankTo(crtMayorEnt, WD_PROP_POLITICAL_PARTY, knownMayorParty.getId(), Rank.NORMAL);
                DWIKI.addQualifier(knownMayorParty.getId(), WD_PROP_END_TIME.getId(), FIRST_NOV_2024);
                //create new party claim with start time and reference with election result

                Claim newMayorParty = new Claim(WD_PROP_POLITICAL_PARTY, new Item(new Entity(electedMayor.getPartyQId())));
                newMayorParty.addQualifier(WD_PROP_START_TIME, FIRST_NOV_2024);
                String newMayorPartyClaimId = DWIKI.addClaim(crtMayorEnt.getId(), newMayorParty);
                crtMayorEnt = setClaimRankTo(crtMayorEnt, WD_PROP_POLITICAL_PARTY, newMayorPartyClaimId, Rank.PREFERRED);
                DWIKI.addQualifier(newMayorPartyClaimId, WD_PROP_START_TIME.getId(), FIRST_NOV_2024);
                DWIKI.addReference(newMayorPartyClaimId, ref);
            }
        }
        else
        {
            Claim newMayorParty = new Claim(WD_PROP_POLITICAL_PARTY, new Item(new Entity(electedMayor.getPartyQId())));
            String mayorPartyClaimId = DWIKI.addClaim(crtMayorEnt.getId(), newMayorParty);
            DWIKI.addReference(mayorPartyClaimId, ref);
        }
        return crtMayorEnt;
    }

    private static boolean nameEquals(String name1, String name2)
    {
        long nullsCnt = Stream.of(name1, name2).filter(Objects::isNull).count();
        if (nullsCnt == 1l)
        {
            return false;
        }
        else if (nullsCnt == 2l)
        {
            return true;
        }

        String[] nameParts1 = name1.split("[\\s\\-]+");
        String[] nameParts2 = name2.split("[\\s\\-]+");
        final List<String> namePartsList1 = Arrays.stream(nameParts1).map(s -> s.replaceAll("â", "î")).collect(Collectors.toList());
        final List<String> namePartsList2 = Arrays.stream(nameParts2).map(s -> s.replaceAll("â", "î")).collect(Collectors.toList());

        return namePartsList1.stream().allMatch(namePartsList2::contains) || namePartsList2.stream().allMatch(namePartsList1::contains);
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
        appender.setName("consolez");
        appender.setEncoder(ple);
        appender.start();
        
        FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
        fileAppender.setContext(logbackContext);
        fileAppender.setName("filez");
        fileAppender.setEncoder(ple);
        fileAppender.setFile("elections-wiki.log");
        fileAppender.start();
        
        Logger roWikiLog = logbackContext.getLogger("org.wikipedia.ro");
        roWikiLog.setAdditive(false);
        roWikiLog.setLevel(Level.INFO);
        roWikiLog.addAppender(appender);
        
        Logger wikiLog =  logbackContext.getLogger("wiki");
        wikiLog.setAdditive(false);
        wikiLog.setLevel(Level.INFO);
        wikiLog.addAppender(fileAppender);
        
        Logger mongoLog = logbackContext.getLogger("org.mongodb.driver");
        mongoLog.setAdditive(false);
        mongoLog.setLevel(Level.WARN);
        mongoLog.addAppender(appender);

        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();        
    }
}
