package org.wikipedia.ro.populationdb.ua;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.indexOf;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.replace;
import static org.apache.commons.lang3.StringUtils.substring;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.apache.commons.lang3.StringUtils.trim;

import java.awt.Color;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.security.auth.login.FailedLoginException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.jfree.data.general.DefaultPieDataset;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;
import org.wikibase.Wikibase;
import org.wikipedia.Wiki;
import org.wikipedia.ro.populationdb.ua.dao.Hibernator;
import org.wikipedia.ro.populationdb.ua.model.Commune;
import org.wikipedia.ro.populationdb.ua.model.Language;
import org.wikipedia.ro.populationdb.ua.model.LanguageStructurable;
import org.wikipedia.ro.populationdb.ua.model.Raion;
import org.wikipedia.ro.populationdb.ua.model.Region;
import org.wikipedia.ro.populationdb.ua.model.Settlement;
import org.wikipedia.ro.populationdb.util.Executor;
import org.wikipedia.ro.populationdb.util.ParameterReader;
import org.wikipedia.ro.populationdb.util.SysoutExecutor;
import org.wikipedia.ro.populationdb.util.UkrainianTransliterator;
import org.wikipedia.ro.populationdb.util.Utilities;

public class UAWikiGenerator {

    private static final String SEP = "\n";

    public static void main(final String[] args) throws Exception {
        final UAWikiGenerator generator = new UAWikiGenerator();
        try {
            generator.init();
            generator.generateRegions();
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            generator.close();
        }
    }

    private Wiki rowiki;
    private Wikibase dwiki;
    private Executor executor;
    private Hibernator hib;
    private final Map<LanguageStructurable, LazyInitializer<String>> roArticleNames = new HashMap<LanguageStructurable, LazyInitializer<String>>();
    private final Map<LanguageStructurable, LazyInitializer<String>> uaArticleNames = new HashMap<LanguageStructurable, LazyInitializer<String>>();
    private final Map<Language, Color> nationColorMap = new HashMap<Language, Color>();
    private final Map<String, Language> nationNameMap = new HashMap<String, Language>();
    private final Map<String, String> urls = new HashMap<String, String>() {
        private static final long serialVersionUID = 4662530944404292327L;

        {
            put("Crimeea",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_001&ti=19A050501_02_001.%20Distribution%20of%20the%20population%20by%20native%20language,%20Avtonomna%20Respublika%20Krym%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Vinnița",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_005&ti=19A050501_02_005.%20Distribution%20of%20the%20population%20by%20native%20language,%20Vinnytska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Volînia",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_007&ti=19A050501_02_007.%20Distribution%20of%20the%20population%20by%20native%20language,%20Volynska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Dnipropetrovsk",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_012&ti=19A050501_02_012.%20Distribution%20of%20the%20population%20by%20native%20language,%20Dnipropetrovska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Donețk",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_014&ti=19A050501_02_014.%20Distribution%20of%20the%20population%20by%20native%20language,%20Donetska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Jîtomîr",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_018&ti=19A050501_02_018.%20Distribution%20of%20the%20population%20by%20native%20language,%20Zhytomyrska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Zaporijjea",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_023&ti=19A050501_02_023.%20Distribution%20of%20the%20population%20by%20native%20language,%20Zaporizka%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Ivano-Frankivsk",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_026&ti=19A050501_02_026.%20Distribution%20of%20the%20population%20by%20native%20language,%20Ivano-Frankivska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Kiev",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_032&ti=19A050501_02_032.%20Distribution%20of%20the%20population%20by%20native%20language,%20Kyivska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Kirovohrad",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_035&ti=19A050501_02_035.%20Distribution%20of%20the%20population%20by%20native%20language,%20Kirovohradska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Luhansk",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_044&ti=19A050501_02_044.%20Distribution%20of%20the%20population%20by%20native%20language,%20Luhanska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Mîkolaiiv",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_048&ti=19A050501_02_048.%20Distribution%20of%20the%20population%20by%20native%20language,%20Mykolaivska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Odesa",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_051&ti=19A050501_02_051.%20Distribution%20of%20the%20population%20by%20native%20language,%20Odeska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Poltava",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_053&ti=19A050501_02_053.%20Distribution%20of%20the%20population%20by%20native%20language,%20Poltavska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Rivne",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_056&ti=19A050501_02_056.%20Distribution%20of%20the%20population%20by%20native%20language,%20Rivnenska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Sumî",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_059&ti=19A050501_02_059.%20Distribution%20of%20the%20population%20by%20native%20language,%20Sumska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Ternopil",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_061&ti=19A050501_02_061.%20Distribution%20of%20the%20population%20by%20native%20language,%20Ternopilska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Harkiv",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_063&ti=19A050501_02_063.%20Distribution%20of%20the%20population%20by%20native%20language,%20Kharkivska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Herson",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_065&ti=19A050501_02_065.%20Distribution%20of%20the%20population%20by%20native%20language,%20Khersonska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Hmelnîțkîi",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_068&ti=19A050501_02_068.%20Distribution%20of%20the%20population%20by%20native%20language,%20Khmelnytska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Cerkasî",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_071&ti=19A050501_02_071.%20Distribution%20of%20the%20population%20by%20native%20language,%20Cherkaska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Cernăuți",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_073&ti=19A050501_02_073.%20Distribution%20of%20the%20population%20by%20native%20language,%20Chernivetska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Cernigău",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_074&ti=19A050501_02_074.%20Distribution%20of%20the%20population%20by%20native%20language,%20Chernihivska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Sevastopol",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_085&ti=19A050501_02_085.%20Distribution%20of%20the%20population%20by%20native%20language,%20Sevastopol%20(miskrada)%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Transcarpatia",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_021&ti=19A050501_02_021.%20Distribution%20of%20the%20population%20by%20native%20language,%20Zakarpatska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/01/&lang=2&multilang=en");
            put("Liov",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_046&ti=19A050501_02_046.%20Distribution%20of%20the%20population%20by%20native%20language,%20Lvivska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/02/&lang=2&multilang=en");
        }
    };
    private Wiki ukwiki;

    private void init() throws FailedLoginException, IOException {
        rowiki = new Wiki("ro.wikipedia.org");
        ukwiki = new Wiki("uk.wikipedia.org");
        dwiki = new Wikibase();
        // executor = new WikiEditExecutor(rowiki, dwiki);
        executor = new SysoutExecutor();

        final Properties credentials = new Properties();
        credentials.load(UAWikiGenerator.class.getClassLoader().getResourceAsStream("credentials.properties"));
        final String datauser = credentials.getProperty("UsernameData");
        final String datapass = credentials.getProperty("PasswordData");
        final String user = credentials.getProperty("Username");
        final String pass = credentials.getProperty("Password");
        rowiki.login(user, pass.toCharArray());
        rowiki.setMarkBot(true);
        dwiki.login(datauser, datapass.toCharArray());

        hib = new Hibernator();
        final Session ses = hib.getSession();
        ses.beginTransaction();

        assignColorToLanguage("Română", new Color(85, 85, 255));
        assignColorToLanguage("Romani", new Color(85, 255, 255));
        assignColorToLanguage("Greacă", new Color(0, 0, 192));
        assignColorToLanguage("Maghiară", new Color(85, 255, 85));
        assignColorToLanguage("Belarusă", new Color(32, 192, 32));
        assignColorToLanguage("Bulgară", new Color(0, 192, 0));
        assignColorToLanguage("Tătară crimeeană", new Color(192, 192, 255));
        assignColorToLanguage("Ebraică", new Color(192, 255, 255));
        assignColorToLanguage("Karaim", new Color(32, 192, 192));
        assignColorToLanguage("Germană", new Color(255, 85, 255));
        assignColorToLanguage("Ucraineană", new Color(255, 255, 85));
        assignColorToLanguage("Rusă", new Color(192, 85, 85));
        assignColorToLanguage("Slovacă", new Color(48, 48, 160));
        assignColorToLanguage("Alte limbi", new Color(85, 85, 85));
        ses.getTransaction().rollback();
        blandifyColors(nationColorMap);
    }

    private void generateRegions() throws Exception {
        hib.getSession().beginTransaction();
        final List<Region> regions = hib.getAllRegions();
        for (final Region eachReg : regions) {
            for (final Raion raion : eachReg.getRaioane()) {
                for (final Commune com : raion.getCommunes()) {
                    if (1 < com.getSettlements().size()) {
                        for (final Settlement s : com.getSettlements()) {
                            // generateVillageText(s);
                        }
                        if (1 == com.getTown()) {
                            generateCommuneText(com);
                            generateCommuneNavBox(com);
                        }
                    }
                }
                generateRaionText(raion);
                generateRaionNavBox(raion);
                generateRaionCategory(raion);
            }
            for (final Commune com : eachReg.getCities()) {
                generateCommuneText(com);
                generateCommuneNavBox(com);
            }
            generateRegionText(eachReg);
            generateRegionNavBox(eachReg);
        }
    }

    private void generateRaionCategory(final Raion raion) {
        // TODO Auto-generated method stub

    }

    private void generateRaionNavBox(final Raion raion) {
        // TODO Auto-generated method stub

    }

    private void generateRaionText(final Raion raion) {
        // TODO Auto-generated method stub

    }

    private void generateCommuneText(final Commune com) throws Exception {
        System.out.println("------ generating text for commune " + com.getName());
        final String communeRoName = defaultIfBlank(com.getRomanianName(), com.getTransliteratedName());
        final int countCommunesWithThisName = hib.countCommunesByRomanianOrTransliteratedName(communeRoName);
        final int countCommunesWithThisNameInRegion = hib.countCommunesInRegionByRomanianOrTransliteratedName(communeRoName,
            com.computeRegion());
        final List<String> possibleCommuneNames = UAUtils.getPossibleCommuneNames(com, rowiki,
            1 == countCommunesWithThisName, 1 == countCommunesWithThisNameInRegion);

        String actualTitle = null;
        final StringBuilder currentText = new StringBuilder();
        final boolean[] titleExistance = rowiki
            .exists(possibleCommuneNames.toArray(new String[possibleCommuneNames.size()]));
        for (int i = 0; i < titleExistance.length; i++) {
            if (!titleExistance[i]) {
                continue;
            }
            final String eachCandidateTitle = possibleCommuneNames.get(i);
            actualTitle = defaultString(rowiki.resolveRedirect(eachCandidateTitle), eachCandidateTitle);
        }
        if (null == actualTitle) {
            actualTitle = possibleCommuneNames.get(possibleCommuneNames.size() - 1);
        } else {
            currentText.append(rowiki.getPageText(actualTitle));
        }
        final ParameterReader ibParaReader = new ParameterReader(currentText.toString());
        ibParaReader.run();
        final String villageIBText = generateCommuneInfobox(com, ibParaReader);

        String communeIntro = generateCommuneIntro(com, actualTitle);

        String currentCommuneIntro = substringBefore(substring(currentText.toString(), ibParaReader.getTemplateLength()),
            "==");
        currentCommuneIntro = substringBefore(currentCommuneIntro, "{{Localități în ");
        currentCommuneIntro = substringBefore(currentCommuneIntro, "{{Comune în ");
        currentCommuneIntro = trim(currentCommuneIntro);
        if (currentCommuneIntro.length() > communeIntro.length()) {
            communeIntro = currentCommuneIntro;
        }
        currentText.replace(0, ibParaReader.getTemplateLength(), villageIBText);

        int indexOfFirstSection = currentText.indexOf("==", ibParaReader.getTemplateLength());
        if (0 > indexOfFirstSection) {
            indexOfFirstSection = currentText.indexOf("{{Ucraina}}");
        }
        if (0 <= indexOfFirstSection) {
            currentText.replace(villageIBText.length(), indexOfFirstSection, SEP + communeIntro + SEP);
        } else {
            currentText.append(SEP + communeIntro + SEP);
        }

        final String demografie = generateDemographySection(com);

        final int indexOfCurrentDemography = locateFirstOf(currentText, "==Populați", "== Populați", "== Demografie",
            "==Demografie");
        if (0 <= indexOfCurrentDemography) {
            currentText.replace(indexOfCurrentDemography, currentText.indexOf("==", indexOfCurrentDemography + 2) + 2,
                demografie);
        } else {
            final int indexOfFutureDemographySection = locateFirstOf(currentText, "== Economie", "==Economie", "{{Ucraina",
                "==Legături externe", "== Legături externe", "== Vezi și", "==Vezi și", "{{Localități în ", "{{Comune în ");
            if (0 <= indexOfFutureDemographySection) {
                currentText.insert(indexOfFutureDemographySection, demografie);
            } else {
                currentText.append(demografie);
            }
        }

        generateReferencesSection(currentText);

        if (0 > currentText.indexOf("{{Raionul ") && 0 > currentText.indexOf("{{Orașul regional")) {
            currentText.append("\n{{");
            currentText.append(getArticleName(com.getRaion()));
            currentText.append("}}\n");
        }
        if (0 > currentText.indexOf("{{Comuna ")) {
            currentText.append("\n{{");
            currentText.append(getArticleName(com));
            currentText.append("}}\n");
        }
        if (0 > currentText.indexOf("[[Categorie:")) {
            currentText.append("[[Categorie:Comune în ")
                .append(com.getRaion().isMiskrada() ? "orașul regional " : "raionul ")
                .append(obtainActualRomanianName(com.getRaion())).append("]]\n");
        }

        executor.save(actualTitle, currentText.toString(), "Robot - creare/completare articol despre comuna ucraineană "
            + obtainActualRomanianName(com));
    }

    private String generateCommuneIntro(final Commune com, final String actualTitle) {
        final STGroup stgroup = new STGroupFile("templates/ua/ucraina.stg");
        stgroup.getInstanceOf("introVillage");
        if (com.getTown() == 0) {
            return generateRuralCommuneIntro(com, actualTitle);
        } else if (com.getTown() == 2 && com.getRaion() == null) {
            return generateCityIntro(com, actualTitle);
        } else {
            return generateTownIntro(com, actualTitle);
        }
    }

    private String generateTownIntro(final Commune com, final String actualTitle) {
        final STGroup stgroup = new STGroupFile("templates/ua/ucraina.stg");
        final ST introTmpl = stgroup.getInstanceOf("introTown");
        final String roCommuneName = obtainActualRomanianName(com);
        introTmpl.add("nume", roCommuneName);

        String ukName = com.getName();
        if (!StringUtils.equals(roCommuneName, com.getTransliteratedName())) {
            ukName = ukName + '|' + com.getTransliteratedName();
        }
        introTmpl.add("nume_uk", ukName);

        com.getCapital();

        introTmpl.add("statut", 1 == com.getTown() ? "o așezare de tip urban" : "un oraș");

        final StringBuilder raionPart = new StringBuilder();
        final Raion raion = com.getRaion();
        if (null != raion && !raion.isMiskrada()) {
            raionPart.append("[[");
            raionPart.append(getArticleName(raion));
            raionPart.append("|raionul ");
            raionPart.append(obtainActualRomanianName(raion));
            raionPart.append("]], ");
        }
        introTmpl.add("raion", raionPart.toString());

        final StringBuilder regionPart = new StringBuilder("[[");
        final Region reg = com.computeRegion();
        regionPart.append(getArticleName(reg));
        regionPart.append('|');
        regionPart.append(StringUtils.equals(reg.getRomanianName(), "Crimeea") ? "Republica Autonomă " : "regiunea ");
        regionPart.append(obtainActualRomanianName(reg));
        regionPart.append("]]");
        introTmpl.add("regiune", regionPart.toString());

        if (com.getSettlements().size() > 1) {
            final List<String> villageNames = new ArrayList<String>();
            for (final Settlement eachVillage : com.getSettlements()) {
                final String villageRoName = obtainActualRomanianName(eachVillage);
                final String villageArticleName = getArticleName(eachVillage);
                final StringBuilder villageLinkBuilder = new StringBuilder("[[");
                villageLinkBuilder.append(villageArticleName);
                if (!StringUtils.equals(villageArticleName, villageRoName)) {
                    villageLinkBuilder.append('|').append(villageRoName);
                }
                villageLinkBuilder.append("]]");
                if (eachVillage.equals(com.getCapital())) {
                    continue;
                }
                villageNames.add(villageLinkBuilder.toString());
            }
            final StringBuilder villageEnumeration = new StringBuilder("mai cuprinde și satele ");
            for (int i = 0; i < villageNames.size() - 1; i++) {
                villageEnumeration.append(villageNames.get(i)).append(", ");
            }
            villageEnumeration.setLength(villageEnumeration.length() - 2);
            villageEnumeration.append(" și ").append(villageNames.get(villageNames.size() - 1));
            introTmpl.add("sate", villageEnumeration.toString());
        } else {
            introTmpl.add("sate", "nu cuprinde și alte sate");
        }
        List<Raion> outerRaions = hib.findOuterRaionsForCity(com);
        return introTmpl.render();
    }

    private String generateCityIntro(final Commune com, final String actualTitle) {
        final STGroup stgroup = new STGroupFile("templates/ua/ucraina.stg");
        final ST introTmpl = stgroup.getInstanceOf("introCity");
        final String roCommuneName = obtainActualRomanianName(com);
        introTmpl.add("nume", roCommuneName);

        String ukName = com.getName();
        if (!StringUtils.equals(roCommuneName, com.getTransliteratedName())) {
            ukName = ukName + '|' + com.getTransliteratedName();
        }
        introTmpl.add("nume_uk", ukName);

        com.getCapital();

        final StringBuilder regionPart = new StringBuilder("[[");
        final Region reg = com.computeRegion();
        regionPart.append(getArticleName(reg));
        regionPart.append('|');
        regionPart.append(StringUtils.equals(reg.getRomanianName(), "Crimeea") ? "Republica Autonomă " : "regiunea ");
        regionPart.append(obtainActualRomanianName(reg));
        regionPart.append("]]");
        introTmpl.add("regiune", regionPart.toString());

        if (com.getSettlements().size() > 1) {
            final List<String> villageNames = new ArrayList<String>();
            for (final Settlement eachVillage : com.getSettlements()) {
                final String villageRoName = obtainActualRomanianName(eachVillage);
                final String villageArticleName = getArticleName(eachVillage);
                final StringBuilder villageLinkBuilder = new StringBuilder("[[");
                villageLinkBuilder.append(villageArticleName);
                if (!StringUtils.equals(villageArticleName, villageRoName)) {
                    villageLinkBuilder.append('|').append(villageRoName);
                }
                villageLinkBuilder.append("]]");
                if (eachVillage.equals(com.getCapital())) {
                    villageLinkBuilder.append(" (reședința)");
                }
                villageNames.add(villageLinkBuilder.toString());
            }
            final StringBuilder villageEnumeration = new StringBuilder(
                "În afara localității principale, mai cuprinde și satele ");
            for (int i = 0; i < villageNames.size() - 1; i++) {
                villageEnumeration.append(villageNames.get(i)).append(", ");
            }
            villageEnumeration.setLength(villageEnumeration.length() - 2);
            villageEnumeration.append(" și ").append(villageNames.get(villageNames.size() - 1));
            introTmpl.add("are_sate", villageEnumeration.toString());
        } else {
            introTmpl.add("are_sate", "");
        }
        return introTmpl.render();
    }

    private String generateRuralCommuneIntro(final Commune com, final String actualTitle) {
        final STGroup stgroup = new STGroupFile("templates/ua/ucraina.stg");
        final ST introTmpl = stgroup.getInstanceOf("introCommune");
        final String roCommuneName = obtainActualRomanianName(com);
        introTmpl.add("nume", roCommuneName);

        String ukName = com.getName();
        if (!StringUtils.equals(roCommuneName, com.getTransliteratedName())) {
            ukName = ukName + '|' + com.getTransliteratedName();
        }
        introTmpl.add("nume_uk", ukName);

        com.getCapital();

        final StringBuilder raionPart = new StringBuilder();
        final Raion raion = com.getRaion();
        if (null != raion && !raion.isMiskrada()) {
            raionPart.append("[[");
            raionPart.append(getArticleName(raion));
            raionPart.append("|raionul ");
            raionPart.append(obtainActualRomanianName(raion));
            raionPart.append("]], ");
        }
        introTmpl.add("raion", raionPart.toString());

        final StringBuilder regionPart = new StringBuilder("[[");
        final Region reg = com.computeRegion();
        regionPart.append(getArticleName(reg));
        regionPart.append('|');
        regionPart.append(StringUtils.equals(reg.getRomanianName(), "Crimeea") ? "Republica Autonomă " : "regiunea ");
        regionPart.append(obtainActualRomanianName(reg));
        regionPart.append("]]");
        introTmpl.add("regiune", regionPart.toString());

        if (com.getSettlements().size() > 1) {
            final List<String> villageNames = new ArrayList<String>();
            for (final Settlement eachVillage : com.getSettlements()) {
                final String villageRoName = obtainActualRomanianName(eachVillage);
                final String villageArticleName = getArticleName(eachVillage);
                final StringBuilder villageLinkBuilder = new StringBuilder("[[");
                villageLinkBuilder.append(villageArticleName);
                if (!StringUtils.equals(villageArticleName, villageRoName)) {
                    villageLinkBuilder.append('|').append(villageRoName);
                }
                villageLinkBuilder.append("]]");
                if (eachVillage.equals(com.getCapital())) {
                    villageLinkBuilder.append(" (reședința)");
                }
                villageNames.add(villageLinkBuilder.toString());
            }
            final StringBuilder villageEnumeration = new StringBuilder("din satele ");
            for (int i = 0; i < villageNames.size() - 1; i++) {
                villageEnumeration.append(villageNames.get(i)).append(", ");
            }
            villageEnumeration.setLength(villageEnumeration.length() - 2);
            villageEnumeration.append(" și ").append(villageNames.get(villageNames.size() - 1));
            introTmpl.add("sate", villageEnumeration.toString());
        } else {
            introTmpl.add("sate", "numai din satul de reședință");
        }
        return introTmpl.render();
    }

    private String generateCommuneInfobox(final Commune com, final ParameterReader ibParaReader) throws ConcurrentException {
        final String villageRoName = defaultIfBlank(com.getRomanianName(), com.getTransliteratedName());
        final StringBuilder sb = new StringBuilder("{{Infocaseta Așezare");
        sb.append("\n|tip_asezare = ");
        switch (com.getTown()) {
        case 0:
            sb.append("Comună");
            break;
        case 1:
            sb.append("Așezare de tip urban");
            break;
        case 2:
            sb.append(null != com.getRaion() ? "Oraș" : "Oraș regional");
            break;
        }
        sb.append("\n|nume=").append(villageRoName);
        sb.append("\n|nume_nativ=").append(com.getName());
        if (null != com.getRaion()) {
            obtainActualRomanianName(com.getRaion());
        }
        final Region region = com.computeRegion();
        final String regionRoName = obtainActualRomanianName(region);

        sb.append("\n|tip_subdiviziune=[[Țările lumii|Țară]]");
        sb.append("\n|nume_subdiviziune={{UKR}}");
        sb.append("\n|tip_subdiviziune1=[[Regiunile Ucrainei|Regiune]]");
        sb.append("\n|nume_subdiviziune1=[[");
        sb.append(getArticleName(region));
        sb.append("|").append(regionRoName).append("]]");
        if (null != com.getRaion()) {
            if (com.getRaion().isMiskrada()) {
                sb.append("\n|tip_subdiviziune2=Oraș regional");
                sb.append("\n|nume_subdiviziune2=[[");
                sb.append(getArticleName(com.getRaion().getCapital()));
                sb.append('|');
                sb.append(obtainActualRomanianName(com.getRaion().getCapital()));
            } else {
                sb.append("\n|tip_subdiviziune2=[[Raioanele Ucrainei|Raion]]");
                sb.append("\n|nume_subdiviziune2=[[");
                sb.append(getArticleName(com.getRaion()));
                sb.append('|');
                sb.append(obtainActualRomanianName(com.getRaion()));
            }
            sb.append("]]");
        }

        String coordonate = ibParaReader.getParams().get("coordonate");
        if (StringUtils.isBlank(coordonate)) {
            final String ukrainianCommuneArticleName = getUkrainianCommuneArticleName(com);

            boolean uaTextRead = false;
            String uaText = null;
            do {
                try {
                    uaText = ukwiki.getPageText(ukrainianCommuneArticleName);
                    uaTextRead = true;
                } catch (final FileNotFoundException fnfe) {
                    fnfe.printStackTrace();
                    uaTextRead = true;
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            } while (!uaTextRead);

            if (!StringUtils.isEmpty(uaText)
                && (StringUtils.contains(uaText, "{{Сільська рада") || StringUtils.contains(uaText, "{{Смт") || StringUtils
                    .contains(uaText, "{{Місто"))) {
                final int indexOfIB = StringUtils.indexOfAny(uaText, "{{Село", "{{Смт", "{{Місто");
                final String textFromInfobox = uaText.substring(indexOfIB);
                final ParameterReader ukIBPR = new ParameterReader(textFromInfobox);
                ukIBPR.run();
                final Map<String, String> ukIBParams = ukIBPR.getParams();
                coordonate = ukIBParams.get("координати");
                UAUtils.copyParameterFromTemplate(ukIBPR, sb, "щільність", "densitate");
                UAUtils.copyParameterFromTemplate(ukIBPR, sb, "висота", "altitudine");
                UAUtils.copyParameterFromTemplate(ukIBPR, sb, "телефонний код", "prefix_telefonic");
                UAUtils.copyParameterFromTemplate(ukIBPR, sb, "поштовий індекс", "cod_poștal");
                UAUtils.copyParameterFromTemplate(ukIBPR, sb, "площа", "suprafață_totală_km2");
                UAUtils.copyParameterFromTemplate(ukIBPR, sb, "населення", "populație");
                UAUtils.copyParameterFromTemplate(ukIBPR, sb, "зображення", "imagine");
                if (ukIBParams.containsKey("код КОАТУУ")) {
                    sb.append("|tip_cod_clasificare=").append(
                        "{{Ill|uk|KOATUU|Класифікатор об'єктів адміністративно-територіального устрою України|Cod KOATUU}}");
                    UAUtils.copyParameterFromTemplate(ukIBPR, sb, "код КОАТУУ", "cod_clasificare");
                }
            }
        }

        if (StringUtils.isNotBlank(coordonate)) {
            final ParameterReader coordParaReader = new ParameterReader(coordonate);
            coordParaReader.run();
            final Map<String, String> coordParams = coordParaReader.getParams();
            final List<String> coordArgNames = Arrays.asList("latd", "latm", "lats", "latNS", "longd", "longm", "longs",
                "longEV");
            sb.append('\n');
            for (int i = 0; i < coordArgNames.size(); i++) {
                final String argValue = defaultString(coordParams.get(String.valueOf(1 + i)));
                sb.append('|').append(coordArgNames.get(i)).append("=").append(argValue);
            }
            sb.append("\n|pushpin_map=Ucraina Regiunea ").append(regionRoName);
            sb.append("\n|pushpin_map1=Ucraina");
        }
        if (0 > sb.indexOf("populație")) {
            UAUtils.copyParameterFromTemplate(ibParaReader, sb, "populație");
        }
        UAUtils.copyParameterFromTemplate(ibParaReader, sb, "recensământ");
        if (0 > sb.indexOf("altitudine")) {
            UAUtils.copyParameterFromTemplate(ibParaReader, sb, "altitudine");
        }
        if (0 > sb.indexOf("prefix_telefonic")) {
            UAUtils.copyParameterFromTemplate(ibParaReader, sb, "prefix telefonic");
        }
        if (0 > sb.indexOf("cod_poștal")) {
            UAUtils.copyParameterFromTemplate(ibParaReader, sb, "cod poștal");
        }
        if (0 > sb.indexOf("densitate")) {
            UAUtils.copyParameterFromTemplate(ibParaReader, sb, "densitate");
        }
        UAUtils.copyParameterFromTemplate(ibParaReader, sb, "atestare");
        if (0 > sb.indexOf("suprafață_totală_km2")) {
            UAUtils.copyParameterFromTemplate(ibParaReader, sb, "suprafață", "suprafață_totală_km2");
        }
        sb.append("\n}}\n");
        return sb.toString();
    }

    private String getUkrainianCommuneArticleName(final Commune com) throws ConcurrentException {
        LazyInitializer<String> communeInitializer = uaArticleNames.get(com);
        if (null == communeInitializer) {
            communeInitializer = new LazyInitializer<String>() {

                @Override
                protected String initialize() throws ConcurrentException {
                    final List<String> possibleUkrainianArticleNames = getPossibleUkrainianArticleNames(com);
                    boolean[] existance = null;
                    do {
                        try {
                            existance = ukwiki.exists(possibleUkrainianArticleNames
                                .toArray(new String[possibleUkrainianArticleNames.size()]));
                        } catch (final IOException e) {
                            e.printStackTrace();
                        }
                    } while (null == existance);
                    for (int i = 0; i < possibleUkrainianArticleNames.size(); i++) {
                        if (existance[i]) {
                            final String articleCandidateTitle = UAUtils.resolveRedirect(ukwiki,
                                possibleUkrainianArticleNames.get(i));
                            if (UAUtils.isInAnyCategoryTree(articleCandidateTitle, ukwiki, 2, "Сільські ради України", "Міста України",
                                    "Селища міського типу України")) {
                                return articleCandidateTitle;
                            }
                        }
                    }
                    return possibleUkrainianArticleNames.get(0);
                }
            };
            uaArticleNames.put(com, communeInitializer);
        }
        return communeInitializer.get();
    }

    private void generateCommuneNavBox(final Commune com) throws Exception {
        if (com.getSettlements().size() < 2 && com.getTown() == 0) {
            return;
        }
        if (com.getSettlements().size() < 1 && com.getTown() > 0) {
            return;
        }
        final String communeName = obtainActualRomanianName(com);
        final String articleName = getArticleName(com);
        int section = 0;

        final StringBuilder navBox = new StringBuilder(
            "{{Casetă de navigare simplă\n|stare = {{{stare|autopliabilă}}}\n|titlu=Localități componente ale ");
        switch (com.getTown()) {
        case 0:
            navBox.append("[[").append(articleName).append("|comunei ").append(communeName).append("]]");
            break;
        case 1:
            navBox.append("așezării de tip urban ").append("[[").append(articleName);
            if (!StringUtils.equals(articleName, communeName)) {
                navBox.append("|").append(communeName);
            }
            navBox.append("]]");
            break;
        case 2:
            navBox.append("orașului ").append("[[").append(articleName);
            if (!StringUtils.equals(articleName, communeName)) {
                navBox.append("|").append(communeName);
            }
            navBox.append("]]");
            break;
        }
        navBox.append("\n|nume=" + articleName);

        if (com.getTown() == 0) {
            section++;
            navBox.append("\n|grup").append(section).append("=Reședință");
            navBox.append("\n|listă").append(section).append("=[[");
            final String capitalVillageName = obtainActualRomanianName(com.getCapital());
            final String capitalVillageArticle = getArticleName(com.getCapital());
            navBox.append(capitalVillageArticle);
            if (!StringUtils.equals(capitalVillageName, capitalVillageArticle)) {
                navBox.append('|').append(capitalVillageName);
            }
            navBox.append("]]");
        }
        {
            section++;
            navBox.append("\n|grup").append(section).append("=Sate componente");
            navBox.append("\n|listă").append(section).append("=<div>");
            final List<Settlement> villages = new ArrayList<Settlement>(com.getSettlements());
            Collections.sort(villages, new Comparator<Settlement>() {

                public int compare(final Settlement o1, final Settlement o2) {
                    return obtainActualRomanianName(o1).compareTo(obtainActualRomanianName(o2));
                }
            });
            for (final Settlement eachVillage : villages) {
                if (0 == com.getTown() && eachVillage.getId() == com.getCapital().getId()) {
                    continue;
                }
                final String villageArticleName = getArticleName(eachVillage);
                final String villageName = obtainActualRomanianName(eachVillage);
                navBox.append("\n[[").append(villageArticleName);
                if (!StringUtils.equals(villageArticleName, villageName)) {
                    navBox.append('|').append(villageName);
                }
                navBox.append("]]{{~}}");
            }
            navBox.delete(navBox.length() - "{{~}}".length(), navBox.length());
            navBox.append("\n</div>");
        }
        navBox.append("}}<noinclude>");
        navBox.append("[[Categorie:Formate de navigare comune din Ucraina|");
        navBox.append(communeName);
        navBox.append("]]");
        navBox.append("</noinclude>");

        executor.save("Format:" + articleName, navBox.toString(),
            "Robot: creare/regenerare casetă de navigare pentru comuna ucraineană " + articleName);
    }

    private void generateVillageText(final Settlement s) throws Exception {
        final String villageRoName = defaultIfBlank(s.getRomanianName(), s.getTransliteratedName());
        if (s.getCommune().getSettlements().size() <= 1) {
            return;
        }
        final int countVillagesWithThisName = hib.countSettlementsByRomanianOrTransliteratedName(villageRoName);
        int countVillagesWithThisNameInRaion = 1;
        if (null != s.getCommune().getRaion()) {
            countVillagesWithThisNameInRaion = hib.countSettlementsInRaionByRomanianOrTransliteratedName(villageRoName, s
                .getCommune().getRaion());
        }
        final int countVillagesWithThisNameInRegion = hib.countSettlementsInRegionByRomanianOrTransliteratedName(
            villageRoName, s.computeRegion());
        final List<String> possibleSettlementNames = UAUtils.getPossibleSettlementNames(s, rowiki,
            1 == countVillagesWithThisName, 1 == countVillagesWithThisNameInRegion, 1 == countVillagesWithThisNameInRaion);

        String actualTitle = null;
        final StringBuilder currentText = new StringBuilder();
        final boolean[] titleExistance = rowiki.exists(possibleSettlementNames.toArray(new String[possibleSettlementNames
            .size()]));
        for (int i = 0; i < titleExistance.length; i++) {
            if (!titleExistance[i]) {
                continue;
            }
            final String eachCandidateTitle = possibleSettlementNames.get(i);
            actualTitle = defaultString(rowiki.resolveRedirect(eachCandidateTitle), eachCandidateTitle);
        }
        if (null == actualTitle) {
            actualTitle = possibleSettlementNames.get(possibleSettlementNames.size() - 1);
        } else {
            currentText.append(rowiki.getPageText(actualTitle));
        }
        final ParameterReader ibParaReader = new ParameterReader(currentText.toString());
        ibParaReader.run();
        final String villageIBText = generateVillageInfobox(s, ibParaReader);

        String villageIntro = generateVillageIntro(s, actualTitle);

        String currentVillageIntro = substringBefore(substring(currentText.toString(), ibParaReader.getTemplateLength()),
            "==");
        currentVillageIntro = substringBefore(currentVillageIntro, "{{Localități în ");
        currentVillageIntro = substringBefore(currentVillageIntro, "{{Comune în ");
        currentVillageIntro = trim(currentVillageIntro);
        if (currentVillageIntro.length() > villageIntro.length()) {
            villageIntro = currentVillageIntro;
        }
        currentText.replace(0, ibParaReader.getTemplateLength(), villageIBText);

        int indexOfFirstSection = currentText.indexOf("==", ibParaReader.getTemplateLength());
        if (0 > indexOfFirstSection) {
            indexOfFirstSection = currentText.indexOf("{{Ucraina}}");
        }
        if (0 <= indexOfFirstSection) {
            currentText.replace(villageIBText.length(), indexOfFirstSection, SEP + villageIntro + SEP);
        } else {
            currentText.append(SEP + villageIntro + SEP);
        }

        final String demografie = generateDemographySection(s);

        final int indexOfCurrentDemography = locateFirstOf(currentText, "==Populați", "== Populați", "== Demografie",
            "==Demografie");
        if (0 <= indexOfCurrentDemography) {
            currentText.replace(indexOfCurrentDemography, currentText.indexOf("==", indexOfCurrentDemography + 2) + 2,
                demografie);
        } else {
            final int indexOfFutureDemographySection = locateFirstOf(currentText, "== Economie", "==Economie", "{{Ucraina",
                "==Legături externe", "== Legături externe", "== Vezi și", "==Vezi și", "{{Localități în ", "{{Comune în ");
            if (0 <= indexOfFutureDemographySection) {
                currentText.insert(indexOfFutureDemographySection, demografie);
            } else {
                currentText.append(demografie);
            }
        }

        generateReferencesSection(currentText);

        if (0 > currentText.indexOf("{{Comuna ")) {
            currentText.append("\n{{");
            currentText.append(roArticleNames.get(s.getCommune()).get());
            currentText.append("}}\n");
        }
        if (0 > currentText.indexOf("[[Categorie:")) {
            currentText.append("[[Categorie:Sate în ")
                .append(s.getCommune().getRaion().isMiskrada() ? "orașul regional " : "raionul ")
                .append(obtainActualRomanianName(s.getCommune().getRaion())).append("]]\n");
        }

        executor.save(actualTitle, currentText.toString(), "Robot - creare/completare articol despre satul ucrainean "
            + obtainActualRomanianName(s));
    }

    private String generateVillageIntro(final Settlement s, final String actualTitle) {
        final STGroup stgroup = new STGroupFile("templates/ua/ucraina.stg");
        final ST introTmpl = stgroup.getInstanceOf("introVillage");
        final String roVillageName = obtainActualRomanianName(s);
        introTmpl.add("nume", roVillageName);

        String ukName = s.getName();
        if (!StringUtils.equals(roVillageName, s.getTransliteratedName())) {
            ukName = ukName + '|' + s.getTransliteratedName();
        }
        introTmpl.add("nume_uk", ukName);

        final Settlement communeCapital = s.getCommune().getCapital();
        boolean isCommuneCapital = false;
        if (null != communeCapital && communeCapital.getId() == s.getId()) {
            introTmpl.add("statut", "localitatea de reședință a");
            isCommuneCapital = true;
        } else {
            introTmpl.add("statut", "un sat în");
        }

        final Commune com = s.getCommune();
        final String comArticleName = getArticleName(com);
        final StringBuilder communePart = new StringBuilder();
        if (com.getTown() == 0) {
            communePart.append("[[");
            communePart.append(comArticleName);
            communePart.append("|");
            communePart.append(isCommuneCapital ? "comunei " : "comuna ");
            communePart.append(obtainActualRomanianName(com));
            communePart.append("]]");
        } else {
            if (com.getTown() == 1) {
                communePart.append("așezarea urbană ");
            } else {
                communePart.append("orașul ");
                if (null == com.getRaion() || com.getRaion().isMiskrada()) {
                    communePart.append("regional ");
                }
            }
            communePart.append("[[");
            communePart.append(getArticleName(com));
            communePart.append('|');
            communePart.append(obtainActualRomanianName(com));
            communePart.append("]]");
        }
        introTmpl.add("comuna", communePart.toString());

        final StringBuilder raionPart = new StringBuilder();
        final Raion raion = com.getRaion();
        if (null != raion && !raion.isMiskrada()) {
            raionPart.append("[[");
            raionPart.append(getArticleName(raion));
            raionPart.append("|raionul ");
            raionPart.append(obtainActualRomanianName(raion));
            raionPart.append("]], ");
        }
        introTmpl.add("raion", raionPart.toString());

        final StringBuilder regionPart = new StringBuilder("[[");
        final Region reg = s.computeRegion();
        regionPart.append(getArticleName(reg));
        regionPart.append('|');
        regionPart.append(StringUtils.equals(reg.getRomanianName(), "Crimeea") ? "Republica Autonomă " : "regiunea ");
        regionPart.append(obtainActualRomanianName(reg));
        regionPart.append("]]");
        introTmpl.add("regiune", regionPart.toString());
        return introTmpl.render();
    }

    private String getUkrainianVillageArticleName(final Settlement s) throws ConcurrentException {

        LazyInitializer<String> villageInitializer = uaArticleNames.get(s);
        if (null == villageInitializer) {
            villageInitializer = new LazyInitializer<String>() {

                @Override
                protected String initialize() throws ConcurrentException {
                    final List<String> possibleUkrainianArticleNames = getPossibleUkrainianArticleNames(s);
                    boolean[] existance = null;
                    do {
                        try {
                            existance = ukwiki.exists(possibleUkrainianArticleNames
                                .toArray(new String[possibleUkrainianArticleNames.size()]));
                        } catch (final IOException e) {
                            e.printStackTrace();
                        }
                    } while (null == existance);
                    for (int i = 0; i < possibleUkrainianArticleNames.size(); i++) {
                        if (existance[i]) {
                            final String articleCandidateTitle = UAUtils.resolveRedirect(ukwiki,
                                possibleUkrainianArticleNames.get(i));
                            if (UAUtils.isInCategoryTree(articleCandidateTitle, ukwiki, 2, "Населені пункти України")) {
                                return articleCandidateTitle;
                            }
                        }
                    }
                    return possibleUkrainianArticleNames.get(0);
                }
            };
            uaArticleNames.put(s, villageInitializer);
        }
        return villageInitializer.get();

    }

    private String generateVillageInfobox(final Settlement s, final ParameterReader ibParaReader) throws ConcurrentException {
        final String villageRoName = defaultIfBlank(s.getRomanianName(), s.getTransliteratedName());
        final StringBuilder sb = new StringBuilder("{{Infocaseta Așezare");
        sb.append("\n|tip_asezare = Sat");
        sb.append("\n|nume=").append(villageRoName);
        sb.append("\n|nume_nativ=").append(s.getName());
        final Commune commune = s.getCommune();
        final String communeRoName = obtainActualRomanianName(commune);
        if (null != commune.getRaion()) {
            obtainActualRomanianName(commune.getRaion());
        }
        final Region region = s.computeRegion();
        final String regionRoName = obtainActualRomanianName(region);

        sb.append("\n|tip_subdiviziune=[[Țările lumii|Țară]]");
        sb.append("\n|nume_subdiviziune={{UKR}}");
        sb.append("\n|tip_subdiviziune1=[[Regiunile Ucrainei|Regiune]]");
        sb.append("\n|nume_subdiviziune1=[[");
        sb.append(getArticleName(region));
        sb.append("|").append(regionRoName).append("]]");
        if (null != commune.getRaion()) {
            if (commune.getRaion().isMiskrada()) {
                sb.append("\n|tip_subdiviziune2=Oraș regional");
                sb.append("\n|nume_subdiviziune2=[[");
                sb.append(getArticleName(commune.getRaion().getCapital()));
                sb.append('|');
                sb.append(obtainActualRomanianName(commune.getRaion().getCapital()));
            } else {
                sb.append("\n|tip_subdiviziune2=[[Raioanele Ucrainei|Raion]]");
                sb.append("\n|nume_subdiviziune2=[[");
                sb.append(getArticleName(commune.getRaion()));
                sb.append('|');
                sb.append(obtainActualRomanianName(commune.getRaion()));
            }
            sb.append("]]");
        }

        sb.append("\n|tip_subdiviziune3=[[Comunele Ucrainei|Comună]]");
        sb.append("\n|nume_subdiviziune3=[[");
        sb.append(getArticleName(commune));
        sb.append("|").append(communeRoName).append("]]");

        String coordonate = ibParaReader.getParams().get("coordonate");
        if (StringUtils.isBlank(coordonate)) {
            final String ukrainianVillageArticleName = getUkrainianVillageArticleName(s);

            boolean uaTextRead = false;
            String uaText = null;
            do {
                try {
                    uaText = ukwiki.getPageText(ukrainianVillageArticleName);
                    uaTextRead = true;
                } catch (final FileNotFoundException fnfe) {
                    fnfe.printStackTrace();
                    uaTextRead = true;
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            } while (!uaTextRead);

            if (!StringUtils.isEmpty(uaText) && (StringUtils.contains(uaText, "{{Село"))) {
                final int indexOfIB = uaText.indexOf("{{Село");
                final String textFromInfobox = uaText.substring(indexOfIB);
                final ParameterReader ukIBPR = new ParameterReader(textFromInfobox);
                ukIBPR.run();
                final Map<String, String> ukIBParams = ukIBPR.getParams();
                coordonate = ukIBParams.get("координати");
                UAUtils.copyParameterFromTemplate(ukIBPR, sb, "щільність", "densitate");
                UAUtils.copyParameterFromTemplate(ukIBPR, sb, "висота", "altitudine");
                UAUtils.copyParameterFromTemplate(ukIBPR, sb, "телефонний код", "prefix_telefonic");
                UAUtils.copyParameterFromTemplate(ukIBPR, sb, "поштовий індекс", "cod_poștal");
                UAUtils.copyParameterFromTemplate(ukIBPR, sb, "площа", "suprafață_totală_km2");
                UAUtils.copyParameterFromTemplate(ukIBPR, sb, "населення", "populație");
                UAUtils.copyParameterFromTemplate(ukIBPR, sb, "розташування", "imagine");
                if (ukIBParams.containsKey("код КОАТУУ")) {
                    sb.append("|tip_cod_clasificare=").append(
                        "{{Ill|uk|KOATUU|Класифікатор об'єктів адміністративно-територіального устрою України|Cod KOATUU}}");
                    UAUtils.copyParameterFromTemplate(ukIBPR, sb, "код КОАТУУ", "cod_clasificare");
                }
            }
        }

        if (StringUtils.isNotBlank(coordonate)) {
            final ParameterReader coordParaReader = new ParameterReader(coordonate);
            coordParaReader.run();
            final Map<String, String> coordParams = coordParaReader.getParams();
            final List<String> coordArgNames = Arrays.asList("latd", "latm", "lats", "latNS", "longd", "longm", "longs",
                "longEV");
            sb.append('\n');
            for (int i = 0; i < coordArgNames.size(); i++) {
                final String argValue = defaultString(coordParams.get(String.valueOf(1 + i)));
                sb.append('|').append(coordArgNames.get(i)).append("=").append(argValue);
            }
            sb.append("\n|pushpin_map=Ucraina Regiunea ").append(regionRoName);
            sb.append("\n|pushpin_map1=Ucraina");
        }
        if (0 > sb.indexOf("populație")) {
            UAUtils.copyParameterFromTemplate(ibParaReader, sb, "populație");
        }
        UAUtils.copyParameterFromTemplate(ibParaReader, sb, "recensământ");
        if (0 > sb.indexOf("altitudine")) {
            UAUtils.copyParameterFromTemplate(ibParaReader, sb, "altitudine");
        }
        if (0 > sb.indexOf("prefix_telefonic")) {
            UAUtils.copyParameterFromTemplate(ibParaReader, sb, "prefix telefonic");
        }
        if (0 > sb.indexOf("cod_poștal")) {
            UAUtils.copyParameterFromTemplate(ibParaReader, sb, "cod poștal");
        }
        if (0 > sb.indexOf("densitate")) {
            UAUtils.copyParameterFromTemplate(ibParaReader, sb, "densitate");
        }
        UAUtils.copyParameterFromTemplate(ibParaReader, sb, "atestare");
        if (0 > sb.indexOf("suprafață_totală_km2")) {
            UAUtils.copyParameterFromTemplate(ibParaReader, sb, "suprafață", "suprafață_totală_km2");
        }
        sb.append("\n}}\n");
        return sb.toString();
    }

    private void generateRegionNavBox(final Region region) throws Exception {
        if (equalsIgnoreCase("orașul Kiev", region.getRomanianName())) {
            return;
        }
        final List<Commune> regionalCities = new ArrayList<Commune>(hib.getRegionalCitiesForRegion(region));
        regionalCities.remove(region.getCapital());
        Collections.sort(regionalCities, new Comparator<Commune>() {

            public int compare(final Commune o1, final Commune o2) {
                return obtainActualRomanianName(o1).compareTo(obtainActualRomanianName(o2));
            }
        });
        final List<Raion> raions = new ArrayList<Raion>(hib.getRaionsForRegion(region));
        Collections.sort(raions, new Comparator<Raion>() {

            public int compare(final Raion o1, final Raion o2) {
                return obtainActualRomanianName(o1).compareTo(obtainActualRomanianName(o2));
            }
        });
        int section = 0;

        final StringBuilder navBox = new StringBuilder(
            "{{Casetă de navigare simplă\n|stare = {{{stare|autopliabilă}}}\n|titlu=Diviziuni administrative ale [[Regiunea");
        final String regionRomanianName = obtainActualRomanianName(region);
        navBox.append(regionRomanianName);
        navBox.append('|');
        navBox.append(regionRomanianName);
        navBox.append("]]\n|nume=Regiunea " + regionRomanianName);

        {
            section++;
            navBox.append("\n|grup").append(section).append("=Reședință");
            navBox.append("\n|listă").append(section).append("=<div>");
            final String capitalArticleName = getArticleName(region.getCapital());
            final String capitalName = obtainActualRomanianName(region.getCapital());
            navBox.append("[[").append(capitalArticleName);
            if (!StringUtils.equals(capitalArticleName, capitalName)) {
                navBox.append('|').append(capitalName);
            }
            navBox.append("]]");
            navBox.append("\n</div>");
        }
        if (null != regionalCities && 0 < regionalCities.size()) {
            section++;
            navBox.append("\n|grup").append(section).append("=Orașe regionale");
            navBox.append("\n|listă").append(section).append("=<div>");
            for (final Commune regCity : regionalCities) {
                final String regCityArticleName = getArticleName(regCity);
                final String regCityName = obtainActualRomanianName(regCity);
                navBox.append("[[").append(regCityArticleName);
                if (!StringUtils.equals(regCityArticleName, regCityName)) {
                    navBox.append('|').append(regCityName);
                }
                navBox.append("]]{{~}}");
            }
            navBox.delete(navBox.length() - "{{~}}".length(), navBox.length());
            navBox.append("</div>");
        }
        if (null != raions && 0 < raions.size()) {
            section++;
            navBox.append("\n|grup").append(section).append("=[[Raioanele Ucrainei|Raioane]]");
            navBox.append("\n|listă").append(section).append("=<div>");
            for (final Raion raion : raions) {
                final String raionArticleName = getArticleName(raion);
                final String raionName = obtainActualRomanianName(raion);
                navBox.append("[[").append(raionArticleName).append('|').append(raionName).append("]]{{~}}");
            }
            navBox.append("\n</div>");
        }
        navBox.append("}}<noinclude>");
        navBox.append("[[Categorie:Formate de navigare regiuni din Ucraina|");
        navBox.append(regionRomanianName);
        navBox.append("]]");
        navBox.append("</noinclude>");

        executor.save("Format:Regiunea " + regionRomanianName, navBox.toString(),
            "Robot: creare/regenerare casetă de navigare pentru regiunea ucraineană " + regionRomanianName);
    }

    private void generateRegionText(final Region region) throws Exception {
        if (equalsIgnoreCase("orașul Kiev", region.getRomanianName())) {
            return;
        }

        final List<String> candidateRegionArticleNames = new ArrayList<String>();
        if (isNotBlank(region.getRomanianName())) {
            candidateRegionArticleNames.add("Regiunea " + region.getRomanianName());
            candidateRegionArticleNames.add("Regiunea " + region.getRomanianName() + ", Ucraina");
        }
        candidateRegionArticleNames.add("Regiunea " + region.getTransliteratedName());
        candidateRegionArticleNames.add("Regiunea " + region.getTransliteratedName() + ", Ucraina");

        if (equalsIgnoreCase(region.getRomanianName(), "Crimeea")) {
            for (int i = 0; i < candidateRegionArticleNames.size(); i++) {
                candidateRegionArticleNames.set(i,
                    replace(candidateRegionArticleNames.get(i), "Regiunea", "Republica Autonomă"));
            }
        }

        String actualTitle = null;
        final StringBuilder currentText = new StringBuilder();
        final boolean[] titleExistance = rowiki.exists(candidateRegionArticleNames
            .toArray(new String[candidateRegionArticleNames.size()]));
        for (int i = 0; i < titleExistance.length; i++) {
            if (!titleExistance[i]) {
                continue;
            }
            final String eachCandidateTitle = candidateRegionArticleNames.get(i);
            actualTitle = defaultString(rowiki.resolveRedirect(eachCandidateTitle), eachCandidateTitle);
        }
        if (null == actualTitle) {
            actualTitle = candidateRegionArticleNames.get(0);
        } else {
            currentText.append(rowiki.getPageText(actualTitle));
        }
        final ParameterReader ibParaReader = new ParameterReader(currentText.toString());
        ibParaReader.run();
        int templateLength = ibParaReader.getTemplateLength();
        if (!equalsIgnoreCase(region.getRomanianName(), "Crimeea")) {
            final String regionInfobox = generateRegionInfobox(region, ibParaReader);
            String regionIntro = generateRegionIntro(region, actualTitle);

            String currentRegionIntro = substringBefore(substring(currentText.toString(), templateLength), "==");
            currentRegionIntro = substringBefore(currentRegionIntro, "{{Ucraina}}");
            currentRegionIntro = trim(currentRegionIntro);
            if (currentRegionIntro.length() < regionIntro.length()) {
                regionIntro = currentRegionIntro;
            }

            currentText.replace(0, ibParaReader.getTemplateLength(), regionInfobox);
            templateLength = regionInfobox.length();
            int indexOfFirstSection = currentText.indexOf("==", ibParaReader.getTemplateLength());
            if (0 > indexOfFirstSection) {
                indexOfFirstSection = currentText.indexOf("{{Ucraina}}");
            }
            currentText.replace(templateLength, indexOfFirstSection, SEP + regionIntro + SEP);

            final String demografie = generateDemographySection(region);

            final int indexOfCurrentDemography = locateFirstOf(currentText, "==Populați", "== Populați", "== Demografie",
                "==Demografie");
            if (0 <= indexOfCurrentDemography) {
                currentText.replace(indexOfCurrentDemography, currentText.indexOf("==", indexOfCurrentDemography + 2) + 2,
                    demografie);
            } else {
                final int indexOfFutureDemographySection = locateFirstOf(currentText, "== Economie", "==Economie",
                    "{{Ucraina", "==Legături externe", "== Legături externe", "== Vezi și", "==Vezi și");
                if (0 <= indexOfFutureDemographySection) {
                    currentText.insert(indexOfFutureDemographySection, demografie);
                } else {
                    currentText.append(demografie);
                }
            }

            generateReferencesSection(currentText);
            executor.save(actualTitle, currentText.toString(),
                "Robot - creare / editare articol despre regiunea ucraineană " + obtainActualRomanianName(region));
        }
    }

    private void generateReferencesSection(final StringBuilder currentText) {
        final int indexOfCurrentReferencesSection = locateFirstOf(currentText, "== Note", "==Note", "== Referințe",
            "==Referințe");
        if (0 > indexOfCurrentReferencesSection) {
            final int indexOfFutureReferencesSection = locateFirstOf(currentText, "{{Ucraina", "==Legături externe",
                "== Legături externe", "{{Localități în ", "{{Comune în ");
            if (0 <= indexOfFutureReferencesSection) {
                currentText.insert(indexOfFutureReferencesSection, "\n== Note ==\n{{Reflist}}\n");
            } else {
                currentText.append("\n== Note ==\n{{Reflist}}\n");
            }
        }
    }

    private String generateRegionIntro(final Region region, final String articleName) throws IOException {
        final STGroup stgroup = new STGroupFile("templates/ua/ucraina.stg");
        final ST introTmpl = stgroup.getInstanceOf("introReg");
        introTmpl.add("nume_reg_ro", defaultIfBlank(region.getRomanianName(), region.getTransliteratedName()));
        final String ukArticleName = getUkrainianRegionName(articleName);
        final String transliteratedUkArticleName = replace(new UkrainianTransliterator(ukArticleName).transliterate(),
            "Oblast", "oblast");

        introTmpl.add("nume_reg_uk", ukArticleName + "|" + transliteratedUkArticleName);

        introTmpl.add("nume_capitala", null == region.getCapital() ? "" : obtainActualRomanianName(region.getCapital()));
        return introTmpl.render();
    }

    private String generateRegionInfobox(final Region region, final ParameterReader ibParaReader) {
        final StringBuilder sb = new StringBuilder("{{Infocaseta Regiune");
        sb.append("\n|nume = ");
        final String roRegName = defaultIfBlank(region.getRomanianName(), region.getTransliteratedName());
        sb.append(roRegName);
        if (null != region.getCapital()) {
            final Commune capital = region.getCapital();
            sb.append("\n|capitala = [[");
            sb.append(defaultIfBlank(capital.getRomanianName(), capital.getTransliteratedName()));
            sb.append("]]");
            sb.append("\n|nume=").append(roRegName);
            sb.append("\n|numegen=").append(region.getGenitive()).append(' ').append(roRegName);
            sb.append("\n|tara={{UKR}}");
            sb.append("\n|stemă={{#property:P94}}");
            sb.append("\n|steag_imagine={{#property:P41}}");
            final List<String> ourParamsList = Arrays.asList("capitala", "tara", "nume", "numegen", "steag_imagine",
                "stemă", "simbol", "simbol1", "simbol2");
            for (final String paramName : ibParaReader.getParams().keySet()) {
                if (!ourParamsList.contains(trim(paramName))) {
                    UAUtils.copyParameterFromTemplate(ibParaReader, sb, paramName);
                }
            }
        }
        sb.append("\n}}\n");
        return sb.toString();
    }

    private String getArticleName(final LanguageStructurable ls) {
        LazyInitializer<String> lazyInitializer = roArticleNames.get(ls);
        if (null == lazyInitializer) {
            if (ls instanceof Region) {
                lazyInitializer = new RegionNameInitializer((Region) ls, rowiki);
            } else if (ls instanceof Raion) {
                lazyInitializer = new RaionNameInitializer((Raion) ls, rowiki, hib);
            } else if (ls instanceof Commune) {
                lazyInitializer = new CommuneNameInitializer((Commune) ls, rowiki, hib);
            } else if (ls instanceof Settlement) {
                lazyInitializer = new SettlementNameInitializer((Settlement) ls, rowiki, hib);
            }
            roArticleNames.put(ls, lazyInitializer);
        }
        if (null == lazyInitializer) {
            return null;
        }
        try {
            return lazyInitializer.get();
        } catch (final ConcurrentException e) {
            e.printStackTrace();
            return null;
        }
    }

    private int locateFirstOf(final CharSequence haystack, final CharSequence... needles) {
        final List<Integer> locations = new ArrayList<Integer>();
        for (final CharSequence eachNeedle : needles) {
            final int locationOfNeedleInHaystack = indexOf(haystack, eachNeedle);
            if (0 <= locationOfNeedleInHaystack) {
                locations.add(locationOfNeedleInHaystack);
            }
        }
        if (0 == locations.size()) {
            return -1;
        }
        Collections.sort(locations);
        return locations.get(0);
    }

    private String generateDemographySection(final LanguageStructurable place) {
        if (0 == place.getLanguageStructure().size()) {
            return "";
        }
        final StringBuilder sb = new StringBuilder("== Demografie ==");
        sb.append(SEP);
        sb.append("<!-- Start secțiune generată de Andrebot -->");
        final STGroup templateGroup = new STGroupFile("templates/ua/ucraina.stg");
        final ST piechart = templateGroup.getInstanceOf("piechart");
        piechart.add("nume", defaultIfBlank(place.getRomanianName(), place.getTransliteratedName()));
        piechart.add("tip_genitiv", place.getGenitive());

        final DefaultPieDataset datasetLang = new DefaultPieDataset();
        computeEthnicityDataset(place.getLanguageStructure(), datasetLang);
        renderPiechart(sb, piechart, datasetLang);

        String templateName = "NoMaj";
        Language majLanguage = null;
        for (final Entry<Language, Double> eachEntry : place.getLanguageStructure().entrySet()) {
            final Double val = eachEntry.getValue();
            if (null == val) {
                continue;
            }
            if (val.doubleValue() >= 100.0) {
                templateName = "Total";
                majLanguage = eachEntry.getKey();
                break;
            }
            if (val.doubleValue() >= 50.0) {
                majLanguage = eachEntry.getKey();
                templateName = "Maj";
                break;
            }
        }
        final ST demoText = templateGroup.getInstanceOf("demography" + templateName);
        final NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
        nf.setMaximumFractionDigits(2);
        demoText.add("nume_unitate_genitiv", place.getGenitive() + " " + obtainActualRomanianName(place));
        if (null != majLanguage) {
            demoText.add("nume_limba_majoritara", linkifyLanguage(majLanguage));
        }
        if (StringUtils.equals("Maj", templateName)) {
            demoText.add("procent_limba_majoritara",
                "{{formatnum:" + nf.format(place.getLanguageStructure().get(majLanguage)) + "}}");
        }
        if (!StringUtils.equals("Total", templateName)) {
            final List<Language> otherLanguages = new ArrayList<Language>();
            for (final Language eachLang : place.getLanguageStructure().keySet()) {
                if (!eachLang.equals(majLanguage) && place.getLanguageStructure().get(eachLang) > 1.0) {
                    otherLanguages.add(eachLang);
                }
            }
            Collections.sort(otherLanguages, new Comparator<Language>() {

                public int compare(final Language arg0, final Language arg1) {
                    final double pop0 = defaultIfNull(place.getLanguageStructure().get(arg0), 0.0);
                    final double pop1 = defaultIfNull(place.getLanguageStructure().get(arg1), 0.0);
                    return (int) Math.signum(pop1 - pop0);
                }
            });
            final List<String> languageEnumerationList = new ArrayList<String>();
            for (final Language eachOtherLang : otherLanguages) {
                Double speakers;
                if (null != (speakers = place.getLanguageStructure().get(eachOtherLang))) {
                    final StringBuilder langBuilder = new StringBuilder(linkifyLanguage(eachOtherLang));
                    langBuilder.append(" (");
                    langBuilder.append("{{formatnum:");
                    langBuilder.append(nf.format(speakers.doubleValue()));
                    langBuilder.append("}}%)");
                    languageEnumerationList.add(langBuilder.toString());
                }
            }
            if (0 == languageEnumerationList.size()) {
                demoText.add("enum_alte_limbi", "alte limbi");
            } else if (1 == languageEnumerationList.size()) {
                demoText.add("enum_alte_limbi", languageEnumerationList.get(0));
            } else {
                final String[] languageEnumerationArray = languageEnumerationList.toArray(new String[languageEnumerationList
                    .size()]);
                demoText.add("enum_alte_limbi",
                    join(ArrayUtils.subarray(languageEnumerationArray, 0, languageEnumerationArray.length - 1), ", ")
                        + " și " + languageEnumerationArray[languageEnumerationArray.length - 1]);
            }
        }
        final StringBuilder refBuilder = new StringBuilder("<ref name=\"populatie_ucraina_2001\">{{Citat web|url=");
        final String regionName = obtainActualRomanianName(place.computeRegion());
        refBuilder.append(urls.get(regionName));
        refBuilder.append("|publisher=Institutul Național de Statistică al Ucrainei");
        refBuilder.append("|title=Rezultatele recensământului din 2001 cu structura lingvistică a regiunii ");
        refBuilder.append(regionName);
        refBuilder.append(" pe localități");
        refBuilder.append("|accessdate=2014-08-25}}");
        refBuilder.append("</ref>");
        demoText.add("ref", refBuilder.toString());
        sb.append(demoText.render());
        sb.append(SEP);

        return sb.toString();
    }

    private String obtainActualRomanianName(final LanguageStructurable place) {
        return defaultIfBlank(place.getRomanianName(), place.getTransliteratedName());
    }

    private String linkifyLanguage(final Language lang) {
        final StringBuilder linkBuilder = new StringBuilder("[[Limba ");
        final String langName = lowerCase(lang.getName());
        linkBuilder.append(langName);
        linkBuilder.append('|');
        linkBuilder.append(langName).append("]]");

        return linkBuilder.toString();
    }

    private void computeEthnicityDataset(final Map<Language, Double> languageStructure, final DefaultPieDataset dataset) {
        final Set<Language> ethnicitiesSet = nationColorMap.keySet();
        final List<Language> ethnicitiesList = new ArrayList<Language>(ethnicitiesSet);
        Collections.sort(ethnicitiesList, new Comparator<Language>() {

            public int compare(final Language arg0, final Language arg1) {
                final double natpop0 = defaultIfNull(languageStructure.get(arg0), 0.0);
                final double natpop1 = defaultIfNull(languageStructure.get(arg1), 0.0);
                return (int) Math.signum(natpop1 - natpop0);
            }

        });
        double others = 0.0;
        for (final Language nat : ethnicitiesList) {
            final Double natpop = languageStructure.get(nat);
            if (null != natpop && 1.0 < natpop.doubleValue()) {
                dataset.setValue(nat.getName(), natpop.doubleValue());
            } else if (null != natpop) {
                others += natpop;
            }
        }
        if (0.0 < others) {
            dataset.setValue("Alte limbi", others);
        }
    }

    private void renderPiechart(final StringBuilder demographics, final ST piechart, final DefaultPieDataset dataset) {
        final StringBuilder pieChartLangProps = new StringBuilder();
        int i = 1;
        demographics.append("<div style=\"float:left\">");

        final NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
        nf.setMaximumFractionDigits(2);

        for (final Object k : dataset.getKeys()) {
            pieChartLangProps.append("\n|label");
            pieChartLangProps.append(i);
            pieChartLangProps.append('=');
            pieChartLangProps.append(k.toString());
            pieChartLangProps.append("|value");
            pieChartLangProps.append(i);
            pieChartLangProps.append('=');
            pieChartLangProps.append(nf.format(dataset.getValue(k.toString()).doubleValue()));
            pieChartLangProps.append("|color");
            pieChartLangProps.append(i);
            pieChartLangProps.append('=');
            final Color color = nationColorMap.get(nationNameMap.get(k.toString()));
            if (null == color) {
                throw new RuntimeException("Unknown color for nationality " + k);
            }
            pieChartLangProps.append(Utilities.colorToHtml(color));
            i++;
        }
        piechart.add("props", pieChartLangProps.toString());
        demographics.append(piechart.render());
        demographics.append("</div>");
        demographics.append(SEP);
    }

    private void assignColorToLanguage(final String languageName, final Color color) throws HibernateException {
        final Language nat = hib.getLanguageByName(languageName);
        if (null != nat) {
            nationColorMap.put(nat, color);
            nationNameMap.put(nat.getName(), nat);
        }
    }

    private void close() {
        Session ses;
        if (null != (ses = hib.getSession())) {
            final org.hibernate.Transaction tx = ses.getTransaction();
            if (null != tx) {
                tx.rollback();
            }
        }
        if (null != rowiki) {
            rowiki.logout();
        }
        if (null != ukwiki) {
            ukwiki.logout();
        }
        if (null != dwiki) {
            dwiki.logout();
        }

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

    public List<String> getPossibleUkrainianArticleNames(final Settlement s) {
        final List<String> ret = new ArrayList<String>();
        final Raion r = s.getCommune().getRaion();
        final Region reg = s.computeRegion();
        final String regName = getUkrainianRegionName(reg);
        if (null != r && !r.isMiskrada()) {
            ret.add(s.getName() + " (" + r.getOriginalName() + " район, " + regName + ")");
        }
        if (null != r && !r.isMiskrada()) {
            ret.add(s.getName() + " (" + r.getOriginalName() + " район)");
        }
        ret.add(s.getName() + " (село)");
        ret.add(s.getName());
        return ret;
    }

    public List<String> getPossibleUkrainianArticleNames(final Commune c) {
        final List<String> ret = new ArrayList<String>();
        final Raion r = c.getRaion();
        final Region reg = c.computeRegion();
        final String regName = getUkrainianRegionName(reg);
        c.getOriginalName().replace("сілрада", "сільська рада");
        if (null != r && !r.isMiskrada()) {
            ret.add(c.getName() + " (" + r.getOriginalName() + " район, " + regName + ")");
            ret.add(c.getOriginalName().replace("сілрада", "сільська рада") + " (" + r.getOriginalName() + " район, "
                + regName + ")");
        }
        if (null != r && !r.isMiskrada()) {
            ret.add(c.getName() + " (" + r.getOriginalName() + " район)");
            ret.add(c.getOriginalName().replace("сілрада", "сільська рада") + " (" + r.getOriginalName() + " район, "
                + regName + ")");
        }
        if (1 == c.getTown()) {
            ret.add(c.getName() + " (смт)");
        } else if (2 == c.getTown()) {
            ret.add(c.getName() + " (місто)");
        }
        ret.add(c.getOriginalName().replace("сілрада", "сільська рада"));
        ret.add(c.getName());
        return ret;
    }

    public String getUkrainianRegionName(final Region region) {

        final String articleName = getArticleName(region);

        boolean operationSuccessful = false;
        String ukTitle = null;
        do {
            try {
                ukTitle = dwiki.getTitleInLanguage("rowiki", articleName, "uk");
                operationSuccessful = true;
            } catch (final IOException ex) {
                ex.printStackTrace();
            }
        } while (!operationSuccessful);
        region.setOriginalName(ukTitle);
        // hib.saveRegion(region);
        // hib.getSession().beginTransaction();

        return ukTitle;
    }

    public String getUkrainianRegionName(final String articleName) throws IOException {
        final String ukTitle = dwiki.getTitleInLanguage("rowiki", articleName, "uk");

        return ukTitle;
    }
}
