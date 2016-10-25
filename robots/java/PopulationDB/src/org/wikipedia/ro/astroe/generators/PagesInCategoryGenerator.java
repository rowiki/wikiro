package org.wikipedia.ro.astroe.generators;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.Wiki;

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
                .getCategoryMembers(StringUtils.prependIfMissing(page, wiki.namespaceIdentifier(Wiki.CATEGORY_NAMESPACE)));
            pagesList = Arrays.asList(pagesArray);
        }
        return pagesList;
    }
    @Override
    public String getDescriptionKey() {
        return "generator.pagesInCat.description";
    }

    @Override
    public int getNumberOfTextFields() {
        return 1;
    }

    @Override
    public String[] getTextFieldsLabelKeys() {
        return new String[]{"generator.pagesInCat.cat"};
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

}
