package org.wikipedia.ro.populationdb.ua;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.wikipedia.Wiki;
import org.wikipedia.ro.populationdb.ua.dao.Hibernator;
import org.wikipedia.ro.populationdb.ua.model.Commune;

public class CommuneNameInitializer extends LazyInitializer<String> {
    private final Commune commune;
    private final Wiki wiki;
    private final Hibernator hib;

    public CommuneNameInitializer(final Commune commune, final Wiki wiki, final Hibernator hib) {
        super();
        this.commune = commune;
        this.wiki = wiki;
        this.hib = hib;
    }

    @Override
    protected String initialize() throws ConcurrentException {
        int nameOccurences = hib.countCommunesByRomanianOrTransliteratedName(StringUtils.defaultIfBlank(
            commune.getTransliteratedName(), commune.getRomanianName()));
        int nameOccurencesInRegion = hib.countCommunesInRegionByRomanianOrTransliteratedName(
            StringUtils.defaultIfBlank(commune.getTransliteratedName(), commune.getRomanianName()), commune.computeRegion());
        if (0 < commune.getTown()) {
            nameOccurences = hib.countTownsByRomanianOrTransliteratedName(StringUtils.defaultIfBlank(
                commune.getTransliteratedName(), commune.getRomanianName()));
            nameOccurencesInRegion = hib.countTownsInRegionByRomanianOrTransliteratedName(
                StringUtils.defaultIfBlank(commune.getTransliteratedName(), commune.getRomanianName()), commune.computeRegion());
        }

        List<String> candidateNames;
        boolean[] existanceArray = null;
        try {
            candidateNames = UAUtils
                .getPossibleCommuneNames(commune, wiki, 1 >= nameOccurences, 1 >= nameOccurencesInRegion);
            existanceArray = wiki.exists(candidateNames.toArray(new String[candidateNames.size()]));
        } catch (final IOException e) {
            throw new ConcurrentException(e);
        }
        Set<String> toRemove = new HashSet<String>();
        for (int i = 0; i < candidateNames.size(); i++) {
            if (existanceArray[i]) {
                final String actualCandidateTitle = UAUtils.resolveRedirect(wiki, candidateNames.get(i));
                if (UAUtils.isInAnyCategoryTree(actualCandidateTitle, wiki, 3, "Așezări de tip urban în Ucraina", "Comunele Ucrainei", "Regiuni ale Ucrainei", "Raioanele Ucrainei",
                    "Orașe în Ucraina", "Localități în Ucraina")) {
                    return actualCandidateTitle;
                } else {
                    toRemove.add(actualCandidateTitle);
                    toRemove.add(candidateNames.get(i));
                }
            }
        }
        candidateNames.removeAll(toRemove);
        if (0 == candidateNames.size()) {
        	return null;
        }
        return candidateNames.get(candidateNames.size() - 1);
    }

}
