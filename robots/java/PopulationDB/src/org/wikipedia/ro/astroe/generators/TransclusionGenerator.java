package org.wikipedia.ro.astroe.generators;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.Wiki;

@PageGenerator(labelKey = "generator.transclusion.description", stringsConfigNumber = 1, stringsConfigLabelKeys = {
    "generator.transclusion.template" })
public class TransclusionGenerator implements Generator {
    private String template;
    private Wiki wiki;
    private List<String> pagesList = null;

    public TransclusionGenerator(Wiki wiki, String template) {
        this.wiki = wiki;
        this.template = template;
    }

    public TransclusionGenerator(Wiki wiki) {
        this.wiki = wiki;
    }

    @Override
    public List<String> getGeneratedTitles() throws IOException {
        if (null == pagesList) {
            String[] transclusions = wiki.whatTranscludesHere(
                StringUtils.prependIfMissing(template, wiki.namespaceIdentifier(Wiki.TEMPLATE_NAMESPACE)));
            pagesList = Arrays.asList(transclusions);
        }
        return pagesList;
    }


    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

}
