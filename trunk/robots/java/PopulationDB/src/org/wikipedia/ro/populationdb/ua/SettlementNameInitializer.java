package org.wikipedia.ro.populationdb.ua;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.wikipedia.Wiki;
import org.wikipedia.ro.populationdb.ua.dao.Hibernator;
import org.wikipedia.ro.populationdb.ua.model.Settlement;

public class SettlementNameInitializer extends LazyInitializer<String> {
    private final Settlement commune;
    private final Wiki wiki;
    private final Hibernator hib;

    public SettlementNameInitializer(final Settlement commune, final Wiki wiki, final Hibernator hib) {
        super();
        this.commune = commune;
        this.wiki = wiki;
        this.hib = hib;
    }

    @Override
    protected String initialize() throws ConcurrentException {
        final int nameOccurences = hib.countSettlementsByRomanianOrTransliteratedName(defaultIfBlank(
            commune.getTransliteratedName(), commune.getRomanianName()));
        final int nameOccurencesInRegion = hib.countSettlementsInRegionByRomanianOrTransliteratedName(
            defaultIfBlank(commune.getTransliteratedName(), commune.getRomanianName()), commune.computeRegion());
        final int nameOccurencesInRaion = hib.countSettlementsInRaionByRomanianOrTransliteratedName(
            defaultIfBlank(commune.getTransliteratedName(), commune.getRomanianName()), commune.getCommune().getRaion());

        final List<String> candidateNames = UAUtils.getPossibleSettlementNames(commune, wiki, 1 == nameOccurences,
            1 == nameOccurencesInRegion, 1 == nameOccurencesInRaion);
        boolean[] existanceArray = null;
        try {
            existanceArray = wiki.exists(candidateNames.toArray(new String[candidateNames.size()]));
        } catch (final IOException e) {
            throw new ConcurrentException(e);
        }
        for (int i = 0; i < candidateNames.size(); i++) {
            try {
                if (existanceArray[i]) {
                    final String actualCandidateTitle = UAUtils.resolveRedirect(wiki, candidateNames.get(i));
                    if (UAUtils.isInAnyCategoryTree(actualCandidateTitle, wiki, 5, "Regiuni ale Ucrainei",
                        "Raioanele Ucrainei")) {
                        return actualCandidateTitle;
                    }
                }
            } catch (final IOException e) {
                throw new ConcurrentException(e);
            }
        }
        return candidateNames.get(candidateNames.size() - 1);
    }

}
