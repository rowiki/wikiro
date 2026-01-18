package org.wikipedia.ro.utils;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.security.auth.login.LoginException;

import org.wikipedia.Wiki;

public class WikipediaPageCache {
    private static final Logger LOG = Logger.getLogger(WikipediaPageCache.class.getCanonicalName());
    public static class CachedPage {
        private final String text;
        private final Boolean exist;
        private final Boolean redirect;
        private final String realTitle;
        private final OffsetDateTime cachedAt = OffsetDateTime.now();

        public CachedPage(String text, Boolean exist, Boolean redirect, String realTitle) {
            this.text = text;
            this.exist = exist;
            this.redirect = redirect;
            this.realTitle = realTitle;
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

        public String getRealTitle() {
            return realTitle;
        }

        public OffsetDateTime getCachedAt() {
            return cachedAt;
        }
    }

    private final Map<String, CachedPage> cache = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<String> textsLoaded = new ConcurrentLinkedDeque<>();
    private long cachedTextSize = 0l;
    private long maxCachedTextSize = -1l;

    private static WikipediaPageCache singletonInstance = null;

    public static WikipediaPageCache createInstance() {
            singletonInstance = new WikipediaPageCache();
            String maxSizeStr = System.getProperty("wikipedia.page.cache.maxsize");
            singletonInstance.maxCachedTextSize = parseSizeWithSuffix(maxSizeStr);
            return singletonInstance;
    }
    
    public static WikipediaPageCache getInstance() {
        if (singletonInstance == null) {
            createInstance();
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

    private WikipediaPageCache() {
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

            // Identify and resolve redirects
            List<String> redirectTitles = titlesToLoad.stream()
                .filter(title -> {
                    Map<String, Object> info = pageInfoMap.get(titlesToLoad.indexOf(title));
                    return info != null && Boolean.TRUE.equals(info.get("redirect"));
                })
                .toList();
            
            Map<String, String> redirectMap = redirectTitles.isEmpty() ? Map.of() : 
                RetryHelper.retry(() -> {
                    try {
                        List<String> resolved = wiki.resolveRedirects(redirectTitles);
                        return IntStream.range(0, redirectTitles.size())
                            .boxed()
                            .collect(Collectors.toMap(redirectTitles::get, resolved::get));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, 3);

            for (int titleIdx = 0; titleIdx < titlesToLoad.size(); titleIdx++) {
                String title = titlesToLoad.get(titleIdx);
                String cacheKey = computeCacheKey(wiki, title);
                Map<String, Object> info = pageInfoMap.get(titleIdx);
                Boolean exists = info != null && (Boolean) info.get("exists");
                Boolean redirect = info != null && (Boolean) info.get("redirect");
                String realTitle = Boolean.TRUE.equals(redirect) ? redirectMap.get(title) : null;
                cache.put(cacheKey, new CachedPage(cache.containsKey(cacheKey) ? cache.get(cacheKey).text : null, exists, redirect, realTitle));
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
        return pagesExist(wiki, title)[0];
    }

    public boolean[] pagesExist(Wiki wiki, String... titles) {
        List<String> titlesToLoad = List.of(titles).stream()
            .filter(title -> !cache.containsKey(computeCacheKey(wiki, title)) || cache.get(computeCacheKey(wiki, title)).exist == null)
            .toList();
        
        if (!titlesToLoad.isEmpty()) {
            try {
                boolean[] existsArray = RetryHelper.retry(() -> {
                    try {
                        return wiki.exists(titlesToLoad);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, 3);
                
                for (int idx = 0; idx < titlesToLoad.size(); idx++) {
                    String title = titlesToLoad.get(idx);
                    String cacheKey = computeCacheKey(wiki, title);
                    CachedPage existingCache = cache.get(cacheKey);
                    cache.put(cacheKey, new CachedPage(existingCache != null ? existingCache.text : null, existsArray[idx], existingCache != null ? existingCache.redirect : null, existingCache != null ? existingCache.realTitle : null));
                }
            } catch (Exception e) {
                LOG.log(Level.SEVERE, e, () -> "Pages metainfo failed to load for wiki " + wiki.getDomain());
            }
        }
        
        boolean[] result = new boolean[titles.length];
        for (int i = 0; i < titles.length; i++) {
            String cacheKey = computeCacheKey(wiki, titles[i]);
            CachedPage cachedPage = cache.get(cacheKey);
            result[i] = cachedPage != null && Boolean.TRUE.equals(cachedPage.exist);
        }
        return result;
    }

    private String computeCacheKey(Wiki wiki, String title) {
        String cacheKey = String.format("%s:%s", wiki.getDomain(), title);
        return cacheKey;
    }
    
    public CachedPage getPage(Wiki wiki, String title) {
        return getPages(wiki, title).get(0);
    }
    
    public List<String> getPageTexts(Wiki wiki, String... titles) {
        return getPages(wiki, titles).stream()
            .map(page -> page != null ? page.getText() : null)
            .toList();
    }
    
    public String getPageText(Wiki wiki, String title) {
        return getPageTexts(wiki, title).get(0);
    }
    
    public List<String> getRealTitles(Wiki wiki, String... titles) {
        loadPagesInfo(wiki, titles);
        return List.of(titles).stream()
            .map(title -> {
                String cacheKey = computeCacheKey(wiki, title);
                CachedPage page = cache.get(cacheKey);
                return page != null && page.realTitle != null ? page.realTitle : title;
            })
            .toList();
    }
        
    public String getRealTitle(Wiki wiki, String title) {
        return getRealTitles(wiki, title).get(0);
    }
        
    public List<CachedPage> getPages(Wiki wiki, String... titles) {
        // First, identify which pages need to be loaded
        List<String> titlesToLoad = List.of(titles).stream()
            .filter(title -> {
                String cacheKey = computeCacheKey(wiki, title);
                return !cache.containsKey(cacheKey) || cache.get(cacheKey).text == null;
            })
            .toList();
        
        if (!titlesToLoad.isEmpty()) {
            try {
                // Load page info for all missing pages
                loadPagesInfo(wiki, titlesToLoad.toArray(new String[0]));
                
                // Load page texts for existing pages
                List<String> existingTitles = titlesToLoad.stream()
                    .filter(title -> {
                        String cacheKey = computeCacheKey(wiki, title);
                        CachedPage cachedPage = cache.get(cacheKey);
                        return cachedPage != null && cachedPage.exist == Boolean.TRUE;
                    })
                    .toList();
                
                if (!existingTitles.isEmpty()) {
                    List<String> texts = RetryHelper.retry(() -> {
                        try {
                            return wiki.getPageText(existingTitles);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }, 3);
                    
                    for (int i = 0; i < existingTitles.size(); i++) {
                        String title = existingTitles.get(i);
                        String cacheKey = computeCacheKey(wiki, title);
                        String pageText = texts.get(i);
                        CachedPage cachedPage = cache.get(cacheKey);
                        addTextToCache(cacheKey, new CachedPage(pageText, cachedPage.exist, cachedPage.redirect, cachedPage.realTitle));
                    }
                }
            } catch (TimeoutException e) {
                LOG.log(Level.SEVERE, e, () -> "Pages failed to load for wiki " + wiki.getDomain());
            }
        }
        
        // Return all requested pages from cache
        return List.of(titles).stream()
            .map(title -> cache.get(computeCacheKey(wiki, title)))
            .toList();
    }

    public void invalidatePage(Wiki wiki, String title) {
        CachedPage removedCacheEntry = cache.remove(computeCacheKey(wiki, title));
        if (removedCacheEntry != null && removedCacheEntry.text != null) {
            cachedTextSize -= removedCacheEntry.text.length();
            textsLoaded.remove(computeCacheKey(wiki, title));
        }
    }

    public void invalidateAll() {
        cache.clear();
        cachedTextSize = 0l;
        textsLoaded.clear();
    }
    
    public void invalidatePagesIf(Wiki wiki, Predicate<String> predicate) {
        String prefix = wiki.getDomain() + ":";
        List<String> entriesToRemove = cache.keySet().stream()
            .filter(k -> k.startsWith(prefix) && predicate.test(k.substring(prefix.length())))
            .toList();

        Integer removedSize = entriesToRemove.stream()
            .map(k -> cache.get(k).text.length())
            .reduce(0, Integer::sum);
        entriesToRemove.forEach(cache::remove);
        textsLoaded.removeAll(entriesToRemove);
        cachedTextSize -= removedSize;
    }
    
    public void savePage(Wiki wiki, String title, String text, String editSummary) {
        String cacheKey = computeCacheKey(wiki, title);
        CachedPage cachedPage = cache.get(cacheKey);
        
        OffsetDateTime cachedAt = cachedPage != null ? cachedPage.getCachedAt() : null;
        
        try {
            if (cachedAt != null) {
                wiki.edit(title, text, editSummary, cachedAt);
            } else {
                wiki.edit(title, text, editSummary);
            }
        } catch (IOException | LoginException e) {
            throw new RuntimeException("Failed to save page: " + title, e);
        }

        // Renew the cache with the new content
        if (cachedPage != null) {
            addTextToCache(cacheKey,
                    new CachedPage(text, cachedPage.exists(), cachedPage.redirects(), cachedPage.getRealTitle()));
        } else {
            addTextToCache(cacheKey, new CachedPage(text, true, false, null));
        }
    }

    private void addTextToCache(String cacheKey, CachedPage newPage) {
        if (newPage.text != null) {
            textsLoaded.addLast(cacheKey);
            cachedTextSize += newPage.text.length();
            cleanupCacheIfNeeded();
        }
        cache.put(cacheKey, newPage);
    }

    private void cleanupCacheIfNeeded() {
        while (maxCachedTextSize > 0 && cachedTextSize > maxCachedTextSize && !textsLoaded.isEmpty()) {
            String oldestKey = textsLoaded.removeFirst();
            CachedPage removedPage = cache.get(oldestKey);
            if (removedPage != null && removedPage.text != null) {
                cachedTextSize -= removedPage.text.length();
                cache.put(oldestKey, new CachedPage(null, removedPage.exists(), removedPage.redirects(), removedPage.getRealTitle()));
            }
        }
    }
}