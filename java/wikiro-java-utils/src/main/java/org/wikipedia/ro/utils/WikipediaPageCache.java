package org.wikipedia.ro.utils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.wikipedia.Wiki;

public class WikipediaPageCache {
    private static final Logger LOG = Logger.getLogger(WikipediaPageCache.class.getCanonicalName());
    public static class CachedPage {
        private final String text;
        private final boolean exists;

        public CachedPage(String text, boolean exists) {
            this.text = text;
            this.exists = exists;
        }

        public String getText() {
            return text;
        }

        public boolean exists() {
            return exists;
        }
    }

    private final Map<String, CachedPage> cache = new ConcurrentHashMap<>();

    public boolean pageExists(Wiki wiki, String title) {
        String cacheKey = computeCacheKey(wiki, title);
        if (!cache.containsKey(title)) {
            boolean[] exists = null;
            try {
                exists = RetryHelper.retry(() -> {
                    try {
                        return wiki.exists(List.of(title));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, 3);
                cache.put(cacheKey, new CachedPage(null, exists[0]));
            } catch (TimeoutException e) {
                LOG.log(Level.SEVERE, e, () -> "Metainfo of page " + cacheKey + " failed to load");
                return false;
            }
        }
        return cache.get(cacheKey).exists; 
    }

    private String computeCacheKey(Wiki wiki, String title) {
        String cacheKey = String.format("%s:%s", wiki.getDomain(), title);
        return cacheKey;
    }
    
    public CachedPage getPage(Wiki wiki, String title) {
        String cacheKey = computeCacheKey(wiki, title);
        if (cache.containsKey(title) && cache.get(cacheKey).text != null) {
            return cache.get(cacheKey);
        }
        try {
            boolean pageExists = pageExists(wiki, title);
            String pageText = null;
            if (pageExists) {
                List<String> texts = RetryHelper.retry(() -> {
                    try {
                        return wiki.getPageText(List.of(title));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, 3);
                pageText = texts.get(0);
            }
            cache.put(cacheKey, new CachedPage(pageText, pageExists));
        } catch (TimeoutException e) {
            LOG.log(Level.SEVERE, e, () -> "Page " + cacheKey + " failed to load");
        }
        
        return cache.get(title);
    }
    
    public String getPageText(Wiki wiki, String title) {
        CachedPage page = getPage(wiki, title);
        return page != null ? page.getText() : null;
    }

    public void invalidatePage(Wiki wiki, String title) {
        cache.remove(computeCacheKey(wiki, title));
    }

    public void invalidateAll() {
        cache.clear();
    }
    
    public void invalidatePagesIf(Wiki wiki, Predicate<String> predicate) {
        String prefix = wiki.getDomain() + ":";
        cache.keySet().removeIf(key -> 
            key.startsWith(prefix) && predicate.test(key.substring(prefix.length()))
        );
    }
    
}