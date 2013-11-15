package org.wikipedia.ro.populationdb.util;

public interface Executor {
    void save(String title, String text, String comment) throws Exception;

    void link(String fromwiki, String fromtitle, String towiki, String totitle) throws Exception;
}
