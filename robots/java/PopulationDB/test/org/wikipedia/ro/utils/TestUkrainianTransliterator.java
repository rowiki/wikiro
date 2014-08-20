package org.wikipedia.ro.utils;

import org.junit.Test;
import org.wikipedia.ro.populationdb.util.Transliterator;
import org.wikipedia.ro.populationdb.util.UkrainianTransliterator;

public class TestUkrainianTransliterator {
    @Test
    public void testTransliterateIIAfterI() {
        final Transliterator t = new UkrainianTransliterator("Великокарабчіївська");
        t.transliterate();
    }
}
