package org.wikipedia.ro.java.lister.generators;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.wikibase.Wikibase;
import org.wikibase.WikibaseException;
import org.wikibase.data.Entity;
import org.wikibase.data.Item;
import org.wikipedia.ro.cache.WikidataEntitiesCache;

public class SettlementListsGenerator implements WikidataListGenerator {

    private final static List<String> COUNTRY_IDS = new ArrayList<>();

    private final static String COMMUNE_QUERY_TEMPLATE = "select ?commune ?communeLabel ?img ?flag ?coA ?pop ?area\n"
        + "WHERE {\n" + " ?commune wdt:P31 ?instance.\n" + " VALUES ?instance {wd:Q659103 wd:Q640364 wd:Q16858213}\n"
        + " ?commune wdt:P131 wd:%s.\n" + " OPTIONAL {?commune wdt:P41 ?flag}\n" + " OPTIONAL {?commune wdt:P94 ?coA}\n"
        + " OPTIONAL {?commune wdt:P18 ?img }\n" + " OPTIONAL {?commune wdt:P1082 ?pop}\n"
        + " OPTIONAL {?commune wdt:P2046 ?area}\n"
        + "SERVICE wikibase:label { bd:serviceParam wikibase:language \"ro\" } }\n" + "ORDER BY ?communeLabel";

    static {
        COUNTRY_IDS.add("Q218"); // Romania
    }

    private WikidataEntitiesCache cache = null;

    public SettlementListsGenerator(WikidataEntitiesCache cache) {
        super();
        this.cache = cache;
    }

    @Override
    public String generateListContent(Entity wdEntity) {
        String communesQuery = String.format(COMMUNE_QUERY_TEMPLATE, wdEntity.getId());

        StringBuilder communesStr = new StringBuilder();

        communesStr.append("{{#invoke:UATList|displayUATList|");
        Wikibase wd = cache.getWiki();
        try {
            List<Map<String, Object>> resultSet = wd.query(communesQuery);

            String params = resultSet.stream().map(m -> m.get("commune")).map(o -> (Item) o).map(Item::getEnt)
                .map(Entity::getId).collect(Collectors.joining("|"));
            communesStr.append(params);

        } catch (IOException | WikibaseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            communesStr.append("}}");
        }

        return communesStr.toString();
    }

}
