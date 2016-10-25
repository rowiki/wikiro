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

}
