package org.wikipedia.ro.astroe.generators;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.wikipedia.Wiki;

@PageGenerator(labelKey = "generator.search.description", stringsConfigNumber = 1, stringsConfigLabelKeys = {
"generator.search.searchText" })
public class TextSearchGenerator implements Generator {
    private String text;
    private Wiki wiki;
    private List<String> pagesList = null;

    public TextSearchGenerator(Wiki wiki, String text) {
        this.wiki = wiki;
        this.text = text;
    }
    public TextSearchGenerator(Wiki wiki) {
        this.wiki = wiki;
    }

    @Override
    public List<String> getGeneratedTitles() throws IOException {
        if (null == pagesList) {
            String[][] searchResultsArray = wiki.search(text);
            pagesList = new ArrayList<String>();
            for (String[] eachArray : searchResultsArray) {
                pagesList.add(eachArray[0]);
            }
        }
        return pagesList;
    }

    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }

}
