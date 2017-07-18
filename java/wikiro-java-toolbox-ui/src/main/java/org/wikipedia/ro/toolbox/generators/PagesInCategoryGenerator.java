package org.wikipedia.ro.toolbox.generators;

import static org.apache.commons.lang3.StringUtils.prependIfMissing;

import java.io.IOException;
import java.util.Arrays;
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
            String[] pagesArray = wiki
                .getCategoryMembers(prependIfMissing(page, wiki.namespaceIdentifier(Wiki.CATEGORY_NAMESPACE) + ":"));
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
