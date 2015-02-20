package org.wikipedia.ro.utils;

import org.junit.Assert;
import org.junit.Test;
import org.wikipedia.ro.populationdb.util.UkrainianTransliterator;

public class TestUkrainianTransliterator {
    @Test
    public void testScha() {
        UkrainianTransliterator trans = new UkrainianTransliterator("Піщанка");
        String translName = trans.transliterate();
        Assert.assertEquals("Pișceanka", translName);
    }

    @Test
    public void testYaAfterHardSign() {
        UkrainianTransliterator trans = new UkrainianTransliterator("Слов`яносербський");
        String translName = trans.transliterate();
        Assert.assertEquals("Slovianoserbskîi", translName);
    }
}
