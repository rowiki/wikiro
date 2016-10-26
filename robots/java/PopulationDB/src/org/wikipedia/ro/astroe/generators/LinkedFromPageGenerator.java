package org.wikipedia.ro.astroe.generators;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.wikipedia.Wiki;

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

    @Override
    public List<String> getGeneratedTitles() throws IOException {
        if (null == pagesList) {
            String[] pagesArray = wiki.getLinksOnPage(page);
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

    @Override
    public String getDescriptionKey() {
        return "generator.linkedFromPage.description";
    }

    @Override
    public int getNumberOfTextFields() {
        return 1;
    }

    @Override
    public String[] getTextFieldsLabelKeys() {
        return new String[] { "generator.linkedFromPage.page" };
    }
}
