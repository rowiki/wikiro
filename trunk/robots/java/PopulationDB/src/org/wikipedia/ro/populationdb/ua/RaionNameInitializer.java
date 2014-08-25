package org.wikipedia.ro.populationdb.ua;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.wikipedia.Wiki;
import org.wikipedia.ro.populationdb.ua.dao.Hibernator;
import org.wikipedia.ro.populationdb.ua.model.Raion;

public class RaionNameInitializer extends LazyInitializer<String> {
    private final Raion raion;
    private final Wiki wiki;
    private final Hibernator hib;

    public RaionNameInitializer(final Raion raion, final Wiki wiki, final Hibernator hib) {
        super();
        this.raion = raion;
        this.wiki = wiki;
        this.hib = hib;
    }

    @Override
    protected String initialize() throws ConcurrentException {

        final int nameOccurences = hib.countRaionsByRomanianOrTransliteratedName(StringUtils.defaultIfBlank(
            raion.getTransliteratedName(), raion.getRomanianName()));
        final List<String> candidateNames = UAUtils.getPossibleRaionNames(raion, wiki, 1 == nameOccurences);
        boolean[] existanceArray = null;
        try {
            existanceArray = wiki.exists(candidateNames.toArray(new String[candidateNames.size()]));
        } catch (final IOException e) {
            throw new ConcurrentException(e);
        }
        for (int i = 0; i < candidateNames.size(); i++) {
            if (existanceArray[i]
                && UAUtils.isInAnyCategoryTree(candidateNames.get(i), wiki, 3, "Regiuni ale Ucrainei", "Raioanele Ucrainei")) {
                return candidateNames.get(i);
            }
        }
        return candidateNames.get(candidateNames.size() - 1);
    }
}
