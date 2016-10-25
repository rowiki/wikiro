package org.wikipedia.ro.astroe.generators;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class SinglePageGenerator implements Generator {

    private String page;
    private List<String> pagesList = null;

    public SinglePageGenerator(String page) {
        this.page = page;
    }

    @Override
    public List<String> getGeneratedTitles() throws IOException {
        if (null == pagesList) {
            pagesList = Arrays.asList(page);
        }
        return pagesList;
    }
    @Override
    public String getDescriptionKey() {
        return "generator.singlePage.description";
    }

    @Override
    public int getNumberOfTextFields() {
        return 1;
    }

    @Override
    public String[] getTextFieldsLabelKeys() {
        return new String[]{"generator.singlePage.page"};
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

}
