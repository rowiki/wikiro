package org.wikipedia.ro.transliterator;

public abstract class Transliterator {
    protected final String text;

    public abstract String transliterate();

    public Transliterator(final String txt) {
        text = txt;
    }
}
