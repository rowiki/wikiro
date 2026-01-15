package org.wikipedia.ro.utils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.wikipedia.Wiki;

public class WikipediaPageCache {
    private static final Logger LOG = Logger.getLogger(WikipediaPageCache.class.getCanonicalName());
    public static class CachedPage {
        private final String text;
        private final Boolean exist;
        private final Boolean redirect;

        public CachedPage(String text, Boolean exist, Boolean redirect) {
            this.text = text;
            this.exist = exist;
            this.redirect = redirect;
        }

        public String getText() {
            return text;
        }

        public Boolean exists() {
            return exist;
        }
        
        public Boolean redirects() {
            return redirect;
        }
    }

    private final Map<String, CachedPage> cache = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<String> textsLoaded = new ConcurrentLinkedDeque<>();
    private long cachedTextSize = 0l;
    private long maxCachedTextSize = -1l;

    private static WikipediaPageCache singletonInstance = null;

    
    public static WikipediaPageCache getInstance() {
        if (singletonInstance == null) {
            singletonInstance = new WikipediaPageCache();
            String maxSizeStr = System.getProperty("wikipedia.page.cache.maxsize");
            singletonInstance.maxCachedTextSize = parseSizeWithSuffix(maxSizeStr);
        }
        return singletonInstance;
    }

    private static long parseSizeWithSuffix(String sizeStr)    {
        if (sizeStr == null)        {
            return -1L;
        }
        String trimmed = sizeStr.trim().toUpperCase();
        long multiplier = 1L;
        if (trimmed.endsWith("K"))        {
            multiplier = 1024L;
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        else if (trimmed.endsWith("M"))        {
            multiplier = 1024L * 1024L;
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        else if (trimmed.endsWith("G"))        {
            multiplier = 1024L * 1024L * 1024L;
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        try        {
            return Long.parseLong(trimmed) * multiplier;
        }        catch (NumberFormatException e)        {
            LOG.log(Level.WARNING, e, () -> "Invalid wikipedia.page.cache.maxsize value: " + sizeStr);
            return -1L;
        }
    }
    
    public void loadPagesInfo(Wiki wiki, String... titles) {
        List<String> titlesToLoad = List.of(titles).stream()
            .filter(title -> !cache.containsKey(computeCacheKey(wiki, title)) || cache.get(computeCacheKey(wiki, title)).redirect == null)
            .toList();
        
        if (titlesToLoad.isEmpty()) {
            return;
        }
        try {
            List<Map<String, Object>> pageInfoMap = RetryHelper.retry(() -> {
                try {
                    return wiki.getPageInfo(titlesToLoad);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, 3);

            for (int titleIdx = 0; titleIdx < titlesToLoad.size(); titleIdx++) {
                String cacheKey = computeCacheKey(wiki, titlesToLoad.get(titleIdx));
                Map<String, Object> info = pageInfoMap.get(titleIdx);
                Boolean exists = info != null && (Boolean) info.get("exists");
                Boolean redirect = info != null && (Boolean) info.get("redirect");
                cache.put(cacheKey, new CachedPage(cache.containsKey(cacheKey) ? cache.get(cacheKey).text : null, exists, redirect));
            }
        } catch (TimeoutException e) {
            LOG.log(Level.SEVERE, e, () -> "Pages info failed to load for wiki " + wiki.getDomain());
        }
    }

    public boolean pageRedirects(Wiki wiki, String title) {
        String cacheKey = computeCacheKey(wiki, title);
        if (!cache.containsKey(cacheKey))
        {
            try
            {
                loadPagesInfo(wiki, title);
            }
            catch (Exception e)
            {
                LOG.log(Level.SEVERE, e, () -> "Metainfo of page " + cacheKey + " failed to load");
                return false;
            }
        }
        return cache.get(cacheKey).redirect == Boolean.TRUE;
    }
    
    public boolean pageExists(Wiki wiki, String title) {
        String cacheKey = computeCacheKey(wiki, title);
        if (!cache.containsKey(cacheKey)) {
            boolean[] exists = null;
            try {
                exists = RetryHelper.retry(() -> {
                    try {
                        return wiki.exists(List.of(title));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, 3);
                cache.put(cacheKey, new CachedPage(null, exists[0], null));
            } catch (Exception e) {
                LOG.log(Level.SEVERE, e, () -> "Metainfo of page " + cacheKey + " failed to load");
                return false;
            }
        }
        return cache.get(cacheKey).exist == Boolean.TRUE; 
    }

    private String computeCacheKey(Wiki wiki, String title) {
        String cacheKey = String.format("%s:%s", wiki.getDomain(), title);
        return cacheKey;
    }
    
    public CachedPage getPage(Wiki wiki, String title) {
        String cacheKey = computeCacheKey(wiki, title);
        if (cache.containsKey(cacheKey) && cache.get(cacheKey).text != null) {
            return cache.get(cacheKey);
        }
        try {
            loadPagesInfo(wiki, title);
            String pageText = null;
            CachedPage cachedPage = cache.get(cacheKey);
            if (cachedPage.exist == Boolean.TRUE) {
                List<String> texts = RetryHelper.retry(() -> {
                    try {
                        return wiki.getPageText(List.of(title));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, 3);
                pageText = texts.get(0);
            }
            cache.put(cacheKey, new CachedPage(pageText, cachedPage.exist, cachedPage.redirect));
            textsLoaded.addLast(cacheKey);
            cachedTextSize += pageText != null ? pageText.length() : 0;
            while (maxCachedTextSize > 0 && cachedTextSize > maxCachedTextSize && !textsLoaded.isEmpty())
            {
                String oldestKey = textsLoaded.removeFirst();
                CachedPage removedPage = cache.get(oldestKey);
                if (removedPage != null && removedPage.text != null)
                {
                    cachedTextSize -= removedPage.text.length();
                    cache.put(oldestKey, new CachedPage(null, removedPage.exist, removedPage.redirect));
                }
            }
        } catch (TimeoutException e) {
            LOG.log(Level.SEVERE, e, () -> "Page " + cacheKey + " failed to load");
        }
        
        return cache.get(cacheKey);
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