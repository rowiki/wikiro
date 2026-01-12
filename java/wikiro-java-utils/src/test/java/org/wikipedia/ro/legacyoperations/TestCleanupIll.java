package org.wikipedia.ro.legacyoperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.wikibase.Wikibase;
import org.wikibase.data.Entity;
import org.wikibase.data.Sitelink;
import org.wikipedia.Wiki;
import org.wikipedia.ro.cache.WikidataEntitiesCache;
import org.wikipedia.ro.utils.WikidataCacheManager;
import org.wikipedia.ro.utils.WikipediaPageCache;

class TestCleanupIll {
    private Wiki targetWiki;
    private Wiki sourceWiki;
    private Wikibase dataWiki;
    private WikipediaPageCache pageCache;

    @BeforeEach
    void setUp() {
        targetWiki = mock(Wiki.class, "target=ro");
        sourceWiki = mock(Wiki.class, "source=fr");
        dataWiki = mock(Wikibase.class, "wikidata");
        pageCache = mock(WikipediaPageCache.class);

        when(targetWiki.getDomain()).thenReturn("ro.wikipedia.org");
        when(sourceWiki.getDomain()).thenReturn("fr.wikipedia.org");
    }
    
    @Test
    void testExecuteWithInitialText_IllWdTemplateWithoutLabelGetsLabelFromWikidata() throws Exception {
        String pageText = "Before {{Ill-wd|Q123}} After";
        Entity wdEntity = mock(Entity.class);
        Sitelink sitelink = new Sitelink("rowiki", "Paris (Franța)");
        when(wdEntity.getSitelinks()).thenReturn(Map.of("rowiki", sitelink));
        when(wdEntity.getLabels()).thenReturn(Map.of("ro", "Paris"));

        try (MockedStatic<WikipediaPageCache> cacheMock = mockStatic(WikipediaPageCache.class);
             MockedStatic<WikidataCacheManager> wdMock = mockStatic(WikidataCacheManager.class)) {
            cacheMock.when(WikipediaPageCache::getInstance).thenReturn(pageCache);
            WikidataEntitiesCache wec = mock(WikidataEntitiesCache.class);
            when(wec.get("Q123")).thenReturn(wdEntity);

            wdMock.when(() -> WikidataCacheManager.getWikidataEntitiesCache(dataWiki)).thenReturn(wec);
            wdMock.when(() -> WikidataCacheManager.getCachedRedirect(dataWiki, "Q123")).thenReturn("Q123");
            wdMock.when(() -> WikidataCacheManager.getWikidataEntitiesCache(dataWiki)).thenReturn(wec);

            CleanupIll op = new CleanupIll(targetWiki, sourceWiki, dataWiki, "Test");
            String result = op.executeWithInitialText(pageText);

            assertEquals("Before [[Paris (Franța)|Paris]] After", result);
        }
    }

    @Test
    void testExecuteWithInitialText_IllWdTemplateReplacedWithWikiLink() throws Exception {
        String pageText = "Before {{Ill-wd|Q123|Paris|Paris label}} After";
        Entity wdEntity = mock(Entity.class);
        Sitelink sitelink = new Sitelink("rowiki", "Paris");
        when(wdEntity.getSitelinks()).thenReturn(Map.of("rowiki", sitelink));

        try (MockedStatic<WikipediaPageCache> cacheMock = mockStatic(WikipediaPageCache.class);
             MockedStatic<WikidataCacheManager> wdMock = mockStatic(WikidataCacheManager.class)) {
            cacheMock.when(WikipediaPageCache::getInstance).thenReturn(pageCache);
            WikidataEntitiesCache wec = mock(WikidataEntitiesCache.class);
            when(wec.get("Q123")).thenReturn(wdEntity);

            wdMock.when(() -> WikidataCacheManager.getWikidataEntitiesCache(dataWiki)).thenReturn(wec);
            wdMock.when(() -> WikidataCacheManager.getCachedRedirect(dataWiki, "Q123")).thenReturn("Q123");
            wdMock.when(() -> WikidataCacheManager.getWikidataEntitiesCache(dataWiki)).thenReturn(wec);

            CleanupIll op = new CleanupIll(targetWiki, sourceWiki, dataWiki, "Test");
            String result = op.executeWithInitialText(pageText);

            assertEquals("Before [[Paris|Paris label]] After", result);
        }
    }
    
    @Test
    void testExecuteWithInitialText_IllWdTemplatePreserved() throws Exception {
        String pageText = "Before {{Ill-wd|Q123|Paris|Paris label}} After";
        Entity wdEntity = mock(Entity.class);
        Sitelink sitelink = new Sitelink("frwiki", "Paris");
        when(wdEntity.getSitelinks()).thenReturn(Map.of("frwiki", sitelink));

        try (MockedStatic<WikipediaPageCache> cacheMock = mockStatic(WikipediaPageCache.class);
             MockedStatic<WikidataCacheManager> wdMock = mockStatic(WikidataCacheManager.class)) {
            cacheMock.when(WikipediaPageCache::getInstance).thenReturn(pageCache);
            WikidataEntitiesCache wec = mock(WikidataEntitiesCache.class);
            when(wec.get("Q123")).thenReturn(wdEntity);
            
            wdMock.when(() -> WikidataCacheManager.getWikidataEntitiesCache(dataWiki)).thenReturn(wec);
            wdMock.when(() -> WikidataCacheManager.getCachedRedirect(dataWiki, "Q123")).thenReturn("Q123");
            wdMock.when(() -> WikidataCacheManager.getWikidataEntitiesCache(dataWiki)).thenReturn(wec);

            CleanupIll op = new CleanupIll(targetWiki, sourceWiki, dataWiki, "Test");
            String result = op.executeWithInitialText(pageText);

            assertEquals("Before {{Ill-wd|Q123|Paris|Paris label}} After", result);
        }
    }
    
    @Test
    void testExecuteWithInitialText_IllTemplateWithExistingTargetPageWithDifferentName() throws Exception {
        String pageText = "Before {{Ill|fr|Paris|Paris|Paris label}} After";
        Entity wdEntity = mock(Entity.class);
        Sitelink sitelink = new Sitelink("rowiki", "Paris (Franța)");
        when(wdEntity.getSitelinks()).thenReturn(Map.of("rowiki", sitelink));

        try (MockedStatic<WikipediaPageCache> cacheMock = mockStatic(WikipediaPageCache.class);
             MockedStatic<WikidataCacheManager> wdMock = mockStatic(WikidataCacheManager.class);
             MockedStatic<Wiki> wikiMock = mockStatic(Wiki.class)) {
            when(pageCache.pageExists(targetWiki, "Paris")).thenReturn(false);
            when(pageCache.pageExists(sourceWiki, "Paris")).thenReturn(true);
            cacheMock.when(WikipediaPageCache::getInstance).thenReturn(pageCache);

            wikiMock.when(() -> Wiki.newSession("fr.wikipedia.org")).thenReturn(sourceWiki);
            
            WikidataEntitiesCache wec = mock(WikidataEntitiesCache.class);
            when(wec.getByArticle("frwiki", "Paris")).thenReturn(wdEntity);
            
            wdMock.when(() -> WikidataCacheManager.getWikidataEntitiesCache(dataWiki)).thenReturn(wec);
            wdMock.when(() -> WikidataCacheManager.getCachedRedirect(targetWiki, "Paris")).thenReturn("Paris");
            wdMock.when(() -> WikidataCacheManager.getCachedRedirect(sourceWiki, "Paris")).thenReturn("Paris");

            CleanupIll op = new CleanupIll(targetWiki, sourceWiki, dataWiki, "Test");
            String result = op.executeWithInitialText(pageText);

            assertEquals("Before [[Paris (Franța)|Paris label]] After", result);
        }
    }

    @Test
    void testExecuteWithInitialText_IllTemplateWithExistingTargetPage() throws Exception {
        String pageText = "Before {{Ill|fr|Paris|Paris|Paris label}} After";

        try (MockedStatic<WikipediaPageCache> cacheMock = mockStatic(WikipediaPageCache.class);
             MockedStatic<WikidataCacheManager> wdMock = mockStatic(WikidataCacheManager.class)) {
            when(pageCache.pageExists(eq(targetWiki), eq("Paris"))).thenReturn(true);
            cacheMock.when(WikipediaPageCache::getInstance).thenReturn(pageCache);
            
            wdMock.when(() -> WikidataCacheManager.getCachedRedirect(targetWiki, "Paris")).thenReturn("Paris");
            wdMock.when(() -> WikidataCacheManager.getCachedRedirect(sourceWiki, "Paris")).thenReturn("Paris");

            CleanupIll op = new CleanupIll(targetWiki, sourceWiki, dataWiki, "Test");
            String result = op.executeWithInitialText(pageText);

            assertEquals("Before [[Paris|Paris label]] After", result);
        }
    }

    @Test
    void testExecuteWithInitialText_IllTemplateWithNonExistingTargetPage() throws Exception {
        String pageText = "Before {{Ill|fr|Paris|Paris|Paris label}} After";
        try (MockedStatic<WikipediaPageCache> cacheMock = mockStatic(WikipediaPageCache.class);
                MockedStatic<WikidataCacheManager> wdMock = mockStatic(WikidataCacheManager.class)) {
            cacheMock.when(WikipediaPageCache::getInstance).thenReturn(pageCache);
            when(pageCache.pageExists(targetWiki, "Paris")).thenReturn(false);
            Entity wdEntity = mock(Entity.class);
            Sitelink sitelink = new Sitelink("frwiki", "Paris");
            when(wdEntity.getSitelinks()).thenReturn(Map.of("frwiki", sitelink));

            WikidataEntitiesCache wec = mock(WikidataEntitiesCache.class);
            when(wec.getByArticle("frwiki", "Paris")).thenReturn(wdEntity);
            
            wdMock.when(() -> WikidataCacheManager.getWikidataEntitiesCache(dataWiki)).thenReturn(wec);
            wdMock.when(() -> WikidataCacheManager.getCachedRedirect(targetWiki, "Paris")).thenReturn("Paris");

            CleanupIll op = new CleanupIll(targetWiki, sourceWiki, dataWiki, "Test");
            String result = op.executeWithInitialText(pageText);

            assertTrue(result.contains("Before {{Ill|fr|Paris|Paris|Paris label}} After"));
        }
    }

    @Test
    void testExecuteWithInitialText_NoIllTemplate_NoChange() throws Exception {
        String pageText = "This is a page without Ill template.";
        try (MockedStatic<WikipediaPageCache> cacheMock = mockStatic(WikipediaPageCache.class)) {
            cacheMock.when(WikipediaPageCache::getInstance).thenReturn(pageCache);

            CleanupIll op = new CleanupIll(targetWiki, sourceWiki, dataWiki, "Test");
            String result = op.executeWithInitialText(pageText);

            assertEquals(pageText, result);
        }
    }
}
