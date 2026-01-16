package org.wikipedia.ro.legacyoperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.*;

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

    @BeforeEach
    void setUp() {
        targetWiki = mock(Wiki.class, "target=ro");
        sourceWiki = mock(Wiki.class, "source=fr");
        dataWiki = mock(Wikibase.class, "wikidata");

        when(targetWiki.getDomain()).thenReturn("ro.wikipedia.org");
        when(sourceWiki.getDomain()).thenReturn("fr.wikipedia.org");
    }

    @AfterEach
    void tearDown() {
        reset(targetWiki, sourceWiki, dataWiki);
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
            when(targetWiki.resolveRedirects(argThat(list -> list.contains("Paris")))).thenAnswer(invocation -> invocation.getArgument(0));
            when(targetWiki.exists(argThat(list -> list.contains("Paris")))).thenAnswer(inv -> {
                List<String> titles = inv.getArgument(0);
                boolean[] result = new boolean[titles.size()];
                Arrays.fill(result, false);
                return result;
            });

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

        when(targetWiki.resolveRedirects(argThat(list -> list.contains("Paris")))).thenAnswer(invocation -> invocation.getArgument(0));
        when(targetWiki.exists(argThat(list -> list.contains("Paris")))).thenAnswer(inv -> {
            List<String> titles = inv.getArgument(0);
            boolean[] result = new boolean[titles.size()];
            for (int i = 0; i < titles.size(); i++) {
                result[i] = titles.get(i).equals("Paris");
            }
            return result;
        });
        
        CleanupIll op = new CleanupIll(targetWiki, sourceWiki, dataWiki, "Test");
        String result = op.executeWithInitialText(pageText);

        assertEquals("Before [[Paris|Paris label]] After", result);
    }

    @Test
    void testExecuteWithInitialText_IllTemplateWithNonExistingTargetPage() throws Exception {
        String pageText = "Before {{Ill|fr|Paris|Paris|Paris label}} After";

        when(targetWiki.resolveRedirects(argThat(list -> list.contains("Paris")))).thenAnswer(invocation -> invocation.getArgument(0));
        when(targetWiki.exists(argThat(list -> list.contains("Paris")))).thenAnswer(inv -> {
            List<String> titles = inv.getArgument(0);
            boolean[] result = new boolean[titles.size()];
            Arrays.fill(result, false);
            return result;
        });
        
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
