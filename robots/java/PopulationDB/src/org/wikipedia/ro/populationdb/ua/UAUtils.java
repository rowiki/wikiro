package org.wikipedia.ro.populationdb.ua;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.wikipedia.Wiki;
import org.wikipedia.ro.populationdb.ua.model.Commune;
import org.wikipedia.ro.populationdb.ua.model.Raion;
import org.wikipedia.ro.populationdb.ua.model.Settlement;
import org.wikipedia.ro.populationdb.util.ParameterReader;

public class UAUtils {
    public static List<String> getPossibleRaionNames(final Raion raion,
            final Wiki wiki, final boolean singleInWiki) {
        final String roName = StringUtils.defaultIfBlank(
                raion.getRomanianName(), raion.getTransliteratedName());
        final String translRaionName = raion.getTransliteratedName();
        final String roRegionName = StringUtils.defaultIfBlank(raion
                .getRegion().getRomanianName(), raion.getRegion()
                .getTransliteratedName());
        final String translRegionName = raion.getRegion()
                .getTransliteratedName();

        final Set<String> ret = new LinkedHashSet<String>();
        final String raionulText = raion.isMiskrada() ? "Orașul regional "
                : "Raionul ";
        if (singleInWiki) {
            ret.add(raionulText + roName);
            ret.add(raionulText + translRaionName);
        }
        ret.add(raionulText + translRaionName + ", " + translRegionName);
        ret.add(raionulText + translRaionName + ", " + roRegionName);
        ret.add(raionulText + roName + ", " + translRegionName);
        ret.add(raionulText + roName + ", " + roRegionName);

        if (raion.isMiskrada()) {
            ret.add(roName + ", " + translRegionName);
            ret.add(roName + ", " + roRegionName);
            ret.add(roName);
        }

        return new ArrayList<String>(ret);
    }

    public static List<String> getPossibleCommuneNames(final Commune commune,
            final Wiki wiki, final boolean singleInWiki,
            final boolean singleInRegion) throws IOException {
        final String roName = StringUtils.defaultIfBlank(
                commune.getRomanianName(), commune.getTransliteratedName());
        final String translCommuneName = commune.getTransliteratedName();
        final String roRegionName = StringUtils.defaultIfBlank(commune
                .computeRegion().getRomanianName(), commune.computeRegion()
                .getTransliteratedName());
        final String translRegionName = commune.computeRegion()
                .getTransliteratedName();

        final Set<String> ret = new LinkedHashSet<String>();
        Raion rai = commune.getRaion();
        boolean communeIsOwnedByMiskrada = null == rai
            || (rai.isMiskrada() && !StringUtils.equals(translCommuneName, rai.getTransliteratedName()));

        if (null == rai || singleInRegion) {
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
        if (singleInWiki && commune.getSettlements().size() < 2
                && 0 == commune.getTown()) {
            ret.add(translCommuneName);
            ret.add(roName);
        }
        if (null != rai) {
            final String roRaionName = StringUtils.defaultIfBlank(rai.getRomanianName(), rai.getTransliteratedName());
            final String translRaionName = rai.getTransliteratedName();
            String regType = StringUtils.equals(roRegionName, "Crimeea") ? " " : " regiunea ";
            String nameOfUpperUnit = communeIsOwnedByMiskrada ? " orașul regional " : " raionul ";
            if (0 == commune.getTown()) {
                    
                ret.add("Comuna " + translCommuneName + "," + nameOfUpperUnit + translRaionName + "," + regType + translRegionName);
                ret.add("Comuna " + translCommuneName + "," + nameOfUpperUnit + roRaionName + "," + regType + translRegionName);
                ret.add("Comuna " + translCommuneName + "," + nameOfUpperUnit + translRaionName + "," + regType + roRegionName);
                ret.add("Comuna " + translCommuneName + "," + nameOfUpperUnit + roRaionName + "," + regType + roRegionName);
                ret.add("Comuna " + roName + "," + nameOfUpperUnit + translRaionName + "," + regType + translRegionName);
                ret.add("Comuna " + roName + "," + nameOfUpperUnit + roRaionName + "," + regType + translRegionName);
                ret.add("Comuna " + roName + "," + nameOfUpperUnit + translRaionName + "," + regType + roRegionName);
                ret.add("Comuna " + roName + "," + nameOfUpperUnit + roRaionName + "," + regType + roRegionName);
                if (singleInWiki) {
                    ret.add("Comuna " + translCommuneName);
                    ret.add("Comuna " + roName);
                }
                ret.add("Comuna " + translCommuneName + ", " + translRaionName);
                ret.add("Comuna " + translCommuneName + ", " + roRaionName);
                ret.add("Comuna " + roName + ", " + translRaionName);
                ret.add("Comuna " + roName + ", " + roRaionName);

            }
            if (0 < commune.getTown() && !commune.equals(rai.getCapital())
                || commune.getSettlements().size() < 2) {
                ret.add(roName + "," + nameOfUpperUnit + roRaionName + "," + regType + roRegionName);
                ret.add(roName + "," + nameOfUpperUnit + translRaionName + "," + regType + roRegionName);
                ret.add(roName + "," + nameOfUpperUnit + roRaionName + "," + regType + translRegionName);
                ret.add(roName + "," + nameOfUpperUnit + translRaionName + "," + regType + translRegionName);
                ret.add(translCommuneName + "," + nameOfUpperUnit + roRaionName + "," + regType + roRegionName);
                ret.add(translCommuneName + "," + nameOfUpperUnit + translRaionName + "," + regType + roRegionName);
                ret.add(translCommuneName + "," + nameOfUpperUnit + roRaionName + "," + regType + translRegionName);
                ret.add(translCommuneName + "," + nameOfUpperUnit + translRaionName + "," + regType + translRegionName);
                ret.add(translCommuneName + ", " + translRaionName);
                ret.add(translCommuneName + ", " + roRaionName);
                if (StringUtils.equals(roName, translRaionName)) {
                    ret.add(roName);
                } else {
                    ret.add(roName + ", " + translRaionName);
                }
                if (StringUtils.equals(roName, roRaionName)) {
                    ret.add(roName + ", " + translRaionName);
                } else {
                    ret.add(roName + ", " + roRaionName);
                }
            }
        }

        if (singleInWiki && (0 < commune.getTown())) {
            ret.add(translCommuneName + ", Ucraina");
            ret.add(roName + ", Ucraina");
            ret.add(translCommuneName);
            ret.add(roName);
        }

        List<String> retList = new ArrayList<String>();
        retList.addAll(ret);
        return retList;
    }

    public static List<String> getPossibleSettlementNames(final Settlement settlement, final Wiki wiki,
                                                          final boolean singleInWiki, final boolean singleInRegion,
                                                          final boolean singleInRaion) throws IOException {
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

        boolean communeIsOwnedByMiskrada = null == rai
            || (rai.isMiskrada() && !StringUtils.equals(translCommuneName, rai.getTransliteratedName()));

        if (singleInRegion) {
            ret.add(roSettlementName + ", " + roRegionName);
            ret.add(roSettlementName + ", " + translRegionName);
            ret.add(translSettlementName + ", " + roRegionName);
            ret.add(translSettlementName + ", " + translRegionName);
        }
        String regType = StringUtils.equals(roRegionName, "Crimeea") ? " " : " regiunea ";
        if (communeIsOwnedByMiskrada && !StringUtils.equals(translSettlementName, translCommuneName)) {
            ret.add(roSettlementName + " (" + roCommuneName + ")," + regType + roRegionName);
            ret.add(roSettlementName + " (" + translCommuneName + ")," + regType + roRegionName);
            ret.add(translSettlementName + " (" + roCommuneName + ")," + regType + roRegionName);
            ret.add(translSettlementName + " (" + translCommuneName + ")," + regType + roRegionName);
            ret.add(roSettlementName + " (" + roCommuneName + "), " + roRegionName);
            ret.add(roSettlementName + " (" + translCommuneName + "), " + roRegionName);
            ret.add(translSettlementName + " (" + roCommuneName + "), " + roRegionName);
            ret.add(translSettlementName + " (" + translCommuneName + "), " + roRegionName);
            if (null != rai) {
                String translMiskradaName = rai.getTransliteratedName();
                String roMiskradaName = StringUtils.defaultIfBlank(rai.getRomanianName(), translMiskradaName);
                ret.add(roSettlementName + " (" + roCommuneName + "), orașul regional " + roMiskradaName + ", "
                    + roRegionName);
                ret.add(roSettlementName + " (" + translCommuneName + "), orașul regional " + roMiskradaName + ", "
                    + roRegionName);
                ret.add(translSettlementName + " (" + roCommuneName + "), orașul regional " + roMiskradaName + ", "
                    + roRegionName);
                ret.add(translSettlementName + " (" + translCommuneName + "), orașul regional " + roMiskradaName + ", "
                    + roRegionName);
                if (singleInRaion) {
                    ret.add(roSettlementName + ", orașul regional " + roMiskradaName + "," + regType + roRegionName);
                    ret.add(roSettlementName + ", orașul regional " + translMiskradaName + "," + regType + roRegionName);
                    ret.add(roSettlementName + ", orașul regional " + roMiskradaName + "," + regType + translRegionName);
                    ret.add(roSettlementName + ", orașul regional " + translMiskradaName + "," + regType + translRegionName);
                    ret.add(translSettlementName + ", orașul regional " + roMiskradaName + "," + regType + roRegionName);
                    ret.add(translSettlementName + ", orașul regional " + translMiskradaName + "," + regType + roRegionName);
                    ret.add(translSettlementName + ", orașul regional " + roMiskradaName + "," + regType + translRegionName);
                    ret.add(translSettlementName + ", orașul regional " + translMiskradaName + "," + regType
                        + translRegionName);
                    ret.add(translSettlementName + ", orașul regional " + roMiskradaName + "," + regType + translRegionName);
                    ret.add(translSettlementName + ", orașul regional " + translMiskradaName + "," + regType
                        + translRegionName);
                    ret.add(translSettlementName + ", " + translMiskradaName);
                    ret.add(translSettlementName + ", " + roMiskradaName);
                    ret.add(roSettlementName + ", " + translMiskradaName);
                    ret.add(roSettlementName + ", " + roMiskradaName);
                }
            }
        } else if (StringUtils.equals(translSettlementName, translCommuneName)) {
            ret.add(roSettlementName + " (" + roCommuneName + ")," + regType + roRegionName);
            ret.add(roSettlementName + " (" + translCommuneName + ")," + regType + roRegionName);
            ret.add(translSettlementName + " (" + roCommuneName + ")," + regType + roRegionName);
            ret.add(translSettlementName + " (" + translCommuneName + ")," + regType + roRegionName);
            ret.add(roSettlementName + " (" + roCommuneName + "), " + roRegionName);
            ret.add(roSettlementName + " (" + translCommuneName + "), " + roRegionName);
            ret.add(translSettlementName + " (" + roCommuneName + "), " + roRegionName);
            ret.add(translSettlementName + " (" + translCommuneName + "), " + roRegionName);
            if (null != rai) {
                String translMiskradaName = rai.getTransliteratedName();
                String roMiskradaName = StringUtils.defaultIfBlank(rai.getRomanianName(), translMiskradaName);
                    ret.add(roSettlementName + ", orașul regional " + roMiskradaName + "," + regType + roRegionName);
                    ret.add(roSettlementName + ", orașul regional " + translMiskradaName + "," + regType + roRegionName);
                    ret.add(roSettlementName + ", orașul regional " + roMiskradaName + "," + regType + translRegionName);
                    ret.add(roSettlementName + ", orașul regional " + translMiskradaName + "," + regType + translRegionName);
                    ret.add(translSettlementName + ", orașul regional " + roMiskradaName + "," + regType + roRegionName);
                    ret.add(translSettlementName + ", orașul regional " + translMiskradaName + "," + regType + roRegionName);
                    ret.add(translSettlementName + ", orașul regional " + roMiskradaName + "," + regType + translRegionName);
                    ret.add(translSettlementName + ", orașul regional " + translMiskradaName + "," + regType
                        + translRegionName);
                    ret.add(translSettlementName + ", orașul regional " + roMiskradaName + "," + regType + translRegionName);
                    ret.add(translSettlementName + ", orașul regional " + translMiskradaName + "," + regType
                        + translRegionName);
                    ret.add(translSettlementName + ", " + translMiskradaName);
                    ret.add(translSettlementName + ", " + roMiskradaName);
                    ret.add(roSettlementName + ", " + translMiskradaName);
                    ret.add(roSettlementName + ", " + roMiskradaName);
            }
            
        }
        if (!communeIsOwnedByMiskrada) {
            final String roRaionName = StringUtils.defaultIfBlank(rai.getRomanianName(), rai.getTransliteratedName());
            final String translRaionName = rai.getTransliteratedName();

            ret.add(roSettlementName + " (" + translCommuneName + "), raionul "
                    + roRaionName + "," + regType + " " + roRegionName);
            ret.add(roSettlementName + " (" + roCommuneName + "), raionul " + roRaionName + "," + regType + " " + roRegionName);
            ret.add(roSettlementName + " (" + translCommuneName + "), raionul " + translRaionName + "," + regType + " "
                + roRegionName);
            ret.add(roSettlementName + " (" + roCommuneName + "), raionul " + translRaionName + "," + regType + " " + roRegionName);
            ret.add(roSettlementName + " (" + translCommuneName + "), raionul " + roRaionName + "," + regType + " "
                + translRegionName);
            ret.add(roSettlementName + " (" + roCommuneName + "), raionul " + roRaionName + "," + regType + " " + translRegionName);
            ret.add(roSettlementName + " (" + translCommuneName + "), raionul " + translRaionName + "," + regType + " "
                + translRegionName);
            ret.add(roSettlementName + " (" + roCommuneName + "), raionul " + translRaionName + "," + regType + " "
                + translRegionName);
            ret.add(translSettlementName + " (" + translCommuneName + "), raionul " + roRaionName + "," + regType + " "
                + roRegionName);
            ret.add(translSettlementName + " (" + roCommuneName + "), raionul " + roRaionName + "," + regType + " " + roRegionName);
            ret.add(translSettlementName + " (" + translCommuneName + "), raionul " + translRaionName + "," + regType + " "
                + roRegionName);
            ret.add(translSettlementName + " (" + roCommuneName + "), raionul " + translRaionName + "," + regType + " "
                + roRegionName);
            ret.add(translSettlementName + " (" + translCommuneName + "), raionul " + roRaionName + "," + regType + " "
                + translRegionName);
            ret.add(translSettlementName + " (" + roCommuneName + "), raionul " + roRaionName + "," + regType + " "
                + translRegionName);
            ret.add(translSettlementName + " (" + translCommuneName + "), raionul " + translRaionName + "," + regType + " "
                + translRegionName);
            ret.add(translSettlementName + " (" + roCommuneName + "), raionul " + translRaionName + "," + regType + " "
                + translRegionName);
            if (singleInRaion) {
                ret.add(roSettlementName + ", raionul " + roRaionName + "," + regType + " " + roRegionName);
                ret.add(roSettlementName + ", raionul " + translRaionName + "," + regType + " " + roRegionName);
                ret.add(roSettlementName + ", raionul " + roRaionName + "," + regType + " " + translRegionName);
                ret.add(roSettlementName + ", raionul " + translRaionName + "," + regType + " " + translRegionName);
                ret.add(translSettlementName + ", raionul " + roRaionName + "," + regType + " " + roRegionName);
                ret.add(translSettlementName + ", raionul " + translRaionName + "," + regType + " " + roRegionName);
                ret.add(translSettlementName + ", raionul " + roRaionName + "," + regType + " " + translRegionName);
                ret.add(translSettlementName + ", raionul " + translRaionName + "," + regType + " " + translRegionName);
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
        List<String> retList = new ArrayList<String>();
        retList.addAll(ret);
        return retList;
    }

    public static boolean isInCategoryTree(final String pageTitle,
            final Wiki wiki, final int depth, final String category) {
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
            if (StringUtils.equals(category,
                    StringUtils.substringAfter(eachCat, ":"))) {
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

    public static boolean isInAnyCategoryTree(final String pageTitle,
            final Wiki wiki, final int depth, final String... categories) {
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
            if (ArrayUtils.contains(categories,
                    StringUtils.substringAfter(eachCat, ":"))) {
                return true;
            }
            if (0 < depth) {
                if (isInAnyCategoryTree(eachCat, wiki, depth - 1, categories)) {
                    return true;
                }
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
                retVal = StringUtils.defaultIfBlank(
                        wiki.resolveRedirect(title), title);
                redirectResolved = true;
            } catch (final IOException e) {
                e.printStackTrace();
                redirectResolved = false;
            }
        } while (!redirectResolved);
        return retVal;
    }

    public static void copyParameterFromTemplate(
            final ParameterReader ibParaReader, final StringBuilder sb,
            final String paramName) {
        copyParameterFromTemplate(ibParaReader, sb, paramName, paramName);
    }

    public static void copyParameterFromTemplate(
            final ParameterReader ibParaReader, final StringBuilder sb,
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
