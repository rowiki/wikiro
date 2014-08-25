package org.wikipedia.ro.populationdb.ua;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.Wiki;
import org.wikipedia.ro.populationdb.ua.model.Commune;
import org.wikipedia.ro.populationdb.ua.model.Raion;

public class UAUtils {
    public static List<String> getPossibleRaionNames(final Raion raion, final Wiki wiki, final boolean singleInWiki) {
        final String roName = StringUtils.defaultIfBlank(raion.getRomanianName(), raion.getTransliteratedName());
        final String translRaionName = raion.getTransliteratedName();
        final String roRegionName = StringUtils.defaultIfBlank(raion.getRegion().getRomanianName(), raion.getRegion()
            .getTransliteratedName());
        final String translRegionName = raion.getRegion().getTransliteratedName();

        final Set<String> ret = new LinkedHashSet<String>();
        final String raionulText = raion.isMiskrada() ? "Orașul regional " : "Raionul ";
        if (singleInWiki) {
            ret.add(raionulText + roName);
            ret.add(raionulText + translRaionName);
        }
        ret.add(raionulText + translRaionName + ", " + translRegionName);
        ret.add(raionulText + translRaionName + ", " + roRegionName);
        ret.add(raionulText + roName + ", " + translRegionName);
        ret.add(raionulText + roName + ", " + roRegionName);

        return new ArrayList<String>(ret);
    }

    public static List<String> getPossibleCommuneNames(final Commune commune, final Wiki wiki, final boolean singleInWiki,
                                                       final boolean singleInRegion) {
        final String roName = StringUtils.defaultIfBlank(commune.getRomanianName(), commune.getTransliteratedName());
        final String translCommuneName = commune.getTransliteratedName();
        final String roRegionName = StringUtils.defaultIfBlank(commune.computeRegion().getRomanianName(), commune
            .computeRegion().getTransliteratedName());
        final String translRegionName = commune.computeRegion().getTransliteratedName();

        final Set<String> ret = new LinkedHashSet<String>();

        if (singleInRegion) {
            if (0 == commune.getTown()) {
                ret.add("Comuna " + roName + ", " + roRegionName);
                ret.add("Comuna " + roName + ", " + translRegionName);
                ret.add("Comuna " + translCommuneName + ", " + roRegionName);
                ret.add("Comuna " + translCommuneName + ", " + translRegionName);
            }
            if (0 < commune.getTown() || commune.getSettlements().size() < 2) {
                ret.add(roName + ", " + roRegionName);
                ret.add(roName + ", " + translRegionName);
                ret.add(translCommuneName + ", " + roRegionName);
                ret.add(translCommuneName + ", " + translRegionName);
            }
        }
        if (null != commune.getRaion()) {
            final Raion raion = commune.getRaion();
            final String roRaionName = StringUtils.defaultIfBlank(raion.getRegion().getRomanianName(), raion.getRegion()
                .getTransliteratedName());
            final String translRaionName = commune.getRaion().getTransliteratedName();

            if (0 == commune.getTown()) {
                ret.add("Comuna " + translCommuneName + ", raionul " + translRaionName + ", regiunea " + translRegionName);
                ret.add("Comuna " + translCommuneName + ", raionul " + roRaionName + ", regiunea " + translRegionName);
                ret.add("Comuna " + translCommuneName + ", raionul " + translRaionName + ", regiunea " + roRegionName);
                ret.add("Comuna " + translCommuneName + ", raionul " + roRaionName + ", regiunea " + roRegionName);
                ret.add("Comuna " + roName + ", raionul " + translRaionName + ", regiunea " + translRegionName);
                ret.add("Comuna " + roName + ", raionul " + roRaionName + ", regiunea " + translRegionName);
                ret.add("Comuna " + roName + ", raionul " + translRaionName + ", regiunea " + roRegionName);
                ret.add("Comuna " + roName + ", raionul " + roRaionName + ", regiunea " + roRegionName);
                if (singleInWiki) {
                    ret.add("Comuna " + translCommuneName);
                    ret.add("Comuna " + roName);
                }
                ret.add("Comuna " + translCommuneName + ", " + translRaionName);
                ret.add("Comuna " + translCommuneName + ", " + roRaionName);
                ret.add("Comuna " + roName + ", " + translRaionName);
                ret.add("Comuna " + roName + ", " + roRaionName);

            }
            if (0 < commune.getTown() || commune.getSettlements().size() < 2) {
                ret.add(roName + ", raionul " + roRaionName + ", regiunea " + roRegionName);
                ret.add(roName + ", raionul " + translRaionName + ", regiunea " + roRegionName);
                ret.add(roName + ", raionul " + roRaionName + ", regiunea " + translRegionName);
                ret.add(roName + ", raionul " + translRaionName + ", regiunea " + translRegionName);
                ret.add(translCommuneName + ", raionul " + roRaionName + ", regiunea " + roRegionName);
                ret.add(translCommuneName + ", raionul " + translRaionName + ", regiunea " + roRegionName);
                ret.add(translCommuneName + ", raionul " + roRaionName + ", regiunea " + translRegionName);
                ret.add(translCommuneName + ", raionul " + translRaionName + ", regiunea " + translRegionName);
                ret.add(translCommuneName + ", " + translRaionName);
                ret.add(translCommuneName + ", " + roRaionName);
                ret.add(roName + ", " + translRaionName);
                ret.add(roName + ", " + roRaionName);
            }
        }

        if (singleInWiki && (0 < commune.getTown() || commune.getSettlements().size() < 2)) {
            ret.add(translCommuneName);
            ret.add(roName);
        }

        return new ArrayList<String>(ret);
    }

    public static boolean isInCategoryTree(final String pageTitle, final Wiki wiki, final int depth, final String category)
        throws IOException {
        String[] cats = null;
        cats = wiki.getCategories(pageTitle);
        for (final String eachCat : cats) {
            if (StringUtils.equals("Categorie:" + category, eachCat)) {
                return true;
            }
            if (0 < depth) {
                return isInCategoryTree(eachCat, wiki, depth - 1, category);
            }
        }
        return false;

    }

    public static boolean isInAnyCategoryTree(final String pageTitle, final Wiki wiki, final int depth,
                                              final String... categories) {
        for (final String eachCat : categories) {
            if (isInAnyCategoryTree(pageTitle, wiki, depth, eachCat)) {
                return true;
            }
        }
        return false;

    }
}
