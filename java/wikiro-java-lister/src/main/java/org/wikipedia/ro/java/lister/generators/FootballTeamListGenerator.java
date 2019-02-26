package org.wikipedia.ro.java.lister.generators;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.prependIfMissing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.wikibase.Wikibase;
import org.wikibase.WikibaseException;
import org.wikibase.WikibasePropertyFactory;
import org.wikibase.data.Claim;
import org.wikibase.data.Entity;
import org.wikibase.data.Item;
import org.wikibase.data.Property;
import org.wikibase.data.Sitelink;
import org.wikibase.data.Snak;
import org.wikipedia.ro.java.lister.util.WikidataEntitiesCache;
import org.wikipedia.ro.model.WikiLink;

public class FootballTeamListGenerator implements WikidataListGenerator {
    private WikidataEntitiesCache cache = null;

    public FootballTeamListGenerator(WikidataEntitiesCache cache) {
        super();
        this.cache = cache;
    }

    private static final Map<String, String> POSN_INDEX = new HashMap<String, String>();

    static {
        POSN_INDEX.put("fundaș", "F");
        POSN_INDEX.put("fundaș central", "F");
        POSN_INDEX.put("fundaș lateral", "F");
        POSN_INDEX.put("atacant", "A");
        POSN_INDEX.put("mijlocaș", "M");
        POSN_INDEX.put("mijlocaș defensiv", "M");
        POSN_INDEX.put("mijlocaș ofensiv", "M");
        POSN_INDEX.put("extremă", "M");
        POSN_INDEX.put("portar", "P");
    }

    @Override
    public String generateListContent(Entity wdEntity) {
        String currentPlayersQueryString = "select distinct ?item ?itemLabel ?posnLabel ?endTime " + "{ "
            + "  ?item wdt:P31 wd:Q5 . " + "  ?item p:P54 ?teamStat . "
            + "  ?teamStat wikibase:rank wikibase:PreferredRank . " + "  ?teamStat ps:P54 wd:" + wdEntity.getId() + " . "
            + "  ?item wdt:P413 ?posn . " + "  MINUS {" + "    ?teamStat pq:P582 ?endTime ." + "  }"
            + "  service wikibase:label { bd:serviceParam wikibase:language \"ro,en\" }" + "} order by ?item";

        String contractedPlayersQueryString = "select distinct ?item ?itemLabel ?posnLabel\n" + "{ "
            + "  ?item wdt:P31 wd:Q5 . " + "  ?item p:P54 ?teamStat . " + "  ?teamStat ps:P54 wd:" + wdEntity.getId() + " . "
            + "  ?item wdt:P413 ?posn . " + "  MINUS { " + "    ?teamStat pq:P582 ?endTime . " + "  }"
            + "  service wikibase:label { bd:serviceParam wikibase:language \"ro,en\" } " + "} order by ?item";

        Calendar now = Calendar.getInstance();
        StringBuilder listBuilder = new StringBuilder("{{Updated|{{Dată|").append(now.get(Calendar.YEAR)).append('|')
            .append(1 + now.get(Calendar.MONTH)).append('|').append(now.get(Calendar.DATE)).append("}}}}\n\n")
            .append("{{Ef start}}\n");

        Set<String> presentPlayerIds = new HashSet<>();

        try {
            Wikibase wd = cache.getWiki();
            List<Map<String, Object>> resultSet = wd.query(currentPlayersQueryString);
            int crtIndex = 0;
            int middleIndex = 1 + (-1 + resultSet.size()) / 2;
            for (Map<String, Object> eachResult : resultSet) {
                Item item = (Item) eachResult.get("item");
                String posn = (String) eachResult.get("posnLabel");
                Entity playerEntity = item.getEnt();
                playerEntity = cache.get(playerEntity);

                Claim countryClaim = null;
                for (String propId : Arrays.asList("P1532", "P27")) {
                    Set<Claim> countryClaims =
                        playerEntity.getBestClaims(WikibasePropertyFactory.getWikibaseProperty(propId));
                    if (null != countryClaims && !countryClaims.isEmpty()) {
                        countryClaim = countryClaims.iterator().next();
                        break;
                    }
                }
                presentPlayerIds.add(playerEntity.getId());

                String countryName = "";
                if (null != countryClaim) {
                    Entity countryEntity = ((Item) countryClaim.getMainsnak().getData()).getEnt();
                    countryEntity = cache.get(countryEntity);
                    countryName = defaultString(countryEntity.getLabels().get("ro"), countryEntity.getLabels().get("en"));
                }

                listBuilder.append("{{Ef jucător|nat=").append(countryName).append("|nume=").append(ill(playerEntity))
                    .append("|poz=").append(defaultString(POSN_INDEX.get(posn))).append("}}\n");

                crtIndex++;
                if (crtIndex == middleIndex) {
                    listBuilder.append("{{Ef mijloc}}\n");
                }
            }
            listBuilder.append("{{Ef sfârșit}}\n");

            List<Map<String, Object>> allIncludingLentPlayersResultSet = wd.query(contractedPlayersQueryString);
            List<Map<String, Object>> lentPlayers = new ArrayList<Map<String, Object>>();
            for (Map<String, Object> eachResult : allIncludingLentPlayersResultSet) {
                Item item = (Item) eachResult.get("item");
                Entity playerEntity = item.getEnt();
                if (presentPlayerIds.contains(playerEntity.getId())) {
                    continue;
                }
                lentPlayers.add(eachResult);
            }

            if (!lentPlayers.isEmpty()) {
                List<String> lentPlayersLines = new ArrayList<>();
                lentPlayersLoop: for (Map<String, Object> eachResult : lentPlayers) {
                    Item item = (Item) eachResult.get("item");
                    Entity playerEntity = item.getEnt();
                    String posn = (String) eachResult.get("posnLabel");

                    playerEntity = cache.get(playerEntity);

                    Claim countryClaim = null;
                    for (String propId : Arrays.asList("P1532", "P27")) {
                        Set<Claim> countryClaims =
                            playerEntity.getBestClaims(WikibasePropertyFactory.getWikibaseProperty(propId));
                        if (null != countryClaims && !countryClaims.isEmpty()) {
                            countryClaim = countryClaims.iterator().next();
                            break;
                        }
                    }
                    presentPlayerIds.add(playerEntity.getId());

                    String countryName = "";
                    if (null != countryClaim) {
                        Entity countryEntity = ((Item) countryClaim.getMainsnak().getData()).getEnt();
                        countryEntity = cache.get(countryEntity);
                        countryName =
                            defaultString(countryEntity.getLabels().get("ro"), countryEntity.getLabels().get("en"));
                    }

                    String crtTeamLink = null;
                    Set<Claim> crtTeamClaims =
                        playerEntity.getBestClaims(WikibasePropertyFactory.getWikibaseProperty("P54"));
                    if (null != crtTeamClaims && !crtTeamClaims.isEmpty()) {
                        
                        Claim crtTeamClaim = crtTeamClaims.iterator().next();
                        
                        boolean isLoan = false;
                        Map<Property, Set<Snak>> teamClaimQuals = crtTeamClaim.getQualifiers();
                        Set<Snak> acqTransQuals = teamClaimQuals.get(WikibasePropertyFactory.getWikibaseProperty("P1642"));
                        if (null != acqTransQuals && !acqTransQuals.isEmpty()) {
                            for (Snak eachAcqTransQual: acqTransQuals) {
                                if (StringUtils.equals(prependIfMissing(((Item) eachAcqTransQual.getData()).getEnt().getId(), "Q"), "Q2914547")) {
                                    isLoan = true;
                                }
                            }
                        }
                        if (!isLoan) {
                            continue lentPlayersLoop;
                        }
                        
                        Entity crtTeamEntity = ((Item) crtTeamClaim.getMainsnak().getData()).getEnt();
                        String crtTeamId = prependIfMissing(crtTeamEntity.getId(), "Q");
                        if (StringUtils.equals(crtTeamId, wdEntity.getId())) {
                            continue lentPlayersLoop;
                        }

                        crtTeamEntity = cache.get(crtTeamEntity);

                        Set<Claim> typeClaims =
                            crtTeamEntity.getBestClaims(WikibasePropertyFactory.getWikibaseProperty("P31"));
                        for (Claim eachTeamInstanceOfClaim : typeClaims) {
                            if ("Q2412834".equals(prependIfMissing(
                                ((Item) eachTeamInstanceOfClaim.getMainsnak().getData()).getEnt().getId(), "Q"))) {
                                Set<Claim> partOfClaims =
                                    crtTeamEntity.getBestClaims(WikibasePropertyFactory.getWikibaseProperty("P361"));
                                for (Claim eachTeamPartOfClaim : partOfClaims) {
                                    if (StringUtils.equals(wdEntity.getId(), prependIfMissing(
                                        ((Item) eachTeamPartOfClaim.getMainsnak().getData()).getEnt().getId(), "Q"))) {
                                        continue lentPlayersLoop;
                                    }
                                }
                            }
                        }
                        crtTeamLink = ill(crtTeamEntity);
                    } else {
                        continue lentPlayersLoop;
                    }

                    lentPlayersLines.add(new StringBuilder().append("{{Ef jucător|nat=").append(countryName).append("|nume=")
                        .append(ill(playerEntity)).append("|poz=").append(defaultString(POSN_INDEX.get(posn)))
                        .append("|other=la ").append(defaultString(crtTeamLink)).append("}}").toString());
                }
                if (!lentPlayersLines.isEmpty()) {
                    StringBuilder lentPlayersBuilder = new StringBuilder();
                    lentPlayersBuilder.append("\n=== Jucători împrumutați ===\n{{Ef start}}\n");
                    int lentPlayerIdx = 0;
                    int middleLentPlayers = 1 + (-1 + lentPlayersLines.size() / 2);

                    for (String eachLentPlayerLine : lentPlayersLines) {
                        lentPlayersBuilder.append(eachLentPlayerLine).append('\n');

                        lentPlayerIdx++;
                        if (lentPlayerIdx == middleLentPlayers) {
                            lentPlayersBuilder.append("{{Ef mijloc}}\n");
                        }
                    }
                    lentPlayersBuilder.append("{{Ef sfârșit}}\n");

                    if (0 != lentPlayersBuilder.length()) {
                        listBuilder.append(lentPlayersBuilder);
                    }
                }

            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return "";
        } catch (WikibaseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return "";
        }

        return listBuilder.toString();
    }

    private String ill(Entity entity) {
        if (entity.isLoaded()) {
            Sitelink roSitelink = entity.getSitelinks().get("rowiki");
            if (null != roSitelink) {
                String roArticle = roSitelink.getPageName();
                String label = defaultString(entity.getLabels().get("ro"), entity.getLabels().get("en"));
                WikiLink wikiLink = new WikiLink();
                wikiLink.setLabel(label);
                wikiLink.setTarget(roArticle);
                return wikiLink.toString();
            }
        }
        return String.format("{{Ill-wd|%s}}", prependIfMissing(entity.getId(), "Q"));
    }

}
