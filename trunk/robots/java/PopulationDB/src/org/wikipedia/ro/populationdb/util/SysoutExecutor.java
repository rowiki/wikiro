package org.wikipedia.ro.populationdb.util;

public class SysoutExecutor implements Executor {

    public void save(String title, String text, String comment) {
        System.out.println("--- Editing page with title \"" + title + "\", setting text to:\n" + text);
    }

    public void link(String fromwiki, String fromtitle, String towiki, String totitle) throws Exception {
        System.out.println("--- Linking " + fromwiki + ":" + fromtitle + " to " + towiki + ":" + totitle);
    }

}
