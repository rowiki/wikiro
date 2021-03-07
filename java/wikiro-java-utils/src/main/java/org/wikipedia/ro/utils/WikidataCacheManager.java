package org.wikipedia.ro.utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.wikibase.Wikibase;
import org.wikipedia.Wiki;
import org.wikipedia.ro.cache.Cache;
import org.wikipedia.ro.cache.WikidataEntitiesCache;

public class WikidataCacheManager {
    private static WikidataEntitiesCache wikidataCache = null;
    
    public static WikidataEntitiesCache getWikidataEntitiesCache(Wikibase wikidata) {
        if (null == wikidataCache) {
            wikidataCache = new WikidataEntitiesCache(wikidata);
        }
        return wikidataCache;
    }
    
    private static Map<Wiki, Cache<String, IOException>> redirectCaches = new HashMap<>();
    
    public static String getCachedRedirect(final Wiki wiki, String s) throws IOException {
        if (!redirectCaches.containsKey(wiki)) {
            redirectCaches.put(wiki, new Cache<>(key -> StringUtils.defaultString(wiki.resolveRedirect(key), key)));
        }
        return redirectCaches.get(wiki).get(s);
    }

}
