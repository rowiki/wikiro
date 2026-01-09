package org.wikipedia.ro.legacyoperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.wikibase.Wikibase;
import org.wikibase.WikibaseException;
import org.wikibase.data.Entity;
import org.wikibase.data.Sitelink;
import org.wikipedia.Wiki;
import org.wikipedia.ro.cache.WikidataEntitiesCache;
import org.wikipedia.ro.utils.WikidataCacheManager;
import org.wikipedia.ro.utils.WikipediaPageCache;

class TestReplaceCrossLinkWithIll {
    private Wiki targetWiki;
    private Wiki sourceWiki;
    private Wikibase dataWiki;
    private WikipediaPageCache pageCache;

    @BeforeEach
    void setUp() {
        targetWiki = mock(Wiki.class);
        sourceWiki = mock(Wiki.class);
        dataWiki = mock(Wikibase.class);
        pageCache = mock(WikipediaPageCache.class);
        // Static singleton
        try (MockedStatic<WikipediaPageCache> cacheMock = mockStatic(WikipediaPageCache.class)) {
            cacheMock.when(WikipediaPageCache::getInstance).thenReturn(pageCache);
        }
    }

    @Test
    void testNoLinks() throws Exception {
        ReplaceCrossLinkWithIll op = new ReplaceCrossLinkWithIll(targetWiki, sourceWiki, dataWiki, "Test");
        String result = op.executeWithInitialText("This is a plain text.");
        assertEquals("This is a plain text.", result);
    }

    @Test
    void testCrossWikiLinkReplacedWithIll() throws Exception {
        ReplaceCrossLinkWithIll op = new ReplaceCrossLinkWithIll(targetWiki, sourceWiki, dataWiki, "Test");
        String text = "Some [[fr:Paris|Paris in French]] text.";
        when(sourceWiki.getDomain()).thenReturn("fr.wikipedia.org");
        when(targetWiki.getDomain()).thenReturn("ro.wikipedia.org");
        when(sourceWiki.resolveRedirects(anyList())).thenReturn(Collections.singletonList("Paris"));
        when(targetWiki.resolveRedirects(anyList())).thenReturn(Collections.singletonList("Paris"));
        when(targetWiki.exists(anyList())).thenReturn(new boolean[]{false});
        when(sourceWiki.exists(anyList())).thenReturn(new boolean[]{true});
        // Mock WikidataCacheManager
        try (MockedStatic<WikidataCacheManager> wdMock = mockStatic(WikidataCacheManager.class)) {
            wdMock.when(() -> WikidataCacheManager.getWikidataEntitiesCache(any())).thenReturn(mock(WikidataEntitiesCache.class));
            wdMock.when(() -> WikidataCacheManager.getCachedRedirect(any(Wiki.class), anyString())).thenReturn("Paris");
            wdMock.when(() -> WikidataCacheManager.getCachedRedirect(any(Wikibase.class), anyString())).thenReturn("Paris");
            wdMock.when(() -> WikidataCacheManager.getCachedRedirect(any(Wiki.class), anyString())).thenReturn("Paris");
            wdMock.when(() -> WikidataCacheManager.getCachedRedirect(any(Wikibase.class), anyString())).thenReturn("Paris");
            String result = op.executeWithInitialText(text);
            assertTrue(result.contains("{{Ill"));
        }
    }

    @Test
    void testLocalLinkExists() throws Exception {
        ReplaceCrossLinkWithIll op = new ReplaceCrossLinkWithIll(targetWiki, sourceWiki, dataWiki, "Test");
        String text = "Some [[Bucharest|București]] text.";
        when(targetWiki.getDomain()).thenReturn("ro.wikipedia.org");
        when(targetWiki.resolveRedirects(anyList())).thenReturn(Collections.singletonList("Bucharest"));
        when(targetWiki.exists(anyList())).thenReturn(new boolean[]{true});
        String result = op.executeWithInitialText(text);
        assertTrue(result.contains("[[Bucharest|București]]"));
    }

    @Test
    void testLocalLinkDoesNotExistAndForeignLinkDoesNotExist() throws Exception {
        ReplaceCrossLinkWithIll op = new ReplaceCrossLinkWithIll(targetWiki, sourceWiki, dataWiki, "Test");
        String text = "Some [[NonexistentPage|Label]] text.";
        when(targetWiki.getDomain()).thenReturn("ro.wikipedia.org");
        when(sourceWiki.getDomain()).thenReturn("fr.wikipedia.org");
        when(targetWiki.resolveRedirects(anyList())).thenReturn(Collections.singletonList("NonexistentPage"));
        when(targetWiki.exists(anyList())).thenReturn(new boolean[]{false});
        when(sourceWiki.resolveRedirects(anyList())).thenReturn(Collections.singletonList("NonexistentPage"));
        when(sourceWiki.exists(anyList())).thenReturn(new boolean[]{false});
        String result = op.executeWithInitialText(text);
        assertTrue(text.equals(result));
    }

    @Test
    void testCrossWikiLinkReplacedWithIllAndWikidataItem() throws Exception
    {
        String text = "Some [[Paris, France|Paris in French]] text.";
        when(sourceWiki.getDomain()).thenReturn("fr.wikipedia.org");
        when(targetWiki.getDomain()).thenReturn("ro.wikipedia.org");
        when(sourceWiki.resolveRedirects(anyList())).thenReturn(Collections.singletonList("Paris, France"));
        when(targetWiki.resolveRedirects(anyList())).thenReturn(Collections.singletonList("Paris, France"));
        when(targetWiki.exists(anyList())).thenReturn(new boolean[] { false });
        when(sourceWiki.exists(anyList())).thenReturn(new boolean[] { true });
        // Mock WikidataCacheManager
        try (MockedStatic<WikidataCacheManager> wdMock = mockStatic(WikidataCacheManager.class))
        {
            wdMock.when(() -> WikidataCacheManager.getWikidataEntitiesCache(any())).thenReturn(mock(WikidataEntitiesCache.class));
            wdMock.when(() -> WikidataCacheManager.getCachedRedirect(any(Wiki.class), anyString())).thenReturn("Paris, France");
            wdMock.when(() -> WikidataCacheManager.getCachedRedirect(any(Wikibase.class), anyString())).thenReturn("Paris, France");
            // Mock Wikibase to return a Q-number
            Entity mockParisEntity = mock(Entity.class);
            when(mockParisEntity.getId()).thenReturn("Q90");
            when(dataWiki.getWikibaseItemBySiteAndTitle("frwiki", "Paris, France")).thenReturn(mockParisEntity);
            
            // act
            ReplaceCrossLinkWithIll op = new ReplaceCrossLinkWithIll(targetWiki, sourceWiki, dataWiki, "Test");
            String result = op.executeWithInitialText(text);
            
            //assert
            assertTrue(result.contains("{{Ill-wd"));
            assertTrue(result.contains("Q90"));
        }
    }

    @Test
    void testCrossWikiLinkReplacedWithTargetRedirect() throws Exception
    {
        String text = "Some [[Paris, France|Paris in French]] text.";
        when(sourceWiki.getDomain()).thenReturn("fr.wikipedia.org");
        when(targetWiki.getDomain()).thenReturn("ro.wikipedia.org");
        when(sourceWiki.resolveRedirects(anyList())).thenReturn(Collections.singletonList("Paris, France"));
        when(targetWiki.resolveRedirects(anyList())).thenReturn(Collections.singletonList("Paris, France"));
        when(targetWiki.exists(anyList())).thenReturn(new boolean[] { false });
        when(sourceWiki.exists(anyList())).thenReturn(new boolean[] { true });

        try (MockedStatic<WikidataCacheManager> wdMock = mockStatic(WikidataCacheManager.class))
        {
            wdMock.when(() -> WikidataCacheManager.getWikidataEntitiesCache(any())).thenReturn(mock(WikidataEntitiesCache.class));
            wdMock.when(() -> WikidataCacheManager.getCachedRedirect(any(Wiki.class), anyString())).thenReturn("Paris, France");
            wdMock.when(() -> WikidataCacheManager.getCachedRedirect(any(Wikibase.class), anyString())).thenReturn("Paris, France");

            // Mock Wikibase to return a Q-number
            Entity mockParisEntity = mock(Entity.class);
            when(mockParisEntity.getId()).thenReturn("Q90");
            when(mockParisEntity.getSitelinks()).thenReturn(Map.of("rowiki", new Sitelink("rowiki", "Paris (Franța)")));
            when(dataWiki.getWikibaseItemBySiteAndTitle("frwiki", "Paris, France")).thenReturn(mockParisEntity);
            
            ReplaceCrossLinkWithIll op = new ReplaceCrossLinkWithIll(targetWiki, sourceWiki, dataWiki, "Test");
            String result = op.executeWithInitialText(text);

            assertTrue(result.contains("[[Paris (Franța)|Paris in French]]"));
        }
    }

}
