package org.wikipedia.ro.legacyoperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.MockedStatic;
import org.wikibase.Wikibase;
import org.wikibase.data.Entity;
import org.wikibase.data.Sitelink;
import org.wikipedia.Wiki;
import org.wikipedia.ro.utils.WikidataCacheManager;
import org.wikipedia.ro.utils.WikipediaPageCache;

@Execution(ExecutionMode.SAME_THREAD)
class TestCleanupIll {
    private Wiki targetWiki;
    private Wiki sourceWiki;
    private Wikibase dataWiki;

    // Share mutable maps with the wiki mocks, same as TestReplaceCrossLinkWithIll
    private Map<String, String> targetWikiTexts = new HashMap<>();
    private Map<String, String> sourceWikiTexts = new HashMap<>();
    private Map<String, String> targetWikiRedirects = new HashMap<>();
    private Map<String, String> sourceWikiRedirects = new HashMap<>();

    @BeforeEach
    void setUp() throws IOException {
        targetWiki = mock(Wiki.class, "target=ro");
        sourceWiki = mock(Wiki.class, "source=fr");
        dataWiki = mock(Wikibase.class, "wikidata");

        initializeWikiMock(targetWiki, "ro.wikipedia.org", targetWikiRedirects, targetWikiTexts);
        initializeWikiMock(sourceWiki, "fr.wikipedia.org", sourceWikiRedirects, sourceWikiTexts);
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
        // Reassign fresh mutable maps (avoids issues if tests used immutable maps)
        targetWikiTexts = new HashMap<>();
        sourceWikiTexts = new HashMap<>();
        targetWikiRedirects = new HashMap<>();
        sourceWikiRedirects = new HashMap<>();
        WikidataCacheManager.clearCaches();
        WikipediaPageCache.getInstance().invalidateAll();
    }
    
    @Test
    void testExecuteWithInitialText_IllWdTemplateWithoutLabelGetsLabelFromWikidata() throws Exception {
        String pageText = "Before {{Ill-wd|Q123}} After";
        Entity wdEntity = mock(Entity.class);
        Sitelink sitelink = new Sitelink("rowiki", "Paris (Franța)");
        when(wdEntity.getSitelinks()).thenReturn(Map.of("rowiki", sitelink));
        when(wdEntity.getLabels()).thenReturn(Map.of("ro", "Paris"));

        when(dataWiki.getWikibaseItemById("Q123")).thenReturn(wdEntity);
        when(dataWiki.resolveRedirects(argThat(list -> list.contains("Q123")))).thenAnswer(inv -> inv.getArgument(0));

        CleanupIll op = new CleanupIll(targetWiki, sourceWiki, dataWiki, "Test");
        String result = op.executeWithInitialText(pageText);

        assertEquals("Before [[Paris (Franța)|Paris]] After", result);
    }

    @Test
    void testExecuteWithInitialText_IllWdTemplateReplacedWithWikiLink() throws Exception {
        String pageText = "Before {{Ill-wd|Q123|Paris|Paris label}} After";
        Entity wdEntity = mock(Entity.class);
        Sitelink sitelink = new Sitelink("rowiki", "Paris");
        when(wdEntity.getSitelinks()).thenReturn(Map.of("rowiki", sitelink));
        when(dataWiki.getWikibaseItemById("Q123")).thenReturn(wdEntity);
        when(dataWiki.resolveRedirects(argThat(list -> list.contains("Q123")))).thenAnswer(inv -> inv.getArgument(0));

        CleanupIll op = new CleanupIll(targetWiki, sourceWiki, dataWiki, "Test");
        String result = op.executeWithInitialText(pageText);

        assertEquals("Before [[Paris|Paris label]] After", result);
    }
    
    @Test
    void testExecuteWithInitialText_IllWdTemplatePreserved() throws Exception {
        String pageText = "Before {{Ill-wd|Q123|Paris|Paris label}} After";
        Entity wdEntity = mock(Entity.class);
        Sitelink sitelink = new Sitelink("frwiki", "Paris");
        when(wdEntity.getSitelinks()).thenReturn(Map.of("frwiki", sitelink));

        when(dataWiki.getWikibaseItemById("Q123")).thenReturn(wdEntity);
        when(dataWiki.resolveRedirects(argThat(list -> list.contains("Q123")))).thenAnswer(inv -> inv.getArgument(0));
        CleanupIll op = new CleanupIll(targetWiki, sourceWiki, dataWiki, "Test");
        String result = op.executeWithInitialText(pageText);

        assertEquals("Before {{Ill-wd|Q123|Paris|Paris label}} After", result);
    }
    
    @Test
    void testExecuteWithInitialText_IllTemplateWithExistingTargetPageWithDifferentName() throws Exception {
        String pageText = "Before {{Ill|fr|Paris|Paris|Paris label}} After";
        Entity wdEntity = mock(Entity.class);
        Sitelink sitelink = new Sitelink("rowiki", "Paris (Franța)");
        when(wdEntity.getSitelinks()).thenReturn(Map.of("rowiki", sitelink));

        try (MockedStatic<Wiki> wikiMock = mockStatic(Wiki.class)) {
            wikiMock.when(() -> Wiki.newSession("fr.wikipedia.org")).thenReturn(sourceWiki);
            when(dataWiki.getWikibaseItemBySiteAndTitle("frwiki", "Paris")).thenReturn(wdEntity);
            
            CleanupIll op = new CleanupIll(targetWiki, sourceWiki, dataWiki, "Test");
            String result = op.executeWithInitialText(pageText);

            assertEquals("Before [[Paris (Franța)|Paris label]] After", result);
        }
    }

    @Test
    void testExecuteWithInitialText_IllTemplateWithExistingTargetPage() throws Exception {
        String pageText = "Before {{Ill|fr|Paris|Paris|Paris label}} After";

        targetWikiTexts.put("Paris", "Some content about Paris");
        
        CleanupIll op = new CleanupIll(targetWiki, sourceWiki, dataWiki, "Test");
        String result = op.executeWithInitialText(pageText);

        assertEquals("Before [[Paris|Paris label]] After", result);
    }

    @Test
    void testExecuteWithInitialText_IllTemplateWithNonExistingTargetPage() throws Exception {
        String pageText = "Before {{Ill|fr|Paris|Paris|Paris label}} After";
        
        Entity wdEntity = mock(Entity.class);
        Sitelink sitelink = new Sitelink("frwiki", "Paris");
        when(wdEntity.getSitelinks()).thenReturn(Map.of("frwiki", sitelink));
        when(dataWiki.getWikibaseItemBySiteAndTitle("frwiki", "Paris")).thenReturn(wdEntity);
        
        CleanupIll op = new CleanupIll(targetWiki, sourceWiki, dataWiki, "Test");
        String result = op.executeWithInitialText(pageText);

        assertTrue(result.contains("Before {{Ill|fr|Paris|Paris|Paris label}} After"));
    }

    @Test
    void testExecuteWithInitialText_NoIllTemplate_NoChange() throws Exception {
        String pageText = "This is a page without Ill template.";

        CleanupIll op = new CleanupIll(targetWiki, sourceWiki, dataWiki, "Test");
        String result = op.executeWithInitialText(pageText);

        assertEquals(pageText, result);
    }
}
