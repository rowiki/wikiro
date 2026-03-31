
package org.wikipedia.ro.textgen;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class VillageHistoryServiceTest {

    private VillageHistoryService service;

    @BeforeEach
    public void setUp() {
        service = new VillageHistoryService();
    }

    // ===== Empty / null params =====

    @Test
    public void testAllParamsNull_producesNoPhrases() {
        VillageHistoryParams params = new VillageHistoryParams();
        List<String> phrases = service.buildPhrases(params);
        assertTrue(phrases.isEmpty());
    }

    @Test
    public void testAllParamsEmpty_producesNoPhrases() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setBauerName("");
        params.setSpechtName("");
        params.setCataName("");
        params.setIdx1954Name("");
        params.setIdx1956Name("");
        List<String> phrases = service.buildPhrases(params);
        assertTrue(phrases.isEmpty());
    }

    @Test
    public void testBuildText_emptyParams_returnsEmptyString() {
        VillageHistoryParams params = new VillageHistoryParams();
        assertEquals("", service.buildText(params));
    }

    // ===== Bauer phrase =====

    @Test
    public void testBauer_nameOnly() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setBauerName("Testsat");
        List<String> phrases = service.buildPhrases(params);
        assertEquals(1, phrases.size());
        assertEquals(
            "Satul este menționat în memoriile generalului Bauer din 1778 ca ''Testsat''.<ref>{{Citat Q|Q136757066}}</ref>",
            phrases.get(0));
    }

    @Test
    public void testBauer_nameAndDescription() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setBauerName("Testsat");
        params.setBauerDescription("un sat mic");
        List<String> phrases = service.buildPhrases(params);
        assertEquals(1, phrases.size());
        assertTrue(phrases.get(0).contains(", drept un sat mic"));
        assertTrue(phrases.get(0).contains(".<ref>"));
    }

    @Test
    public void testBauer_nameAndPage() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setBauerName("Testsat");
        params.setBauerPage("42");
        List<String> phrases = service.buildPhrases(params);
        assertEquals(1, phrases.size());
        assertTrue(phrases.get(0).contains("|p=42"));
    }

    @Test
    public void testBauer_nameDescriptionAndPage() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setBauerName("Testsat");
        params.setBauerDescription("un sat mic");
        params.setBauerPage("42");
        List<String> phrases = service.buildPhrases(params);
        assertEquals(1, phrases.size());
        String phrase = phrases.get(0);
        assertTrue(phrase.contains("''Testsat''"));
        assertTrue(phrase.contains(", drept un sat mic"));
        assertTrue(phrase.contains("|p=42"));
        assertTrue(phrase.endsWith("}}</ref>"));
    }

    @Test
    public void testBauer_nullName_noPhrase() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setBauerName(null);
        params.setBauerDescription("desc");
        params.setBauerPage("10");
        List<String> phrases = service.buildPhrases(params);
        assertTrue(phrases.isEmpty());
    }

    @Test
    public void testBauer_emptyName_noPhrase() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setBauerName("");
        params.setBauerDescription("desc");
        params.setBauerPage("10");
        List<String> phrases = service.buildPhrases(params);
        assertTrue(phrases.isEmpty());
    }

    @Test
    public void testBauer_descriptionNull_noDescriptionInPhrase() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setBauerName("Testsat");
        params.setBauerDescription(null);
        String phrase = service.buildPhrases(params).get(0);
        assertFalse(phrase.contains("drept"));
    }

    @Test
    public void testBauer_pageNull_noPageInRef() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setBauerName("Testsat");
        params.setBauerPage(null);
        String phrase = service.buildPhrases(params).get(0);
        assertFalse(phrase.contains("|p="));
    }

    // ===== Specht phrase =====

    @Test
    public void testSpecht_nameOnly() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setSpechtName("Testdorf");
        List<String> phrases = service.buildPhrases(params);
        assertEquals(1, phrases.size());
        assertEquals(
            "Pe harta Specht a Țării Românești din 1790, apare cu denumirea ''Testdorf''.<ref>{{Citat Q|Q136659961}}</ref>",
            phrases.get(0));
    }

    @Test
    public void testSpecht_nullName_noPhrase() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setSpechtName(null);
        assertTrue(service.buildPhrases(params).isEmpty());
    }

    @Test
    public void testSpecht_emptyName_noPhrase() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setSpechtName("");
        assertTrue(service.buildPhrases(params).isEmpty());
    }

    // ===== Catagraphy phrase =====

    @Test
    public void testCata_nameOnly() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setCataName("Satul Vechi");
        List<String> phrases = service.buildPhrases(params);
        assertEquals(1, phrases.size());
        String phrase = phrases.get(0);
        assertTrue(phrase.startsWith("Catagrafia din 1831 consemnează satul cu numele ''Satul Vechi''"));
        assertTrue(phrase.contains(".<ref>{{Citat Q|Q136354833}}</ref>"));
        assertFalse(phrase.contains("plasa"));
        assertFalse(phrase.contains("județul"));
        assertFalse(phrase.contains("moșia"));
        assertFalse(phrase.contains("familii"));
        assertFalse(phrase.contains("feciori"));
        assertFalse(phrase.contains("|p="));
    }

    @Test
    public void testCata_nullName_noPhrase() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setCataName(null);
        params.setCataPlasa("Plasa X");
        assertTrue(service.buildPhrases(params).isEmpty());
    }

    @Test
    public void testCata_emptyName_noPhrase() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setCataName("");
        assertTrue(service.buildPhrases(params).isEmpty());
    }

    @Test
    public void testCata_withPlasa() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setCataName("Sat");
        params.setCataPlasa("Ocolul de Sus");
        String phrase = service.buildPhrases(params).get(0);
        assertTrue(phrase.contains(", în plasa ''Ocolul de Sus''"));
    }

    @Test
    public void testCata_withCounty_notSecuieni() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setCataName("Sat");
        params.setCataCounty("Neamț");
        String phrase = service.buildPhrases(params).get(0);
        assertTrue(phrase.contains("[[județul Neamț (interbelic)|]]"));
    }

    @Test
    public void testCata_withCounty_Secuieni_caseInsensitive() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setCataName("Sat");
        params.setCataCounty("Secuieni");
        String phrase = service.buildPhrases(params).get(0);
        assertTrue(phrase.contains("[[județul Secuieni]]"));
        assertFalse(phrase.contains("(interbelic)"));
    }

    @Test
    public void testCata_withCounty_SecuieniLowercase() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setCataName("Sat");
        params.setCataCounty("secuieni");
        String phrase = service.buildPhrases(params).get(0);
        assertTrue(phrase.contains("[[județul secuieni]]"));
        assertFalse(phrase.contains("(interbelic)"));
    }

    @Test
    public void testCata_withPlasaAndCounty() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setCataName("Sat");
        params.setCataPlasa("Plasa X");
        params.setCataCounty("Roman");
        String phrase = service.buildPhrases(params).get(0);
        assertTrue(phrase.contains("în plasa ''Plasa X''"));
        assertTrue(phrase.contains("din [[județul Roman (interbelic)|]]"));
    }

    @Test
    public void testCata_withMosieOnly() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setCataName("Sat");
        params.setCataMosie("Moșia Mare");
        String phrase = service.buildPhrases(params).get(0);
        assertTrue(phrase.contains(", pe moșia ''Moșia Mare''"));
    }

    @Test
    public void testCata_withOwnerOnly() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setCataName("Sat");
        params.setCataOwner("Ion Popescu");
        String phrase = service.buildPhrases(params).get(0);
        assertTrue(phrase.contains(", pe moșia "));
        assertTrue(phrase.contains("deținută de Ion Popescu"));
    }

    @Test
    public void testCata_withMosieAndOwner() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setCataName("Sat");
        params.setCataMosie("Moșia Mare");
        params.setCataOwner("Ion Popescu");
        String phrase = service.buildPhrases(params).get(0);
        assertTrue(phrase.contains("pe moșia ''Moșia Mare''"));
        assertTrue(phrase.contains("deținută de Ion Popescu"));
    }

    @Test
    public void testCata_emptyMosieAndEmptyOwner_noMosieSection() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setCataName("Sat");
        params.setCataMosie("");
        params.setCataOwner("");
        String phrase = service.buildPhrases(params).get(0);
        assertFalse(phrase.contains("moșia"));
        assertFalse(phrase.contains("deținută"));
    }

    @Test
    public void testCata_withPopulation() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setCataName("Sat");
        params.setCataPop("120");
        String phrase = service.buildPhrases(params).get(0);
        assertTrue(phrase.contains(", având 120 familii"));
    }

    @Test
    public void testCata_withFeciori() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setCataName("Sat");
        params.setCataFeci("15");
        String phrase = service.buildPhrases(params).get(0);
        assertTrue(phrase.contains(" și 15 feciori de muncă"));
    }

    @Test
    public void testCata_withPopAndFeciori() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setCataName("Sat");
        params.setCataPop("120");
        params.setCataFeci("15");
        String phrase = service.buildPhrases(params).get(0);
        assertTrue(phrase.contains(", având 120 familii și 15 feciori de muncă"));
    }

    @Test
    public void testCata_emptyPop_noFamiliiSection() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setCataName("Sat");
        params.setCataPop("");
        String phrase = service.buildPhrases(params).get(0);
        assertFalse(phrase.contains("familii"));
    }

    @Test
    public void testCata_emptyFeci_noFecioriSection() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setCataName("Sat");
        params.setCataFeci("");
        String phrase = service.buildPhrases(params).get(0);
        assertFalse(phrase.contains("feciori"));
    }

    @Test
    public void testCata_withPage() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setCataName("Sat");
        params.setCataPage("55");
        String phrase = service.buildPhrases(params).get(0);
        assertTrue(phrase.contains("|p=55"));
    }

    @Test
    public void testCata_withoutPage_noPageInRef() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setCataName("Sat");
        String phrase = service.buildPhrases(params).get(0);
        assertFalse(phrase.contains("|p="));
    }

    @Test
    public void testCata_allFields() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setCataName("Satul Nou");
        params.setCataPlasa("Plasa X");
        params.setCataCounty("Neamț");
        params.setCataMosie("Moșia Y");
        params.setCataOwner("Vasile Ion");
        params.setCataPop("80");
        params.setCataFeci("10");
        params.setCataPage("99");
        String phrase = service.buildPhrases(params).get(0);
        assertTrue(phrase.contains("''Satul Nou''"));
        assertTrue(phrase.contains("plasa ''Plasa X''"));
        assertTrue(phrase.contains("județul Neamț (interbelic)"));
        assertTrue(phrase.contains("moșia ''Moșia Y''"));
        assertTrue(phrase.contains("deținută de Vasile Ion"));
        assertTrue(phrase.contains("80 familii"));
        assertTrue(phrase.contains("10 feciori de muncă"));
        assertTrue(phrase.contains("|p=99"));
    }

    // ===== Index 1954 phrase =====

    @Test
    public void testIdx1954_nameOnly() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setIdx1954Name("Sat54");
        List<String> phrases = service.buildPhrases(params);
        assertEquals(1, phrases.size());
        String phrase = phrases.get(0);
        assertTrue(phrase.startsWith("Indicele localităților din 1954 menționează satul cu numele ''Sat54''"));
        assertTrue(phrase.contains(".<ref>{{Citat Q|Q136158772}}</ref>"));
        assertFalse(phrase.contains("descriindu-l"));
        assertFalse(phrase.contains("|p="));
    }

    @Test
    public void testIdx1954_withDescription() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setIdx1954Name("Sat54");
        params.setIdx1954Descr("sat în comuna X");
        String phrase = service.buildPhrases(params).get(0);
        assertTrue(phrase.contains(", descriindu-l drept sat în comuna X"));
    }

    @Test
    public void testIdx1954_withPage() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setIdx1954Name("Sat54");
        params.setIdx1954Page("200");
        String phrase = service.buildPhrases(params).get(0);
        assertTrue(phrase.contains("|p=200"));
    }

    @Test
    public void testIdx1954_allFields() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setIdx1954Name("Sat54");
        params.setIdx1954Descr("sat mic");
        params.setIdx1954Page("200");
        String phrase = service.buildPhrases(params).get(0);
        assertTrue(phrase.contains("''Sat54''"));
        assertTrue(phrase.contains("descriindu-l drept sat mic"));
        assertTrue(phrase.contains("|p=200"));
        assertTrue(phrase.contains("Q136158772"));
    }

    @Test
    public void testIdx1954_nullName_noPhrase() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setIdx1954Name(null);
        params.setIdx1954Descr("desc");
        assertTrue(service.buildPhrases(params).isEmpty());
    }

    @Test
    public void testIdx1954_emptyName_noPhrase() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setIdx1954Name("");
        assertTrue(service.buildPhrases(params).isEmpty());
    }

    @Test
    public void testIdx1954_emptyDescr_noDescrInPhrase() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setIdx1954Name("Sat54");
        params.setIdx1954Descr("");
        String phrase = service.buildPhrases(params).get(0);
        assertFalse(phrase.contains("descriindu-l"));
    }

    @Test
    public void testIdx1954_emptyPage_noPageInRef() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setIdx1954Name("Sat54");
        params.setIdx1954Page("");
        String phrase = service.buildPhrases(params).get(0);
        assertFalse(phrase.contains("|p="));
    }

    // ===== Index 1956 phrase (standalone, no 1954) =====

    @Test
    public void testIdx1956_nameOnly_standalone() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setIdx1956Name("Sat56");
        List<String> phrases = service.buildPhrases(params);
        assertEquals(1, phrases.size());
        String phrase = phrases.get(0);
        assertTrue(phrase.startsWith("Indicele localităților din 1956 menționează satul cu numele ''Sat56''"));
        assertTrue(phrase.contains("Q136158759"));
        assertFalse(phrase.contains("Cel din 1956"));
    }

    @Test
    public void testIdx1956_withDescription_standalone() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setIdx1956Name("Sat56");
        params.setIdx1956Descr("sat în raionul Y");
        String phrase = service.buildPhrases(params).get(0);
        assertTrue(phrase.contains(", drept sat în raionul Y"));
        assertFalse(phrase.contains("descriindu-l"));
    }

    @Test
    public void testIdx1956_withPage_standalone() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setIdx1956Name("Sat56");
        params.setIdx1956Page("300");
        String phrase = service.buildPhrases(params).get(0);
        assertTrue(phrase.contains("|p=300"));
    }

    @Test
    public void testIdx1956_allFields_standalone() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setIdx1956Name("Sat56");
        params.setIdx1956Descr("sat mare");
        params.setIdx1956Page("300");
        String phrase = service.buildPhrases(params).get(0);
        assertTrue(phrase.contains("''Sat56''"));
        assertTrue(phrase.contains("drept sat mare"));
        assertTrue(phrase.contains("|p=300"));
        assertTrue(phrase.contains("Q136158759"));
    }

    @Test
    public void testIdx1956_nullName_noPhrase() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setIdx1956Name(null);
        assertTrue(service.buildPhrases(params).isEmpty());
    }

    @Test
    public void testIdx1956_emptyName_noPhrase() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setIdx1956Name("");
        assertTrue(service.buildPhrases(params).isEmpty());
    }

    @Test
    public void testIdx1956_emptyDescr_standalone_noDescrInPhrase() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setIdx1956Name("Sat56");
        params.setIdx1956Descr("");
        String phrase = service.buildPhrases(params).get(0);
        assertFalse(phrase.contains("drept"));
    }

    @Test
    public void testIdx1956_emptyPage_standalone_noPageInRef() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setIdx1956Name("Sat56");
        params.setIdx1956Page("");
        String phrase = service.buildPhrases(params).get(0);
        assertFalse(phrase.contains("|p="));
    }

    // ===== Index 1956 phrase (following 1954) =====

    @Test
    public void testIdx1956_nameOnly_following1954() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setIdx1954Name("Sat54");
        params.setIdx1956Name("Sat56");
        List<String> phrases = service.buildPhrases(params);
        assertEquals(2, phrases.size());
        String phrase1956 = phrases.get(1);
        assertTrue(phrase1956.startsWith("Cel din 1956 îl menționează cu numele ''Sat56''"));
        assertTrue(phrase1956.contains("Q136158759"));
    }

    @Test
    public void testIdx1956_withDescription_following1954() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setIdx1954Name("Sat54");
        params.setIdx1956Name("Sat56");
        params.setIdx1956Descr("sat în raionul Y");
        String phrase1956 = service.buildPhrases(params).get(1);
        assertTrue(phrase1956.contains("Cel din 1956"));
        assertTrue(phrase1956.contains(", descriindu-l drept sat în raionul Y"));
    }

    @Test
    public void testIdx1956_withPage_following1954() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setIdx1954Name("Sat54");
        params.setIdx1956Name("Sat56");
        params.setIdx1956Page("300");
        String phrase1956 = service.buildPhrases(params).get(1);
        assertTrue(phrase1956.contains("|p=300"));
    }

    @Test
    public void testIdx1956_allFields_following1954() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setIdx1954Name("Sat54");
        params.setIdx1954Descr("desc54");
        params.setIdx1954Page("200");
        params.setIdx1956Name("Sat56");
        params.setIdx1956Descr("desc56");
        params.setIdx1956Page("300");
        List<String> phrases = service.buildPhrases(params);
        assertEquals(2, phrases.size());

        String p1954 = phrases.get(0);
        assertTrue(p1954.contains("Indicele localităților din 1954"));
        assertTrue(p1954.contains("''Sat54''"));
        assertTrue(p1954.contains("descriindu-l drept desc54"));
        assertTrue(p1954.contains("|p=200"));
        assertTrue(p1954.contains("Q136158772"));

        String p1956 = phrases.get(1);
        assertTrue(p1956.contains("Cel din 1956"));
        assertTrue(p1956.contains("''Sat56''"));
        assertTrue(p1956.contains("descriindu-l"));
        assertTrue(p1956.contains("drept desc56"));
        assertTrue(p1956.contains("|p=300"));
        assertTrue(p1956.contains("Q136158759"));
    }

    @Test
    public void testIdx1956_emptyDescr_following1954_noDescrInPhrase() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setIdx1954Name("Sat54");
        params.setIdx1956Name("Sat56");
        params.setIdx1956Descr("");
        String phrase1956 = service.buildPhrases(params).get(1);
        assertFalse(phrase1956.contains("descriindu-l"));
        assertFalse(phrase1956.contains("drept"));
    }

    @Test
    public void testIdx1954_present_idx1956_null_onlyOnePhrase() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setIdx1954Name("Sat54");
        params.setIdx1956Name(null);
        List<String> phrases = service.buildPhrases(params);
        assertEquals(1, phrases.size());
        assertTrue(phrases.get(0).contains("1954"));
    }

    @Test
    public void testIdx1954_present_idx1956_empty_onlyOnePhrase() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setIdx1954Name("Sat54");
        params.setIdx1956Name("");
        List<String> phrases = service.buildPhrases(params);
        assertEquals(1, phrases.size());
    }

    // ===== buildText joining =====

    @Test
    public void testBuildText_singlePhrase() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setBauerName("Sat");
        String text = service.buildText(params);
        assertFalse(text.contains("\n"));
        assertTrue(text.contains("Bauer"));
    }

    @Test
    public void testBuildText_multiplePhrases_joinedByNewline() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setBauerName("SatBauer");
        params.setSpechtName("SatSpecht");
        params.setCataName("SatCata");
        params.setIdx1954Name("Sat54");
        params.setIdx1956Name("Sat56");
        String text = service.buildText(params);
        String[] lines = text.split("\n");
        assertEquals(5, lines.length);
    }

    // ===== Phrase ordering =====

    @Test
    public void testPhraseOrder_bauerSpechtCataIdx1954Idx1956() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setBauerName("B");
        params.setSpechtName("S");
        params.setCataName("C");
        params.setIdx1954Name("I54");
        params.setIdx1956Name("I56");
        List<String> phrases = service.buildPhrases(params);
        assertEquals(5, phrases.size());
        assertTrue(phrases.get(0).contains("Bauer"));
        assertTrue(phrases.get(1).contains("Specht"));
        assertTrue(phrases.get(2).contains("Catagrafia"));
        assertTrue(phrases.get(3).contains("1954"));
        assertTrue(phrases.get(4).contains("1956"));
    }

    @Test
    public void testPhraseOrder_missingMiddle_stillOrdered() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setBauerName("B");
        params.setCataName("C");
        params.setIdx1956Name("I56");
        List<String> phrases = service.buildPhrases(params);
        assertEquals(3, phrases.size());
        assertTrue(phrases.get(0).contains("Bauer"));
        assertTrue(phrases.get(1).contains("Catagrafia"));
        assertTrue(phrases.get(2).contains("1956"));
    }

    // ===== Catagraphy edge cases for mosie/owner =====

    @Test
    public void testCata_mosieNonEmpty_ownerNull() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setCataName("Sat");
        params.setCataMosie("Moșia");
        params.setCataOwner(null);
        String phrase = service.buildPhrases(params).get(0);
        assertTrue(phrase.contains("pe moșia ''Moșia''"));
        assertFalse(phrase.contains("deținută"));
    }

    @Test
    public void testCata_mosieNull_ownerNonEmpty() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setCataName("Sat");
        params.setCataMosie(null);
        params.setCataOwner("Proprietar");
        String phrase = service.buildPhrases(params).get(0);
        assertTrue(phrase.contains("pe moșia"));
        assertTrue(phrase.contains("deținută de Proprietar"));
    }

    @Test
    public void testCata_mosieEmpty_ownerNonEmpty() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setCataName("Sat");
        params.setCataMosie("");
        params.setCataOwner("Proprietar");
        String phrase = service.buildPhrases(params).get(0);
        assertTrue(phrase.contains("deținută de Proprietar"));
    }

    @Test
    public void testCata_mosieNull_ownerNull_noMosieSection() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setCataName("Sat");
        params.setCataMosie(null);
        params.setCataOwner(null);
        String phrase = service.buildPhrases(params).get(0);
        assertFalse(phrase.contains("moșia"));
        assertFalse(phrase.contains("deținută"));
    }

    @Test
    public void testCata_mosieEmpty_ownerEmpty_noMosieSection() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setCataName("Sat");
        params.setCataMosie("");
        params.setCataOwner("");
        String phrase = service.buildPhrases(params).get(0);
        assertFalse(phrase.contains("moșia"));
        assertFalse(phrase.contains("deținută"));
    }

    // ===== Catagraphy: feciori without population =====

    @Test
    public void testCata_fecioriWithoutPop() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setCataName("Sat");
        params.setCataFeci("5");
        String phrase = service.buildPhrases(params).get(0);
        assertTrue(phrase.contains(" și 5 feciori de muncă"));
        assertFalse(phrase.contains("familii"));
    }

    // ===== Catagraphy: county "SECUIENI" uppercase =====

    @Test
    public void testCata_countySecuieniUppercase() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setCataName("Sat");
        params.setCataCounty("SECUIENI");
        String phrase = service.buildPhrases(params).get(0);
        assertFalse(phrase.contains("(interbelic)"));
    }

    // ===== Full integration: all fields populated =====

    @Test
    public void testFullIntegration_allFieldsPopulated() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setBauerName("Bauer Village");
        params.setBauerDescription("a small settlement");
        params.setBauerPage("10");
        params.setSpechtName("Specht Dorf");
        params.setCataName("Cata Sat");
        params.setCataPlasa("Plasa A");
        params.setCataCounty("Suceava");
        params.setCataMosie("Moșia B");
        params.setCataOwner("Boier C");
        params.setCataPop("50");
        params.setCataFeci("8");
        params.setCataPage("77");
        params.setIdx1954Name("Sat 1954");
        params.setIdx1954Descr("desc 1954");
        params.setIdx1954Page("111");
        params.setIdx1956Name("Sat 1956");
        params.setIdx1956Descr("desc 1956");
        params.setIdx1956Page("222");

        List<String> phrases = service.buildPhrases(params);
        assertEquals(5, phrases.size());

        String text = service.buildText(params);
        assertTrue(text.contains("Bauer"));
        assertTrue(text.contains("Specht"));
        assertTrue(text.contains("Catagrafia"));
        assertTrue(text.contains("1954"));
        assertTrue(text.contains("Cel din 1956"));
    }

    // ===== Ref format correctness =====

    @Test
    public void testBauerRef_correctFormat() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setBauerName("X");
        params.setBauerPage("5");
        String phrase = service.buildPhrases(params).get(0);
        assertTrue(phrase.contains("<ref>{{Citat Q|Q136757066|p=5}}</ref>"));
    }

    @Test
    public void testSpechtRef_correctFormat() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setSpechtName("X");
        String phrase = service.buildPhrases(params).get(0);
        assertTrue(phrase.contains("<ref>{{Citat Q|Q136659961}}</ref>"));
    }

    @Test
    public void testCataRef_correctFormat() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setCataName("X");
        params.setCataPage("33");
        String phrase = service.buildPhrases(params).get(0);
        assertTrue(phrase.contains("<ref>{{Citat Q|Q136354833|p=33}}</ref>"));
    }

    @Test
    public void testIdx1954Ref_correctFormat() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setIdx1954Name("X");
        params.setIdx1954Page("44");
        String phrase = service.buildPhrases(params).get(0);
        assertTrue(phrase.contains("<ref>{{Citat Q|Q136158772|p=44}}</ref>"));
    }

    @Test
    public void testIdx1956Ref_correctFormat() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setIdx1956Name("X");
        params.setIdx1956Page("55");
        String phrase = service.buildPhrases(params).get(0);
        assertTrue(phrase.contains("<ref>{{Citat Q|Q136158759|p=55}}</ref>"));
    }

    // ===== Each phrase ends with </ref> =====

    @Test
    public void testAllPhrases_endWithRefClose() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setBauerName("B");
        params.setSpechtName("S");
        params.setCataName("C");
        params.setIdx1954Name("I54");
        params.setIdx1956Name("I56");
        for (String phrase : service.buildPhrases(params)) {
            assertTrue(phrase.endsWith("</ref>"), "Phrase should end with </ref>: " + phrase);
        }
    }

    // ===== Each phrase contains period before ref =====

    @Test
    public void testAllPhrases_containPeriodBeforeRef() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setBauerName("B");
        params.setSpechtName("S");
        params.setCataName("C");
        params.setIdx1954Name("I54");
        params.setIdx1956Name("I56");
        for (String phrase : service.buildPhrases(params)) {
            assertTrue(phrase.contains(".<ref>"), "Phrase should contain .<ref>: " + phrase);
        }
    }
}
