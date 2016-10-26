package org.wikipedia.ro.astroe.generators;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.wikipedia.Wiki;

@PageGenerator(labelKey = "generator.linksToPage.description", stringsConfigNumber = 1, stringsConfigLabelKeys = {
    "generator.linksToPage.page" })
public class PagesThatLinkToGenerator implements Generator {
    private String page;
    private Wiki wiki;
    private List<String> pagesList = null;

    public PagesThatLinkToGenerator(Wiki wiki, String page) {
        this.wiki = wiki;
        this.page = page;
    }

    public PagesThatLinkToGenerator(Wiki wiki) {
        this.wiki = wiki;
    }

    @Override
    public List<String> getGeneratedTitles() throws IOException {
        if (null == pagesList) {
            String[] pagesArray = wiki.whatLinksHere(page);
            pagesList = Arrays.asList(pagesArray);
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
