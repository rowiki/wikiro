package org.wikipedia.ro.populationdb.hu;

import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.startsWithAny;

import java.awt.Color;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.RuleBasedCollator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.login.FailedLoginException;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.jfree.data.UnknownKeyException;
import org.jfree.data.general.DefaultPieDataset;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;
import org.wikibase.Wikibase;
import org.wikipedia.Wiki;
import org.wikipedia.ro.populationdb.hu.dao.Hibernator;
import org.wikipedia.ro.populationdb.hu.model.County;
import org.wikipedia.ro.populationdb.hu.model.Nationality;
import org.wikipedia.ro.populationdb.hu.model.Religion;
import org.wikipedia.ro.populationdb.hu.model.Settlement;
import org.wikipedia.ro.populationdb.util.Executor;
import org.wikipedia.ro.populationdb.util.WikiTemplate;
import org.wikipedia.ro.populationdb.util.Utilities;
import org.wikipedia.ro.populationdb.util.WikiEditExecutor;

public class HUWikiGenerator {
    private static final String NOTE_REFLIST = "\n== Note ==\n{{Reflist}}\n";
    private static final int NEW_PAGE_LIMIT = 1200;
    private Wiki rowiki;
    private Wiki huwiki;
    private Wikibase dwiki;
    private Executor executor;
    private Hibernator hib;
    private Session ses;
    private final Pattern footnotesRegex = Pattern
        .compile("\\{\\{(?:(?:L|l)istănote|(?:R|r)eflist)|(?:\\<\\s*references\\s*\\/\\>)");
    Pattern huWpTemplates1 = Pattern
        .compile("\\{\\{(?:((?:M|m)agyar település infobox))\\s*(\\|(?:\\{\\{[^{}]*+\\}\\}|[^{}])*+)?\\}\\}\\s*");
    private static Pattern regexInfocAsezare = Pattern
        .compile("\\{\\{(?:(?:C|c)asetă așezare|(?:I|i)nfocaseta Așezare|(?:C|c)utie așezare)\\s*(\\|(?:\\{\\{[^{}]*+\\}\\}|[^{}])*+)?\\}\\}\\s*");
    private static Pattern regexCutieOrase = Pattern
        .compile("\\{\\{(?:(?:C|c)utieOrașe|(?:C|c)asetăOraşe|(?:C|c)asetăOrașe)\\s*(\\|(?:\\{\\{[^{}]*+\\}\\}|[^{}])*+)?\\}\\}\\s*");

    private final STGroup communeTemplateGroup = new STGroupFile("templates/hu/town.stg");
    private final STGroup townTemplateGroup = new STGroupFile("templates/hu/town.stg");
    private final Map<Nationality, Color> nationColorMap = new LinkedHashMap<Nationality, Color>();
    private final Map<String, Nationality> nationNameMap = new LinkedHashMap<String, Nationality>();
    private final Map<Religion, Color> religionColorMap = new LinkedHashMap<Religion, Color>();
    private final Map<String, Religion> religionNameMap = new LinkedHashMap<String, Religion>();
    private final Map<Settlement, LazyInitializer<String>> huWpNames = new HashMap<Settlement, LazyInitializer<String>>();
    private final Map<Settlement, LazyInitializer<String>> roWpNames = new HashMap<Settlement, LazyInitializer<String>>();
    private final Map<String, String> relLinkMap = new LinkedHashMap<String, String>() {
        {
            put("Romano-catolici", "[[Biserica Romano-Catolică|romano-catolici]]");
            put("Greco-catolici", "[[Biserici greco-catolice|greco-catolici]]");
            put("Ortodocși", "[[Biserica Ortodoxă|ortodocși]]");
            put("Reformați", "[[Calvinism|reformați]]");
            put("Luterani", "[[Luteranism|luterani]]");
            put("Mozaici", "[[Iudaism|iudaici]]");
            put("Atei", "[[Ateism|atei]]");
            put("Fără religie", "[[Umanism secular|persoane fără religie]]");
            put("Alte religii", "de alte religii");
        }
    };

    public static void main(final String[] args) throws Exception {
        final HUWikiGenerator generator = new HUWikiGenerator();
        try {
            generator.init();
            generator.generateCounties();
            
        } finally {
            generator.close();
        }
    }

    private void generateCounties() throws Exception {
        //final List<County> counties = hib.getAllCounties();
    	final List<County> counties = new ArrayList<County>();
    	counties.add(hib.getCountyByName("Vas"));

        for (final County county : counties) {
            final List<Settlement> communes = hib.getCommunesByCounty(county);
            for (final Settlement eachCommune : communes) {
                initCommune(eachCommune);
            }
            generateCountyCategory(county, false);
            generateCountyCategory(county, true);
            generateCountyNavTemplate(county, communes);
            for (final Settlement com : communes) {
                generateCommune(com);
            }
        }
    }

    private void generateCountyCategory(final County county, final boolean town) throws Exception {
        final String type = town ? "Oraș" : "Sat";
        final String categoryName = "Categorie:" + type + "e în județul " + county.getName();
        final Map pageInfo = rowiki.getPageInfo(categoryName);
        if (!BooleanUtils.isTrue((Boolean) pageInfo.get("exists"))) {
            final StringBuilder catText = new StringBuilder("[[Categorie:");
            catText.append(town ? "Oraș" : "Sat");
            catText.append("e în Ungaria|");
            catText.append(county.getName());
            catText.append("]]");
            executor.save(categoryName, catText.toString(), "Robot: creare categorie pentru " + StringUtils.lowerCase(type)
                + "e din Ungaria");
        }
    }

    private void initCommune(final Settlement com) {
        if (null == roWpNames.get(com)) {
            final LazyInitializer<String> roWpNameIniter = new LazyInitializer<String>() {
                @Override
                protected String initialize() throws ConcurrentException {
                    final List<String> candidateNames = getRoWpCandidateNames(com);

                    final boolean[] existenceArray = new boolean[candidateNames.size()];
                    int i = 0;
                    for (final String candidateName : candidateNames) {
                        try {
                            final Map candidatePageInfo = rowiki.getPageInfo(candidateName);
                            existenceArray[i] = BooleanUtils.isTrue((Boolean) candidatePageInfo.get("exists"));
                            if (existenceArray[i]) {
                                final String actualCandidateTitle = StringUtils.defaultString(
                                    rowiki.resolveRedirect(candidateName), candidateName);
                                final String[] categories = rowiki.getCategories(actualCandidateTitle);
                                for (final String categ : categories) {
                                    if (StringUtils.startsWithAny(categ, "Categorie:Orașe în Ungaria",
                                        "Categorie:Orașe în județul", "Categorie:Sate în Ungaria",
                                        "Categorie:Sate în județul", "Categorie:Orașe în comitatul",
                                        "Categorie:Sate în comitatul", "Categorie:Comune în județul",
                                        "Categorie:Comune în comitatul")) {
                                        return actualCandidateTitle;
                                    }
                                }
                            }
                            i++;
                        } catch (final IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    i = 0;
                    while (i < candidateNames.size() && existenceArray[i]) {
                        i++;
                    }
                    return candidateNames.get(i < candidateNames.size() ? i : 0);
                }

            };
            roWpNames.put(com, roWpNameIniter);
        }
        if (null == huWpNames.get(com)) {
            LazyInitializer<String> huWpNameIniter;
            if (null == (huWpNameIniter = huWpNames.get(com))) {
                huWpNameIniter = new LazyInitializer<String>() {

                    @Override
                    protected String initialize() throws ConcurrentException {
                        final String communeName = retrieveName(com);
                        final List<String> candidateNames = Arrays.asList(communeName + " (Magyarország)", communeName
                            + " (" + com.getDistrict().getCounty().getName() + "megye)", communeName + " (település)",
                            communeName, StringUtils.replace(communeName, "-", " ") + " ("
                                + com.getDistrict().getCounty().getName() + " megye)",
                            StringUtils.replace(communeName, "-", " ") + " (település)",
                            StringUtils.replace(communeName, "-", " "));
                        try {
                            for (final String candidateName : candidateNames) {
                                if (huwiki.exists(new String[] { candidateName })[0]) {
                                    String redirectedPage = huwiki.resolveRedirect(candidateName);
                                    redirectedPage = StringUtils.defaultString(redirectedPage, candidateName);
                                    return redirectedPage;
                                }
                            }
                        } catch (final IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        return null;
                    }
                };
                huWpNames.put(com, huWpNameIniter);
            }
        }
    }

    private List<String> getRoWpCandidateNames(final Settlement com) {
        final String communeName = retrieveName(com);
        if (com.getTown() > 1) {
            return Arrays.asList(communeName, communeName + ", Ungaria", communeName + ", "
                + com.getDistrict().getCounty().getName());
        } else {
            return Arrays.asList(communeName + ", " + com.getDistrict().getCounty().getName(), communeName);
        }
    }

    private void generateCountyNavTemplate(final County county, final List<Settlement> communes) throws Exception {
        Collections.sort(communes, new Comparator<Settlement>() {

            public int compare(final Settlement o1, final Settlement o2) {
                if (o1.getTown() != o1.getTown()) {
                    return o2.getTown() - o1.getTown();
                }
                if (!StringUtils.equals(o1.getName(), o2.getName())) {
                    return o1.getName().compareTo(o2.getName());
                }
                return o1.getDistrict().getCounty().getName().compareTo(o2.getDistrict().getCounty().getName());
            }
        });

        final List<String> townsLinks = new ArrayList<String>();
        final List<String> capitalTownLink = new ArrayList<String>();
        final List<String> majorTownsLinks = new ArrayList<String>();
        final List<String> communeLinks = new ArrayList<String>();
        for (final Settlement com : communes) {
            final String communeName = retrieveNameFromExistingArticle(com);
            initCommune(com);
            switch (com.getTown()) {
            case 1:
                communeLinks.add("[[" + roWpNames.get(com).get() + "|" + communeName + "]]");
                break;
            case 2:
                townsLinks.add("[[" + roWpNames.get(com).get() + "|" + communeName + "]]");
                break;
            case 3:
                majorTownsLinks.add("[[" + roWpNames.get(com).get() + "|" + communeName + "]]");
                break;
            case 4:
                capitalTownLink.add("[[" + roWpNames.get(com).get() + "|" + communeName + "]]");
                break;
            default:
                break;
            }
        }
        final RuleBasedCollator huCollator = new RuleBasedCollator("< a,A < á,Á < b,B < c,C < cs,Cs < d,D < dz,Dz < dzs,Dzs"
            + " < e,E < é,É < f,F < g,G < gy,Gy < h,H < i,I < í,Í < j,J"
            + "< k,K < l,L < ly,Ly < m,M < n,N < ny,Ny < o,O < ó,Ó" + "< ö,Ö < ő,Ő < p,P < q,Q < r,R < s,S < sz,Sz < t,T"
            + "< ty,Ty < u,U < ú,Ú < ü,Ü < ű,Ű < v,V < w,W < x,X < y,Y < z,Z < zs,Zs");
        final Comparator<String> stringComparator = new Comparator<String>() {

            public int compare(final String o1, final String o2) {
                return huCollator.compare(o1, o2);
            }
        };
        Collections.sort(townsLinks, stringComparator);
        Collections.sort(communeLinks, stringComparator);
        Collections.sort(majorTownsLinks, stringComparator);

        final String navTemplateName = "Județul " + county.getName();

        final StringBuilder navTemplateBuilder = new StringBuilder(
            "{{Casetă de navigare simplă\n|titlu=Sate și orașe în [[județul ");
        navTemplateBuilder.append(county.getName());
        navTemplateBuilder.append("]]\n|nume=");
        navTemplateBuilder.append(navTemplateName);
        int nextGroup = 1;
        navTemplateBuilder.append("\n|grup" + nextGroup + "=Reședința");
        navTemplateBuilder.append("\n|listă" + nextGroup + "=<div>\n");
        navTemplateBuilder.append(StringUtils.join(capitalTownLink.toArray(new String[capitalTownLink.size()]), "{{~}}\n"));
        navTemplateBuilder.append("\n</div>");
        if (0 < majorTownsLinks.size()) {
            nextGroup++;
            navTemplateBuilder.append("\n|grup" + nextGroup + "=Orașe de importanță națională");
            navTemplateBuilder.append("\n|listă" + nextGroup + "=<div>\n");
            navTemplateBuilder.append(StringUtils.join(majorTownsLinks.toArray(new String[majorTownsLinks.size()]),
                "{{~}}\n"));
            navTemplateBuilder.append("\n</div>");
        }
        if (0 < townsLinks.size()) {
            nextGroup++;
            navTemplateBuilder.append("\n|grup" + nextGroup + "=Orașe");
            navTemplateBuilder.append("\n|listă" + nextGroup + "=<div>\n");
            navTemplateBuilder.append(StringUtils.join(townsLinks.toArray(new String[townsLinks.size()]), "{{~}}\n"));
            navTemplateBuilder.append("\n</div>");
        }
        if (0 < communeLinks.size()) {
            nextGroup++;
            navTemplateBuilder.append("\n|grup" + nextGroup + "=Sate");
            navTemplateBuilder.append("\n|listă" + nextGroup + "=<div>\n");
            navTemplateBuilder.append(StringUtils.join(communeLinks.toArray(new String[communeLinks.size()]), "{{~}}\n"));
            navTemplateBuilder.append("\n</div>");
        }
        navTemplateBuilder.append("}}<noinclude>[[Categorie:Formate de navigare județe din Ungaria]]</noinclude>");

        executor.save("Format:Județul " + county.getName(), navTemplateBuilder.toString(),
            "Robot: creare format navigare orașe și sate componente ale județului " + county.getName() + " din Ungaria");

    }

    private void generateCommune(final Settlement com) throws Exception {
        final String title = getExistingRoTitleOfArticleWithSubject(com);

        final String demographySection = generateDemographySection(com);
        final String infobox = generateInfoboxForCommune(com);
        final String articleIntro = generateIntroForCommune(com);
        final String refSection = NOTE_REFLIST;
        final String closingStatements = generateClosingStatements(com);

        final String disambigTitle = createDisambig(com);
        final String newArticleContent = (null == disambigTitle ? "" : ("{{Altesensuri2|" + disambigTitle + "}}")) + infobox
            + articleIntro + demographySection + "<br clear=\"left\"/>" + refSection + closingStatements;
        final boolean newPage = isNewPage(com, title, infobox.length() + articleIntro.length() + refSection.length()
            + closingStatements.length());

        if (newPage) {
            generateNewCommuneArticle(com, newArticleContent);
        } else {
            addDemographySectionToExistingArticle(com, title, demographySection, infobox);
        }
    }

    private void generateNewCommuneArticle(final Settlement com, final String text) throws Exception {
        final String roWpArticleTitle = roWpNames.get(com).get();
        final String communeName = retrieveNameFromExistingArticle(com);
        System.out.println("------------------ New settlement article for " + communeName + " title=" + roWpArticleTitle
            + " ----------------");
        System.out.println(text);

        executor.save(roWpArticleTitle, text, "Robot: (re-)creare articol despre " + communeName + ", Ungaria");
        createRedirects(roWpArticleTitle, com);
        executor.link("rowiki", roWpArticleTitle, "huwiki", huWpNames.get(com).get());
    }

    private String createDisambig(final Settlement com) throws Exception {
        final long countCommunesWithName = hib.countCommunesWithName(com.getName());
        if (countCommunesWithName > 1l) {
            final List<Settlement> communesWithName = hib.getCommunesWithName(com.getName());
            final StringBuilder disambig = new StringBuilder("Denumirea de '''");
            disambig.append(com.getName());
            disambig.append("''' se poate referi la următoarele locuri din [[Ungaria]]:");
            int townsCount = 0;
            for (final Settlement eachCom : communesWithName) {
                townsCount += (eachCom.getTown() > 1) ? 1 : 0;
                disambig.append("\n* [[");
                if (null == roWpNames.get(eachCom)) {
                    initCommune(eachCom);
                }
                disambig.append(roWpNames.get(eachCom).get());
                disambig.append("|");
                disambig.append(retrieveName(eachCom));
                disambig.append("]], ");
                disambig.append(eachCom.getTown() > 1 ? "oraș" : "sat");
                disambig.append(" în [[județul ");
                disambig.append(eachCom.getDistrict().getCounty().getName());
                disambig.append("]];");
            }
            disambig.replace(disambig.length() - 1, disambig.length(), ".");
            disambig.append("\n{{Dezambiguizare}}");
            final String title = townsCount > 0 ? (com.getName() + " (dezambiguizare)") : com.getName();
            if (!rowiki.exists(new String[] { title })[0]) {
                executor.save(title, disambig.toString(),
                    "Robot: creare pagină dezambiguizare pentru orașele/comunele maghiare denumite „" + com.getName() + "”");
            } else {
                String disambigTalk = "";
                if (rowiki.exists(new String[] { "Discuție:" + title })[0]) {
                    disambigTalk = rowiki.getPageText("Discuție:" + title);
                }
                executor.save(
                    "Discuție:" + title,
                    disambigTalk + "\n== De adăugat ==\n"
                        + disambigTalk.substring(0, disambigTalk.length() - "{{Dezambiguizare}}".length()),
                    "Robot: date de adăugat în pagina de dezambiguizare despre așezări în Ungaria");
            }
            return title;
        }
        return null;
    }

    private void createRedirects(final String roWpArticleTitle, final Settlement com) throws Exception {
        final List<String> redirects = new ArrayList<String>();
        redirects.add(retrieveName(com));
        redirects.add(retrieveName(com) + ", Ungaria");
        redirects.add(retrieveName(com) + ", " + com.getDistrict().getCounty().getName());
        if (com.getTown() == 1) {
            redirects.add("Comuna " + retrieveName(com));
            redirects.add("Comuna " + retrieveName(com) + ", Ungaria");
            redirects.add("Comuna " + retrieveName(com) + ", " + com.getDistrict().getCounty().getName());
        }

        for (final String redirect : redirects) {
            if (StringUtils.equals(roWpArticleTitle, redirect)) {
                continue;
            }
            if (rowiki.exists(new String[] { redirect })[0]) {
                final String redirectsTo = rowiki.resolveRedirect(redirect);
                if (!StringUtils.equals(redirectsTo, roWpArticleTitle)) {
                    final String logPage = "Utilizator:Andrebot/Comune Ungaria/Redirecționări necreate";
                    String logdata = null;
                    if (rowiki.exists(new String[] { logPage })[0]) {
                        logdata = rowiki.getPageText(logPage);
                    }
                    if (!StringUtils.contains(logdata, "[[" + redirect + "]]")) {
                        executor.save(logPage, StringUtils.defaultString(logdata) + "\n* [[" + redirect + "]]",
                            "Robot: logat redirect necreat");
                    }
                }
            } else {
                executor.save(redirect, "#redirect[[" + roWpArticleTitle + "]]", "Robot: creare redirect către [["
                    + roWpArticleTitle + "]]");
            }
        }
    }

    private String generateClosingStatements(final Settlement com) {
        final StringBuilder closingst = new StringBuilder();

        closingst.append("\n");
        closingst.append("\n{{Județul " + com.getDistrict().getCounty().getName() + "}}");
        closingst.append("\n[[Categorie:");
        closingst.append(com.getTown() > 1 ? "Oraș" : "Sat");
        closingst.append("e în județul " + com.getDistrict().getCounty().getName());
        closingst.append("]]");

        return closingst.toString();
    }

    private String generateIntroForCommune(final Settlement com) {
        final STGroup stgroup = new STGroupFile("templates/hu/town.stg");
        final ST introTmpl = stgroup.getInstanceOf("introTmpl" + (com.getTown() > 1 ? "Town" : "Comm"));
        final String communeName = retrieveNameFromExistingArticle(com);
        introTmpl.add("nume", communeName);
        introTmpl.add("nume_nativ", StringUtils.equals(com.getName(), communeName) ? "" : ("(în {{hu|" + com.getName() + "}})"));
        introTmpl.add("district", StringUtils.removeEnd(com.getDistrict().getName(), "i"));
        introTmpl.add("judet", com.getDistrict().getCounty().getName());
        introTmpl.add("populatie",
            "{{formatnum:" + com.getPopulation() + "}}&nbsp;" + Utilities.de(com.getPopulation(), "", ""));
        return introTmpl.render();
    }

    private String generateInfoboxForCommune(final Settlement com) throws ConcurrentException {
        StringBuilder infoboxText = new StringBuilder("{{Infocaseta Așezare");
        final String name = retrieveNameFromExistingArticle(com);
        infoboxText.append("\n|nume = ");
        infoboxText.append(name);
        if (!StringUtils.equals(com.getName(), name)) {
            infoboxText.append("\n|nume_nativ = ");
            infoboxText.append(com.getName());
        }
        infoboxText.append("}}");
        final WikiTemplate ibReader = new WikiTemplate(infoboxText.toString());
        final Map<String, String> ibParams = ibReader.getParams();

        putBasicDataIntoParams(com, ibParams);
        putDemoDataIntoParams(com, ibParams);
        extractDataFromExternalWikiToInfobox(com, ibParams);

        infoboxText = new StringBuilder("{{Infocaseta Așezare|");
        for (final String param : ibParams.keySet()) {
            infoboxText.append("\n|");
            infoboxText.append(param);
            infoboxText.append(" = ");
            infoboxText.append(ibParams.get(param));
        }
        infoboxText.append("}}\n");
        return infoboxText.toString();
    }

    private void extractDataFromExternalWikiToInfobox(final Settlement com, final Map<String, String> ibParams)
        throws ConcurrentException {
        final LazyInitializer<String> hrWpNameIniter = huWpNames.get(com);
        final String hrWpName = hrWpNameIniter.get();
        try {

            final String huPageText = huwiki.getPageText(hrWpName);
            final Matcher hrwpInfoboxMatcher = huWpTemplates1.matcher(huPageText);
            if (hrwpInfoboxMatcher.find()) {
                final String existingInfobox = hrwpInfoboxMatcher.group();
                final WikiTemplate ibparamreader = new WikiTemplate(existingInfobox);
                final Map<String, String> params = ibparamreader.getParams();
                translateInfoboxParam(ibParams, "stemă", params, "címer");
                translateInfoboxParam(ibParams, "lider_nume", params, "polgármester");
                ibParams.put("lider_titlu", "Primar");
                translateInfoboxParam(ibParams, "codpoștal", params, "irányítószám");
                translateInfoboxParam(ibParams, "prefix_telefonic", params, "körzethívószám");
                translateInfoboxParam(ibParams, "latd", params, "szélességi fok");
                translateInfoboxParam(ibParams, "longd", params, "hosszúsági fok");
                translateInfoboxParam(ibParams, "latm", params, "szélességi ívperc");
                translateInfoboxParam(ibParams, "lats", params, "szélességi ívmásodperc");
                translateInfoboxParam(ibParams, "longm", params, "hosszúsági ívperc");
                translateInfoboxParam(ibParams, "longs", params, "hosszúsági ívmásodperc");
                if (Arrays.asList("Szabolcs-Szatmár-Bereg", "Csongrád", "Hajdú-Bihar", "Békés").contains(
                    com.getDistrict().getCounty().getName())) {
                    ibParams.put("pushpin_label_position", "left");
                }
                if (params.containsKey("szélességi fok")) {
                    ibParams.put("pushpin_map", "Ungaria");
                    ibParams.put("latNS", "N");
                    ibParams.put("longEV", "E");
                }
                translateInfoboxParam(ibParams, "imagine", params, "kép");
            }

        } catch (final IOException e) {
            e.printStackTrace();
        }

    }

    private void translateInfoboxParam(final Map<String, String> ibParams, final String toString,
                                       final Map<String, String> params, final String fromString) {
        if (params.containsKey(fromString) && !StringUtils.isEmpty(params.get(fromString))) {
            ibParams.put(toString, params.get(fromString));
        }
    }

    private void putDemoDataIntoParams(final Settlement com, final Map<String, String> ibParams) {
        ibParams.put("recensamant", "2011");
        ibParams.put("populatie", String.valueOf(com.getPopulation()));
        final long countyId = com.getDistrict().getCounty().getId();
        final String countyCode = StringUtils.leftPad(String.valueOf((countyId < 17 ? 1 : 2) + countyId), 2, '0');
        final String ref = "<ref name=\"hu_census_2011\">{{Citat web|url=http://www.ksh.hu/nepszamlalas/docs/tablak/teruleti/"
            + countyCode
            + "/"
            + countyCode
            + "_4_1_1_1.xls"
            + "|publisher=Biroul Central de Statistică al Ungariei|accessdate=2014-06-11|title=Populația istorică a județului "
            + com.getDistrict().getCounty().getName() + " pe sate și orașe}}</ref> ";

        ibParams.put("populatie_note_subsol", ref);
        final NumberFormat areaNumberFormat = NumberFormat.getNumberInstance();
        areaNumberFormat.setMaximumFractionDigits(2);
        ibParams.put("suprafață_totală_km2", "{{formatnum:" + areaNumberFormat.format(com.getArea() / 100.0) + "}}");
    }

    private void putBasicDataIntoParams(final Settlement com, final Map<String, String> ibParams) {
        switch (com.getTown()) {
        case 4:
        case 3:
            ibParams.put("tip_asezare", "Municipiu");
            break;
        case 2:
            ibParams.put("tip_asezare", "Oraș");
            break;
        default:
            ibParams.put("tip_asezare", "Sat");
        }
        ibParams.put("tip_subdiviziune", "[[Țările lumii|Țară]]");
        ibParams.put("tip_subdiviziune1", "[[Județele Ungariei|Județ]]");
        ibParams.put("tip_subdiviziune2", "[[Districtele Ungariei|District]]");
        ibParams.put("nume_subdiviziune", "{{HUN}}");
        ibParams.put("nume_subdiviziune1", "[[Județul " + com.getDistrict().getCounty().getName() + "|"
            + com.getDistrict().getCounty().getName() + "]]");
        ibParams
            .put("nume_subdiviziune2", "[[Districtul " + StringUtils.removeEnd(com.getDistrict().getName(), "i") + ", "
                + com.getDistrict().getCounty().getName() + "|" + StringUtils.removeEnd(com.getDistrict().getName(), "i")
                + "]]");

        ibParams.put("fus_orar", "[[Ora Europei Centrale|CET]]");
        ibParams.put("fus_orar_DST", "[[Ora de Vară a Europei Centrale|CEST]]");
        ibParams.put("utc_offset", "+1");
        ibParams.put("utc_offset_DST", "+2");
    }

    private void addDemographySectionToExistingArticle(final Settlement com, final String title,
                                                       final String demographySection, final String infobox)
        throws Exception {
        final String pageText = rowiki.getPageText(title);
        final StringBuilder sbuild = new StringBuilder(pageText);
        final Map<String, String> sectionMap = rowiki.getSectionMap(title);
        if (sectionMap.containsValue("Demografie")) {
            int i = 1;
            final List<Integer> demographySectionIndices = new ArrayList<Integer>();
            for (final String key : sectionMap.keySet()) {
                if (StringUtils.equals(sectionMap.get(key), "Demografie")) {
                    demographySectionIndices.add(i);
                }
                i++;
            }
            for (final Integer demographySectionIndex : demographySectionIndices) {
                String demogSection = rowiki.getSectionText(title, demographySectionIndex);
                if (StringUtils.contains(demogSection, "<!--Sfârșit secțiune generată de Andrebot -->== Note ==")) {
                    demogSection = StringUtils.substring(demogSection, 0,
                        StringUtils.indexOf(demogSection, "<!--Sfârșit secțiune generată de Andrebot -->== Note ==")
                            + StringUtils.length("<!--Sfârșit secțiune generată de Andrebot -->"));
                }
                final int indexOfDemogSection = StringUtils.indexOf(sbuild, demogSection);
                if (0 < indexOfDemogSection) {
                    sbuild.delete(indexOfDemogSection - 1, indexOfDemogSection + StringUtils.length(demogSection));
                }
            }
        }

        final List<String> markers = Arrays.asList("==Note", "== Note", "== Vezi și", "==Vezi și", "[[Categorie:", "{{Ciot",
            "{{ciot", "{{Ungaria", "==Referințe", "== Referințe");
        final List<Integer> markerLocations = createMarkerLocationList(sbuild, markers);

        int insertLocation;
        if (markerLocations.size() > 0) {
            insertLocation = Collections.min(markerLocations);
        } else {
            insertLocation = -1;
        }

        if (!footnotesRegex.matcher(sbuild).find()) {
            if (insertLocation > 0) {
                sbuild.insert(insertLocation, NOTE_REFLIST);
            } else {
                insertLocation = sbuild.length();
                sbuild.append(NOTE_REFLIST);
            }
        }

        if (insertLocation > 0) {
            sbuild.insert(insertLocation, demographySection);
        } else {
            sbuild.append(demographySection);
        }

        String currentInfobox = null;
        final Matcher casetaOraseMatcher = regexCutieOrase.matcher(sbuild);
        final Matcher casetaAsezareMatcher = regexInfocAsezare.matcher(sbuild);
        boolean isCasetaOrase = false;
        if (casetaOraseMatcher.find()) {
            isCasetaOrase = true;
            currentInfobox = casetaOraseMatcher.group();
        } else if (casetaAsezareMatcher.find()) {
            currentInfobox = casetaAsezareMatcher.group();
        }
        if (null != currentInfobox && (currentInfobox.length() <= infobox.length() || isCasetaOrase)) {
            sbuild
                .replace(sbuild.indexOf(currentInfobox), sbuild.indexOf(currentInfobox) + currentInfobox.length(), infobox);
        } else if (null != currentInfobox) {
            final WikiTemplate crtIbReader = new WikiTemplate(currentInfobox);
            final Map<String, String> crtIbParams = crtIbReader.getParams();
            final WikiTemplate updatedIbReader = new WikiTemplate(infobox);
            final Map<String, String> updatedParams = updatedIbReader.getParams();
            for (final String updatedParam : updatedParams.keySet()) {
                crtIbParams.put(updatedParam, updatedParams.get(updatedParam));
            }
            final StringBuilder infoboxBuilder = new StringBuilder("{{Infocaseta Așezare");
            for (final String crtIbParam : crtIbParams.keySet()) {
                infoboxBuilder.append("\n|");
                infoboxBuilder.append(crtIbParam);
                infoboxBuilder.append(" = ");
                infoboxBuilder.append(crtIbParams.get(crtIbParam));
            }
            infoboxBuilder.append("\n}}");
            sbuild.replace(sbuild.indexOf(currentInfobox), sbuild.indexOf(currentInfobox) + currentInfobox.length(),
                infoboxBuilder.toString());
        } else {
            sbuild.insert(0, infobox);
        }
        System.out.println("Inserting demography section into article \"" + title + "\", article becomes: "
            + sbuild.toString());
        executor.save(title, sbuild.toString(),
            "Robot: adăugare date demografice conform recensământului din 2011 și date de la hu.wp în infocasetă");
    }

    private List<Integer> createMarkerLocationList(final CharSequence pageText, final List<String> markers) {
        final List<Integer> ret = new ArrayList<Integer>();
        for (final String marker : markers) {
            final int markerLocation = StringUtils.indexOf(pageText, marker);
            if (0 <= markerLocation) {
                ret.add(markerLocation);
            }
        }

        return ret;
    }

    private String generateDemographySection(final Settlement com) {
        final StringBuilder demographics = new StringBuilder(
            "\n== Demografie ==\n<!-- Start secțiune generată de Andrebot -->");
        final STGroup templateGroup = com.getTown() > 1 ? townTemplateGroup : communeTemplateGroup;
        final ST piechart = templateGroup.getInstanceOf("piechart");
        final int population = com.getPopulation();
        final String communeName = retrieveNameFromExistingArticle(com);
        piechart.add("nume", communeName);
        switch (com.getTown()) {
        case 4:
        case 3:
            piechart.add("tip_genitiv", "municipiului");
            break;
        case 2:
            piechart.add("tip_genitiv", "orașului");
            break;
        default:
            piechart.add("tip_genitiv", "satului");
            break;
        }

        final Map<Nationality, Integer> ethnicStructure = com.getEthnicStructure();
        final Map<Religion, Integer> religiousStructure = com.getReligiousStructure();
        final DefaultPieDataset datasetEthnos = new DefaultPieDataset();
        computeEthnicityDataset(population, ethnicStructure, datasetEthnos);
        int totalEthn = 0;
        for (final Nationality nat : nationColorMap.keySet()) {
            if (null != ethnicStructure.get(nat)) {
                totalEthn += ethnicStructure.get(nat);
            }
        }
        totalEthn = 0 == totalEthn ? population : totalEthn;
        final DefaultPieDataset datasetReligion = new DefaultPieDataset();
        computeReligionDataset(population, religiousStructure, datasetReligion);

        renderPiechart(demographics, piechart, population, totalEthn, datasetEthnos, datasetReligion);

        final ST demogIntro = templateGroup.getInstanceOf("demogIntro" + (com.getTown() > 1 ? "Town" : "Comm"));
        demogIntro.add("nume", communeName);
        demogIntro.add("populatie",
            "{{formatnum:" + com.getPopulation() + "}}&nbsp;" + Utilities.de(population, "locuitor", "locuitori"));
        demographics.append(demogIntro.render());

        final Nationality majNat = getMajorityEthnicity(com, totalEthn);
        final List<Nationality> ethnicMinorities = getEthnicMinorities(com, totalEthn);
        final Religion majRel = getMajorityReligion(com);
        final List<Religion> religiousMinorities = getReligiousMinorities(com);

        writeEthnodemographics(templateGroup, demographics, totalEthn, ethnicStructure, majNat, ethnicMinorities);
        writeUnknownEthnicity(templateGroup, demographics, totalEthn, datasetEthnos, com);

        writeReligiousDemographics(templateGroup, demographics, population, religiousStructure, majRel, religiousMinorities);
        writeUnknownReligion(templateGroup, demographics, population, datasetReligion, com);

        demographics.append("\n<!--Sfârșit secțiune generată de Andrebot -->\n");
        return demographics.toString();
    }

    private void writeReligiousDemographics(final STGroup templateGroup, final StringBuilder demographics,
                                            final int population, final Map<Religion, Integer> religiousStructure,
                                            final Religion majRel, final List<Religion> minorities) {
        Collections.sort(minorities, new Comparator<Religion>() {

            public int compare(final Religion o1, final Religion o2) {
                final Integer pop1 = religiousStructure.get(o1);
                final Integer pop2 = religiousStructure.get(o2);
                if (pop1 == null) {
                    return 1;
                }
                if (pop2 == null) {
                    return -1;
                }

                return pop2 - pop1;
            }
        });
        if (null != majRel) {
            final ST demogMajority = templateGroup.getInstanceOf("religiousMaj");
            demogMajority.add("rel_maj", getReligionLink(majRel));
            final double majProcent = 100.0 * religiousStructure.get(majRel) / population;
            demogMajority.add("maj_procent", "{{formatnum:" + (Math.round(majProcent * 100) / 100.0) + "}}%");
            demographics.append(demogMajority.render());

            if (minorities.size() == 1) {
                final ST oneMinority = templateGroup.getInstanceOf("oneReligiousMinority");
                final Religion minority = minorities.get(0);
                final double minProcent = 100.0 * religiousStructure.get(minority) / population;

                oneMinority.add("rel_minority", getReligionLink(minority) + " ({{formatnum:"
                    + (Math.round(minProcent * 100) / 100.0) + "}}%)");
                demographics.append(oneMinority.render());
            } else if (minorities.size() > 1) {
                final ST someMinorities = templateGroup.getInstanceOf("someReligiousMinorities");
                final List<String> nationalitiesData = new ArrayList<String>();
                for (final Religion mino : minorities) {
                    final StringBuilder data = new StringBuilder();
                    data.append(getReligionLink(mino));
                    final double minProcent = 100.0 * religiousStructure.get(mino) / population;
                    data.append(" ({{formatnum:" + (Math.round(minProcent * 100) / 100.0) + "}}%)");
                    nationalitiesData.add(data.toString());
                }
                someMinorities.add(
                    "rel_minorities_enum",
                    join(nationalitiesData.toArray(), ", ", 0, nationalitiesData.size() - 1) + " și "
                        + nationalitiesData.get(nationalitiesData.size() - 1));
                demographics.append(someMinorities.render());
            }
        } else if (minorities.size() > 0) {
            final ST noMaj = templateGroup.getInstanceOf("noReligiousMaj");
            final List<String> nationalitiesData = new ArrayList<String>();

            for (final Religion mino : minorities) {
                final StringBuilder data = new StringBuilder();
                data.append(getReligionLink(mino));
                final double minProcent = 100.0 * religiousStructure.get(mino) / population;
                data.append(" ({{formatnum:" + (Math.round(minProcent * 100) / 100.0) + "}}%)");
                nationalitiesData.add(data.toString());
            }
            noMaj.add("rel_enum", join(nationalitiesData.toArray(), ", ", 0, nationalitiesData.size() - 1) + " și "
                + nationalitiesData.get(nationalitiesData.size() - 1));
            demographics.append(noMaj.render());
        }
        demographics.append(". ");
    }

    private Object getReligionLink(final Religion rel) {
        return relLinkMap.get(rel.getName());
    }

    private List<Religion> getReligiousMinorities(final Settlement com) {
        final List<Religion> ret = new ArrayList<Religion>();
        for (final Religion rel : com.getReligiousStructure().keySet()) {
            final double weight = com.getReligiousStructure().get(rel) / (double) com.getPopulation();
            if (weight <= 0.5 && weight > 0.01 && !startsWithAny(rel.getName(), "Alte", "Ne", "Nu au răspuns")) {
                ret.add(rel);
            }
        }
        return ret;
    }

    private Religion getMajorityReligion(final Settlement com) {
        for (final Religion rel : com.getReligiousStructure().keySet()) {
            if (com.getReligiousStructure().get(rel) / (double) com.getPopulation() > 0.5) {
                return rel;
            }
        }
        return null;
    }

    private void writeUnknownReligion(final STGroup templateGroup, final StringBuilder demographics, final int population,
                                      final DefaultPieDataset datasetRel, final Settlement com) {
        final double undeclaredPercent = 100.0
            * (defaultIfNull(datasetRel.getValue("Necunoscută"), new Integer(0)).doubleValue()) / population;
        if (undeclaredPercent > 0) {
            final ST undeclaredTempl = templateGroup.getInstanceOf("unknownRel");
            undeclaredTempl.add("percent", "{{formatnum:" + (Math.round(undeclaredPercent * 100.0d) / 100.0d) + "}}%");
            demographics.append(undeclaredTempl.render());
        }

        demographics.append(getReligionRef(com));

    }

    private void writeUnknownEthnicity(final STGroup templateGroup, final StringBuilder demographics, final int population,
                                       final DefaultPieDataset datasetEthnos, final Settlement com) {
        double undeclaredPercent = 0.0;
        try {
            undeclaredPercent = 100.0d * (defaultIfNull(datasetEthnos.getValue("Necunoscut"), new Integer(0)).doubleValue())
                / population;
        } catch (final UnknownKeyException e) {
            undeclaredPercent = 0.0;
        }
        if (undeclaredPercent > 0) {
            final ST undeclaredTempl = templateGroup.getInstanceOf("unknownEthn");
            undeclaredTempl.add("percent", "{{formatnum:" + (Math.round(undeclaredPercent * 100.0d) / 100.0d) + "}}%");
            demographics.append(undeclaredTempl.render());
        }

        demographics.append(getEthnicityRef(com));
    }

    private void writeEthnodemographics(final STGroup templateGroup, final StringBuilder demographics, final int population,
                                        final Map<Nationality, Integer> ethnicStructure, final Nationality majNat,
                                        final List<Nationality> minorities) {
        Collections.sort(minorities, new Comparator<Nationality>() {

            public int compare(final Nationality o1, final Nationality o2) {
                final Integer pop1 = ethnicStructure.get(o1);
                final Integer pop2 = ethnicStructure.get(o2);
                if (pop1 == null) {
                    return 1;
                }
                if (pop2 == null) {
                    return -1;
                }

                return pop2 - pop1;
            }
        });
        if (null != majNat) {
            final ST demogMajority = templateGroup.getInstanceOf("ethnicMaj");
            demogMajority.add("etnie_maj", getNationLink(majNat));
            final double majProcent = 100.0 * ethnicStructure.get(majNat) / population;
            demogMajority.add("maj_procent", "{{formatnum:" + (Math.round(majProcent * 100) / 100.0) + "}}%");
            demographics.append(demogMajority.render());

            if (minorities.size() == 1) {
                final ST oneMinority = templateGroup.getInstanceOf("oneEthnicMinority");
                final Nationality minority = minorities.get(0);
                final double minProcent = 100.0 * ethnicStructure.get(minority) / population;

                oneMinority.add("minority", getNationLink(minority) + " ({{formatnum:"
                    + (Math.round(minProcent * 100) / 100.0) + "}}%)");
                demographics.append(oneMinority.render());
            } else if (minorities.size() > 1) {
                final ST someMinorities = templateGroup.getInstanceOf("someEthnicMinorities");
                final List<String> nationalitiesData = new ArrayList<String>();
                for (final Nationality mino : minorities) {
                    final StringBuilder data = new StringBuilder();
                    data.append(getNationLink(mino));
                    final double minProcent = 100.0 * ethnicStructure.get(mino) / population;
                    data.append(" ({{formatnum:" + (Math.round(minProcent * 100) / 100.0) + "}}%)");
                    nationalitiesData.add(data.toString());
                }
                someMinorities.add(
                    "minorities_enum",
                    join(nationalitiesData.toArray(), ", ", 0, nationalitiesData.size() - 1) + " și "
                        + nationalitiesData.get(nationalitiesData.size() - 1));
                demographics.append(someMinorities.render());
            }
        } else if (minorities.size() > 0) {
            final ST noMaj = templateGroup.getInstanceOf("noEthnicMaj");
            final List<String> nationalitiesData = new ArrayList<String>();

            for (final Nationality mino : minorities) {
                final StringBuilder data = new StringBuilder();
                data.append(getNationLink(mino));
                final double minProcent = 100.0 * ethnicStructure.get(mino) / population;
                data.append(" ({{formatnum:" + (Math.round(minProcent * 100) / 100.0) + "}}%)");
                nationalitiesData.add(data.toString());
            }
            noMaj.add("ethnicities_enum", join(nationalitiesData.toArray(), ", ", 0, nationalitiesData.size() - 1) + " și "
                + nationalitiesData.get(nationalitiesData.size() - 1));
            demographics.append(noMaj.render());
        }
        demographics.append(". ");
    }

    private Object getNationLink(final Nationality majNat) {
        if (!startsWithAny(majNat.getName(), "Necunoscut", "Nu au răspuns", "Nu au declarat", "Maghiari")) {
            return "[[" + majNat.getName() + "i din Ungaria|" + lowerCase(majNat.getName()) + "]]";
        } else if (startsWith(majNat.getName(), "Maghiari")) {
            return "[[maghiari]]";
        } else {
            return lowerCase(majNat.getName());
        }
    }

    private int computeReligionDataset(final int population, final Map<Religion, Integer> religiousStructure,
                                       final DefaultPieDataset dataset) {
        final Map<String, Integer> smallGroups = new HashMap<String, Integer>();
        hib.getReligionByName("Necunoscută");
        int totalKnownReligion = 0;
        final Set<Religion> religionsSet = religionColorMap.keySet();
        final List<Religion> religionsList = new ArrayList<Religion>(religionsSet);
        Collections.sort(religionsList, new Comparator<Religion>() {

            public int compare(final Religion arg0, final Religion arg1) {
                final int natpop0 = defaultIfNull(religiousStructure.get(arg0), 0);
                final int natpop1 = defaultIfNull(religiousStructure.get(arg1), 0);
                return natpop1 - natpop0;
            }

        });
        int otherRel = 0;
        for (final Religion rel : religionsList) {
            final int natpop = defaultIfNull(religiousStructure.get(rel), 0);
            if (natpop * 100.0 / population > 1.0 && !startsWithAny(rel.getName(), "Necunoscut", "Nu au răspuns", "Alte")
                && natpop > 0) {
                dataset.setValue(rel.getName(), natpop);
            } else if (natpop * 100.0 / population <= 1.0
                && !startsWithAny(rel.getName(), "Necunoscut", "Nu au răspuns", "Alte") && natpop > 0) {
                smallGroups.put(rel.getName(), natpop);
            } else if (startsWithAny(rel.getName(), "Necunoscut", "Nu au răspuns")) {
            } else if (startsWith(rel.getName(), "Alte")) {
                otherRel += natpop;
            }
            if (!startsWithAny(rel.getName(), "Necunoscut", "Nu au răspuns", "Alte")) {
                totalKnownReligion += natpop;
            }
        }
        dataset.setValue("Necunoscută", population - totalKnownReligion);
        if (1 < smallGroups.size()) {
            smallGroups.put("Alte religii", otherRel);
            int smallSum = 0;
            for (final String smallGroup : smallGroups.keySet()) {
                smallSum += ObjectUtils.defaultIfNull(smallGroups.get(smallGroup), 0);
            }
            dataset.setValue("Alte religii", smallSum);
        } else {
            for (final String relname : smallGroups.keySet()) {
                if (!startsWithAny(relname, "Necunoscut", "Nu au răspuns") && smallGroups.containsKey(relname)) {
                    dataset.setValue(relname, smallGroups.get(relname));
                }
            }
            if (0 < otherRel) {
                dataset.setValue("Alte religii", otherRel);
            }
        }
        return totalKnownReligion;
    }

    private void renderPiechart(final StringBuilder demographics, final ST piechart, final int population,
                                final int totalEthn, final DefaultPieDataset datasetEthnos,
                                final DefaultPieDataset datasetReligion) {
        final StringBuilder pieChartEthnosProps = new StringBuilder();
        final StringBuilder pieChartReligProps = new StringBuilder();
        int i = 1;
        demographics.append("<div style=\"float:left\">");

        for (final Object k : datasetEthnos.getKeys()) {
            pieChartEthnosProps.append("\n|label");
            pieChartEthnosProps.append(i);
            pieChartEthnosProps.append('=');
            pieChartEthnosProps.append(k.toString());
            pieChartEthnosProps.append("|value");
            pieChartEthnosProps.append(i);
            pieChartEthnosProps.append('=');
            pieChartEthnosProps
                .append(Math.round(100 * ((datasetEthnos.getValue(k.toString()).doubleValue() * 100) / totalEthn)) / 100.0d);
            pieChartEthnosProps.append("|color");
            pieChartEthnosProps.append(i);
            pieChartEthnosProps.append('=');
            final Color color = nationColorMap.get(nationNameMap.get(k.toString()));
            if (null == color) {
                throw new RuntimeException("Unknown color for nationality " + k);
            }
            pieChartEthnosProps.append(Utilities.colorToHtml(color));
            i++;
        }
        i = 1;
        for (final Object k : datasetReligion.getKeys()) {
            pieChartReligProps.append("\n|label");
            pieChartReligProps.append(i);
            pieChartReligProps.append('=');
            pieChartReligProps.append(k.toString());
            pieChartReligProps.append("|value");
            pieChartReligProps.append(i);
            pieChartReligProps.append('=');
            pieChartReligProps
                .append(Math.round(100 * (datasetReligion.getValue(k.toString()).doubleValue() * 100.0 / population)) / 100.0);
            pieChartReligProps.append("|color");
            pieChartReligProps.append(i);
            pieChartReligProps.append('=');
            final Color color = religionColorMap.get(religionNameMap.get(k.toString()));
            if (null == color) {
                throw new RuntimeException("Unknown color for religion " + k);
            }
            pieChartReligProps.append(Utilities.colorToHtml(color));
            i++;
        }
        piechart.add("propsEthnos", pieChartEthnosProps.toString());
        piechart.add("propsRelig", pieChartReligProps.toString());
        demographics.append(piechart.render());
        demographics.append("</div>\n");
    }

    /**
     * There are two meta-groups: other, that group all small ethnic groups, and unknown that group all people whose
     * ethnicity is unknown
     * 
     * @param population
     * @param ethnicStructure
     * @param dataset
     * @return
     */
    private int computeEthnicityDataset(final int population, final Map<Nationality, Integer> ethnicStructure,
                                        final DefaultPieDataset dataset) {
        final Map<String, Integer> smallGroups = new HashMap<String, Integer>();
        int totalKnownEthnicity = 0;
        final Set<Nationality> ethnicitiesSet = nationColorMap.keySet();
        final List<Nationality> ethnicitiesList = new ArrayList<Nationality>(ethnicitiesSet);
        Collections.sort(ethnicitiesList, new Comparator<Nationality>() {

            public int compare(final Nationality arg0, final Nationality arg1) {
                final int natpop0 = defaultIfNull(ethnicStructure.get(arg0), 0);
                final int natpop1 = defaultIfNull(ethnicStructure.get(arg1), 0);
                return natpop1 - natpop0;
            }

        });
        int total = 0;
        for (final Nationality nat : ethnicitiesList) {
            if (null != ethnicStructure.get(nat)) {
                total += ethnicStructure.get(nat);
            }
        }
        total = 0 == total ? population : total;
        int otherEthn = 0;
        for (final Nationality nat : ethnicitiesList) {
            final int natpop = defaultIfNull(ethnicStructure.get(nat), 0);
            if (natpop * 100.0 / total > 1.0 && !startsWithAny(nat.getName(), "Ne", "Alți") && 0 < natpop) {
                dataset.setValue(nat.getName(), natpop);
            } else if (natpop * 100.0 / total <= 1.0 && !startsWithAny(nat.getName(), "Ne", "Alți") && 0 < natpop) {
                smallGroups.put(nat.getName(), natpop);
            } else if (startsWithAny(nat.getName(), "Alți", "Neclasificat")) {
                otherEthn += natpop;
            }
            if (!startsWithAny(nat.getName(), "Nec", "Ned", "Alți")) {
                totalKnownEthnicity += natpop;
            }
        }
        if (total != totalKnownEthnicity) {
            dataset.setValue("Necunoscut", total - totalKnownEthnicity);
        }

        // add all small groups to other; if only one, just show that one
        if (1 < smallGroups.size()) {
            int smallSum = 0;
            for (final String smallGroup : smallGroups.keySet()) {
                smallSum += ObjectUtils.defaultIfNull(smallGroups.get(smallGroup), 0);
            }
            if (0 < smallSum) {
                dataset.setValue("Alții", smallSum);
            }
        } else {
            for (final String natname : smallGroups.keySet()) {
                if (!startsWithAny(natname, "Ne") && !startsWithAny(natname, "Alți") && smallGroups.containsKey(natname)) {
                    dataset.setValue(natname, smallGroups.get(natname));
                }
            }
            if (0 < otherEthn) {
                dataset.setValue("Alții", otherEthn);
            }
        }
        return totalKnownEthnicity;
    }

    private boolean isNewPage(final Settlement com, final String title, final int newArticleContentSize) throws IOException {
        if (title == null) {
            return true;
        }
        final Map pageInfo = rowiki.getPageInfo(title);
        final Integer intSup = (Integer) pageInfo.get("size");
        if (null != intSup && (intSup < NEW_PAGE_LIMIT || intSup < newArticleContentSize)) {
            return true;
        }
        return false;
    }

    private void close() {
        if (null != ses) {
            final org.hibernate.Transaction tx = ses.getTransaction();
            if (null != tx) {
                tx.rollback();
            }
        }
        if (null != rowiki) {
            rowiki.logout();
        }
        if (null != huwiki) {
            huwiki.logout();
        }
        if (null != dwiki) {
            dwiki.logout();
        }

    }

    private void init() throws FailedLoginException, IOException {
        rowiki = Wiki.newSession("ro.wikipedia.org");
        huwiki = Wiki.newSession("hu.wikipedia.org");
        dwiki = new Wikibase();
        executor = new WikiEditExecutor(rowiki, dwiki);
        // executor = new SysoutExecutor();

        final Properties credentials = new Properties();
        credentials.load(HUWikiGenerator.class.getClassLoader().getResourceAsStream("credentials.properties"));
        final String datauser = credentials.getProperty("UsernameData");
        final String datapass = credentials.getProperty("PasswordData");
        final String user = credentials.getProperty("Username");
        final String pass = credentials.getProperty("Password");
        rowiki.login(user, pass.toCharArray());
        rowiki.setMarkBot(true);
        dwiki.login(datauser, datapass.toCharArray());

        hib = new Hibernator();
        ses = hib.getSession();
        ses.beginTransaction();

        assignColorToNationality("Români", new Color(85, 85, 255));
        assignColorToNationality("Turci", new Color(255, 85, 85));
        assignColorToNationality("Romi", new Color(85, 255, 255));
        assignColorToNationality("Maghiari", new Color(85, 255, 85));
        assignColorToNationality("Bulgari", new Color(0, 192, 0));
        assignColorToNationality("Evrei", new Color(192, 192, 192));
        assignColorToNationality("Croați", new Color(32, 32, 192));
        assignColorToNationality("Sârbi", new Color(192, 32, 32));
        assignColorToNationality("Bosniaci", new Color(64, 64, 128));
        assignColorToNationality("Germani", new Color(255, 85, 255));
        assignColorToNationality("Vlahi", new Color(128, 128, 255));
        assignColorToNationality("Ucraineni", new Color(255, 255, 85));
        assignColorToNationality("Ruteni", new Color(255, 255, 128));
        assignColorToNationality("Ruși", new Color(192, 85, 85));
        assignColorToNationality("Italieni", new Color(64, 192, 64));
        assignColorToNationality("Sloveni", new Color(32, 32, 128));
        assignColorToNationality("Slovaci", new Color(48, 48, 160));
        assignColorToNationality("Cehi", new Color(128, 128, 32));
        assignColorToNationality("Afiliați religios", new Color(255, 255, 255));
        assignColorToNationality("Neclasificat", new Color(192, 192, 192));
        assignColorToNationality("Armeni", new Color(0x62, 0x46, 0x46));
        assignColorToNationality("Necunoscut", new Color(192, 192, 192));
        assignColorToNationality("Alții", new Color(192, 192, 192));
        blandifyColors(nationColorMap);

        assignColorToReligion("Ortodocși", new Color(85, 85, 255));
        assignColorToReligion("Greco-catolici", new Color(255, 85, 255));
        assignColorToReligion("Romano-catolici", new Color(255, 255, 85));
        assignColorToReligion("Reformați", new Color(85, 255, 85));
        assignColorToReligion("Luterani", new Color(192, 64, 192));
        assignColorToReligion("Mozaici", new Color(32, 32, 192));
        assignColorToReligion("Fără religie", new Color(128, 128, 128));
        assignColorToReligion("Atei", new Color(192, 192, 192));
        assignColorToReligion("Alte religii", new Color(32, 32, 32));
        assignColorToReligion("Necunoscută", new Color(192, 192, 192));

        blandifyColors(religionColorMap);
    }

    private static <T extends Object> void blandifyColors(final Map<T, Color> colorMap) {
        for (final T key : colorMap.keySet()) {
            final Color color = colorMap.get(key);
            final int[] colorcomps = new int[3];
            colorcomps[0] = color.getRed();
            colorcomps[1] = color.getGreen();
            colorcomps[2] = color.getBlue();

            for (int i = 0; i < colorcomps.length; i++) {
                if (colorcomps[i] == 0) {
                    colorcomps[i] = 0x3f;
                } else if (colorcomps[i] == 85) {
                    colorcomps[i] = 128;
                } else if (colorcomps[i] == 64) {
                    colorcomps[i] = 85;
                } else if (colorcomps[i] == 128) {
                    colorcomps[i] = 0x9f;
                }
            }
            colorMap.put(key, new Color(colorcomps[0], colorcomps[1], colorcomps[2]));
        }
    }

    private void assignColorToNationality(final String nationalityName, final Color color) throws HibernateException {
        final Nationality nat = hib.getNationalityByName(nationalityName);
        if (null != nat) {
            nationColorMap.put(nat, color);
            nationNameMap.put(nat.getName(), nat);
        }
    }

    private void assignColorToReligion(final String religionName, final Color color) throws HibernateException {
        final Religion rel = hib.getReligionByName(religionName);
        if (null != rel) {
            religionColorMap.put(rel, color);
            religionNameMap.put(rel.getName(), rel);
        }
    }

    private String getExistingRoTitleOfArticleWithSubject(final Settlement com) throws IOException {
        final List<String> alternativeTitles = getRoWpCandidateNames(com);

        for (final String candidateTitle : alternativeTitles) {
            final Map pageInfo = rowiki.getPageInfo(candidateTitle);
            final Boolean exists = (Boolean) pageInfo.get("exists");
            if (!isTrue(exists)) {
                continue;
            }
            final String pageTitle = defaultString(rowiki.resolveRedirect(candidateTitle), candidateTitle);
            final String[] categs = rowiki.getCategories(pageTitle);
            for (final String categ : categs) {
                if (StringUtils.startsWithAny(StringUtils.removeStart(categ, "Categorie:"), "Orașe în Ungaria",
                    "Orașe în județul ", "Comune în Ungaria", "Sate în Ungaria", "Sate în comitatul ",
                    "Orașe în comitatul ", "Comune în județul " + com.getDistrict().getCounty().getName())) {
                    return pageTitle;
                }
            }
        }
        return null;
    }

    private List<Nationality> getEthnicMinorities(final Settlement settl, final int population) {
        final List<Nationality> ret = new ArrayList<Nationality>();
        for (final Nationality nat : settl.getEthnicStructure().keySet()) {
            final double weight = settl.getEthnicStructure().get(nat) / (double) population;
            if (weight <= 0.5 && weight > 0.01 && !startsWithAny(nat.getName(), "Alți", "Ne")) {
                ret.add(nat);
            }
        }
        return ret;
    }

    private Nationality getMajorityEthnicity(final Settlement settl, final int population) {
        for (final Nationality nat : settl.getEthnicStructure().keySet()) {
            if (settl.getEthnicStructure().get(nat) / (double) population > 0.5) {
                return nat;
            }
        }
        return null;
    }

    private String getEthnicityRef(final Settlement com) {
        final long countyId = com.getDistrict().getCounty().getId();
        final String countyCode = StringUtils.leftPad(String.valueOf((countyId < 17 ? 1 : 2) + countyId), 2, '0');
        return "<ref name=\"hu_census_2011_ethnicity\">{{Citat web|url=http://www.ksh.hu/nepszamlalas/docs/tablak/teruleti/"
            + countyCode
            + "/"
            + countyCode
            + "_4_1_6_1.xls"
            + "|publisher=Biroul Central de Statistică al Ungariei|accessdate=2014-06-11|title=Componența etnică a județului "
            + com.getDistrict().getCounty().getName() + " pe sate și orașe}}</ref> ";
    }

    private String getReligionRef(final Settlement com) {
        final long countyId = com.getDistrict().getCounty().getId();
        final String countyCode = StringUtils.leftPad(String.valueOf((countyId < 17 ? 1 : 2) + countyId), 2, '0');
        return "<ref name=\"hu_census_2011_religion\">{{Citat web|url=http://www.ksh.hu/nepszamlalas/docs/tablak/teruleti/"
            + countyCode
            + "/"
            + countyCode
            + "_4_1_7_1.xls"
            + "|publisher=Biroul Central de Statistică al Ungariei|accessdate=2014-06-11|title=Componența confesională a județului "
            + com.getDistrict().getCounty().getName() + " pe sate și orașe}}</ref> ";
    }

    private String retrieveName(final Settlement com) {
        final String[] names = StringUtils.splitByWholeSeparator(com.getName(), " - ");
        return names[0];
    }

    private String retrieveNameFromExistingArticle(final Settlement com) {
        String name;
        try {
            name = StringUtils.trim(StringUtils.substringBefore(StringUtils.substringBefore(roWpNames.get(com).get(), ","),
                "("));
            if (!StringUtils.isEmpty(name)) {
                return name;
            }
        } catch (final ConcurrentException e) {
            System.out.println("!!! " + e.getMessage());
        }
        return retrieveName(com);
    }
}
