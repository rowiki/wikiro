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
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.wikibase.Wikibase;
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
        when(targetWiki.getDomain()).thenReturn("ro.wikipedia.org");
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
        when(targetWiki.resolveRedirects(anyList())).thenReturn(Collections.singletonList("Bucharest"));
        when(targetWiki.exists(anyList())).thenReturn(new boolean[]{true});
        String result = op.executeWithInitialText(text);
        assertTrue(result.contains("[[Bucharest|București]]"));
    }

    @Test
    void testLocalLinkDoesNotExistAndForeignLinkDoesNotExist() throws Exception {
        ReplaceCrossLinkWithIll op = new ReplaceCrossLinkWithIll(targetWiki, sourceWiki, dataWiki, "Test");
        String text = "Some [[NonexistentPage|Label]] text.";
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

    static Stream<Arguments> wikipediaExternalLinks() {
        return Stream.of(
            Arguments.of("See [https://en.wikipedia.org/wiki/Paris Paris] for more.", "See {{Ill-wd|Q90|3=Paris}} for more."),
            Arguments.of("See [https://ro.wikipedia.org/wiki/Paris_(Franța) Paris] for more.", "See [[Paris (Franța)|Paris]] for more."),
            Arguments.of("See [https://en.m.wikipedia.org/wiki/Paris Paris] for more.", "See {{Ill-wd|Q90|3=Paris}} for more.")
            );
    }

    @ParameterizedTest
    @MethodSource("wikipediaExternalLinks")
    void testWikipediaExternalLinksTransformed(String input, String expected) throws Exception {
        // Mock Wikibase to return a Q-number
        Entity mockParisEntity = mock(Entity.class);
        when(mockParisEntity.getId()).thenReturn("Q90");
        Entity mockParisEntityWithRoLink = mock(Entity.class);
        when(mockParisEntityWithRoLink.getId()).thenReturn("Q90");
        when(mockParisEntityWithRoLink.getSitelinks()).thenReturn(Map.of("rowiki", new Sitelink("rowiki", "Paris (Franța)")));
        when(dataWiki.getWikibaseItemBySiteAndTitle("enwiki", "Paris")).thenReturn(mockParisEntity);
        when(dataWiki.getWikibaseItemBySiteAndTitle("rowiki", "Paris (Franța)")).thenReturn(mockParisEntityWithRoLink);
        when(sourceWiki.getDomain()).thenReturn("en.wikipedia.org");

        try (MockedStatic<Wiki> wikiMock = mockStatic(Wiki.class))
        {
            wikiMock.when(() -> Wiki.newSession("ro.wikipedia.org")).thenReturn(targetWiki);
            wikiMock.when(() -> Wiki.newSession("en.wikipedia.org")).thenReturn(sourceWiki);
            when(targetWiki.resolveRedirects(anyList())).thenReturn(Collections.singletonList("Paris (Franța)"));
            when(targetWiki.exists(anyList())).thenReturn(new boolean[] { true });

            ReplaceCrossLinkWithIll op = new ReplaceCrossLinkWithIll(targetWiki, sourceWiki, dataWiki, "Test");
            String result = op.executeWithInitialText(input);
            assertEquals(expected, result);
        }
    }

}
