package org.wikipedia.ro.utils;

import org.junit.Assert;
import org.junit.Test;
import org.wikipedia.ro.populationdb.util.GreekTransliterator;

public class TestGreekTransliterator {

    @Test
    public void testHalkidiki() {
        final GreekTransliterator transliterator = new GreekTransliterator("Χαλκιδική");
        final String transliteration = transliterator.transliterate();
        Assert.assertEquals("Chalkidiki", transliteration);
    }

    @Test
    public void testAthens() {
        final GreekTransliterator transliterator = new GreekTransliterator("Αθήνα");
        final String transliteration = transliterator.transliterate();
        Assert.assertEquals("Athina", transliteration);
    }

    @Test
    public void testThesaloniki() {
        final GreekTransliterator transliterator = new GreekTransliterator("Θεσσαλονίκη");
        final String transliteration = transliterator.transliterate();
        Assert.assertEquals("Thessaloniki", transliteration);
    }

    @Test
    public void testXanthi() {
        final GreekTransliterator transliterator = new GreekTransliterator("Ξάνθη");
        final String transliteration = transliterator.transliterate();
        Assert.assertEquals("Xanthi", transliteration);
    }

    @Test
    public void testErmoupoli() {
        final GreekTransliterator transliterator = new GreekTransliterator("Ερμούπολη");
        final String transliteration = transliterator.transliterate();
        Assert.assertEquals("Ermoupoli", transliteration);
    }

    @Test
    public void testPiraeus() {
        final GreekTransliterator transliterator = new GreekTransliterator("Πειραιάς");
        final String transliteration = transliterator.transliterate();
        Assert.assertEquals("Peiraias", transliteration);
    }

    @Test
    public void testLykovrysiPefki() {
        final GreekTransliterator transliterator = new GreekTransliterator("Λυκόβρυση-Πεύκη");
        final String transliteration = transliterator.transliterate();
        Assert.assertEquals("Lykovrysi-Pefki", transliteration);
    }

    @Test
    public void testPapagouCholargos() {
        final GreekTransliterator transliterator = new GreekTransliterator("Παπάγου-Χολαργός");
        final String transliteration = transliterator.transliterate();
        Assert.assertEquals("Papagou-Cholargos", transliteration);
    }

    @Test
    public void testVisaltia() {
        final GreekTransliterator transliterator = new GreekTransliterator("Βισαλτία");
        final String transliteration = transliterator.transliterate();
        Assert.assertEquals("Visaltia", transliteration);
    }

    @Test
    public void testDioOlympos() {
        final GreekTransliterator transliterator = new GreekTransliterator("Δίο-Όλυμπος");
        final String transliteration = transliterator.transliterate();
        Assert.assertEquals("Dio-Olympos", transliteration);
    }

    @Test
    public void testOraiokastro() {
        final GreekTransliterator transliterator = new GreekTransliterator("Ωραιόκαστρο");
        final String transliteration = transliterator.transliterate();
        Assert.assertEquals("Oraiokastro", transliteration);
    }

    @Test
    public void testKentrikaTzoumerka() {
        final GreekTransliterator transliterator = new GreekTransliterator("Κεντρικά Τζουμέρκα");
        final String transliteration = transliterator.transliterate();
        Assert.assertEquals("Kentrika Tzoumerka", transliteration);
    }

    @Test
    public void testBampini() {
        final GreekTransliterator transliterator = new GreekTransliterator("Μπαμπίνη");
        final String transliteration = transliterator.transliterate();
        Assert.assertEquals("Bampini", transliteration);
    }
}
