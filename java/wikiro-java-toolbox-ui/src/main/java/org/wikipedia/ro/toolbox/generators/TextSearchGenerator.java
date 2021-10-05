package org.wikipedia.ro.toolbox.generators;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.wikipedia.Wiki;
import org.wikipedia.ro.Generator;

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

    public List<String> getGeneratedTitles() throws IOException {
        if (null == pagesList) {
            List<Map<String, Object>> searchResultsArray = wiki.search(text, wiki.ALL_NAMESPACES);
            pagesList = new ArrayList<String>();
            for (Map<String, Object> eachResultMap : searchResultsArray) {
                pagesList.add(eachResultMap.get("title").toString());
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
