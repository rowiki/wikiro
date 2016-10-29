package org.wikipedia.ro.astroe.generators;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.wikipedia.Wiki;


@PageGenerator(labelKey = "generator.singlePage.description", stringsConfigNumber = 1, stringsConfigLabelKeys = {
    "generator.singlePage.page" })
public class SinglePageGenerator implements Generator {

    private String page;
    private List<String> pagesList = null;

    public SinglePageGenerator(String page) {
        this.page = page;
    }

    public SinglePageGenerator(Wiki wiki, String page) {
        this(page);
    }

    @Override
    public List<String> getGeneratedTitles() throws IOException {
        if (null == pagesList) {
            pagesList = Arrays.asList(page);
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
