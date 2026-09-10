package org.wikipedia.ro.utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.wikibase.Wikibase;
import org.wikibase.WikibaseException;
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
    
    private static Map<Wikibase, Cache<String, IOException>> redirectCaches = new HashMap<>();
    
    public static String getCachedRedirect(final Wikibase wiki, String s) throws IOException {
        if (!redirectCaches.containsKey(wiki)) {
            redirectCaches.put(wiki, new Cache<>(key -> {
                try {
                    // Relogin and retry once if the session dropped, instead of failing outright
                    return wiki.executeWithRelogin(() -> wiki
                        .resolveRedirects(Stream.ofNullable(key).filter(Objects::nonNull).collect(Collectors.toList()))
                        .stream().findFirst().orElse(key));
                } catch (WikibaseException e) {
                    throw new IOException("Failed to resolve redirect for " + key, e);
                }
            }));
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
