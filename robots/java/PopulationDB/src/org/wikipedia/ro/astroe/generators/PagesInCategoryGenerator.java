package org.wikipedia.ro.astroe.generators;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.Wiki;

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

    @Override
    public List<String> getGeneratedTitles() throws IOException {
        if (null == pagesList) {
            String[] pagesArray = wiki
                .getCategoryMembers(StringUtils.prependIfMissing(page, wiki.namespaceIdentifier(Wiki.CATEGORY_NAMESPACE) + ":"));
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
