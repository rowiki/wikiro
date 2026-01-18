package org.wikipedia.ro.legacyoperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.wikibase.Wikibase;
import org.wikibase.data.Entity;
import org.wikibase.data.Sitelink;
import org.wikipedia.Wiki;
import org.wikipedia.ro.utils.WikidataCacheManager;
import org.wikipedia.ro.utils.WikipediaPageCache;

@Execution(ExecutionMode.SAME_THREAD)
class TestReplaceCrossLinkWithIll {
    private Wiki targetWiki;
    private Wiki sourceWiki;
    private Wikibase dataWiki;
    private Map<String, String> targetWikiTexts = new HashMap<>();
    private Map<String, String> sourceWikiTexts = new HashMap<>();
    private Map<String, String> targetWikiRedirects = new HashMap<>();
    private Map<String, String> sourceWikiRedirects = new HashMap<>();

    @BeforeEach
    void setUp() throws IOException {
        targetWiki = mock(Wiki.class);
        initializeWikiMock(targetWiki, "ro.wikipedia.org", targetWikiRedirects, targetWikiTexts);

        sourceWiki = mock(Wiki.class);
        initializeWikiMock(sourceWiki, "fr.wikipedia.org", sourceWikiRedirects, sourceWikiTexts);
        dataWiki = mock(Wikibase.class);
    }

    private void initializeWikiMock(Wiki wiki, String domain, Map<String, String> redirectMap, Map<String, String> textMap) throws IOException {
        when(wiki.getDomain()).thenReturn(domain);
        when(wiki.getPageText(any())).thenAnswer(invocation -> {
            List<String> titles = invocation.getArgument(0);
            List<String> texts = new ArrayList<>();
            for (String title : titles) {
                texts.add(textMap.get(title));
            }
            return texts;
        });
        when(wiki.exists(any())).thenAnswer(inv -> {
            List<String> titles = inv.getArgument(0);
            boolean[] result = new boolean[titles.size()];
            for (int i = 0; i < titles.size(); i++) {
                result[i] = textMap.containsKey(titles.get(i)) || redirectMap.containsKey(titles.get(i));
            }
            return result;
        });
        when(wiki.resolveRedirects(any())).thenAnswer(inv -> {
            List<String> titles = inv.getArgument(0);
            List<String> redirects = new ArrayList<>();
            for (String title : titles) {
                redirects.add(redirectMap.getOrDefault(title, title));
            }
            return redirects;
        });
        when(wiki.getPageInfo(any())).thenAnswer(invocation -> {
            List<String> titles = invocation.getArgument(0);
            List<Map<String, Object>> infoList = new ArrayList<>();
            for (String title : titles) {
                Map<String, Object> info = new HashMap<>();
                info.put("exists", textMap.containsKey(title) || redirectMap.containsKey(title));
                info.put("redirect", redirectMap.containsKey(title));
                infoList.add(info);
            }
            return infoList;
        });
    }

    @AfterEach
    void tearDown() {
        reset(targetWiki, sourceWiki, dataWiki);

        // Avoid clearing immutable maps; reassign fresh mutable maps
        targetWikiTexts = new HashMap<>();
        sourceWikiTexts = new HashMap<>();
        targetWikiRedirects = new HashMap<>();
        sourceWikiRedirects = new HashMap<>();

        WikidataCacheManager.clearCaches();
        WikipediaPageCache.getInstance().invalidateAll();
    }

    @Test
    void testNoLinks() throws Exception {
        ReplaceCrossLinkWithIll op = new ReplaceCrossLinkWithIll(targetWiki, sourceWiki, dataWiki, "Test");
        String result = op.executeWithInitialText("This is a plain text.");
        assertEquals("This is a plain text.", result);
    }

    @Test
    void testCrossWikiLinkReplacedWithIll() throws Exception {
        String text = "Some [[:fr:Paris|Paris in French]] text.";
        sourceWikiTexts.put("Paris", "Some content about Paris.");
        
        ReplaceCrossLinkWithIll op = new ReplaceCrossLinkWithIll(targetWiki, sourceWiki, dataWiki, "Test");
        when(dataWiki.getWikibaseItemBySiteAndTitle("frwiki", "Paris")).thenReturn(null);

        try (MockedStatic<Wiki> wikiMock = mockStatic(Wiki.class)) {
            wikiMock.when(() -> Wiki.newSession("fr.wikipedia.org")).thenReturn(sourceWiki);

            String result = op.executeWithInitialText(text);
            assertTrue(result.contains("{{Ill"));
        }
    }

    @Test
    void testLocalLinkExists() throws Exception {
        // mutate existing maps instead of reassigning
        targetWikiTexts.clear();
        targetWikiRedirects.clear();
        targetWikiTexts.put("Bucharest", "Some content about Bucharest.");

        ReplaceCrossLinkWithIll op = new ReplaceCrossLinkWithIll(targetWiki, sourceWiki, dataWiki, "Test");
        String text = "Some [[Bucharest|București]] text.";
        String result = op.executeWithInitialText(text);
        assertTrue(result.contains("[[Bucharest|București]]"));
    }

    @Test
    void testLocalLinkDoesNotExistAndForeignLinkDoesNotExist() throws Exception {

        ReplaceCrossLinkWithIll op = new ReplaceCrossLinkWithIll(targetWiki, sourceWiki, dataWiki, "Test");
        String text = "Some [[NonexistentPage|Label]] text.";
        
        String result = op.executeWithInitialText(text);
        assertTrue(text.equals(result));
    }

    @Test
    void testCrossWikiLinkReplacedWithIllAndWikidataItem() throws Exception
    {
        String text = "Some [[Paris, France|Paris in French]] text.";

        // keep maps mutable and update contents
        targetWikiRedirects.clear();
        sourceWikiRedirects.clear();
        targetWikiTexts.clear();
        sourceWikiTexts.clear();
        sourceWikiTexts.put("Paris, France", "Some content about Paris.");

        // Mock Wikibase to return a Q-number
        Entity mockParisEntity = mock(Entity.class);
        when(mockParisEntity.getId()).thenReturn("Q90");
        when(dataWiki.getWikibaseItemBySiteAndTitle("frwiki", "Paris, France")).thenReturn(mockParisEntity);
        
        ReplaceCrossLinkWithIll op = new ReplaceCrossLinkWithIll(targetWiki, sourceWiki, dataWiki, "Test");
        String result = op.executeWithInitialText(text);

        assertTrue(result.contains("{{Ill-wd"));
        assertTrue(result.contains("Q90"));
    }

    @Test
    void testCrossWikiLinkReplacedWithTargetRedirect() throws Exception
    {
        String text = "Some [[Paris, France|Paris in French]] text.";

        // mutate, don't reassign
        targetWikiRedirects.clear();
        sourceWikiRedirects.clear();
        targetWikiTexts.clear();
        sourceWikiTexts.clear();
        targetWikiTexts.put("Paris (Franța)", "Some content about Paris.");
        sourceWikiTexts.put("Paris, France", "Some content about Paris.");

        // Mock Wikibase to return a Q-number and rowiki sitelink
        Entity mockParisEntity = mock(Entity.class);
        when(mockParisEntity.getId()).thenReturn("Q90");
        when(mockParisEntity.getSitelinks()).thenReturn(Map.of("rowiki", new Sitelink("rowiki", "Paris (Franța)")));
        when(dataWiki.getWikibaseItemBySiteAndTitle("frwiki", "Paris, France")).thenReturn(mockParisEntity);
        
        ReplaceCrossLinkWithIll op = new ReplaceCrossLinkWithIll(targetWiki, sourceWiki, dataWiki, "Test");
        String result = op.executeWithInitialText(text);

        assertTrue(result.contains("[[Paris (Franța)|Paris in French]]"));
    }

    static Stream<Arguments> wikipediaExternalLinks() {
        return Stream.of(
            Arguments.of("See [https://fr.wikipedia.org/wiki/Paris Paris] for more.", "See {{Ill-wd|Q90|3=Paris}} for more."),
            Arguments.of("See [https://ro.wikipedia.org/wiki/Paris_(Franța) Paris] for more.", "See [[Paris (Franța)|Paris]] for more."),
            Arguments.of("See [https://fr.m.wikipedia.org/wiki/Paris Paris] for more.", "See {{Ill-wd|Q90|3=Paris}} for more.") 
            );
    }

    @ParameterizedTest
    @MethodSource("wikipediaExternalLinks")
    void testWikipediaExternalLinksTransformed(String input, String expected) throws Exception {
        Entity mockParisEntity = mock(Entity.class);
        when(mockParisEntity.getId()).thenReturn("Q90");

        Entity mockParisEntityWithRoLink = mock(Entity.class);
        when(mockParisEntityWithRoLink.getId()).thenReturn("Q90");
        when(mockParisEntityWithRoLink.getSitelinks()).thenReturn(Map.of("rowiki", new Sitelink("rowiki", "Paris (Franța)")));

        // Return Q90 for both "Paris" and "Paris, France"
        when(dataWiki.getWikibaseItemBySiteAndTitle("frwiki", "Paris")).thenReturn(mockParisEntity);
        when(dataWiki.getWikibaseItemBySiteAndTitle("frwiki", "Paris, France")).thenReturn(mockParisEntity);
        when(dataWiki.getWikibaseItemBySiteAndTitle("rowiki", "Paris (Franța)")).thenReturn(mockParisEntityWithRoLink);

        try (MockedStatic<Wiki> wikiMock = mockStatic(Wiki.class)) {
            wikiMock.when(() -> Wiki.newSession("ro.wikipedia.org")).thenReturn(targetWiki);
            wikiMock.when(() -> Wiki.newSession("fr.wikipedia.org")).thenReturn(sourceWiki);

            ReplaceCrossLinkWithIll op = new ReplaceCrossLinkWithIll(targetWiki, sourceWiki, dataWiki, "Test");
            String result = op.executeWithInitialText(input);
            assertEquals(expected, result);
        }
    }

}
