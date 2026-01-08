
// File: src/test/java/org/wikipedia/ro/utils/WikipediaPageCacheTest.java
package org.wikipedia.ro.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.wikipedia.Wiki;

class TestWikipediaPageCache {
    @Mock
    Wiki wiki;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(wiki.getDomain()).thenReturn("testwiki.org");
    }

    @Test
    void testLoadPagesInfoAndCache() throws Exception {
        WikipediaPageCache cache = new WikipediaPageCache();
        List<Map<String, Object>> pageInfo = List.of(
            Map.of("exists", true, "redirect", false),
            Map.of("exists", false, "redirect", false)
        );
        List<String> pageTexts = new ArrayList<>();
        pageTexts.add("This is the text of Page1.");
        pageTexts.add(null);

        when(wiki.getPageInfo(List.of("Page1", "Page2"))).thenReturn(pageInfo);
        when(wiki.getPageText(List.of("Page1", "Page2"))).thenReturn(pageTexts);
        when(wiki.getPageText(List.of("Page1"))).thenReturn(pageTexts.subList(0, 1));
        when(wiki.getPageText(List.of("Page2"))).thenReturn(pageTexts.subList(1, 2));

        // Mock RetryHelper
        try (MockedStatic<RetryHelper> retryHelper = mockStatic(RetryHelper.class)) {
            retryHelper.when(() -> RetryHelper.retry(any(), eq(3))).thenCallRealMethod();

            cache.loadPagesInfo(wiki, "Page1", "Page2");
            WikipediaPageCache.CachedPage p1 = cache.getPage(wiki, "Page1");
            WikipediaPageCache.CachedPage p2 = cache.getPage(wiki, "Page2");

            assertNotNull(p1);
            assertTrue(p1.exists());
            assertFalse(p1.redirects());
            assertNotNull(p2);
            assertFalse(p2.exists());
        }
    }

    @Test
    void testInvalidatePageAndAll() throws Exception {
        WikipediaPageCache cache = new WikipediaPageCache();
        cache.invalidateAll();
        List<Map<String, Object>> pageInfo = List.of(
            Map.of("exists", true, "redirect", false)
        );
        when(wiki.getPageInfo(List.of("Foo"))).thenReturn(pageInfo);
        when(wiki.getPageText(List.of("Foo"))).thenReturn(List.of("Foo text")).thenReturn(List.of("Bar text")).thenReturn(List.of("Foo text"));
        String pageText1 = cache.getPageText(wiki, "Foo");
        
        String pageText2 = cache.getPageText(wiki, "Foo");
        
        cache.invalidatePage(wiki, "Foo");
        String pageText3 = cache.getPageText(wiki, "Foo");

        assertEquals("Foo text", pageText1);
        assertEquals("Foo text", pageText2);
        assertEquals("Bar text", pageText3);
    }

    @Test
    void testInvalidatePagesIf() throws Exception {
        List<Map<String, Object>> pageInfo = List.of(
            Map.of("exists", true, "redirect", false)
        );
        when(wiki.getPageInfo(any())).thenReturn(pageInfo);
        when(wiki.getPageText(List.of("Foo"))).thenReturn(List.of("Foo text")).thenReturn(List.of("Foo text2"));
        when(wiki.getPageText(List.of("Bar"))).thenReturn(List.of("Bar text")).thenReturn(List.of("Bar text2"));
        when(wiki.getPageText(List.of("Bat"))).thenReturn(List.of("Bat text")).thenReturn(List.of("Bat text2"));
        when(wiki.getPageText(List.of("Quux"))).thenReturn(List.of("Quux text")).thenReturn(List.of("Quux text2"));
        WikipediaPageCache cache = new WikipediaPageCache();
        String fooText1 = cache.getPageText(wiki, "Foo");
        String barText1 = cache.getPageText(wiki, "Bar");
        String batText1 = cache.getPageText(wiki, "Bat");
        String quuxText1 = cache.getPageText(wiki, "Quux");
        
        String fooText2 = cache.getPageText(wiki, "Foo");
        String barText2 = cache.getPageText(wiki, "Bar");
        String batText2 = cache.getPageText(wiki, "Bat");
        String quuxText2 = cache.getPageText(wiki, "Quux");
        
        Predicate<String> pred = title -> title.startsWith("Ba");
        cache.invalidatePagesIf(wiki, pred);
        String fooText3 = cache.getPageText(wiki, "Foo");
        String barText3 = cache.getPageText(wiki, "Bar");
        String batText3 = cache.getPageText(wiki, "Bat");
        String quuxText3 = cache.getPageText(wiki, "Quux");

        assertEquals("Foo text", fooText1);
        assertEquals("Bar text", barText1);
        assertEquals("Bat text", batText1);
        assertEquals("Quux text", quuxText1);
        assertEquals("Foo text", fooText2);
        assertEquals("Bar text", barText2);
        assertEquals("Bat text", batText2);
        assertEquals("Quux text", quuxText2);
        assertEquals("Foo text", fooText3);
        assertEquals("Bar text2", barText3);
        assertEquals("Bat text2", batText3);
        assertEquals("Quux text", quuxText3);
    }

    @Test
    void testPageExists() throws Exception {
        WikipediaPageCache cache = new WikipediaPageCache();
        when(wiki.exists(List.of("Page1"))).thenReturn(new boolean[]{true});
        try (MockedStatic<RetryHelper> retryHelper = mockStatic(RetryHelper.class)) {
            retryHelper.when(() -> RetryHelper.retry(any(), eq(3))).thenReturn(new boolean[]{true});
            assertTrue(cache.pageExists(wiki, "Page1"));
        }
    }

    @Test
    void testGetPageText() throws Exception {
        WikipediaPageCache cache = new WikipediaPageCache();
        List<Map<String, Object>> pageInfo = List.of(
            Map.of("exists", true, "redirect", false),
            Map.of("exists", false, "redirect", false)
        );
        
        when(wiki.exists(List.of("Page1"))).thenReturn(new boolean[]{true});
        when(wiki.getPageText(List.of("Page1"))).thenReturn(List.of("Some text"));
        try (MockedStatic<RetryHelper> retryHelper = mockStatic(RetryHelper.class)) {
            retryHelper.when(() -> RetryHelper.retry(any(), eq(3)))
                .thenReturn(pageInfo)
                .thenReturn(List.of("Some text"));

            //act
            String text = cache.getPageText(wiki, "Page1");
            
            //assert
            assertEquals("Some text", text);
        }
    }
}
