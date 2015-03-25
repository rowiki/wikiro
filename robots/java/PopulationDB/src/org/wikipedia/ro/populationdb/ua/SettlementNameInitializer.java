package org.wikipedia.ro.populationdb.ua;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

        List<String> candidateNames;
        try {
            candidateNames = UAUtils.getPossibleSettlementNames(commune, wiki, 1 >= nameOccurences,
                1 >= nameOccurencesInRegion, 1 >= nameOccurencesInRaion);
        } catch (IOException e1) {
            throw new ConcurrentException(e1);
        }
        boolean[] existanceArray = null;
        try {
            existanceArray = wiki.exists(candidateNames.toArray(new String[candidateNames.size()]));
        } catch (final IOException e) {
            throw new ConcurrentException(e);
        }
        Set<String> toRemove = new HashSet<String>();
        for (int i = 0; i < candidateNames.size(); i++) {
            if (existanceArray[i]) {
                final String actualCandidateTitle = UAUtils.resolveRedirect(wiki, candidateNames.get(i));
                if (UAUtils.isInAnyCategoryTree(actualCandidateTitle, wiki, 5, "Sate în Ucraina", "Localități în Ucraina",
                    "Regiuni ale Ucrainei", "Raioanele Ucrainei")) {
                    return actualCandidateTitle;
                } else {
                    System.out.println("Removing " + actualCandidateTitle + " and " + candidateNames.get(i));
                    toRemove.add(actualCandidateTitle);
                    toRemove.add(candidateNames.get(i));
                }
            }
        }
        if (candidateNames.size() == 0) {
            System.out.println("Village name could not be found for " + commune);
        }
        candidateNames.removeAll(toRemove);
        return candidateNames.get(candidateNames.size() - 1);
    }

}
