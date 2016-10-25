package org.wikipedia.ro.astroe.generators;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.wikipedia.Wiki;

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
    @Override
    public String getDescriptionKey() {
        return "generator.linksToPage.description";
    }

    @Override
    public int getNumberOfTextFields() {
        return 1;
    }

    @Override
    public String[] getTextFieldsLabelKeys() {
        return new String[]{"generator.linksToPage.page"};
    }
    public String getPage() {
        return page;
    }
    public void setPage(String page) {
        this.page = page;
    }
}
