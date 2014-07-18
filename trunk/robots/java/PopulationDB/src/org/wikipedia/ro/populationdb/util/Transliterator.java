package org.wikipedia.ro.populationdb.util;

public abstract class Transliterator {
    protected final String text;

    public abstract String transliterate();

    public Transliterator(final String txt) {
        text = txt;
    }
}
