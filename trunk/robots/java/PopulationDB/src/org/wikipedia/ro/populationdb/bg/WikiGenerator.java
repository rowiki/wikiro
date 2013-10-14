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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

import org.apache.commons.collections.Transformer;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.jfree.data.general.DefaultPieDataset;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;
import org.wikipedia.Wiki;
import org.wikipedia.ro.populationdb.bg.model.Nationality;
import org.wikipedia.ro.populationdb.bg.model.Obshtina;
import org.wikipedia.ro.populationdb.bg.model.Settlement;
import org.wikipedia.ro.populationdb.util.ParameterReader;
import org.wikipedia.ro.populationdb.util.Utilities;

public class WikiGenerator {

    private static final String findNationalityByNameQueryString = "from Nationality nat where nat.nume=:name";
    private static final int MAX_ENUMED_VILLAGES = 12;
    private final Wiki rowiki = new Wiki("ro.wikipedia.org");
    private final Wiki bgwiki = new Wiki("bg.wikipedia.org");
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

    private final Transformer settlementLinkGetter = new Transformer() {

        public Object transform(final Object input) {
            final Settlement settlement = (Settlement) input;
            String title = null;
            try {
                title = getExistingRoTitleOfArticleWithSubject(settlement);
                if (StringUtils.equals(settlement.getNumeRo(), title)) {
                    return "[[" + title + "]]";
                } else if (!isEmpty(title)) {
                    return "[[" + title + "|" + settlement.getNumeRo() + "]]";
                } else {
                    return getNonExistingRoTitleArticleWithSubject(settlement);
                }
            } catch (final IOException e) {
                e.printStackTrace();
            }
            return null;
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
        .compile("\\{\\{\uFEFF\u041E\u0431\u0449\u0438\u043D\u0430 \u0432 \u0411\u044A\u043B\u0433\u0430\u0440\u0438\u044F\\s*(\\|(?:\\{\\{[^{}]*+\\}\\}|[^{}])*+)?\\}\\}\\s*");

    public static void main(final String[] args) {
        final WikiGenerator generator = new WikiGenerator();
        try {
            generator.login();
            generator.generateObshtinas();
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
            final StringBuilder demographics = new StringBuilder(
                "\n== Demografie ==\n<!-- Start secțiune generată de Andrebot -->");
            demographics.append("<div style=\"float:left\">");
            final ST piechart = templateGroup.getInstanceOf("piechart");
            piechart.add("nume", obshtina.getNumeRo());

            final Map<Nationality, Integer> ethnicStructure = obshtina.getEthnicStructure();
            final StringBuilder pieChartProps = new StringBuilder();
            final DefaultPieDataset dataset = new DefaultPieDataset();
            int totalPerNat = 0;
            final Map<String, Integer> smallGroups = new HashMap<String, Integer>();
            Nationality otherNat = null;
            for (final Nationality nat : nationColorMap.keySet()) {
                final int natpop = defaultIfNull(ethnicStructure.get(nat), 0);
                if (natpop * 100.0 / obshtina.getPopulation() > 1.0 && !StringUtils.equals(nat.getNume(), "Altele")) {
                    dataset.setValue(nat.getNume(), natpop);
                    totalPerNat += natpop;
                } else if (natpop * 100.0 / obshtina.getPopulation() <= 1.0 && !StringUtils.equals(nat.getNume(), "Altele")) {
                    smallGroups.put(nat.getNume(), natpop);
                } else if (StringUtils.equals(nat.getNume(), "Altele")) {
                    otherNat = nat;
                }
            }
            if (1 < smallGroups.size()) {
                dataset.setValue("Altele", obshtina.getPopulation() - totalPerNat);
            } else {
                for (final String natname : smallGroups.keySet()) {
                    dataset.setValue(natname, smallGroups.get(natname));
                }
                dataset.setValue("Altele", ethnicStructure.get(otherNat));
            }

            int i = 1;
            for (final Object k : dataset.getKeys()) {
                pieChartProps.append("\n|label");
                pieChartProps.append(i);
                pieChartProps.append('=');
                pieChartProps.append(k.toString());
                pieChartProps.append("|value");
                pieChartProps.append(i);
                pieChartProps.append('=');
                pieChartProps.append((int) (100 * (dataset.getValue(k.toString()).doubleValue() * 100.0 / obshtina
                    .getPopulation())) / 100.0);
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

            final ST demogIntro = templateGroup.getInstanceOf("introTmpl");
            demogIntro.add("nume", obshtina.getNumeRo());
            demogIntro.add("populatie", obshtina.getPopulation());
            demographics.append(demogIntro.render());

            final Nationality majNat = getMajority(obshtina);
            if (null != majNat) {
                final ST demogMajority = templateGroup.getInstanceOf("ethnicMaj");
                demogMajority.add("etnie_maj", nationLinkMap.get(majNat.getNume()));
                final double majProcent = 100.0 * obshtina.getEthnicStructure().get(majNat) / obshtina.getPopulation();
                demogMajority.add("maj_procent", "{{formatnum:" + ((long) (majProcent * 100) / 100.0) + "}}%");
                demographics.append(demogMajority.render());

                final List<Nationality> minorities = getMinorities(obshtina);
                if (minorities.size() == 1) {
                    final ST oneMinority = templateGroup.getInstanceOf("oneMinority");
                    final Nationality minority = minorities.get(0);
                    final double minProcent = 100.0 * obshtina.getEthnicStructure().get(minority) / obshtina.getPopulation();

                    oneMinority.add("minority", nationLinkMap.get(minority.getNume()) + " ({{formatnum:"
                        + ((long) (minProcent * 100) / 100.0) + "}}%)");
                    demographics.append(oneMinority.render());
                } else if (minorities.size() > 1) {
                    final ST someMinorities = templateGroup.getInstanceOf("someMinorities");
                    final List<String> nationalitiesData = new ArrayList<String>();
                    for (final Nationality mino : minorities) {
                        final StringBuilder data = new StringBuilder();
                        data.append(nationLinkMap.get(mino.getNume()));
                        final double minProcent = 100.0 * obshtina.getEthnicStructure().get(mino) / obshtina.getPopulation();
                        data.append(" ({{formatnum:" + (((long) minProcent * 100) / 100.0) + "}}%)");
                        nationalitiesData.add(data.toString());
                    }
                    someMinorities.add("minorities_enum",
                        join(nationalitiesData.toArray(), ", ", 0, nationalitiesData.size() - 1) + " și "
                            + nationalitiesData.get(nationalitiesData.size() - 1));
                }
            } else {
                final List<Nationality> minorities = getMinorities(obshtina);
                final ST noMaj = templateGroup.getInstanceOf("noMaj");
                final List<String> nationalitiesData = new ArrayList<String>();

                for (final Nationality mino : minorities) {
                    final StringBuilder data = new StringBuilder();
                    data.append(nationLinkMap.get(mino.getNume()));
                    final double minProcent = 100.0 * obshtina.getEthnicStructure().get(mino) / obshtina.getPopulation();
                    data.append(" ({{formatnum:" + (((long) minProcent * 100) / 100.0) + "}}%)");
                    nationalitiesData.add(data.toString());
                }
                noMaj.add("ethnicities_enum", join(nationalitiesData.toArray(), ", ", 0, nationalitiesData.size() - 1)
                    + " și " + nationalitiesData.get(nationalitiesData.size() - 1));
            }
            demographics.append(".");
            demographics
                .append("<ref>{{Citat web|url=http://www.nsi.bg/census2011/PDOCS2/Census2011_ethnos.xls|title=Distribuția etnică a populației localităților Bulgariei|publisher=Institutul Național de Statistică al Bulgariei|accessdate=2013-10-15}}</ref>");
            demographics
                .append("<ref>{{Citat web|url=http://www.nsi.bg/census2011/PDOCS2/Census2011_Age.xls|title=Distribuția pe vârste a populației localităților Bulgariei|publisher=Institutul Național de Statistică al Bulgariei|accessdate=2013-10-15}}</ref> ");
            final Nationality undeclared = findNationalityByName("Nicio identificare");
            if (null != undeclared) {
                final double undeclaredPercent = 100.0 * obshtina.getEthnicStructure().get(undeclared)
                    / obshtina.getPopulation();
                final ST undeclaredTempl = templateGroup.getInstanceOf("unknownEthn");
                undeclaredTempl.add("percent", "{{formatnum:" + (((long) undeclaredPercent * 100) / 100.0) + "}}%");
                demographics.append(undeclaredTempl.render());
            }
            demographics.append("\n<!--Sfârșit secțiune generată de Andrebot -->");

            // System.out.println(demographics.toString());

            final String title = getExistingRoTitleOfArticleWithSubject(obshtina);
            final StringBuilder articleText = new StringBuilder();
            final String categoryName = "Comune în regiunea " + obshtina.getRegion().getNumeRo();
            if (null == title) {
                articleText.append(generateIntroForObshtina(obshtina));
                articleText.append(demographics);
                articleText.append("\n== Note ==\n{{reflist}}\n");
                articleText.append("\n[[Categorie:" + categoryName + "|" + obshtina.getNumeRo() + "]]");
                // rowiki.edit(getNonExistingRoTitleArticleWithSubject(obshtina), articleText.toString(),
                // "Robot: creare automată articol despre comună din Bulgaria");
            } else {
                articleText.append(rowiki.getPageText(title));
                final int startOfEnding = Math.max(articleText.indexOf("{{ciot-Bulgaria"),
                    articleText.indexOf("{{Ciot-Bulgaria"));
                articleText.insert(startOfEnding, demographics);
                final int generalCategoryStart = articleText.indexOf("[[Categorie:Comunele Bulgariei");
                final int generalCategoryEnd = 2 + articleText.indexOf("]]", generalCategoryStart);

                articleText.replace(generalCategoryStart, generalCategoryEnd,
                    "[[Categorie:" + categoryName + "|" + obshtina.getNumeRo() + "]]");

                if (!footnotesRegex.matcher(articleText).find()) {
                    articleText.insert(generalCategoryStart, "\n== Note ==\n{{reflist}}");
                }
                // rowiki.edit(title, articleText.toString(),
                // "Robot: adăugare secțiune demografie pentru comună din Bulgaria");
                // rowiki.move(title, getNonExistingRoTitleArticleWithSubject(obshtina),
                // "Robot: Redenumire articol despre comună din Bulgaria conform [[Wikipedia:Titluri]]");

            }
            System.out.println(articleText);
        }
    }

    private String getNonExistingRoTitleArticleWithSubject(final Obshtina obshtina) {
        return "Comuna " + obshtina.getNumeRo() + ", " + obshtina.getRegion().getNumeRo();
    }

    private List<Nationality> getMinorities(final Obshtina obshtina) {
        final List<Nationality> ret = new ArrayList<Nationality>();
        for (final Nationality nat : obshtina.getEthnicStructure().keySet()) {
            final double weight = obshtina.getEthnicStructure().get(nat) / (double) obshtina.getPopulation();
            if (weight <= 0.5 && weight > 0.01 && !startsWithAny(nat.getNume(), "Altele", "Nicio")) {
                ret.add(nat);
            }
        }
        return ret;
    }

    private Nationality getMajority(final Obshtina obshtina) {
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
        infoboxParams.put("", "[[Țările lumii|Țară]]");

        final String bgText = bgwiki.getPageText("Община " + obshtina.getNumeBg());
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
        }
        final String longd = bgparams.get("сев-ширина");
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
        sb.append("}}\n");

        final STGroup templateGroup = new STGroupFile("templates/bg/section_obshtina.stg");
        final ST intro = templateGroup.getInstanceOf("introObshtina");
        intro.add("numeRo", obshtina.getNumeRo());
        intro.add("numeBg", obshtina.getNumeBg());
        intro.add("regiune", obshtina.getRegion().getNumeRo());
        sb.append(intro.render());
        sb.append(", formată din ");

        final int villagesCnt = countSettlementsInObshtina(obshtina, false);
        final int townsCnt = countSettlementsInObshtina(obshtina, true);

        List townsLinkLists = null;
        List villagesLinkLists = null;
        List townsWikiLinkLists = null;
        List villagesWikiLinkLists = null;
        if (townsCnt > 0) {
            final List<Settlement> towns = findSettlementsInObshtina(obshtina, true);
            townsLinkLists = transformedList(new ArrayList<String>(), settlementLinkGetter);
            townsWikiLinkLists = transformedList(new ArrayList<String>(), wikilinkifier);
            townsLinkLists.addAll(towns);
            townsWikiLinkLists.addAll(townsLinkLists);

            if (townsCnt == 1) {
                sb.append("orașul ");
                sb.append(townsLinkLists.get(0));
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
            final List villages = findSettlementsInObshtina(obshtina, false);
            villagesLinkLists = transformedList(new ArrayList<String>(), settlementLinkGetter);
            villagesWikiLinkLists = transformedList(new ArrayList<String>(), wikilinkifier);
            villagesLinkLists.addAll(villages);
            villagesWikiLinkLists.addAll(villagesLinkLists);
            if (villagesCnt > MAX_ENUMED_VILLAGES) {
                villagesTemplate = templateGroup.getInstanceOf("compositionObshtinaCountVillages");
                villagesTemplate.add("nrVillages", String.valueOf(villagesCnt));
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
            sb.append("\n==Localități componente==");
            if (townsCnt > 0) {
                sb.append("\n=== Orașe ===\n* ");
                sb.append(join(townsWikiLinkLists, "\n* "));
            }
            sb.append("\n=== Sate ===\n");
            final long firstColumnSize = (long) Math.ceil(villagesCnt / 3.0);
            final long followingColumnsSize = (long) Math.floor(villagesCnt / 3.0);
            sb.append("{|\n|valign=\"top\"|\n* ");
            sb.append(join(villagesWikiLinkLists.toArray(), "\n* ", 0, firstColumnSize));
            sb.append("|valign=\"top\"|\n* ");
            sb.append(join(villagesWikiLinkLists.toArray(), "\n* ", firstColumnSize, firstColumnSize + followingColumnsSize));
            sb.append("|valign=\"top\"|\n* ");
            sb.append(join(villagesWikiLinkLists.toArray(), "\n* ", firstColumnSize + followingColumnsSize,
                villagesLinkLists.size()));
            sb.append("\n|}\n");
        }

        return sb.toString();
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
        final String user = credentials.getProperty("Username");
        final String pass = credentials.getProperty("Password");
        rowiki.login(user, pass.toCharArray());
        rowiki.setMarkBot(true);

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
            final HashMap pageInfo = rowiki.getPageInfo(candidateTitle);
            final Boolean exists = (Boolean) pageInfo.get("exists");
            if (!isTrue(exists)) {
                return candidateTitle;
            }
        }
        return null;

    }

    private String translateNationalityToLink(final Nationality nat) {
        return defaultString(nationLinkMap.get(nat.getNume()), nat.getNume());
    }

}
