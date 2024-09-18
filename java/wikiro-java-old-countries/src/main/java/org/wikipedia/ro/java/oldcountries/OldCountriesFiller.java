package org.wikipedia.ro.java.oldcountries;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.wikibase.WikibaseException;
import org.wikibase.WikibasePropertyFactory;
import org.wikibase.data.Claim;
import org.wikibase.data.Entity;
import org.wikibase.data.Item;
import org.wikibase.data.Time;
import org.wikibase.data.WikibaseData;
import org.wikipedia.ro.cache.WikidataEntitiesCache;
import org.wikipedia.ro.java.oldcountries.data.CountryPeriod;
import org.wikipedia.ro.java.oldcountries.data.HistoricalRegion;
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
    @Override
    protected void init() throws FailedLoginException, IOException
    {
        initLogging();
        super.init();
        cache = new WikidataEntitiesCache(dwiki);
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
                System.out.printf("COMMUNE: %s (%s) COUNTY: %s%n", commune.getName(), commune.getWdId(), commune.getParent().getName());
                for (Settlement settlement : commune.getSettlements())
                {
                    System.out.printf("   SETTLEMENT: %s (%s)%n", settlement.getName(), settlement.getWdId());
                    //    figure out historical region
                    HistoricalRegion reg = findRegionForSettlement(settlement);
                    if (null == reg) {
                        System.err.printf("No region found for settlement %s (%s)%n", settlement.getName(), settlement.getWdId());
                        continue;
                    }
                    
                    //    get historical region countries
                    List<CountryPeriod> countries = reg.getCountries();
                    //    merge historical regions with wikidata
                }
            }
            
            UAT uat = new UAT();
            uat.setName(uatEntity.getLabels().get("ro"));
        }        
        // for each entity
    }


    private HistoricalRegion findRegionForSettlement(Settlement settlement)
    {
        // TODO Auto-generated method stub
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
