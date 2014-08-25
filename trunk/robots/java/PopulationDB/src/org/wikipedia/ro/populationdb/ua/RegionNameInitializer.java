package org.wikipedia.ro.populationdb.ua;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.wikipedia.Wiki;
import org.wikipedia.ro.populationdb.ua.model.Region;

public class RegionNameInitializer extends LazyInitializer<String> {

    private final Region region;
    private final Wiki wiki;

    public RegionNameInitializer(final Region region, final Wiki wiki) {
        super();
        this.region = region;
        this.wiki = wiki;
    }

    @Override
    protected String initialize() throws ConcurrentException {
        final String roName = StringUtils.defaultIfBlank(region.getRomanianName(), region.getTransliteratedName());
        final List<String> candidateNames = Arrays.asList("Regiunea " + roName, "Regiunea " + roName + ", Ucraina");

        boolean[] existanceArray = null;
        try {
            existanceArray = wiki.exists(candidateNames.toArray(new String[candidateNames.size()]));
        } catch (final IOException e) {
            throw new ConcurrentException(e);
        }
        for (int i = 0; i < candidateNames.size(); i++) {
            try {
                if (!existanceArray[i] || UAUtils.isInCategoryTree(candidateNames.get(i), wiki, 3, "Regiuni ale Ucrainei")) {
                    return candidateNames.get(i);
                }
            } catch (final IOException e) {
                throw new ConcurrentException(e);
            }

        }
        return candidateNames.get(candidateNames.size() - 1);
    }
}
