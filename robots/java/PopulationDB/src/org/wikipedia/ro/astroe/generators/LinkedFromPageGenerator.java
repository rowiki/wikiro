package org.wikipedia.ro.astroe.generators;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.wikipedia.Wiki;

public class LinkedFromPageGenerator implements Generator {

    private String page;
    private Wiki wiki;
    private List<String> pagesList = null;
    
    public LinkedFromPageGenerator(Wiki wiki, String page) {
        this.wiki = wiki;
        this.page = page;
    }
    
    @Override
    public List<String> getGeneratedTitles() throws IOException {
        if (null == pagesList) {
            String[] pagesArray = wiki.getLinksOnPage(page);
            pagesList = Arrays.asList(pagesArray);
        }
        return pagesList;
    }
}
