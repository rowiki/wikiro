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
import org.wikipedia.ro.populationdb.ua.model.Settlement;
import org.wikipedia.ro.populationdb.util.ParameterReader;

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
            final String roRaionName = StringUtils.defaultIfBlank(raion.getRomanianName(), raion.getTransliteratedName());
            final String translRaionName = raion.getTransliteratedName();

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
            if (0 < commune.getTown() && !commune.equals(commune.getRaion().getCapital()) || commune.getSettlements().size() < 2) {
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

    public static List<String> getPossibleSettlementNames(final Settlement settlement, final Wiki wiki,
                                                          final boolean singleInWiki, final boolean singleInRegion,
                                                          final boolean singleInRaion) {
        final Commune com = settlement.getCommune();
        final Raion rai = com.getRaion();
        settlement.computeRegion();
        final String roSettlementName = StringUtils.defaultIfBlank(settlement.getRomanianName(),
            settlement.getTransliteratedName());
        final String translSettlementName = settlement.getTransliteratedName();
        final String roRegionName = StringUtils.defaultIfBlank(settlement.computeRegion().getRomanianName(), settlement
            .computeRegion().getTransliteratedName());
        final String translRegionName = settlement.computeRegion().getTransliteratedName();
        final String roCommuneName = StringUtils.defaultIfBlank(com.getRomanianName(), com.getTransliteratedName());
        final String translCommuneName = com.getTransliteratedName();

        final Set<String> ret = new LinkedHashSet<String>();

        if (singleInRegion) {
            ret.add(roSettlementName + ", " + roRegionName);
            ret.add(roSettlementName + ", " + translRegionName);
            ret.add(translSettlementName + ", " + roRegionName);
            ret.add(translSettlementName + ", " + translRegionName);
        }
        if (null != rai) {
            final String roRaionName = StringUtils.defaultIfBlank(rai.getRomanianName(), rai.getTransliteratedName());
            final String translRaionName = rai.getTransliteratedName();

            ret.add(roSettlementName + " (" + translCommuneName + "), raionul " + roRaionName + ", regiunea " + roRegionName);
            ret.add(roSettlementName + " (" + roCommuneName + "), raionul " + roRaionName + ", regiunea " + roRegionName);
            ret.add(roSettlementName + " (" + translCommuneName + "), raionul " + translRaionName + ", regiunea "
                + roRegionName);
            ret.add(roSettlementName + " (" + roCommuneName + "), raionul " + translRaionName + ", regiunea " + roRegionName);
            ret.add(roSettlementName + " (" + translCommuneName + "), raionul " + roRaionName + ", regiunea "
                + translRegionName);
            ret.add(roSettlementName + " (" + roCommuneName + "), raionul " + roRaionName + ", regiunea " + translRegionName);
            ret.add(roSettlementName + " (" + translCommuneName + "), raionul " + translRaionName + ", regiunea "
                + translRegionName);
            ret.add(roSettlementName + " (" + roCommuneName + "), raionul " + translRaionName + ", regiunea "
                + translRegionName);
            ret.add(translSettlementName + " (" + translCommuneName + "), raionul " + roRaionName + ", regiunea "
                + roRegionName);
            ret.add(translSettlementName + " (" + roCommuneName + "), raionul " + roRaionName + ", regiunea " + roRegionName);
            ret.add(translSettlementName + " (" + translCommuneName + "), raionul " + translRaionName + ", regiunea "
                + roRegionName);
            ret.add(translSettlementName + " (" + roCommuneName + "), raionul " + translRaionName + ", regiunea "
                + roRegionName);
            ret.add(translSettlementName + " (" + translCommuneName + "), raionul " + roRaionName + ", regiunea "
                + translRegionName);
            ret.add(translSettlementName + " (" + roCommuneName + "), raionul " + roRaionName + ", regiunea "
                + translRegionName);
            ret.add(translSettlementName + " (" + translCommuneName + "), raionul " + translRaionName + ", regiunea "
                + translRegionName);
            ret.add(translSettlementName + " (" + roCommuneName + "), raionul " + translRaionName + ", regiunea "
                + translRegionName);
            if (singleInRaion) {
                ret.add(roSettlementName + ", raionul " + roRaionName + ", regiunea " + roRegionName);
                ret.add(roSettlementName + ", raionul " + translRaionName + ", regiunea " + roRegionName);
                ret.add(roSettlementName + ", raionul " + roRaionName + ", regiunea " + translRegionName);
                ret.add(roSettlementName + ", raionul " + translRaionName + ", regiunea " + translRegionName);
                ret.add(translSettlementName + ", raionul " + roRaionName + ", regiunea " + roRegionName);
                ret.add(translSettlementName + ", raionul " + translRaionName + ", regiunea " + roRegionName);
                ret.add(translSettlementName + ", raionul " + roRaionName + ", regiunea " + translRegionName);
                ret.add(translSettlementName + ", raionul " + translRaionName + ", regiunea " + translRegionName);
            }
            if (singleInWiki) {
                ret.add(translSettlementName);
                ret.add(roSettlementName);
            }
            ret.add(translSettlementName + " (" + translCommuneName + "), " + translRaionName);
            ret.add(translSettlementName + " (" + roCommuneName + "), " + translRaionName);
            ret.add(translSettlementName + " (" + translCommuneName + "), " + roRaionName);
            ret.add(translSettlementName + " (" + roCommuneName + "), " + roRaionName);
            ret.add(roSettlementName + " (" + translCommuneName + "), " + translRaionName);
            ret.add(roSettlementName + " (" + roCommuneName + "), " + translRaionName);
            ret.add(roSettlementName + " (" + translCommuneName + "), " + roRaionName);
            ret.add(roSettlementName + " (" + roCommuneName + "), " + roRaionName);
            if (singleInRaion) {
                ret.add(translSettlementName + ", " + translRaionName);
                ret.add(translSettlementName + ", " + roRaionName);
                ret.add(roSettlementName + ", " + translRaionName);
                ret.add(roSettlementName + ", " + roRaionName);
            }
        }

        return new ArrayList<String>(ret);
    }

    public static boolean isInCategoryTree(final String pageTitle, final Wiki wiki, final int depth, final String category) {
        String[] cats = null;
        boolean categoriesRead = false;

        do {
            try {
                cats = wiki.getCategories(pageTitle);
                categoriesRead = true;
            } catch (final IOException e) {
                e.printStackTrace();
                System.err.println("Retrying");
                categoriesRead = false;
            }
        } while (!categoriesRead);

        for (final String eachCat : cats) {
            if (StringUtils.equals(category, StringUtils.substringAfter(eachCat, ":"))) {
                return true;
            }
            if (0 < depth) {
                if (isInCategoryTree(eachCat, wiki, depth - 1, category)) {
                    return true;
                }
            }
        }
        return false;

    }

    public static boolean isInAnyCategoryTree(final String pageTitle, final Wiki wiki, final int depth,
                                              final String... categories) {
        for (final String eachCat : categories) {
            if (isInCategoryTree(pageTitle, wiki, depth, eachCat)) {
                return true;
            }
        }
        return false;

    }

    public static String resolveRedirect(final Wiki wiki, final String title) {
        boolean existenceChecked = false;

        do {
            try {
                if (!wiki.exists(new String[] { title })[0]) {
                    existenceChecked = true;
                    return null;
                }
                existenceChecked = true;
            } catch (final IOException e) {
                e.printStackTrace();
                System.err.println("Retrying...");
                existenceChecked = false;
            }
        } while (!existenceChecked);

        String retVal = null;
        boolean redirectResolved = false;
        do {
            try {
                retVal = StringUtils.defaultIfBlank(wiki.resolveRedirect(title), title);
                redirectResolved = true;
            } catch (final IOException e) {
                e.printStackTrace();
                redirectResolved = false;
            }
        } while (!redirectResolved);
        return retVal;
    }

    public static void copyParameterFromTemplate(final ParameterReader ibParaReader, final StringBuilder sb,
                                                 final String paramName) {
        copyParameterFromTemplate(ibParaReader, sb, paramName, paramName);
    }

    public static void copyParameterFromTemplate(final ParameterReader ibParaReader, final StringBuilder sb,
                                                 final String paramName, final String targetParamName) {
        if (!ibParaReader.getParams().containsKey(paramName)) {
            return;
        }
        sb.append("\n|");
        sb.append(targetParamName);
        sb.append('=');
        sb.append(ibParaReader.getParams().get(paramName));
    }

}
