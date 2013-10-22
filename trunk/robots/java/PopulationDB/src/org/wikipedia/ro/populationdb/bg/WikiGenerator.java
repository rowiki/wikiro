package org.wikipedia.ro.populationdb.bg;

import static org.apache.commons.collections.ListUtils.transformedList;
import static org.apache.commons.lang3.ArrayUtils.contains;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.startsWithAny;

import java.awt.Color;
import java.awt.Paint;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

import org.apache.commons.collections.Transformer;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.jfree.data.general.DefaultPieDataset;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;
import org.wikibase.Wikibase;
import org.wikipedia.Wiki;
import org.wikipedia.ro.populationdb.bg.model.EthnicallyStructurable;
import org.wikipedia.ro.populationdb.bg.model.Nationality;
import org.wikipedia.ro.populationdb.bg.model.Obshtina;
import org.wikipedia.ro.populationdb.bg.model.Region;
import org.wikipedia.ro.populationdb.bg.model.Settlement;
import org.wikipedia.ro.populationdb.util.ParameterReader;
import org.wikipedia.ro.populationdb.util.Utilities;

public class WikiGenerator {

    private static final String findNationalityByNameQueryString = "from Nationality nat where nat.nume=:name";
    private static final int MAX_ENUMED_VILLAGES = 12;
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_OBSHTINA = false;
    private final Wiki rowiki = new Wiki("ro.wikipedia.org");
    private final Wiki bgwiki = new Wiki("bg.wikipedia.org");
    private final Wikibase dwiki = new Wikibase();
    private final Properties credentials = new Properties();
    private Session ses = null;
    private final Map<Nationality, Paint> nationColorMap = new HashMap<Nationality, Paint>();
    private final Map<String, Nationality> nationNameMap = new HashMap<String, Nationality>();
    private final Map<String, String> nationLinkMap = new HashMap<String, String>() {
        {
            put("Bulgari", "[[bulgari]]");
            put("Turci", "[[Turcii din Bulgaria|turci]]");
            put("Romi", "[[Romii din Bulgaria|romi]]");
            put("Altele", "alte naționalități");
            put("Nicio identificare", "persoane neidentificate etnic");
        }
    };

    private final Transformer wikilinkifier = new Transformer() {

        public Object transform(final Object input) {
            if (input.toString().startsWith("[[") && input.toString().endsWith("]]")) {
                return input;
            }
            return "[[" + input + "|]]";
        }
    };
    private final Pattern footnotesRegex = Pattern
        .compile("\\{\\{(?:(?:L|l)istănote|(?:R|r)eflist)|(?:\\<\\s*references\\s*\\/\\>)");
    private final Pattern regexBgInfobox = Pattern
        .compile("\\{\\{\uFEFF\u041E\u0431\u0449\u0438\u043D\u0430 \u0432 \u0411\u044A\u043B\u0433\u0430\u0440\u0438\u044F\\s*(\\|\\s*(?:\\{\\{[^{}]*+\\}\\}|[^{}])*+)?\\}\\}\\s*");
    private static Pattern regexBgSettlementInfobox = Pattern
        .compile("\\{\\{\uFEFF\u0421\u0435\u043B\u0438\u0449\u0435 \u0432 \u0411\u044A\u043B\u0433\u0430\u0440\u0438\u044F\\s*(\\|\\s*(?:\\{\\{[^{}]*+\\}\\}|(?:[^{}]))*+)?\\}\\}\\s*");

    public static void main(final String[] args) {
        final WikiGenerator generator = new WikiGenerator();
        try {
            generator.login();
            generator.generateRegionNavTemplates();
            generator.generateObshtinas();
            generator.generateVillages();
        } catch (final FailedLoginException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final LoginException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            generator.cleanup();
        }
    }

    private void generateVillages() throws IOException, LoginException {
        /*
         * final Criteria settlementCriteria = ses.createCriteria(Settlement.class).addOrder(Order.asc("numeRo")); final
         * List<Settlement> stlmnts = settlementCriteria.list();
         */
        final Query partialQuery = ses.createQuery("from Settlement s where s.numeRo=:ablanita");
        partialQuery.setParameter("ablanita", "Ablanița");
        final List<Settlement> stlmnts = partialQuery.list();

        final STGroup templateGroup = new STGroupFile("templates/bg/section_settlement.stg");

        for (final Settlement stlmnt : stlmnts) {

            final String demogSection = generateDemographicsForVillage(templateGroup, stlmnt);
            final String existingRoTitle = getExistingRoTitleOfArticleWithSubject(stlmnt);
            final StringBuilder articleText = new StringBuilder();
            final String desiredRoTitle = getNonExistingRoTitleArticleWithSubject(stlmnt, existingRoTitle);
            if (null == existingRoTitle && null == desiredRoTitle) {
                continue;
            }
            if (null == existingRoTitle) {
                articleText.append(generateIntroSectionForVillage(templateGroup, stlmnt));
                articleText.append(demogSection);
                articleText.append("\n== Note ==\n{{reflist}}\n");
                final Obshtina ob = stlmnt.getObshtina();
                final Region reg = ob.getRegion();
                articleText.append("\n{{Localități în comuna " + ob.getNumeRo() + ", " + reg.getNumeRo() + "}}");
                articleText.append("\n[[Categorie:" + (stlmnt.isTown() ? "Orașe" : "Sate") + " în regiunea "
                    + reg.getNumeRo() + "]]");

                if (!DEBUG) {
                    rowiki.edit(desiredRoTitle, articleText.toString(), "Robot: creare articol despre "
                        + (stlmnt.isTown() ? "oraș" : "sat") + " din Bulgaria");
                    final String bgArticleTitle = findBgCounterpartForSettlement(stlmnt);
                    dwiki.linkPages("rowiki", desiredRoTitle, "bgwiki", bgArticleTitle);
                }
            } else {
                articleText.append(rowiki.getPageText(existingRoTitle));
                final int startOfCategories = StringUtils.indexOf(articleText, "[[Categor");
                final int startOfNavTemplate = StringUtils.indexOf(articleText, "{{Obștina");
                final int startOfNavTemplate1 = StringUtils.indexOf(articleText, "{{Comuna");
                final int startOfNavTemplate2 = StringUtils.indexOf(articleText, "{{Bulgaria-");
                final int startOfNote = Math.max(StringUtils.indexOf(articleText, "== Note"),
                    StringUtils.indexOf(articleText, "==Note"));
                final int startOfSeeAlso = Math.max(StringUtils.indexOf(articleText, "== Vezi"),
                    StringUtils.indexOf(articleText, "==Vezi"));

                int insertLocation = StringUtils.length(articleText) - 1;

                for (final int index : new int[] { startOfCategories, startOfNavTemplate, startOfNavTemplate1,
                    startOfNavTemplate2, startOfNote, startOfSeeAlso }) {
                    if (0 < index) {
                        insertLocation = Math.min(index, insertLocation);
                    }
                }
                if (!footnotesRegex.matcher(articleText).find()) {
                    articleText.insert(insertLocation, "== Note ==\n{{reflist}}\n");
                }
                if (!StringUtils.contains(articleText, "== Demografie")
                    && !StringUtils.contains(articleText, "==Demografie")) {
                    articleText.insert(insertLocation, demogSection);
                }

                if (!DEBUG) {
                    rowiki.edit(existingRoTitle, articleText.toString(),
                        "Robot: generare automată a secțiunii de demografie");
                }
            }
            createSettlementCategoryIfNotExist(stlmnt);
            redirectAlternativeSettlementNames(defaultString(existingRoTitle, desiredRoTitle), stlmnt);
        }
    }

    private void createSettlementCategoryIfNotExist(final Settlement stlmnt) throws LoginException, IOException {
        final String stlmType = stlmnt.isTown() ? "Oraș" : "Sat";
        final String categoryName = stlmType + "e în regiunea "
            + stlmnt.getObshtina().getRegion().getNumeRo();

        final Map categoryInfo = rowiki.getPageInfo("Categorie:" + categoryName);
        final boolean categoryExists = isTrue((Boolean) categoryInfo.get("exists"));

        if (!categoryExists) {
            final StringBuilder catSB = new StringBuilder("[[Categorie:");
            catSB.append(stlmType);
            catSB.append("e în Bulgaria|");
            catSB.append(stlmnt.getObshtina().getRegion().getNumeRo());
            catSB.append("]]");

            rowiki.edit("Categorie:" + categoryName, catSB.toString(),
                "Robot: creare categorie de localități din Bulgaria pe regiuni");
        }
    }

    private String generateIntroSectionForVillage(final STGroup group, final Settlement stlmnt) throws IOException {
        final StringBuilder sb = new StringBuilder("{{Infocaseta Așezare|");
        final Map<String, String> infoboxParams = new LinkedHashMap<String, String>();
        infoboxParams.put("nume", stlmnt.getNumeRo());
        infoboxParams.put("nume_nativ", stlmnt.getNumeBg());
        infoboxParams.put("tip_subdiviziune", "[[Țările lumii|Țară]]");
        infoboxParams.put("nume_subdiviziune", "{{BUL}}");
        infoboxParams.put("tip_subdiviziune1", "[[Regiunile Bulgariei|Regiune]]");
        infoboxParams.put("nume_subdiviziune1", "[[Regiunea " + stlmnt.getObshtina().getRegion().getNumeRo() + "|"
            + stlmnt.getObshtina().getRegion().getNumeRo() + "]]");
        infoboxParams.put("tip_subdiviziune2", "[[Comunele Bulgariei|Comună]]");
        infoboxParams.put("nume_subdiviziune2", "[[Comuna " + stlmnt.getObshtina().getNumeRo() + "|"
            + stlmnt.getObshtina().getNumeRo() + "]]");
        infoboxParams.put("tip_asezare", stlmnt.isTown() ? "Oraș" : "Sat");
        infoboxParams.put("recensământ", "2011");
        infoboxParams.put("populație", String.valueOf(stlmnt.getPopulation()));
        infoboxParams.put("populație_note_subsol", "<ref name=\"varste2011\"/>");

        final String bgText = bgwiki.getPageText(findBgCounterpartForSettlement(stlmnt));
        final Matcher infoboxMatcher = regexBgSettlementInfobox.matcher(bgText);
        boolean foundRegex = false;
        if ((foundRegex = infoboxMatcher.find()) || StringUtils.contains(bgText, "{{Селище в България")) {
            ParameterReader params = null;
            if (foundRegex) {
                params = new ParameterReader(infoboxMatcher.group());
            } else {
                params = new ParameterReader(StringUtils.substring(bgText,
                    StringUtils.indexOf(bgText, "{{Селище в България")));
            }
            params.run();
            final Map<String, String> bgparams = params.getParams();
            final String img = bgparams.get("картинка");
            if (!StringUtils.isEmpty(img)) {
                infoboxParams.put("imagine", img);
            }
            final String altit = bgparams.get("надм-височина");
            if (!StringUtils.isEmpty(altit)) {
                infoboxParams.put("altitudine", altit);
            }
            final String codpostal = bgparams.get("пощ-код");
            if (!isEmpty(codpostal)) {
                infoboxParams.put("codpoștal", codpostal);
            }
            final String ucattu = bgparams.get("екатте");
            if (!isEmpty(ucattu)) {
                infoboxParams.put("tip_cod_clasificare", "UCATTU");
                infoboxParams.put("cod_clasificare", ucattu);
            }
            final String latd = bgparams.get("сев-ширина");
            if (!StringUtils.isEmpty(latd)) {
                infoboxParams.put("latd", latd);
                infoboxParams.put("latNS", "N");
                infoboxParams.put("pushpin_map", "Bulgaria");
            }
            final String longd = bgparams.get("изт-дължина");
            if (!StringUtils.isEmpty(longd)) {
                infoboxParams.put("longd", longd);
                infoboxParams.put("longEV", "E");
            }
            final String aria = bgparams.get("площ");
            if (!StringUtils.isEmpty(aria)) {
                infoboxParams.put("suprafață_totală_km2", "{{formatnum:" + aria + "}}");
            }
            final String prefix = bgparams.get("тел-код");
            if (!isEmpty(prefix)) {
                infoboxParams.put("prefix_telefonic", prefix);
            }
        }

        for (final String ibParam : infoboxParams.keySet()) {
            sb.append("\n|");
            sb.append(ibParam);
            sb.append(" = ");
            sb.append(infoboxParams.get(ibParam));
        }
        sb.append("\n}}\n");

        final ST intro = group.getInstanceOf("introSettlement");
        intro.add("tip", stlmnt.isTown() ? "oraș" : "sat");
        intro.add("numeRo", stlmnt.getNumeRo());
        intro.add("numeBg", stlmnt.getNumeBg());
        intro.add("obstina", stlmnt.getObshtina().getNumeRo());
        intro.add("regiune", stlmnt.getObshtina().getRegion().getNumeRo());
        sb.append(intro.render());

        return sb.toString();
    }

    private String findBgCounterpartForSettlement(final Settlement stlmnt) throws IOException {
        final List<String> possibleNames = Arrays.asList(stlmnt.getNumeBg() + " (община " + stlmnt.getObshtina().getNumeBg()
            + ")", stlmnt.getNumeBg() + " (Област " + stlmnt.getObshtina().getRegion().getNumeBg() + ")", stlmnt.getNumeBg()
            + " (село)", stlmnt.getNumeBg());

        for (final String possibleName : possibleNames) {
            final HashMap pageInfo = bgwiki.getPageInfo(possibleName);
            final Boolean exists = (Boolean) pageInfo.get("exists");
            if (isTrue(exists)) {
                return defaultString(bgwiki.resolveRedirect(possibleName), possibleName);
            }
        }
        return null;
    }

    private String generateDemographicsForVillage(final STGroup stgroup, final Settlement settlement) {
        final StringBuilder demographics = new StringBuilder(
            "\n== Demografie ==\n<!-- Start secțiune generată de Andrebot -->");
        final ST piechart = stgroup.getInstanceOf("piechart");
        final int population = settlement.getPopulation();
        final String tip = settlement.isTown() ? "oraș" : "sat";
        piechart.add("nume", settlement.getNumeRo());
        piechart.add("tip", tip);

        final Map<Nationality, Integer> ethnicStructure = settlement.getEthnicStructure();
        final DefaultPieDataset dataset = new DefaultPieDataset();
        final int totalKnownEthnicity = computeDataset(population, ethnicStructure, dataset);

        renderPiechart(demographics, piechart, population, dataset);

        final ST demogIntro = stgroup.getInstanceOf("introTmpl");
        demogIntro.add("nume", settlement.getNumeRo());
        demogIntro.add("populatie", population);
        demogIntro.add("tip", tip);
        demographics.append(demogIntro.render());

        final Nationality majNat = getMajority(settlement);
        final List<Nationality> minorities = getMinorities(settlement);
        writeEthnodemographics(stgroup, demographics, population, ethnicStructure, majNat, minorities);

        writeUnknownEthnicity(stgroup, demographics, population, totalKnownEthnicity);

        demographics.append("\n<!--Sfârșit secțiune generată de Andrebot -->");
        return demographics.toString();
    }

    private void writeUnknownEthnicity(final STGroup stgroup, final StringBuilder demographics, final int population,
                                       final int totalKnownEthnicity) {
        final double undeclaredPercent = 100.0 * (population - totalKnownEthnicity) / population;
        final ST undeclaredTempl = stgroup.getInstanceOf("unknownEthn");
        undeclaredTempl.add("percent", "{{formatnum:" + ((long) (undeclaredPercent * 100) / 100.0) + "}}%");
        demographics.append(undeclaredTempl.render());
    }

    private void generateRegionNavTemplates() throws LoginException, IOException {
        final Criteria regionCriteria = ses.createCriteria(Region.class).addOrder(Order.asc("numeRo"));
        final List<Region> regions = regionCriteria.list();

        for (final Region region : regions) {
            final StringBuilder navTemplateBuilder = new StringBuilder("{{Casetă de navigare simplă|");
            navTemplateBuilder.append("\n|titlu=Comune în [[regiunea " + region.getNumeRo() + "]]");
            navTemplateBuilder.append("\n|nume=Comune în regiunea " + region.getNumeRo());
            navTemplateBuilder.append("\n|stare={{{stare|autopliabilă}}}");
            navTemplateBuilder.append("\n|listă1=<div>\n");
            final Set<Obshtina> communes = region.getObshtinas();
            final SortedSet<String> communeNames = new TreeSet<String>();
            for (final Obshtina obshtina : communes) {
                communeNames.add(obshtina.getNumeRo());
            }
            final List<String> communeLinks = transformedList(new ArrayList<String>(), new Transformer() {
                public Object transform(final Object input) {
                    return "[[Comuna " + input + ", " + region.getNumeRo() + "|" + input + "]]";
                }
            });
            communeLinks.addAll(communeNames);
            navTemplateBuilder.append(join(communeLinks, "{{~}}\n"));
            navTemplateBuilder.append("\n</div>");
            navTemplateBuilder.append("\n}}<noinclude>");
            navTemplateBuilder.append("[[Categorie:Formate de navigare regiuni din Bulgaria]]</noinclude>");

            if (!DEBUG_OBSHTINA) {
                final Map templateInfo = rowiki.getPageInfo("Format:Comune în regiunea " + region.getNumeRo());
                if (!isTrue((Boolean) templateInfo.get("exists"))) {
                    rowiki.edit("Format:Comune în regiunea " + region.getNumeRo(), navTemplateBuilder.toString(),
                        "Robot: creare format navigare pentru regiunea bulgară " + region.getNumeRo());
                }
            }
        }
    }

    private void generateObshtinas() throws IOException, LoginException {
        /*
         * final Criteria allObshtinasCriteria = ses.createCriteria(Obshtina.class); final List<Obshtina> obshtinas =
         * allObshtinasCriteria.list();
         */
        final Query tempQ = ses.createQuery("from Obshtina ob where ob.numeRo=:nume1 or ob.numeRo=:nume2");
        tempQ.setParameter("nume1", "Bansko");
        tempQ.setParameter("nume2", "Bratea Daskalovi");
        final List<Obshtina> obshtinas = tempQ.list();

        final STGroup templateGroup = new STGroupFile("templates/bg/section_obshtina.stg");
        for (final Obshtina obshtina : obshtinas) {
            final String demographics = generateDemographicsForObshtina(templateGroup, obshtina);

            final String title = getExistingRoTitleOfArticleWithSubject(obshtina);
            String articleTitle = null;
            final StringBuilder articleText = new StringBuilder();
            final String categoryName = "Comune în regiunea " + obshtina.getRegion().getNumeRo();
            if (null == title) {
                articleText.append(generateIntroForObshtina(obshtina));
                articleText.append(demographics);
                articleText.append("\n<br clear=\"left\"/>");
                articleText.append("\n== Note ==\n{{reflist}}\n");
                articleText.append("\n\n{{Comune în regiunea " + obshtina.getRegion().getNumeRo() + "}}");
                articleText.append("\n[[Categorie:" + categoryName + "|" + obshtina.getNumeRo() + "]]");
                if (!DEBUG_OBSHTINA) {
                    articleTitle = getNotNecessarilyExistingRoTitleArticleWithSubject(obshtina);
                    final String bgCounterpart = findBgCounterpartForObshtina(obshtina);
                    rowiki.edit(articleTitle, articleText.toString(),
                        "Robot: creare automată articol despre comună din Bulgaria");
                    dwiki.linkPages("rowiki", articleTitle, "bgwiki", bgCounterpart);
                }
            } else {
                articleTitle = getNotNecessarilyExistingRoTitleArticleWithSubject(obshtina);
                final String initialText = rowiki.getPageText(title);
                articleText.append(initialText);

                int startOfEnding = Math.max(articleText.indexOf("{{ciot-Bulgaria"), articleText.indexOf("{{Ciot-Bulgaria"));
                int generalCategoryStart = articleText.indexOf("[[Categorie:Comunele Bulgariei");
                if (generalCategoryStart > 0 && startOfEnding > 0) {
                    startOfEnding = Math.min(startOfEnding, generalCategoryStart);
                } else if (startOfEnding < 0) {
                    startOfEnding = generalCategoryStart;
                }
                final int generalCategoryEnd = 2 + articleText.indexOf("]]", generalCategoryStart);

                if (generalCategoryStart > 0) {
                    articleText.replace(generalCategoryStart, generalCategoryEnd, "[[Categorie:" + categoryName + "|"
                        + obshtina.getNumeRo() + "]]");
                }

                if (!StringUtils.contains(articleText, "== Demografie")
                    && !StringUtils.contains(articleText, "==Demografie")) {
                    articleText.insert(startOfEnding - 1, demographics);
                }

                if (!footnotesRegex.matcher(articleText).find()) {
                    startOfEnding = Math.max(articleText.indexOf("{{ciot-Bulgaria"), articleText.indexOf("{{Ciot-Bulgaria"));
                    generalCategoryStart = articleText.indexOf("[[Categorie:");
                    if (generalCategoryStart > 0 && startOfEnding > 0) {
                        startOfEnding = Math.min(startOfEnding, generalCategoryStart);
                    } else if (startOfEnding < 0) {
                        startOfEnding = generalCategoryStart;
                    }

                    articleText.insert(startOfEnding, "\n<br clear=\"left\"/>\n== Note ==\n{{reflist}}\n");
                }
                articleTitle = getNotNecessarilyExistingRoTitleArticleWithSubject(obshtina);
                if (!DEBUG_OBSHTINA) {
                    if (!StringUtils.equals(articleText, initialText)) {
                        rowiki.edit(title, articleText.toString(),
                            "Robot: adăugare secțiune demografie pentru comună din Bulgaria");
                    }
                    if (!StringUtils.equals(title, articleTitle)) {
                        rowiki.move(title, articleTitle,
                            "Robot: Redenumire articol despre comună din Bulgaria conform [[Wikipedia:Titluri]]");
                    }
                    final String bgCounterpart = findBgCounterpartForObshtina(obshtina);
                    dwiki.linkPages("rowiki", articleTitle, "bgwiki", bgCounterpart);
                }
            }
            redirectAlternativeObshtinaNames(articleTitle, obshtina);
            createObshtinaCategoryIfNotExist(obshtina);
            generateNavTemplateForObshtina(obshtina);
            System.out.println(articleText);
        }
    }

    private void createObshtinaCategoryIfNotExist(final Obshtina obshtina) throws IOException, LoginException {
        final String categoryName = "Comune în regiunea " + obshtina.getRegion().getNumeRo();

        final Map categoryInfo = rowiki.getPageInfo("Categorie:" + categoryName);
        final boolean categoryExists = isTrue((Boolean) categoryInfo.get("exists"));

        if (!categoryExists) {
            final StringBuilder catSB = new StringBuilder("[[Categorie:Comunele Bulgariei|");
            catSB.append(obshtina.getRegion().getNumeRo());
            catSB.append("]]");

            rowiki.edit("Categorie:" + categoryName, catSB.toString(),
                "Robot: creare categorie de comune din Bulgaria pe regiuni");
        }
    }

    private String generateDemographicsForObshtina(final STGroup templateGroup, final Obshtina obshtina) {
        final StringBuilder demographics = new StringBuilder(
            "\n== Demografie ==\n<!-- Start secțiune generată de Andrebot -->");
        final ST piechart = templateGroup.getInstanceOf("piechart");
        final int population = obshtina.getPopulation();
        piechart.add("nume", obshtina.getNumeRo());

        final Map<Nationality, Integer> ethnicStructure = obshtina.getEthnicStructure();
        final DefaultPieDataset dataset = new DefaultPieDataset();
        final int totalKnownEthnicity = computeDataset(population, ethnicStructure, dataset);

        renderPiechart(demographics, piechart, population, dataset);

        final ST demogIntro = templateGroup.getInstanceOf("introTmpl");
        demogIntro.add("nume", obshtina.getNumeRo());
        demogIntro.add("populatie", population);
        demographics.append(demogIntro.render());

        final Nationality majNat = getMajority(obshtina);
        final List<Nationality> minorities = getMinorities(obshtina);
        writeEthnodemographics(templateGroup, demographics, population, ethnicStructure, majNat, minorities);

        writeUnknownEthnicity(templateGroup, demographics, population, totalKnownEthnicity);

        demographics.append("\n<!--Sfârșit secțiune generată de Andrebot -->");
        return demographics.toString();
    }

    private void writeEthnodemographics(final STGroup templateGroup, final StringBuilder demographics, final int population,
                                        final Map<Nationality, Integer> ethnicStructure, final Nationality majNat,
                                        final List<Nationality> minorities) {
        if (null != majNat) {
            final ST demogMajority = templateGroup.getInstanceOf("ethnicMaj");
            demogMajority.add("etnie_maj", nationLinkMap.get(majNat.getNume()));
            final double majProcent = 100.0 * ethnicStructure.get(majNat) / population;
            demogMajority.add("maj_procent", "{{formatnum:" + ((long) (majProcent * 100) / 100.0) + "}}%");
            demographics.append(demogMajority.render());

            if (minorities.size() == 1) {
                final ST oneMinority = templateGroup.getInstanceOf("oneMinority");
                final Nationality minority = minorities.get(0);
                final double minProcent = 100.0 * ethnicStructure.get(minority) / population;

                oneMinority.add("minority", nationLinkMap.get(minority.getNume()) + " ({{formatnum:"
                    + ((long) (minProcent * 100) / 100.0) + "}}%)");
                demographics.append(oneMinority.render());
            } else if (minorities.size() > 1) {
                final ST someMinorities = templateGroup.getInstanceOf("someMinorities");
                final List<String> nationalitiesData = new ArrayList<String>();
                for (final Nationality mino : minorities) {
                    final StringBuilder data = new StringBuilder();
                    data.append(nationLinkMap.get(mino.getNume()));
                    final double minProcent = 100.0 * ethnicStructure.get(mino) / population;
                    data.append(" ({{formatnum:" + ((long) (minProcent * 100) / 100.0) + "}}%)");
                    nationalitiesData.add(data.toString());
                }
                someMinorities.add(
                    "minorities_enum",
                    join(nationalitiesData.toArray(), ", ", 0, nationalitiesData.size() - 1) + " și "
                        + nationalitiesData.get(nationalitiesData.size() - 1));
                demographics.append(someMinorities.render());
            }
        } else {
            final ST noMaj = templateGroup.getInstanceOf("noMaj");
            final List<String> nationalitiesData = new ArrayList<String>();

            for (final Nationality mino : minorities) {
                final StringBuilder data = new StringBuilder();
                data.append(nationLinkMap.get(mino.getNume()));
                final double minProcent = 100.0 * ethnicStructure.get(mino) / population;
                data.append(" ({{formatnum:" + ((long) (minProcent * 100) / 100.0) + "}}%)");
                nationalitiesData.add(data.toString());
            }
            noMaj.add("ethnicities_enum", join(nationalitiesData.toArray(), ", ", 0, nationalitiesData.size() - 1) + " și "
                + nationalitiesData.get(nationalitiesData.size() - 1));
        }
        demographics.append(".");
        demographics
            .append("<ref name=\"etnic2011\">{{Citat web|url=http://www.nsi.bg/census2011/PDOCS2/Census2011_ethnos.xls|title=Distribuția etnică a populației localităților Bulgariei|publisher=Institutul Național de Statistică al Bulgariei|accessdate=2013-10-15}}</ref>");
        demographics
            .append("<ref name=\"varste2011\">{{Citat web|url=http://www.nsi.bg/census2011/PDOCS2/Census2011_Age.xls|title=Distribuția pe vârste a populației localităților Bulgariei|publisher=Institutul Național de Statistică al Bulgariei|accessdate=2013-10-15}}</ref> ");
    }

    private void renderPiechart(final StringBuilder demographics, final ST piechart, final int population,
                                final DefaultPieDataset dataset) {
        final StringBuilder pieChartProps = new StringBuilder();
        int i = 1;
        demographics.append("<div style=\"float:left\">");

        for (final Object k : dataset.getKeys()) {
            pieChartProps.append("\n|label");
            pieChartProps.append(i);
            pieChartProps.append('=');
            pieChartProps.append(k.toString());
            pieChartProps.append("|value");
            pieChartProps.append(i);
            pieChartProps.append('=');
            pieChartProps.append((int) (100 * (dataset.getValue(k.toString()).doubleValue() * 100.0 / population)) / 100.0);
            pieChartProps.append("|color");
            pieChartProps.append(i);
            pieChartProps.append('=');
            final Color color = (Color) nationColorMap.get(nationNameMap.get(k.toString()));
            pieChartProps.append(Utilities.colorToHtml(color));
            i++;
        }
        piechart.add("props", pieChartProps.toString());
        demographics.append(piechart.render());
        demographics.append("</div>\n");
    }

    private int computeDataset(final int population, final Map<Nationality, Integer> ethnicStructure,
                               final DefaultPieDataset dataset) {
        final Map<String, Integer> smallGroups = new HashMap<String, Integer>();
        final Nationality undeclared = findNationalityByName("Nicio identificare");
        int totalKnownEthnicity = 0;
        Nationality otherNat = null;
        for (final Nationality nat : nationColorMap.keySet()) {
            final int natpop = defaultIfNull(ethnicStructure.get(nat), 0);
            if (natpop * 100.0 / population > 1.0 && !StringUtils.equals(nat.getNume(), "Altele")) {
                dataset.setValue(nat.getNume(), natpop);
            } else if (natpop * 100.0 / population <= 1.0 && !StringUtils.equals(nat.getNume(), "Altele")) {
                smallGroups.put(nat.getNume(), natpop);
            } else if (StringUtils.equals(nat.getNume(), "Altele")) {
                otherNat = nat;
            }
            if (!StringUtils.equals(undeclared.getNume(), nat.getNume())) {
                totalKnownEthnicity += natpop;
            }
        }
        dataset.setValue("Nicio identificare", population - totalKnownEthnicity);
        if (1 < smallGroups.size()) {
            smallGroups.put("Altele", ethnicStructure.get(otherNat));
            int smallSum = 0;
            for (final String smallGroup : smallGroups.keySet()) {
                smallSum += ObjectUtils.defaultIfNull(smallGroups.get(smallGroup), 0);
            }
            dataset.setValue("Altele", smallSum);
        } else {
            for (final String natname : smallGroups.keySet()) {
                if (!startsWithAny(natname, "Nicio ") && smallGroups.containsKey(natname)) {
                    dataset.setValue(natname, smallGroups.get(natname));
                }
            }
            if (ethnicStructure.containsKey(otherNat)) {
                dataset.setValue("Altele", ethnicStructure.get(otherNat));
            }
        }
        return totalKnownEthnicity;
    }

    private String findBgCounterpartForObshtina(final Obshtina obshtina) {
        return "Община " + obshtina.getNumeBg();
    }

    private void generateNavTemplateForObshtina(final Obshtina obshtina) throws IOException, LoginException {
        final StringBuilder sb = new StringBuilder("{{Casetă de navigare simplă|");
        final String titlu = "Localități în comuna " + obshtina.getNumeRo() + ", " + obshtina.getRegion().getNumeRo();
        sb.append("\n|titlu=Localități componente ale [[Comuna " + obshtina.getNumeRo() + "|comunei " + obshtina.getNumeRo()
            + "]]");
        sb.append("\n|nume=" + titlu);
        sb.append("\n|stare={{{stare|autopliabilă}}}");
        final List<String> townsWikiLinkList = getSettlementLinksForObshtina(obshtina, true);
        int villageGroupIndex = 1;
        if (townsWikiLinkList.size() > 0) {
            villageGroupIndex = 2;
            sb.append("\n|grup1=Orașe");
            sb.append("\n|listă1=<div>");
            sb.append(join(townsWikiLinkList, "{{~}}\n"));
            sb.append("</div>");
        }
        final List<String> villageWikiLinkList = getSettlementLinksForObshtina(obshtina, false);
        sb.append("\n|grup");
        sb.append(villageGroupIndex);
        sb.append("=Sate");
        sb.append("\n|listă");
        sb.append(villageGroupIndex);
        sb.append("=<div>\n");
        sb.append(join(villageWikiLinkList, "{{~}}\n"));
        sb.append("\n</div>");
        sb.append("\n}}<noinclude>");
        sb.append("[[Categorie:Formate de navigare regiunea ");
        sb.append(obshtina.getRegion().getNumeRo());
        sb.append("]]</noinclude>");

        if (!DEBUG_OBSHTINA) {
            final Map templateInfo = rowiki.getPageInfo("Format:" + titlu);
            if (!isTrue((Boolean) templateInfo.get("exists"))) {
                rowiki.edit("Format:" + titlu, sb.toString(),
                    "Robot: generare format navigare comuna " + obshtina.getNumeRo() + ", regiunea "
                        + obshtina.getRegion().getNumeRo());
                final String categoryName = "Categorie:Formate de navigare regiunea " + obshtina.getRegion().getNumeRo();
                final Map catInfo = rowiki.getPageInfo(categoryName);
                if (!isTrue((Boolean) catInfo.get("exists"))) {
                    rowiki.edit(categoryName, "[[Categorie:Formate de navigare regiuni din Bulgaria|"
                        + obshtina.getRegion().getNumeRo() + "]]",
                        "Robot: creare categorie formate de navigare pentru regiune din Bulgaria");
                }
            }
        }
    }

    private void redirectAlternativeObshtinaNames(final String title, final Obshtina obshtina) throws IOException,
    LoginException {
        final List<String> redirectNames = new ArrayList<String>();
        final Query query = ses.createQuery("select count(ob) from Obshtina ob where ob.numeRo=:numeRo");
        query.setParameter("numeRo", obshtina.getNumeRo());
        if ((Long) query.uniqueResult() <= 1) {
            redirectNames.add("Comuna " + obshtina.getNumeRo());
            redirectNames.add("Obștina " + obshtina.getNumeRo());
        }
        redirectNames.add("Obștina " + obshtina.getNumeRo() + ", " + obshtina.getRegion().getNumeRo());

        if (!DEBUG_OBSHTINA) {
            for (final String redir : redirectNames) {
                final Map redirInfo = rowiki.getPageInfo(redir);
                if (!isTrue((Boolean) redirInfo.get("exists"))) {
                    rowiki.edit(redir, "#redirect[[" + title + "]]", "Robot: redirectare titlu comună");
                }

            }
        }
    }

    private void redirectAlternativeSettlementNames(final String title, final Settlement settlement) throws IOException,
    LoginException {
        final List<String> redirectNames = new ArrayList<String>();
        final Query nationalLevelQuery = ses.createQuery("select count(ob) from Settlement ob where ob.numeRo=:numeRo");
        nationalLevelQuery.setParameter("numeRo", settlement.getNumeRo());

        final Long omonymCount = (Long) nationalLevelQuery.uniqueResult();
        if (omonymCount <= 1) {
            redirectNames.add(settlement.getNumeRo());
        } else {
            final Map disambigInfo = rowiki.getPageInfo(settlement.getNumeRo());
            if (!isTrue((Boolean) disambigInfo.get("exists"))) {
                final StringBuilder sb = new StringBuilder("Denumirea de '''");
                sb.append(settlement.getNumeRo());
                sb.append("''' se poate referi la una din următoarele localități din [[Bulgaria]]:\n");
                final Query allOmonymsQuery = ses
                    .createQuery("select s from Settlement s where s.numeRo=:numeRo order by s.town desc, s.obshtina.region.numeRo asc, s.obshtina.numeRo asc");
                allOmonymsQuery.setParameter("numeRo", settlement.getNumeRo());
                final List<Settlement> omonyms = allOmonymsQuery.list();
                for (final Settlement omonym : omonyms) {
                    sb.append("\n* [[");
                    sb.append(getNonExistingRoTitleArticleWithSubject(omonym));
                    sb.append("|");
                    sb.append(omonym.getNumeRo());
                    sb.append("]], un ");
                    sb.append(omonym.isTown() ? "oraș" : "sat");
                    sb.append(" în [[");
                    sb.append(getNotNecessarilyExistingRoTitleArticleWithSubject(omonym.getObshtina()));
                    sb.append("|comuna ");
                    sb.append(omonym.getObshtina().getNumeRo());
                    sb.append("]], ");
                    sb.append("[[regiunea " + omonym.getObshtina().getRegion().getNumeRo() + "]];");
                }
                sb.delete(sb.length() - 1, sb.length());
                sb.append(".\n\n{{dezambiguizare}}");
                rowiki.edit(settlement.getNumeRo(), sb.toString(),
                    "Robot: creare pagină de dezambiguizare pentru localitățile bulgare denumite „" + settlement.getNumeRo()
                        + "”");
            }
        }

        if (!StringUtils.contains(title, ")")) {
            redirectNames.add(settlement.getNumeRo() + " (" + settlement.getObshtina().getNumeRo() + "), "
                + settlement.getObshtina().getRegion().getNumeRo());
        }

        if (settlement.isTown()) {
            redirectNames.add(settlement.getNumeRo() + ", " + settlement.getObshtina().getRegion().getNumeRo());
        }

        if (!DEBUG) {
            for (final String redir : redirectNames) {
                final Map redirInfo = rowiki.getPageInfo(redir);
                if (!isTrue((Boolean) redirInfo.get("exists"))) {
                    rowiki.edit(redir, "#redirect[[" + title + "]]", "Robot: redirectare titlu localitate din Bulgaria");
                }
            }
        }
    }

    private String getNotNecessarilyExistingRoTitleArticleWithSubject(final Obshtina obshtina) {
        return "Comuna " + obshtina.getNumeRo() + ", " + obshtina.getRegion().getNumeRo();
    }

    private List<Nationality> getMinorities(final EthnicallyStructurable obshtina) {
        final List<Nationality> ret = new ArrayList<Nationality>();
        for (final Nationality nat : obshtina.getEthnicStructure().keySet()) {
            final double weight = obshtina.getEthnicStructure().get(nat) / (double) obshtina.getPopulation();
            if (weight <= 0.5 && weight > 0.01 && !startsWithAny(nat.getNume(), "Altele", "Nicio")) {
                ret.add(nat);
            }
        }
        return ret;
    }

    private Nationality getMajority(final EthnicallyStructurable obshtina) {
        for (final Nationality nat : obshtina.getEthnicStructure().keySet()) {
            if (obshtina.getEthnicStructure().get(nat) / (double) obshtina.getPopulation() > 0.5) {
                return nat;
            }
        }
        return null;
    }

    private String generateIntroForObshtina(final Obshtina obshtina) throws IOException {
        final StringBuilder sb = new StringBuilder("{{Infocaseta Așezare|");
        final Map<String, String> infoboxParams = new LinkedHashMap<String, String>();
        infoboxParams.put("nume", obshtina.getNumeRo());
        infoboxParams.put("nume_nativ", obshtina.getNumeBg());
        infoboxParams.put("tip_subdiviziune", "[[Țările lumii|Țară]]");
        infoboxParams.put("nume_subdiviziune", "{{BUL}}");
        infoboxParams.put("tip_subdiviziune1", "[[Regiunile Bulgariei|Regiune]]");
        infoboxParams.put("nume_subdiviziune1", "[[Regiunea " + obshtina.getRegion().getNumeRo() + "|"
            + obshtina.getRegion().getNumeRo() + "]]");
        infoboxParams.put("tip_asezare", "[[Comunele Bulgariei|Comună]]");
        infoboxParams.put("recensământ", "2011");
        infoboxParams.put("populație", String.valueOf(obshtina.getPopulation()));
        infoboxParams.put("populație_note_subsol", "<ref name=\"varste2011\"/>");

        final String bgText = bgwiki.getPageText(findBgCounterpartForObshtina(obshtina));
        final ParameterReader params = new ParameterReader(bgText);
        params.run();
        final Map<String, String> bgparams = params.getParams();
        final String map = bgparams.get("карта");
        if (!StringUtils.isEmpty(map)) {
            infoboxParams.put("hartă", map);
        }
        final String coa = bgparams.get("герб");
        if (!StringUtils.isEmpty(coa)) {
            infoboxParams.put("stemă", coa);
        }
        final String latd = bgparams.get("сев-ширина");
        if (!StringUtils.isEmpty(latd)) {
            infoboxParams.put("latd", latd);
            infoboxParams.put("latNS", "N");
            infoboxParams.put("pushpin_map", "Bulgaria");
        }
        final String longd = bgparams.get("изт-дължина");
        if (!StringUtils.isEmpty(longd)) {
            infoboxParams.put("longd", longd);
            infoboxParams.put("longEV", "E");
        }
        final String aria = bgparams.get("площ");
        if (!StringUtils.isEmpty(aria)) {
            infoboxParams.put("suprafață_totală_km2", "{{formatnum:" + aria + "}}");
        }

        for (final String ibParam : infoboxParams.keySet()) {
            sb.append("\n|");
            sb.append(ibParam);
            sb.append(" = ");
            sb.append(infoboxParams.get(ibParam));
        }
        sb.append("\n}}\n");

        final STGroup templateGroup = new STGroupFile("templates/bg/section_obshtina.stg");
        final ST intro = templateGroup.getInstanceOf("introObshtina");
        intro.add("numeRo", obshtina.getNumeRo());
        intro.add("numeBg", obshtina.getNumeBg());
        intro.add("regiune", obshtina.getRegion().getNumeRo());
        sb.append(intro.render());
        sb.append(", formată din ");

        final int villagesCnt = countSettlementsInObshtina(obshtina, false);
        final int townsCnt = countSettlementsInObshtina(obshtina, true);

        List<String> townsWikiLinkLists = null;
        List<String> villagesWikiLinkLists = null;
        if (townsCnt > 0) {
            townsWikiLinkLists = getSettlementLinksForObshtina(obshtina, true);

            if (townsCnt == 1) {
                sb.append("orașul ");
                sb.append(townsWikiLinkLists.get(0));
            } else {
                final String townsList = join(townsWikiLinkLists.toArray(), ", ", 0, townsWikiLinkLists.size() - 1) + " și "
                    + townsWikiLinkLists.get(townsWikiLinkLists.size() - 1);
                final ST townsListTemplate = templateGroup.getInstanceOf("compositionObshtinaEnumTowns");
                townsListTemplate.add("enumTowns", townsList);
                sb.append(townsListTemplate.render());
            }
            if (villagesCnt > 0) {
                sb.append(" și ");
            } else {
                sb.append(". ");
            }
        }
        if (villagesCnt > 0) {
            ST villagesTemplate = null;
            villagesWikiLinkLists = getSettlementLinksForObshtina(obshtina, false);
            if (villagesCnt > MAX_ENUMED_VILLAGES) {
                villagesTemplate = templateGroup.getInstanceOf("compositionObshtinaCountVillages");
                villagesTemplate.add("nrVillages", String.valueOf(villagesCnt) + (villagesCnt > 19 ? " de" : ""));
            } else {
                villagesTemplate = templateGroup.getInstanceOf("compositionObshtinaEnumVillages");
                final String villagesList = join(villagesWikiLinkLists.toArray(), ", ", 0, villagesWikiLinkLists.size() - 1)
                    + " și " + villagesWikiLinkLists.get(villagesWikiLinkLists.size() - 1);
                villagesTemplate.add("enumVillages", villagesList);
            }
            sb.append(villagesTemplate.render());
            sb.append(". ");
        }
        if (villagesCnt > MAX_ENUMED_VILLAGES) {
            sb.append("\n== Localități componente ==");
            if (townsCnt > 0) {
                sb.append("\n=== Orașe ===\n* ");
                sb.append(join(townsWikiLinkLists, "\n* "));
            }
            sb.append("\n=== Sate ===\n");
            final int firstColumnSize = (int) Math.ceil(villagesCnt / 3.0);
            final int followingColumnsSize = (int) Math.floor(villagesCnt / 3.0);
            sb.append("{|\n|valign=\"top\"|\n* ");
            sb.append(join(villagesWikiLinkLists.toArray(), "\n* ", 0, firstColumnSize));
            sb.append("\n|valign=\"top\"|\n* ");
            sb.append(join(villagesWikiLinkLists.toArray(), "\n* ", firstColumnSize, firstColumnSize + followingColumnsSize));
            sb.append("\n|valign=\"top\"|\n* ");
            sb.append(join(villagesWikiLinkLists.toArray(), "\n* ", firstColumnSize + followingColumnsSize,
                villagesWikiLinkLists.size()));
            sb.append("\n|}\n");
        }

        return sb.toString();
    }

    private List<String> getSettlementLinksForObshtina(final Obshtina obshtina, final boolean town) throws IOException {
        List<String> villagesLinkLists;
        List<String> villagesWikiLinkLists;
        final List<Settlement> villages = findSettlementsInObshtina(obshtina, town);
        villagesWikiLinkLists = transformedList(new ArrayList<String>(), wikilinkifier);
        villagesLinkLists = new ArrayList<String>();
        for (final Settlement village : villages) {
            final String title = getExistingRoTitleOfArticleWithSubject(village);
            if (StringUtils.equals(village.getNumeRo(), title)) {
                villagesLinkLists.add("[[" + title + "]]");
            } else if (!isEmpty(title)) {
                villagesLinkLists.add("[[" + title + "|" + village.getNumeRo() + "]]");
            } else {
                villagesLinkLists.add(getNonExistingRoTitleArticleWithSubject(village));
            }
        }
        Collections.sort(villagesLinkLists);
        villagesWikiLinkLists.addAll(villagesLinkLists);
        return villagesWikiLinkLists;
    }

    private int countSettlementsInObshtina(final Obshtina obshtina, final boolean town) {
        final Query q = ses
            .createQuery("select count(*) from Settlement settlement where settlement.obshtina=:obshtina and settlement.town=:town");
        q.setParameter("obshtina", obshtina);
        q.setParameter("town", town);
        final Long res = (Long) q.uniqueResult();
        return res.intValue();
    }

    private List<Settlement> findSettlementsInObshtina(final Obshtina obshtina, final boolean town) {
        final Query q = ses
            .createQuery("from Settlement settlement where settlement.obshtina=:obshtina and settlement.town=:town");
        q.setParameter("obshtina", obshtina);
        q.setParameter("town", town);
        return q.list();
    }

    private void login() throws IOException, FailedLoginException {
        credentials.load(WikiGenerator.class.getClassLoader().getResourceAsStream("credentials.properties"));
        final String datauser = credentials.getProperty("UsernameData");
        final String datapass = credentials.getProperty("PasswordData");
        final String user = credentials.getProperty("Username");
        final String pass = credentials.getProperty("Password");
        rowiki.login(user, pass.toCharArray());
        rowiki.setMarkBot(true);
        dwiki.login(datauser, datapass.toCharArray());

        BGPopulationParser.initHibernate();
        ses = BGPopulationParser.sessionFactory.getCurrentSession();
        ses.beginTransaction();

        assignColorToNationality("Bulgari", new Color(85, 255, 85));
        assignColorToNationality("Turci", new Color(255, 85, 85));
        assignColorToNationality("Romi", new Color(85, 255, 255));
        assignColorToNationality("Altele", new Color(64, 64, 64));
        assignColorToNationality("Nicio identificare", new Color(192, 192, 192));
    }

    private void assignColorToNationality(final String nationalityName, final Paint color) throws HibernateException {
        final Nationality nat = findNationalityByName(nationalityName);
        if (null != nat) {
            nationColorMap.put(nat, color);
            nationNameMap.put(nat.getNume(), nat);
        }
    }

    private Nationality findNationalityByName(final String nationalityName) {
        final Query findNationalityByName = ses.createQuery(findNationalityByNameQueryString);
        findNationalityByName.setParameter("name", nationalityName);
        final List<Nationality> nats = findNationalityByName.list();
        if (nats.size() > 0) {
            return nats.get(0);
        }
        return null;
    }

    private void cleanup() {
        if (null != rowiki) {
            rowiki.logout();
        }
        if (null != ses) {
            final Transaction tx = ses.getTransaction();
            if (null != tx) {
                tx.rollback();
            }
        }
    }

    private String getExistingRoTitleOfArticleWithSubject(final Obshtina obshtina) throws IOException {
        final List<String> alternativeTitles = new ArrayList<String>();
        alternativeTitles.add("Comuna " + obshtina.getNumeRo() + ", " + obshtina.getRegion().getNumeRo());
        alternativeTitles.add("Comuna " + obshtina.getNumeRo());
        alternativeTitles.add("Obștina " + obshtina.getNumeRo() + ", " + obshtina.getRegion().getNumeRo());
        alternativeTitles.add("Obștina " + obshtina.getNumeRo());

        for (final String candidateTitle : alternativeTitles) {
            final HashMap pageInfo = rowiki.getPageInfo(candidateTitle);
            final Boolean exists = (Boolean) pageInfo.get("exists");
            if (!isTrue(exists)) {
                continue;
            }
            final String pageTitle = defaultString(rowiki.resolveRedirect(candidateTitle), candidateTitle);
            return pageTitle;
        }
        return null;
    }

    private String getExistingRoTitleOfArticleWithSubject(final Settlement settlement) throws IOException {
        final List<String> alternativeTitles = new ArrayList<String>();

        alternativeTitles.add(settlement.getNumeRo() + " (" + settlement.getObshtina().getNumeRo() + "), "
            + settlement.getObshtina().getRegion().getNumeRo());
        alternativeTitles.add(settlement.getNumeRo() + ", " + settlement.getObshtina().getRegion().getNumeRo());
        alternativeTitles.add(settlement.getNumeRo() + ", Bulgaria");
        alternativeTitles.add(settlement.getNumeRo());

        for (final String candidateTitle : alternativeTitles) {
            final HashMap pageInfo = rowiki.getPageInfo(candidateTitle);
            final Boolean exists = (Boolean) pageInfo.get("exists");
            if (!isTrue(exists)) {
                continue;
            }
            final String pageTitle = defaultString(rowiki.resolveRedirect(candidateTitle), candidateTitle);
            final String[] categories = rowiki.getCategories(candidateTitle);
            if ((settlement.isTown() && contains(categories, "Categorie:Orașe în Bulgaria"))
                || (!settlement.isTown() && contains(categories, "Categorie:Sate în Bulgaria"))) {
                return pageTitle;
            }

        }
        return null;
    }

    private String getNonExistingRoTitleArticleWithSubject(final Settlement settlement) throws IOException {
        return getNonExistingRoTitleArticleWithSubject(settlement, null);
    }

    private String getNonExistingRoTitleArticleWithSubject(final Settlement settlement, final String existingTitle)
        throws IOException {
        final List<String> alternativeTitles = new ArrayList<String>();

        if (settlement.isTown()) {
            alternativeTitles.add(settlement.getNumeRo());
            alternativeTitles.add(settlement.getNumeRo() + ", Bulgaria");
            alternativeTitles.add(settlement.getNumeRo() + ", " + settlement.getObshtina().getRegion().getNumeRo());
            alternativeTitles.add(settlement.getNumeRo() + " (" + settlement.getObshtina().getNumeRo() + "), "
                + settlement.getObshtina().getRegion().getNumeRo());
        } else {
            alternativeTitles.add(settlement.getNumeRo() + ", " + settlement.getObshtina().getRegion().getNumeRo());
            alternativeTitles.add(settlement.getNumeRo() + " (" + settlement.getObshtina().getNumeRo() + "), "
                + settlement.getObshtina().getRegion().getNumeRo());
            alternativeTitles.add(settlement.getNumeRo());
        }
        for (final String candidateTitle : alternativeTitles) {
            final Map pageInfo = rowiki.getPageInfo(candidateTitle);
            final Boolean exists = (Boolean) pageInfo.get("exists");
            if (!isTrue(exists) || (isTrue(exists) && StringUtils.equals(candidateTitle, existingTitle))) {
                return candidateTitle;
            }
        }
        return null;

    }

    private String translateNationalityToLink(final Nationality nat) {
        return defaultString(nationLinkMap.get(nat.getNume()), nat.getNume());
    }

}
