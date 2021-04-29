package org.wikipedia.ro.java.lister.generators;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.SetUtils;
import org.jooq.lambda.Unchecked;
import org.wikibase.Wikibase;
import org.wikibase.WikibaseException;
import org.wikibase.WikibasePropertyFactory;
import org.wikibase.data.Claim;
import org.wikibase.data.CommonsMedia;
import org.wikibase.data.Entity;
import org.wikibase.data.Item;
import org.wikibase.data.Property;
import org.wikibase.data.Quantity;
import org.wikipedia.ro.cache.WikidataEntitiesCache;
import org.wikipedia.ro.utils.LinkUtils;
import org.wikipedia.ro.utils.TextUtils;

public class SettlementListsGenerator implements WikidataListGenerator {

    private static final Property PROP_INSTANCE_OF = WikibasePropertyFactory.getWikibaseProperty("P31");
    private static final Property PROP_IMG = WikibasePropertyFactory.getWikibaseProperty("P18");
    private static final Property PROP_CONTAINS_SETTL = WikibasePropertyFactory.getWikibaseProperty("P1383");
    private static final Property PROP_POP = WikibasePropertyFactory.getWikibaseProperty("P1082");

    private final static List<String> COUNTRY_IDS = new ArrayList<>();

    private final static String COMMUNE_QUERY_TEMPLATE = "select distinct ?commune ?communeLabel (sample(?img) as ?img) (sample(?flag) as ?flag) (sample(?coA) as ?coA) (sample(?pop) as ?pop) (sample(?area) as ?area)\n"
        + "WHERE {\n" + " ?commune wdt:P31 ?instance.\n" + " VALUES ?instance {wd:Q659103 wd:Q640364 wd:Q16858213}\n"
        + " ?commune wdt:P131 wd:%s.\n" + " OPTIONAL {?commune wdt:P41 ?flag}\n" + " OPTIONAL {?commune wdt:P94 ?coA}\n"
        + " OPTIONAL {?commune wdt:P18 ?img }\n" + " OPTIONAL {?commune wdt:P1082 ?pop}\n"
        + " OPTIONAL {?commune wdt:P2046 ?area}\n"
        + "SERVICE wikibase:label { bd:serviceParam wikibase:language \"ro\" } }\n" + "GROUP BY ?commune ?communeLabel ORDER BY ?communeLabel";

    static {
        COUNTRY_IDS.add("Q218"); // Romania
    }

    private WikidataEntitiesCache cache = null;

    public SettlementListsGenerator(WikidataEntitiesCache cache) {
        super();
        this.cache = cache;
    }
    
    private static final Pattern COMMONS_IMG_PATTERN = Pattern.compile("^http://commons\\.wikimedia\\.org/wiki/Special:FilePath/(.*+)$");

    @Override
    public String generateListContent(Entity wdEntity) {
        String communesQuery = String.format(COMMUNE_QUERY_TEMPLATE, wdEntity.getId());

        StringBuilder communesStr = new StringBuilder();

        communesStr.append("{{#invoke:UATList|displayUATListFromFrame");
        Wikibase wd = cache.getWiki();
        try {
            List<Map<String, Object>> resultSet = wd.query(communesQuery);

            int communeIdx = 1;
            for (Map<String, Object> eachResult: resultSet) {
                Item communeItem = (Item) eachResult.get("commune");
                Entity communeEnt = communeItem.getEnt();
                communeEnt = cache.get(communeEnt);
                
                communesStr.append("\n");
                communesStr.append("\n|uat").append(communeIdx).append("link=").append(LinkUtils.createLinkViaWikidata(communeEnt, null));
                communesStr.append("\n|uat").append(communeIdx).append("name=").append(eachResult.get("communeLabel"));
                String img = (String) eachResult.get("img");
                if (null != img) {
                    Matcher imgMatcher = COMMONS_IMG_PATTERN.matcher(img);
                    if (imgMatcher.matches()) {
                        communesStr.append("\n|uat").append(communeIdx).append("image=").append(URLDecoder.decode(imgMatcher.group(1), StandardCharsets.UTF_8.name()));
                    }
                }
                String coa = (String) eachResult.get("coA");
                if (null != coa) {
                    Matcher coaMatcher = COMMONS_IMG_PATTERN.matcher(coa);
                    if (coaMatcher.matches()) {
                        communesStr.append("\n|uat").append(communeIdx).append("coa=").append(URLDecoder.decode(coaMatcher.group(1), StandardCharsets.UTF_8.name()));
                    }
                }
                String population = (String) eachResult.get("pop");
                if (null != population) {
                    communesStr.append("\n|uat").append(communeIdx).append("population=").append(population);
                }
                int typeIndex = 0;
                Set<Claim> typesClaims = SetUtils.emptyIfNull(communeEnt.getBestClaims(PROP_INSTANCE_OF));
                for(Claim eachTypeClaim: typesClaims) {
                    Entity eachTypeEntity = cache.get(((Item) eachTypeClaim.getMainsnak().getData()).getEnt());
                    communesStr.append("\n|uat").append(communeIdx).append("type").append(++typeIndex).append('=').append(LinkUtils.createLinkViaWikidata(eachTypeEntity, null));
                }
                communesStr.append("\n");
                
                Set<Claim> containedSettlementClaims = SetUtils.emptyIfNull(communeEnt.getBestClaims(PROP_CONTAINS_SETTL));
                if (!containedSettlementClaims.isEmpty()) {
                    List<Entity> settlementEnts = containedSettlementClaims.stream()
                        .filter(c -> "value".equals(c.getMainsnak().getSnaktype()))
                        .map(Claim::getValue)
                        .map(Item.class::cast)
                        .map(Item::getEnt)
                        .map(Unchecked.function(cache::get))
                        .sorted((e1, e2) -> TextUtils.compareRoStrings(e1.getLabels().get("ro"), e2.getLabels().get("ro")))
                        .collect(Collectors.toList());
                    int settlementIndex = 0;
                    for (Entity settlementEnt: settlementEnts) {
                        communesStr.append("\n");
                        communesStr.append(String.format("\n|uat%dsettlement%dlink=%s", communeIdx, ++settlementIndex, LinkUtils.createLinkViaWikidata(settlementEnt, null)));
                        
                        Stream<Claim> typeOfClaimStream = Optional.ofNullable(settlementEnt.getBestClaims(PROP_INSTANCE_OF)).map(Collection::stream).orElse(Stream.empty());
                        List<String> typeList = typeOfClaimStream
                            .filter(c -> "value".equals(c.getMainsnak().getSnaktype()))
                            .map(Claim::getValue)
                            .map(Item.class::cast)
                            .map(Item::getEnt)
                            .map(Unchecked.function(cache::get))
                            .map(e -> LinkUtils.createLinkViaWikidata(e, null))
                            .collect(Collectors.toList());
                        for (int idx = 0; idx < typeList.size(); idx++) {
                            communesStr.append(String.format("\n|uat%dsettlement%dtype%d=%s", communeIdx, settlementIndex, 1 + idx, typeList.get(idx)));
                        }
                        
                        Optional<String> imgOpt = Optional.ofNullable(settlementEnt.getBestClaims(PROP_IMG)).map(Collection::stream).orElse(Stream.empty())
                            .filter(c -> "value".equals(c.getMainsnak().getSnaktype()))
                            .findFirst()
                            .map(Claim::getValue)
                            .map(CommonsMedia.class::cast)
                            .map(CommonsMedia::getFileName);
                        if (imgOpt.isPresent()) {
                            communesStr.append(String.format("\n|uat%dsettlement%dimage=%s", communeIdx, settlementIndex, imgOpt.get()));
                        }
                        Optional<Double> popOpt = Optional.ofNullable(settlementEnt.getBestClaims(PROP_POP)).map(Collection::stream).orElse(Stream.empty())
                            .filter(c -> "value".equals(c.getMainsnak().getSnaktype()))
                            .findFirst()
                            .map(Claim::getValue)
                            .map(Quantity.class::cast)
                            .map(Quantity::getAmount);
                        if (popOpt.isPresent()) {
                            communesStr.append(String.format("\n|uat%dsettlement%dpopulation=%d", communeIdx, settlementIndex, Math.round(popOpt.get())));
                        }
                           
                    }
                }
                
                communeIdx++;
            }
            //communesStr.append(params);

        } catch (IOException | WikibaseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            communesStr.append("}}");
        }

        return communesStr.toString();
    }

}
