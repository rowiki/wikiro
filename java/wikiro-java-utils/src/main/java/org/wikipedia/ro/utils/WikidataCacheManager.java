package org.wikipedia.ro.utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
            redirectCaches.put(wiki, new Cache<>(key -> wiki.resolveRedirects(Stream.ofNullable(key).filter(Objects::nonNull).collect(Collectors.toList())).stream().findFirst().orElse(key)));
        }
        return redirectCaches.get(wiki).get(s);
    }
    
    /**
     * Clears all caches. This method is intended for testing purposes only.
     */
    public static void clearCaches() {
        wikidataCache = null;
        redirectCaches.clear();
    }

}
