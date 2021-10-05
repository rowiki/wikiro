package org.wikipedia.ro.toolbox.generators;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.wikipedia.Wiki;
import org.wikipedia.ro.Generator;

@PageGenerator(labelKey = "generator.linkedFromPage.description", stringsConfigNumber = 1, stringsConfigLabelKeys = {
    "generator.linkedFromPage.page" })
public class LinkedFromPageGenerator implements Generator {

    private String page;
    private Wiki wiki;
    private List<String> pagesList = null;

    public LinkedFromPageGenerator(Wiki wiki, String page) {
        this.wiki = wiki;
        this.page = page;
    }

    public LinkedFromPageGenerator(Wiki wiki) {
        this.wiki = wiki;
    }

    public List<String> getGeneratedTitles() throws IOException {
        if (null == pagesList) {
            pagesList = wiki.getLinksOnPage(page);
        }
        return pagesList;
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

}
