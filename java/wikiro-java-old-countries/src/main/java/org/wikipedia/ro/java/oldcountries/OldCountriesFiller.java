package org.wikipedia.ro.java.oldcountries;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.wikibase.WikibaseException;
import org.wikibase.WikibasePropertyFactory;
import org.wikibase.data.Claim;
import org.wikibase.data.Entity;
import org.wikibase.data.Item;
import org.wikibase.data.Snak;
import org.wikibase.data.Time;
import org.wikibase.data.WikibaseData;
import org.wikipedia.ro.cache.WikidataEntitiesCache;
import org.wikipedia.ro.java.oldcountries.data.CountryPeriod;
import org.wikipedia.ro.java.oldcountries.data.HistoricCountry;
import org.wikipedia.ro.java.oldcountries.data.HistoricalRegion;
import org.wikipedia.ro.java.oldcountries.data.InceptedWbObject;
import org.wikipedia.ro.java.oldcountries.data.Operation;
import org.wikipedia.ro.java.oldcountries.data.OperationType;
import org.wikipedia.ro.java.oldcountries.data.Settlement;
import org.wikipedia.ro.java.oldcountries.data.UAT;
import org.wikipedia.ro.utility.AbstractExecutable;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OldCountriesFiller extends AbstractExecutable
{
    private Map<String, HistoricalRegion> REGIONS_MAP = new HashMap<>();
    private static final Pattern Q_PATTERN = Pattern.compile("Q\\d+");
    private static final long YEARS_DIFF_TOLERANCE = 1;
    
    @Override
    protected void init() throws FailedLoginException, IOException
    {
        initLogging();
        super.init();
        cache = new WikidataEntitiesCache(dwiki);
        
        loadRegionsMap();
    }
    
    private void loadRegionsMap()
    {
        URL resourceURL = getClass().getClassLoader().getResource("ROMANIA");
        try
        {
            Path resDirPath = Paths.get(resourceURL.toURI());
            log.info("Loading regions from {}", resDirPath);
            Files.list(resDirPath).forEach(regionPath -> {

                String regName = regionPath.getFileName().toString().replaceAll("(?<!^)[.].*", "");
                HistoricalRegion reg = HistoricalRegion.valueOf(regName);
                
                try
                {
                    Files.lines(regionPath).forEach(line -> {
                        Matcher qMatcher = Q_PATTERN.matcher(line);
                        if (qMatcher.find())
                        {
                            REGIONS_MAP.put(qMatcher.group(), reg);
                        }
                    });
                }
                catch (IOException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            });
        }
        catch (URISyntaxException | IOException e)
        {
            e.printStackTrace();
        }
        
    }

    private WikidataEntitiesCache cache;
    private Map<String, UAT> countyIndex = new HashMap<>();
    //https://query.wikidata.org/#SELECT%20DISTINCT%20%3Fitem%20%3FitemLabel%20%3FcountyLabel%20WHERE%20%7B%0A%20%20VALUES%20%3Frouats%20%7Bwd%3AQ659103%20wd%3AQ16858213%20wd%3AQ640364%7D%0A%20%20%3Fitem%20wdt%3AP31%20%3Frouats.%0A%20%20%3Fitem%20wdt%3AP17%20wd%3AQ218.%0A%20%20%3Fitem%20wdt%3AP131%20%3Fcounty.%0A%20%20%3Fcounty%20wdt%3AP31%20wd%3AQ1776764.%0A%20%20SERVICE%20wikibase%3Alabel%20%7B%0A%20%20%20%20bd%3AserviceParam%20wikibase%3Alanguage%20%22ro%2Cen%22.%0A%20%20%20%20%3Fitem%20rdfs%3Alabel%20%3FitemLabel.%0A%20%20%20%20%3Fcounty%20rdfs%3Alabel%20%3FcountyLabel.%0A%20%20%7D%0A%7D%0AORDER%20BY%20%3FcountyLabel%20%3FitemLabel
    String allUATsQuery = "SELECT DISTINCT ?item ?county ?itemLabel ?countyLabel WHERE {\n"
        + "  VALUES ?rouats {wd:Q659103 wd:Q16858213 wd:Q640364}\n"
        + "  ?item wdt:P31 ?rouats.\n"
        + "  ?item wdt:P17 wd:Q218.\n"
        + "  ?item wdt:P131 ?county.\n"
        + "  ?county wdt:P31 wd:Q1776764.\n"
        + "  SERVICE wikibase:label {\n"
        + "    bd:serviceParam wikibase:language \"ro,en\".\n"
        + "    ?item rdfs:label ?itemLabel.\n"
        + "    ?county rdfs:label ?countyLabel.\n"
        + "  }\n"
        + "}\n"
        + "ORDER BY ?countyLabel ?itemLabel";

    @Override
    protected void execute() throws IOException, WikibaseException, LoginException
    {
        
        List<Map<String, Object>> resultSet = dwiki.query(allUATsQuery);
        for (Map<String, Object> result : resultSet)
        {
            Item uatItem = (Item) result.get("item");
            Entity uatEntity = cache.get(uatItem.getEnt());
            
            Item countyItem = (Item) result.get("county");
            Entity countyEntity = cache.get(countyItem.getEnt());
            UAT parent = countyIndex.computeIfAbsent(countyEntity.getId(), countyId -> new UAT().setName(countyEntity.getLabels().get("ro")).setWdId(countyId));
            UAT commune = new UAT();
            commune.setWdId(uatEntity.getId());
            commune.setName(uatEntity.getLabels().get("ro"));
            commune.setParent(parent);
            
            Set<Claim> settlementsClaims = uatEntity.getClaims(WikibasePropertyFactory.getWikibaseProperty("P1383"));
            
            for (Claim eachSettlementClaim: settlementsClaims) {
                WikibaseData value = eachSettlementClaim.getValue();
                if (value != WikibaseData.UNKNOWN_VALUE && value != WikibaseData.NO_VALUE)
                {
                    Item settlementItem = (Item) value;
                    Entity settlementEntity = cache.get(settlementItem.getEnt());
                    Settlement settlement = new Settlement();
                    settlement.setWdId(settlementEntity.getId());
                    settlement.setName(settlementEntity.getLabels().get("ro"));
                    
                    Set<Claim> inceptionClaims = settlementEntity.getClaims(WikibasePropertyFactory.getWikibaseProperty("P571"));
                    if (null != inceptionClaims && !inceptionClaims.isEmpty()) {
                        inceptionClaims.stream().map(Claim::getValue).filter(iv -> iv != WikibaseData.UNKNOWN_VALUE && iv != WikibaseData.NO_VALUE).findFirst().map(Time.class::cast).ifPresent(inceptionValue -> {
                            settlement.setInception(inceptionValue.getDate());
                        });
                    }
                    settlement.setUat(commune);
                    commune.getSettlements().add(settlement);
                }
            }
            System.out.printf("COMMUNE: %s (%s) COUNTY: %s%n", commune.getName(), commune.getWdId(), commune.getParent().getName());
            setCountriesForEntity(commune, parent);
            for (Settlement settlement : commune.getSettlements())
            {
                System.out.printf("   SETTLEMENT: %s (%s)%n", settlement.getName(), settlement.getWdId());
                //    figure out historical region
                setCountriesForEntity(settlement, commune);
            }
        }        
        // for each entity
    }

    private void setCountriesForEntity(InceptedWbObject settlement, UAT uat) throws IOException, WikibaseException
    {
        HistoricalRegion reg = findRegionByEntityWdIdAndUat(settlement.getWdId(), uat);
        if (null == reg) {
            System.err.printf("No region found for settlement %s (%s)%n", settlement.getName(), settlement.getWdId());
            return;
        }
        //    get historical region countries
        List<CountryPeriod> countries = reg.getCountries();
        //    merge historical regions with wikidata
        
        Entity settlementEntity = cache.get(settlement.getWdId());
        Set<Claim> inceptionClaim = settlementEntity.getClaims(WikibasePropertyFactory.getWikibaseProperty("P571"));
        if (null != inceptionClaim && !inceptionClaim.isEmpty())
        {
            inceptionClaim.stream().map(Claim::getValue).filter(iv -> iv != WikibaseData.UNKNOWN_VALUE && iv != WikibaseData.NO_VALUE).findFirst().map(Time.class::cast)
                .ifPresent(inceptionValue -> {
                    settlement.setInception(inceptionValue.getDate());
                });
        }
        List<Operation> ops = new ArrayList<>();
        
        Set<Claim> countryClaims = settlementEntity.getClaims(WikibasePropertyFactory.getWikibaseProperty("P17"));
        for (Claim eachCountryClaim: countryClaims) {
            if (eachCountryClaim.getValue() == WikibaseData.UNKNOWN_VALUE || eachCountryClaim.getValue() == WikibaseData.NO_VALUE)
            {
                continue;
            }
            Set<Snak> startDates = eachCountryClaim.getQualifiers().get(WikibasePropertyFactory.getWikibaseProperty("P580"));
            Set<Snak> endDates = eachCountryClaim.getQualifiers().get(WikibasePropertyFactory.getWikibaseProperty("P582"));

            Time endDate = null;
            Time startDate = null;
            if (null != startDates && !startDates.isEmpty())
            {
                startDate = (Time) startDates.stream().findFirst().map(Snak::getData).orElse(null);
            }
            if (null != endDates && endDates.isEmpty())
            {
                endDate = (Time) endDates.stream().findFirst().map(Snak::getData).orElse(null);
            }
            Item countryItem = (Item) eachCountryClaim.getValue();
            Entity countryEntity = countryItem.getEnt();
            String countryId = StringUtils.prependIfMissing(countryEntity.getId(), "Q");

            if (countries.stream().map(CountryPeriod::getCountry).map(HistoricCountry::getqId).noneMatch(countryId::equals))
            {
                Operation op = new Operation();
                op.setClaimId(eachCountryClaim.getId());
                op.setOldClaim(eachCountryClaim);
                op.setNewClaim(null);
                op.setType(OperationType.DELETE);
                ops.add(op);
                continue;
            }
            
            //adjust or fill in current existing claims
            for (CountryPeriod countryPeriod : countries)
            {
                if (countryPeriod.getCountry().getqId().equals(countryId))
                {
                    Time periodStartTime = countryPeriod.getStartTime();
                    if (null == startDate && periodStartTime != null)
                    {
                        Operation op = new Operation();
                        op.setType(OperationType.ADD_QUALIFIER);
                        op.setClaimId(eachCountryClaim.getId());
                        op.setOldClaim(eachCountryClaim);
                        op.setQualifierData(periodStartTime);
                        op.setQualifierProperty(WikibasePropertyFactory.getWikibaseProperty("P580"));
                    }
                    if (null == endDate && countryPeriod.getEndTime() != null)
                    {
                        Operation op = new Operation();
                        op.setType(OperationType.ADD_QUALIFIER);
                        op.setClaimId(eachCountryClaim.getId());
                        op.setOldClaim(eachCountryClaim);
                        op.setQualifierData(countryPeriod.getEndTime());
                        op.setQualifierProperty(WikibasePropertyFactory.getWikibaseProperty("P582"));
                    }
                    System.out.printf("---- computing duration between country period start %s to startDate %s%n", Optional.ofNullable(periodStartTime).map(Time::getLocalDateTime).orElse(null), Optional.ofNullable(startDate).map(Time::getLocalDateTime).orElse(null));
                    
                    if (periodStartTime != null && startDate != null
                        && Math.abs(TimeUnit.SECONDS.toDays(Duration.between(periodStartTime.getLocalDateTime(), startDate.getLocalDateTime()).get(ChronoUnit.SECONDS))) <= YEARS_DIFF_TOLERANCE * 365
                        || null == startDate && null == endDate
                        || null == startDate && periodStartTime != null
                        || null == endDate && countryPeriod.getEndTime() != null)
                    {
                        Claim newClaim = countryPeriod.toWikibaseClaim();
                        newClaim.setId(eachCountryClaim.getId());
                        
                        Operation op = new Operation();
                        op.setClaimId(eachCountryClaim.getId());
                        op.setNewClaim(newClaim);
                        op.setOldClaim(eachCountryClaim);
                        op.setType(OperationType.REPLACE);
                        ops.add(op);
                        continue;
                    }
                }
            }
        }
        //add claims that were not present
        for (CountryPeriod countryPeriod: countries)
        {
            if (ops.stream().map(Operation::getNewClaim).filter(Objects::nonNull).map(Claim::getValue).map(Item.class::cast).map(Item::getEnt).map(Entity::getId).noneMatch(countryPeriod.getCountry().getqId()::equals)
                && (null == settlement.getInception() || null == countryPeriod.getEndTime() || settlement.getInception().isBefore(countryPeriod.getEndTime().getDate())
                    ))
            {
                Claim newClaim = countryPeriod.toWikibaseClaim();
                Operation op = new Operation();
                op.setClaimId(null);
                op.setOldClaim(null);
                op.setNewClaim(newClaim);
                op.setType(OperationType.CREATE);
                ops.add(op);
            }
        }
         
        for (Operation op: ops)
        {
            switch (op.getType())
            {
            case CREATE:
                System.out.printf("         CREATE: %s with quals: %s%n", op.getNewClaim(), op.getNewClaim().getQualifiers());
                String claimId = dwiki.addClaim(settlement.getWdId(), op.getNewClaim());
                for (var eachQualEntry: op.getNewClaim().getQualifiers().entrySet()) {
                    for (var eachQualSnak: eachQualEntry.getValue()) {
                        dwiki.addQualifier(claimId, eachQualEntry.getKey().getId(), eachQualSnak.getData());
                    }
                }
                break;
            case REPLACE:
                System.out.printf("         REPLACE CLAIM: %s with %s with quals %s%n", op.getOldClaim().getId(), op.getNewClaim(), op.getNewClaim().getQualifiers());
                dwiki.removeClaim(op.getOldClaim().getId());
                claimId = dwiki.addClaim(settlement.getWdId(), op.getNewClaim());
                for (var eachQualEntry: op.getNewClaim().getQualifiers().entrySet()) {
                    for (var eachQualSnak: eachQualEntry.getValue()) {
                        dwiki.addQualifier(claimId, eachQualEntry.getKey().getId(), eachQualSnak.getData());
                    }
                }
                break;
            case DELETE:
                System.out.printf("         DELETE CLAIM: %s%n", op.getOldClaim());
                break;
            case ADD_QUALIFIER:
                System.out.printf("         EDIT CLAIM: %s ADD QUALIFIER:%s=%s%n", op.getOldClaim(), op.getQualifierProperty(), op.getQualifierData());
            }
        }
    }

    private HistoricalRegion findRegionByEntityWdIdAndUat(String wdId, UAT uat)
    {
        if (REGIONS_MAP.containsKey(wdId)) {
            return REGIONS_MAP.get(wdId);
        }
        do {
            if (REGIONS_MAP.containsKey(uat.getWdId()))
            {
                return REGIONS_MAP.get(uat.getWdId());
            }
            uat = uat.getParent();
        }
        while (null != uat);
        return null;
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
