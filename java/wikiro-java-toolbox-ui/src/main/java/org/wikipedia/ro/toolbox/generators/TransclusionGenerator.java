package org.wikipedia.ro.toolbox.generators;

import static org.apache.commons.lang3.StringUtils.prependIfMissing;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.wikipedia.Wiki;
import org.wikipedia.ro.Generator;

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

    public List<String> getGeneratedTitles() throws IOException {
        if (null == pagesList) {
            pagesList = wiki.whatTranscludesHere(
                List.of(prependIfMissing(template, wiki.namespaceIdentifier(Wiki.TEMPLATE_NAMESPACE) + ":")),
                Wiki.ALL_NAMESPACES).stream().findFirst().orElse(List.of());
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
