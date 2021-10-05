package org.wikipedia.ro.toolbox.generators;

import java.io.IOException;
import java.util.List;

import org.wikipedia.Wiki;
import org.wikipedia.ro.Generator;

@PageGenerator(labelKey = "generator.pagesInCat.description", stringsConfigNumber = 1, stringsConfigLabelKeys = {
    "generator.pagesInCat.cat" })
public class PagesInCategoryGenerator implements Generator {
    private String page;
    private Wiki wiki;
    private List<String> pagesList = null;

    public PagesInCategoryGenerator(Wiki wiki, String page) {
        this.wiki = wiki;
        this.page = page;
    }

    public PagesInCategoryGenerator(Wiki wiki) {
        this.wiki = wiki;
    }

    public List<String> getGeneratedTitles() throws IOException {
        if (null == pagesList) {
            pagesList = wiki.getCategoryMembers(page, Wiki.CATEGORY_NAMESPACE);
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
