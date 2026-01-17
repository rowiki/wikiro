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
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.wikipedia.Wiki;

class TestWikipediaPageCache {
    @Mock
    Wiki wiki;

    Map<String, String> wikiTexts;
    Map<String, String> wikiRedirects;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        when(wiki.getDomain()).thenReturn("testwiki.org");
        when(wiki.getPageText(any())).thenAnswer(invocation -> {
            List<String> titles = invocation.getArgument(0);
            List<String> texts = new ArrayList<>();
            for (String title : titles) {
                texts.add(wikiTexts.get(title));
            }
            return texts;
        });
        when(wiki.exists(any())).thenAnswer(inv -> {
            List<String> titles = inv.getArgument(0);
            boolean[] result = new boolean[titles.size()];
            for (int i = 0; i < titles.size(); i++) {
                result[i] = wikiTexts.containsKey(titles.get(i)) || wikiRedirects.containsKey(titles.get(i));
            }
            return result;
        });
        when(wiki.resolveRedirects(any())).thenAnswer(inv -> {
            List<String> titles = inv.getArgument(0);
            List<String> redirects = new ArrayList<>();
            for (String title : titles) {
                redirects.add(wikiRedirects.containsKey(title) ? wikiRedirects.get(title) : title);
            }
            return redirects;
        });
        when(wiki.getPageInfo(any())).thenAnswer(invocation -> {
            List<String> titles = invocation.getArgument(0);
            List<Map<String, Object>> infoList = new ArrayList<>();
            for (String title : titles) {
                Map<String, Object> info = new HashMap<>();
                info.put("exists", wikiTexts.containsKey(title) || wikiRedirects.containsKey(title));
                info.put("redirect", wikiRedirects.containsKey(title));
                infoList.add(info);
            }
            return infoList;
        });
    }

    @AfterEach
    void tearDown() {
        reset(wiki);
        wikiTexts = null;
        wikiRedirects = null;
    }

    @Test
    void testLoadPagesInfoAndCache() throws Exception {
        WikipediaPageCache cache = WikipediaPageCache.createInstance();

        wikiTexts = Map.of("Page1", "This is the text of Page1.");
        wikiRedirects = Collections.emptyMap();

        // Mock RetryHelper
        cache.loadPagesInfo(wiki, "Page1", "Page2");
        WikipediaPageCache.CachedPage p1 = cache.getPage(wiki, "Page1");
        WikipediaPageCache.CachedPage p2 = cache.getPage(wiki, "Page2");

        assertNotNull(p1);
        assertTrue(p1.exists());
        assertFalse(p1.redirects());
        assertNotNull(p2);
        assertFalse(p2.exists());
    }

    @Test
    void testInvalidatePageAndAll() throws Exception {
        WikipediaPageCache cache = WikipediaPageCache.createInstance();
        cache.invalidateAll();
        reset(wiki);
        List<Map<String, Object>> pageInfo = List.of(
            Map.of("exists", true, "redirect", false)
        );
        when(wiki.getPageInfo(List.of("Foo"))).thenReturn(pageInfo);
        when(wiki.getPageText(List.of("Foo"))).thenReturn(List.of("Foo text")).thenReturn(List.of("Bar text"));
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
        reset(wiki);
        when(wiki.getPageInfo(any())).thenReturn(pageInfo);
        when(wiki.getPageText(List.of("Foo"))).thenReturn(List.of("Foo text")).thenReturn(List.of("Foo text2"));
        when(wiki.getPageText(List.of("Bar"))).thenReturn(List.of("Bar text")).thenReturn(List.of("Bar text2"));
        when(wiki.getPageText(List.of("Bat"))).thenReturn(List.of("Bat text")).thenReturn(List.of("Bat text2"));
        when(wiki.getPageText(List.of("Quux"))).thenReturn(List.of("Quux text")).thenReturn(List.of("Quux text2"));
        WikipediaPageCache cache = WikipediaPageCache.createInstance();
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
        WikipediaPageCache cache = WikipediaPageCache.createInstance();
        wikiTexts = Map.of("Page1", "This is the text of Page1.");
        wikiRedirects = Collections.emptyMap();
     
        assertTrue(cache.pageExists(wiki, "Page1"));
    }

    @Test
    void testGetPageText() throws Exception {
        WikipediaPageCache cache = WikipediaPageCache.createInstance();
        wikiTexts = Map.of("Page1", "Some text");
        wikiRedirects = Collections.emptyMap();

        //act
        String text = cache.getPageText(wiki, "Page1");
        
        //assert
        assertEquals("Some text", text);
    }

    @Test
    void testGetPage() throws Exception {
        WikipediaPageCache cache = WikipediaPageCache.createInstance();
        String expectedText = "This is the page content";
        wikiTexts = Map.of("TestPage", expectedText);
        wikiRedirects = Collections.emptyMap();
        
        // Act
        WikipediaPageCache.CachedPage cachedPage = cache.getPage(wiki, "TestPage");
        
        // Assert
        assertNotNull(cachedPage);
        assertEquals(expectedText, cachedPage.getText());
        assertTrue(cachedPage.exists());
        assertFalse(cachedPage.redirects());
    }

    @Test
    void testGetRealTitles() throws Exception {
        WikipediaPageCache cache = WikipediaPageCache.createInstance();
        wikiTexts = Map.of("RealPage1", "Content 1", "RealPage2", "Content 2", "NormalPage", "Normal content");
        wikiRedirects = Map.of("RedirectPage1", "RealPage1", "RedirectPage2", "RealPage2");
        
        // Act
        List<String> realTitles = cache.getRealTitles(wiki, "RedirectPage1", "NormalPage", "RedirectPage2");
        
        // Assert
        assertEquals(3, realTitles.size());
        assertEquals("RealPage1", realTitles.get(0));
        assertEquals("NormalPage", realTitles.get(1));
        assertEquals("RealPage2", realTitles.get(2));
    }

    @Test
    void testGetPageTexts() throws Exception {
        WikipediaPageCache cache = WikipediaPageCache.createInstance();
        wikiTexts = Map.of("Page1", "Text 1", "Page2", "Text 2", "Page3", "Text 3");
        wikiRedirects = Collections.emptyMap();
        
        // Act
        List<String> texts = cache.getPageTexts(wiki, "Page1", "Page2", "NonExistent", "Page3");
        
        // Assert
        assertEquals(4, texts.size());
        assertEquals("Text 1", texts.get(0));
        assertEquals("Text 2", texts.get(1));
        assertNull(texts.get(2)); // Non-existent page
        assertEquals("Text 3", texts.get(3));
    }

    
}
