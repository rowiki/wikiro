package org.wikipedia.ro.populationdb.ua;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.StringUtils.appendIfMissing;
import static org.apache.commons.lang3.StringUtils.contains;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.indexOf;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.replace;
import static org.apache.commons.lang3.StringUtils.replaceEach;
import static org.apache.commons.lang3.StringUtils.substring;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.apache.commons.lang3.StringUtils.trim;

import java.awt.Color;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.Collator;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.security.auth.login.FailedLoginException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.property.Getter;
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
import org.wikipedia.ro.populationdb.util.UkrainianTransliterator;
import org.wikipedia.ro.populationdb.util.Utilities;
import org.wikipedia.ro.populationdb.util.WikiEditExecutor;

public class UAWikiGenerator {

    private static final String SEP = "\n";
    private static final Map<String, Set<Commune>> communeDisambigLists = new HashMap<String, Set<Commune>>();

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
    public static final Collator ROMANIAN_COLLATOR = Collator.getInstance(new Locale("ro"));

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
        executor = new WikiEditExecutor(rowiki, dwiki);
        // executor = new SysoutExecutor();

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
        Session ses = hib.getSession();
        ses.beginTransaction();
        List<Region> regions = hib.getAllRegions();
        Set<String> regionsFinished = new HashSet<String>();
        regionsFinished.add("Vinnîțea");
        regionsFinished.add("Volîn");
        regionsFinished.add("Dnipropetrovsk");
        String regionWithCitiesFinished = "";
        regions = Arrays.asList(hib.getRegionByTransliteratedName("Jîtomîr"));

        for (final Region eachReg : regions) {
            if (regionsFinished.contains(eachReg.getTransliteratedName())) {
                continue;
            }
            Set<String> raionsFinished = new HashSet<String>();
            raionsFinished.addAll(Arrays.asList("Andrușivka", "Baranivka", "Berdîciv", "Brusîliv", "Volodarsk-Volînskîi",
                "Dzerjînsk", "Iemilciîne", "Jîtomîr", "Korosten", "Korostîșiv", "Luhînî", "Liubar", "Malîn", "Narodîci",
                "Novohrad-Volînskîi", "Ovruci", "Olevsk", "Popilnea", "Dovbîș", "Radomîșl"));
            for (final Raion raion : eachReg.getRaioane()) {
                if (raionsFinished.contains(raion.getTransliteratedName())) {
                    // generateRaionCategories(raion);
                    continue;
                }
                if (StringUtils.equals(eachReg.getTransliteratedName(), regionWithCitiesFinished) && raion.isMiskrada()) {
                    continue;
                }
                for (final Commune com : raion.getCommunes()) {
                    List<Settlement> settlementsOtherThanMain = getSettlementsOtherThanMain(com);

                    if (0 < settlementsOtherThanMain.size()) {
                        for (final Settlement s : settlementsOtherThanMain) {
                            generateVillageText(s);
                        }
                    }
                    if ((1 > com.getSettlements().size() || null == com.getCapital()) && 0 == com.getTown()) {
                        Settlement s = new Settlement();
                        s.setName(com.getName());
                        s.setRomanianName(com.getRomanianName());
                        s.setTransliteratedName(com.getTransliteratedName());
                        Map<Language, Double> villageEntries = new HashMap<Language, Double>();
                        for (Entry<Language, Double> langEntries : com.getLanguageStructure().entrySet()) {
                            villageEntries.put(langEntries.getKey(), langEntries.getValue());
                        }
                        s.setLanguageStructure(villageEntries);
                        com.setCapital(s);
                        s.setCommune(com);
                        Set<Settlement> comVillages = new HashSet<Settlement>();
                        comVillages.addAll(com.getSettlements());
                        com.setSettlements(comVillages);
                        ses.saveOrUpdate(s);
                        ses.saveOrUpdate(com);
                    }
                    generateCommuneText(com);
                    generateCommuneNavBox(com);
                }
                if (!raion.isMiskrada()) {
                    generateRaionText(raion);
                }
                generateRaionNavBox(raion);
                generateRaionCategories(raion);
            }

            for (final Commune com : eachReg.getCities()) {
                generateCommuneText(com);
                if (1 < com.getSettlements().size()) {
                    for (final Settlement s : com.getSettlements()) {
                        if (!StringUtils.equals(s.getTransliteratedName(), com.getTransliteratedName())) {
                            generateVillageText(s);
                        }
                    }
                }
                generateCommuneNavBox(com);
                generateCityCategories(com);
            }
            generateRegionText(eachReg);
            generateRegionNavBox(eachReg);
            generateRegionCategories(eachReg);

        }
    }

    private List<Settlement> getSettlementsOtherThanMain(final Commune com) {
        List<Settlement> settlementsOtherThanMain = new ArrayList<Settlement>();
        CollectionUtils.addAll(settlementsOtherThanMain, com.getSettlements());
        CollectionUtils.filter(settlementsOtherThanMain, new Predicate<Settlement>() {

            public boolean evaluate(Settlement arg0) {
                return !StringUtils.equals(arg0.getTransliteratedName(), com.getTransliteratedName());
            }
        });
        return settlementsOtherThanMain;
    }

    private void generateRegionCategories(Region region) throws Exception {
        String regiuneRoName = obtainActualRomanianName(region.computeRegion());
        String regiuneCategoryKey = generateCategoryKey(regiuneRoName);
        Map<String, String> categories = new HashMap<String, String>();

        StringBuilder mainRegionCategory = new StringBuilder("[[Categorie:Regiuni ale Ucrainei");
        mainRegionCategory.append(regiuneRoName).append("|").append(regiuneCategoryKey).append("]]");
        categories.put("Regiunea " + regiuneRoName, mainRegionCategory.toString());

        StringBuilder mainCommunesRegionCategory = new StringBuilder("[[Categorie:Comunele Ucrainei după regiune");
        mainCommunesRegionCategory.append('|').append(regiuneRoName);
        mainCommunesRegionCategory.append("]]");
        mainCommunesRegionCategory.append("\n[[Categorie:Regiunea " + regiuneRoName);
        mainCommunesRegionCategory.append("]]");
        categories.put("Comune în regiunea " + regiuneRoName, mainCommunesRegionCategory.toString());

        StringBuilder byRaionCommunesRegionCategory = new StringBuilder();
        byRaionCommunesRegionCategory.append("[[Categorie:Comune în regiunea " + regiuneRoName);
        byRaionCommunesRegionCategory.append("| ]]");
        categories.put("Comune în regiunea " + regiuneRoName + " după raion", byRaionCommunesRegionCategory.toString());

        StringBuilder smtsRegionCategory = new StringBuilder("[[Categorie:Așezări de tip urban în Ucraina");
        smtsRegionCategory.append('|').append(regiuneCategoryKey);
        smtsRegionCategory.append("]]");
        smtsRegionCategory.append("\n[[Categorie:Așezări de tip urban în Ucraina după regiune");
        smtsRegionCategory.append('|').append(regiuneCategoryKey);
        smtsRegionCategory.append("]]");

        smtsRegionCategory.append("\n[[Categorie:Regiunea " + regiuneRoName);
        smtsRegionCategory.append("]]");
        categories.put("Așezări de tip urban în regiunea " + regiuneRoName, smtsRegionCategory.toString());

        StringBuilder byRaionSmtsRegionCategory = new StringBuilder();
        byRaionSmtsRegionCategory.append("[[Categorie:Așezări de tip urban în regiunea " + regiuneRoName);
        byRaionSmtsRegionCategory.append("| ]]");
        categories.put("Așezări de tip urban în regiunea " + regiuneRoName + " după raion",
            byRaionSmtsRegionCategory.toString());

        StringBuilder mainRaionCityRegionCategory = new StringBuilder();
        mainRaionCityRegionCategory.append("[[Categorie:Orașe raionale în Ucraina după regiune");
        mainRaionCityRegionCategory.append('|').append(regiuneCategoryKey);
        mainRaionCityRegionCategory.append("]]");
        mainRaionCityRegionCategory.append("\n[[Categorie:Regiunea " + regiuneRoName);
        mainRaionCityRegionCategory.append("]]");
        categories.put("Orașe raionale în regiunea " + regiuneRoName, mainRaionCityRegionCategory.toString());

        StringBuilder byRaionCityRegionCategory = new StringBuilder();
        byRaionCityRegionCategory.append("[[Categorie:Orașe raionale în regiunea " + regiuneRoName);
        byRaionCityRegionCategory.append("| ]]");
        categories.put("Orașe raionale în regiunea " + regiuneRoName + " după raion", byRaionCityRegionCategory.toString());

        StringBuilder villageRegionCategory = new StringBuilder();
        villageRegionCategory.append("[[Categorie:Sate în Ucraina după regiune");
        villageRegionCategory.append('|').append(regiuneCategoryKey);
        villageRegionCategory.append("]]");
        villageRegionCategory.append(regiuneRoName).append(" după raion").append("]]");
        villageRegionCategory.append("\n[[Categorie:Regiunea " + regiuneRoName);
        villageRegionCategory.append("| ]]");
        categories.put("Sate în regiunea " + regiuneRoName, villageRegionCategory.toString());

        StringBuilder byRaionVillageRegionCategory = new StringBuilder();
        byRaionVillageRegionCategory.append("[[Categorie:Sate în regiunea " + regiuneRoName);
        byRaionVillageRegionCategory.append("| ]]");
        categories.put("Sate în regiunea " + regiuneRoName + " după raion", byRaionVillageRegionCategory.toString());

        StringBuilder raionsCategory = new StringBuilder();
        raionsCategory.append("[[Categorie:Raioanele Ucrainei după regiune|");
        raionsCategory.append(regiuneCategoryKey);
        raionsCategory.append("]]");
        raionsCategory.append("\n[[Categorie:Regiunea ").append(regiuneRoName);
        raionsCategory.append("| ]]");
        categories.put("Raioane în regiunea " + regiuneRoName, raionsCategory.toString());

        for (Entry<String, String> eachCatToCreate : categories.entrySet()) {
            executor.save("Categorie:" + eachCatToCreate.getKey(), eachCatToCreate.getValue(),
                "Robot - creare categorie pentru regiunea " + regiuneRoName);
        }
    }

    private String generateCategoryKey(String bruteKey) {
        String regiuneCategoryKey = replaceEach(bruteKey, new String[] { "â" }, new String[] { "ăâ" });
        regiuneCategoryKey = replaceEach(regiuneCategoryKey, new String[] { "ă" }, new String[] { "aă" });
        regiuneCategoryKey = replaceEach(regiuneCategoryKey, new String[] { "î" }, new String[] { "iî" });
        regiuneCategoryKey = replaceEach(regiuneCategoryKey, new String[] { "ș" }, new String[] { "sș" });
        regiuneCategoryKey = replaceEach(regiuneCategoryKey, new String[] { "ț" }, new String[] { "tț" });
        return regiuneCategoryKey;
    }

    private void generateRaionCategories(final Raion raion) throws Exception {
        if (raion.isMiskrada()) {
            return;
        }
        String raionRoName = obtainActualRomanianName(raion);
        String regiuneRoName = obtainActualRomanianName(raion.computeRegion());
        String raionCategoryKey = generateCategoryKey(raionRoName);
        String regiuneCategoryKey = generateCategoryKey(regiuneRoName);
        Map<String, String> categories = new HashMap<String, String>();

        StringBuilder mainRaionCategory = new StringBuilder();
        mainRaionCategory.append("[[Categorie:Raioane în regiunea ");
        mainRaionCategory.append(regiuneRoName);
        mainRaionCategory.append('|').append(raionCategoryKey);
        mainRaionCategory.append("]]");
        mainRaionCategory.append("\n[[Categorie:Raioanele Ucrainei");
        mainRaionCategory.append('|');
        mainRaionCategory.append(raionCategoryKey).append(", ").append(regiuneCategoryKey);
        mainRaionCategory.append("]]");
        categories.put("Raionul " + raionRoName + ", " + regiuneRoName, mainRaionCategory.toString());

        StringBuilder communesRaionCategory = new StringBuilder();
        communesRaionCategory.append("[[Categorie:Comunele Ucrainei după raion");
        communesRaionCategory.append('|').append(raionCategoryKey).append(", ").append(regiuneCategoryKey);
        communesRaionCategory.append("]]");
        communesRaionCategory.append("\n[[Categorie:Comune în regiunea ").append(regiuneRoName).append(" după raion");
        communesRaionCategory.append('|').append(raionCategoryKey);
        communesRaionCategory.append("]]");
        communesRaionCategory.append("\n[[Categorie:Raionul " + raionRoName + ", " + regiuneRoName);
        communesRaionCategory.append("| ]]");
        categories.put("Comune în raionul " + raionRoName + ", " + regiuneRoName, communesRaionCategory.toString());

        StringBuilder smtsRaionCategory = new StringBuilder("[[Categorie:Așezări de tip urban în regiunea ");
        smtsRaionCategory.append(regiuneRoName).append(" după raion");
        smtsRaionCategory.append('|').append(raionCategoryKey);
        smtsRaionCategory.append("]]");
        smtsRaionCategory.append("\n[[Categorie:Așezări de tip urban în Ucraina după raion");
        smtsRaionCategory.append('|').append(raionCategoryKey).append(", ").append(regiuneCategoryKey);
        smtsRaionCategory.append("]]");
        smtsRaionCategory.append("\n[[Categorie:Raionul " + raionRoName + ", " + regiuneRoName);
        smtsRaionCategory.append('|').append(" ");
        smtsRaionCategory.append("]]");
        categories
            .put("Așezări de tip urban în raionul " + raionRoName + ", " + regiuneRoName, smtsRaionCategory.toString());

        StringBuilder cityRaionCategory = new StringBuilder("[[Categorie:Orașe raionale în regiunea ");
        cityRaionCategory.append(regiuneRoName).append(" după raion");
        cityRaionCategory.append('|').append(raionCategoryKey);
        cityRaionCategory.append("]]");
        cityRaionCategory.append("\n[[Categorie:Orașe raionale în Ucraina după raion");
        cityRaionCategory.append('|').append(raionCategoryKey).append(", ").append(regiuneCategoryKey);
        cityRaionCategory.append("]]");
        cityRaionCategory.append("\n[[Categorie:Raionul " + raionRoName + ", " + regiuneRoName);
        cityRaionCategory.append("| ]]");
        categories.put("Orașe raionale în raionul " + raionRoName + ", " + regiuneRoName, cityRaionCategory.toString());

        StringBuilder villageRaionCategory = new StringBuilder("[[Categorie:Sate în regiunea ");
        villageRaionCategory.append(regiuneRoName).append(" după raion");
        villageRaionCategory.append('|').append(raionCategoryKey);
        villageRaionCategory.append("]]");
        villageRaionCategory.append("\n[[Categorie:Sate în Ucraina după raion");
        villageRaionCategory.append('|').append(raionCategoryKey).append(", ").append(regiuneCategoryKey);
        villageRaionCategory.append("]]");

        villageRaionCategory.append("\n[[Categorie:Raionul " + raionRoName + ", " + regiuneRoName);
        villageRaionCategory.append("| ]]");
        categories.put("Sate în raionul " + raionRoName + ", " + regiuneRoName, villageRaionCategory.toString());

        for (Entry<String, String> eachCatToCreate : categories.entrySet()) {
            if (!rowiki.exists(new String[] { "Categorie:" + eachCatToCreate.getKey() })[0]) {
                executor.save("Categorie:" + eachCatToCreate.getKey(), eachCatToCreate.getValue(),
                    "Robot - creare categorie pentru raionul " + raionRoName);
            }
        }
    }

    private void generateCityCategories(final Commune com) throws Exception {
        if (2 > com.getTown()) {
            return;
        }
        String cityRoName = obtainActualRomanianName(com);
        String regiuneRoName = obtainActualRomanianName(com.computeRegion());
        String cityCategoryKey = generateCategoryKey(cityRoName);
        String regiuneCategoryKey = generateCategoryKey(regiuneRoName);

        int count = hib.countCommunesByRomanianOrTransliteratedName(cityRoName);
        String disambiggedName = cityRoName + (1 < count ? ", " + regiuneRoName : "");
        Map<String, String> categories = new HashMap<String, String>();

        StringBuilder mainRaionCategory = new StringBuilder();
        mainRaionCategory.append("[[Categorie:Orașe regionale în regiunea ");
        mainRaionCategory.append(regiuneRoName);
        mainRaionCategory.append('|').append(cityCategoryKey);
        mainRaionCategory.append("]]");
        mainRaionCategory.append("\n[[Categorie:Orașe regionale în Ucraina");
        mainRaionCategory.append('|');
        mainRaionCategory.append(cityCategoryKey).append(", ").append(regiuneCategoryKey);
        mainRaionCategory.append("]]");
        categories.put(disambiggedName, mainRaionCategory.toString());

        StringBuilder smtsRaionCategory = new StringBuilder("[[Categorie:Așezări de tip urban în regiunea ");
        smtsRaionCategory.append(regiuneRoName).append(" după raion");
        smtsRaionCategory.append('|').append(cityCategoryKey);
        smtsRaionCategory.append("]]");
        smtsRaionCategory.append("\n[[Categorie:Așezări de tip urban în Ucraina după raion");
        smtsRaionCategory.append('|').append(cityCategoryKey).append(", ").append(regiuneCategoryKey);
        smtsRaionCategory.append("]]");
        smtsRaionCategory.append("\n[[Categorie:" + disambiggedName);
        categories.put("Așezări de tip urban în orașul regional " + disambiggedName, smtsRaionCategory.toString());

        StringBuilder villageRaionCategory = new StringBuilder("[[Categorie:Sate în regiunea ");
        villageRaionCategory.append(regiuneRoName).append(" după raion");
        villageRaionCategory.append('|').append(cityCategoryKey);
        villageRaionCategory.append("]]");
        villageRaionCategory.append("\n[[Categorie:Sate în Ucraina după raion");
        villageRaionCategory.append('|').append(cityCategoryKey).append(", ").append(regiuneCategoryKey);
        villageRaionCategory.append("]]");
        villageRaionCategory.append("\n[[Categorie:" + disambiggedName);
        categories.put("Sate în orașul regional " + disambiggedName, villageRaionCategory.toString());

        for (Entry<String, String> eachCatToCreate : categories.entrySet()) {
            if (!rowiki.exists(new String[] { eachCatToCreate.getKey() })[0]) {
                executor.save("Categorie:" + eachCatToCreate.getKey(), eachCatToCreate.getValue(),
                    "Robot - creare categorie pentru raionul " + cityRoName);
            }
        }
    }

    private void generateRaionNavBox(final Raion raion) throws Exception {
        if (raion.getCommunes().size() < 2) {
            return;
        }
        final String raionName = obtainActualRomanianName(raion);
        final String articleName = getArticleName(raion);
        int section = 0;

        final StringBuilder navBox = new StringBuilder("{{Casetă de navigare simplă\n|stare = {{{stare|autopliabilă}}}\n|");
        navBox.append("nume=" + articleName);
        navBox.append("\n|titlu=Unități administrative componente ale ");
        if (raion.isMiskrada()) {
            navBox.append("orașului regional ").append("[[").append(articleName);
            if (!StringUtils.equals(raionName, articleName)) {
                navBox.append('|').append(raionName);
            }
            navBox.append("]]");
        } else {
            navBox.append("[[").append(articleName).append('|').append("raionului ").append(raionName).append("]]");
        }

        if (!raion.isMiskrada()) {
            section++;
            navBox.append("\n|grup").append(section).append("=Reședință");
            navBox.append("\n|listă").append(section).append("=[[");
            final String capitalCommuneName = obtainActualRomanianName(raion.getCapital());
            final String capitalCommuneArticle = getArticleName(raion.getCapital());
            navBox.append(capitalCommuneArticle);
            if (!StringUtils.equals(capitalCommuneName, capitalCommuneArticle)) {
                navBox.append('|').append(capitalCommuneName);
            }
            navBox.append("]]");
            if (null == raion.getCapital().getRaion()) {
                navBox.append(" <small>(nu aparține raionului)</small>");
            }
        }
        List<Commune> subentities = new ArrayList<Commune>(raion.getCommunes());
        subentities.remove(raion.getCapital());
        final List<Commune> cities = new ArrayList<Commune>();
        final List<Commune> smts = new ArrayList<Commune>();
        final List<Commune> communes = new ArrayList<Commune>();
        Map<Integer, List<Commune>> codeToListMap = new HashMap<Integer, List<Commune>>() {
            {
                put(0, communes);
                put(1, smts);
                put(2, cities);
            }
        };

        for (Commune eachsubentity : subentities) {
            codeToListMap.get(eachsubentity.getTown()).add(eachsubentity);
        }
        for (List<Commune> eachList : codeToListMap.values()) {
            Collections.sort(eachList, new Comparator<Commune>() {

                public int compare(Commune arg0, Commune arg1) {

                    return ROMANIAN_COLLATOR.compare(obtainActualRomanianName(arg0), obtainActualRomanianName(arg1));
                }
            });
        }
        List<Entry<Integer, List<Commune>>> entryList = new ArrayList<Map.Entry<Integer, List<Commune>>>(
            codeToListMap.entrySet());
        Collections.sort(entryList, new Comparator<Entry<Integer, List<Commune>>>() {

            public int compare(Entry<Integer, List<Commune>> o1, Entry<Integer, List<Commune>> o2) {
                return o2.getKey().intValue() - o1.getKey().intValue();
            }
        });
        for (Entry<Integer, List<Commune>> eachListEntry : entryList) {
            if (0 == eachListEntry.getValue().size()) {
                continue;
            }
            section++;
            navBox.append("\n|grup").append(section);
            switch (eachListEntry.getKey()) {
            case 0:
                navBox.append("=Comune");
                break;
            case 1:
                navBox.append("=Așezări de tip urban");
                break;
            case 2:
                navBox.append("=Orașe");
                break;
            }
            navBox.append("\n|listă").append(section).append("=<div>");
            final List<Commune> parts = eachListEntry.getValue();
            for (final Commune eachPart : parts) {
                final String partArticleName = getArticleName(eachPart);
                final String partName = obtainActualRomanianName(eachPart);
                navBox.append("\n[[").append(partArticleName);
                if (!StringUtils.equals(partArticleName, partName)) {
                    navBox.append('|').append(partName);
                }
                navBox.append("]]{{~}}");
            }
            navBox.delete(navBox.length() - "{{~}}".length(), navBox.length());
            navBox.append("\n</div>");
        }
        navBox.append("}}<noinclude>");
        navBox.append("[[Categorie:Formate de navigare raioane din Ucraina|");
        navBox.append(raionName);
        navBox.append("]]");
        navBox.append("</noinclude>");

        executor.save("Format:" + articleName, navBox.toString(),
            "Robot: creare/regenerare casetă de navigare pentru comuna ucraineană " + articleName);

    }

    private void generateRaionText(final Raion raion) throws Exception {
        if (raion.isMiskrada()) {
            return;
        }
        final String raionRoName = defaultIfBlank(raion.getRomanianName(), raion.getTransliteratedName());
        System.out.println("------ generating text for raion " + raion.getName() + "/" + raionRoName);
        final int countRaionsWithThisName = hib.countRaionsByRomanianOrTransliteratedName(raionRoName);

        String actualTitle = getArticleName(raion);
        final StringBuilder currentText = new StringBuilder();
        final boolean[] titleExistance = rowiki.exists(new String[] { actualTitle });

        if (titleExistance[0]) {
            currentText.append(rowiki.getPageText(actualTitle));
        }
        final ParameterReader ibParaReader = new ParameterReader(currentText.toString());
        ibParaReader.run();
        final String raionIBText = generateRaionInfobox(raion, ibParaReader);

        String communeIntro = generateRaionIntro(raion, actualTitle);

        String currentRaionIntro = substringBefore(substring(currentText.toString(), ibParaReader.getTemplateLength()), "==");
        currentRaionIntro = substringBefore(currentRaionIntro, "{{Raioane ");
        currentRaionIntro = substringBefore(currentRaionIntro, "{{Regiunea ");
        currentRaionIntro = substringBefore(currentRaionIntro, "{{Localități în ");
        currentRaionIntro = substringBefore(currentRaionIntro, "{{Diviziuni administrative ");
        currentRaionIntro = trim(currentRaionIntro);
        if (currentRaionIntro.length() > communeIntro.length()) {
            communeIntro = currentRaionIntro;
        }
        currentText.replace(0, ibParaReader.getTemplateLength(), raionIBText);

        int indexOfFirstSection = currentText.indexOf("==", raionIBText.length());
        if (0 > indexOfFirstSection) {
            indexOfFirstSection = currentText.indexOf("{{Ucraina}}");
        }
        if (0 <= indexOfFirstSection) {
            currentText.replace(raionIBText.length(), indexOfFirstSection, SEP + communeIntro + SEP);
        } else {
            currentText.append(SEP + communeIntro + SEP);
        }

        final String demografie = generateDemographySection(raion);

        final int indexOfCurrentDemography = locateFirstOf(currentText, "==Populați", "== Populați", "== Demografie",
            "==Demografie");
        if (0 <= indexOfCurrentDemography) {
            if (!StringUtils.contains(currentText, "<!-- Start secțiune generată de Andrebot -->")) {
                currentText.replace(indexOfCurrentDemography, currentText.indexOf("==", indexOfCurrentDemography + 2) + 2,
                    demografie);
            }
        } else {
            final int indexOfFutureDemographySection = locateFirstOf(currentText, "== Economie", "==Economie", "{{Ucraina",
                "==Legături externe", "== Legături externe", "== Vezi și", "==Vezi și", "{{Localități în ", "{{Comune în ");
            if (0 <= indexOfFutureDemographySection) {
                currentText.insert(indexOfFutureDemographySection, demografie + "\n{{clearleft}}\n");
            } else {
                currentText.append(demografie).append("\n{{clearleft}}\n");
            }
        }

        generateReferencesSection(currentText);

        if (0 > currentText.indexOf("{{Raionul ") && 0 > currentText.indexOf("{{Orașul regional")) {
            currentText.append("\n{{");
            currentText.append(getArticleName(raion));
            currentText.append("}}\n");
        }

        if (0 > StringUtils.indexOfIgnoreCase(currentText, "{{Regiunea")) {
            currentText.append("\n{{");
            currentText.append(getArticleName(raion.getRegion()));
            currentText.append("}}\n");
        }

        String raionKey = generateCategoryKey(raionRoName);
        String regionRoName = obtainActualRomanianName(raion.computeRegion());
        StringBuilder categories = new StringBuilder();
        categories.append("[[Categorie:Raioane în regiunea ");
        categories.append(regionRoName);
        categories.append('|').append(raionKey);
        categories.append("]]\n");
        categories.append("[[Categorie:Raioanele Ucrainei");
        categories.append('|').append(raionKey).append(", ").append(generateCategoryKey(regionRoName));
        categories.append("]]\n");

        if (0 > currentText.indexOf("[[Categorie:")) {
            currentText.append(categories);
        } else {
            if (!StringUtils.contains(currentText, categories)) {
                currentText.insert(currentText.indexOf("[[Categorie:"), categories);
            }
        }

        executor.save(actualTitle, currentText.toString(), "Robot - creare/completare articol despre raionul ucrainean "
            + obtainActualRomanianName(raion));
        for (String eachPossibleRaionName : UAUtils.getPossibleRaionNames(raion, rowiki, 1 == countRaionsWithThisName)) {
            if (!StringUtils.equals(eachPossibleRaionName, actualTitle)) {
                if (!rowiki.exists(new String[] { eachPossibleRaionName })[0]) {
                    executor.save(eachPossibleRaionName, "#redirect[[" + actualTitle + "]]",
                        "Robot - creare redirecționare de la nume alternativ");
                }
            }
        }
        executor.link("rowiki", actualTitle, "ukwiki", getUkrainianRaionArticleName(raion));

    }

    private String generateRaionIntro(Raion raion, String actualTitle) {
        final STGroup stgroup = new STGroupFile("templates/ua/ucraina.stg");
        final ST introTmpl = stgroup.getInstanceOf("introRaion");
        final String roRaionName = obtainActualRomanianName(raion);
        introTmpl.add("nume", roRaionName);

        String ukName = appendIfMissing(raion.getOriginalName(), " район");
        UkrainianTransliterator transl = new UkrainianTransliterator(ukName);
        ukName = ukName + '|' + transl.transliterate();
        introTmpl.add("nume_uk", ukName);

        Commune capitala = raion.getCapital();
        String statutCapitala = null;
        if (capitala.getTown() == 0) {
            statutCapitala = "comuna";
        } else if (capitala.getTown() == 1) {
            statutCapitala = "așezarea de tip urban";
        } else if (capitala.getTown() == 2 && null == capitala.getRaion()) {
            statutCapitala = "orașul regional";
        } else {
            statutCapitala = "orașul";
        }
        introTmpl.add("statut_resedinta", statutCapitala);
        introTmpl.add("resedinta", "[[" + getArticleName(capitala) + "|" + obtainActualRomanianName(capitala) + "]]");
        introTmpl.add("apartine_raionului", null == capitala.getRaion() ? ", care nu aparține raionului" : "");

        final StringBuilder regionPart = new StringBuilder("[[");
        final Region reg = raion.computeRegion();
        regionPart.append(getArticleName(reg));
        regionPart.append('|');
        regionPart.append(StringUtils.equals(reg.getRomanianName(), "Crimeea") ? "Republica Autonomă " : "regiunea ");
        regionPart.append(obtainActualRomanianName(reg));
        regionPart.append("]]");
        introTmpl.add("regiune", regionPart.toString());

        return introTmpl.render();
    }

    private String generateRaionInfobox(Raion raion, ParameterReader ibParaReader) throws ConcurrentException {
        final String raionRoName = defaultIfBlank(raion.getRomanianName(), raion.getTransliteratedName());
        final StringBuilder sb = new StringBuilder("{{Infocaseta Așezare");
        sb.append("\n|tip_asezare = Raion");
        sb.append("\n|nume=").append(raionRoName);
        sb.append("\n|nume_nativ=").append(raion.getOriginalName()).append(" район");
        final Region region = raion.computeRegion();
        final String regionRoName = obtainActualRomanianName(region);

        sb.append("\n|tip_subdiviziune=[[Țările lumii|Țară]]");
        sb.append("\n|nume_subdiviziune={{UKR}}");
        sb.append("\n|tip_subdiviziune1=[[Regiunile Ucrainei|Regiune]]");
        sb.append("\n|nume_subdiviziune1=[[");
        sb.append(getArticleName(region));
        sb.append('|').append(regionRoName).append("]]");
        sb.append("\n|").append("resedinta").append('=').append(makeLink(raion.getCapital(), false));

        String coordonate = ibParaReader.getParams().get("coordonate");
        if (StringUtils.isBlank(coordonate)) {
            final String ukrainianRaionArticleName = getUkrainianRaionArticleName(raion);

            boolean uaTextRead = false;
            String uaText = null;
            do {
                try {
                    uaText = ukwiki.getPageText(ukrainianRaionArticleName);
                    uaTextRead = true;
                } catch (final FileNotFoundException fnfe) {
                    fnfe.printStackTrace();
                    uaTextRead = true;
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            } while (!uaTextRead);

            if (!isEmpty(uaText) && (contains(uaText, "{{Район"))) {
                final int indexOfIB = indexOf(uaText, "{{Район");
                final String textFromInfobox = uaText.substring(indexOfIB);
                final ParameterReader ukIBPR = new ParameterReader(textFromInfobox);
                ukIBPR.run();
                final Map<String, String> ukIBParams = ukIBPR.getParams();
                coordonate = ukIBParams.get("координати");
                UAUtils.copyParameterFromTemplate(ukIBPR, sb, "густота", "densitate");
                UAUtils.copyParameterFromTemplate(ukIBPR, sb, "висота", "altitudine");
                UAUtils.copyParameterFromTemplate(ukIBPR, sb, "телефонний код", "prefix_telefonic");
                UAUtils.copyParameterFromTemplate(ukIBPR, sb, "поштовий індекс", "cod_poștal");
                UAUtils.copyParameterFromTemplate(ukIBPR, sb, "площа", "suprafață_totală_km2");
                UAUtils.copyParameterFromTemplate(ukIBPR, sb, "зображення", "imagine");
                UAUtils.copyParameterFromTemplate(ukIBPR, sb, "розташування", "hartă");
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

    private void generateCommuneText(final Commune com) throws Exception {
        System.out.println("------ generating text for commune " + com.getName());
        final String communeRoName = defaultIfBlank(com.getRomanianName(), com.getTransliteratedName());
        final int countCommunesWithThisName = hib.countCommunesByRomanianOrTransliteratedName(communeRoName);
        final int countCommunesWithThisNameInRegion = hib.countCommunesInRegionByRomanianOrTransliteratedName(communeRoName,
            com.computeRegion());
        String actualTitle = getArticleName(com);
        if (null == actualTitle) {
            return;
        }
        final StringBuilder currentText = new StringBuilder();
        final boolean[] titleExistance = rowiki.exists(new String[] { actualTitle });
        if (titleExistance[0]) {
            currentText.append(rowiki.getPageText(actualTitle));
        }
        if (StringUtils.contains(currentText, "de Andrebot -->")) {
            return;
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

        int indexOfFirstSection = currentText.indexOf("==", villageIBText.length());
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
            if (!StringUtils.contains(currentText, "<!-- Start secțiune generată de Andrebot -->")) {
                currentText.replace(indexOfCurrentDemography, currentText.indexOf("==", indexOfCurrentDemography + 2) + 2,
                    demografie);
            }
        } else {
            final int indexOfFutureDemographySection = locateFirstOf(currentText, "== Economie", "==Economie", "{{Ucraina",
                "==Legături externe", "== Legături externe", "== Vezi și", "==Vezi și", "{{Localități în ", "{{Comune în ");
            if (0 <= indexOfFutureDemographySection) {
                currentText.insert(indexOfFutureDemographySection, demografie + "\n{{clearleft}}\n");
            } else {
                currentText.append(demografie).append("\n{{clearleft}}\n");
            }
        }

        generateReferencesSection(currentText);

        if (null != com.getRaion()) {
            if (0 > currentText.indexOf("{{Raionul ") && 0 > currentText.indexOf("{{Orașul regional")) {

                currentText.append("\n{{");
                currentText.append(getArticleName(com.getRaion()));
                currentText.append("}}\n");
            }
        }
        if (1 < com.getSettlements().size() && 0 > currentText.indexOf("{{Comuna ")) {
            currentText.append("\n{{");
            currentText.append(getArticleName(com));
            currentText.append("}}\n");
        }

        String nationalCategoryName = null;
        String unarticulatedTypeName = null;
        if (com.getTown() == 0) {
            nationalCategoryName = "Comunele Ucrainei";
            unarticulatedTypeName = "Comune";
        } else if (com.getTown() == 1) {
            nationalCategoryName = "Așezări de tip urban în Ucraina";
            unarticulatedTypeName = "Așezări de tip urban";
        } else if (com.getRaion() == null) {
            nationalCategoryName = "Orașe regionale în Ucraina";
            unarticulatedTypeName = "Orașe regionale";
        } else {
            nationalCategoryName = "Orașe raionale în Ucraina";
            unarticulatedTypeName = "Orașe raionale";
        }

        String communeKey = generateCategoryKey(communeRoName);
        String communeRaionKey = communeKey
            + (null != com.getRaion() ? (", " + generateCategoryKey(obtainActualRomanianName(com.getRaion()))) : "");
        String regionRoName = obtainActualRomanianName(com.computeRegion());

        StringBuilder categories = new StringBuilder();
        if (null != com.getRaion()) {
            categories.append("[[Categorie:").append(unarticulatedTypeName).append(" în ")
                .append(com.getRaion().isMiskrada() ? "orașul regional " : "raionul ")
                .append(obtainActualRomanianName(com.getRaion())).append(", ").append(regionRoName);
            categories.append('|').append(communeKey);
            categories.append("]]\n");
        }
        categories.append("[[Categorie:").append(unarticulatedTypeName).append(" în regiunea ");
        categories.append(regionRoName);
        categories.append('|').append(communeRaionKey);
        categories.append("]]\n");
        categories.append("[[Categorie:").append(nationalCategoryName);
        categories.append('|').append(communeRaionKey).append(", ").append(generateCategoryKey(regionRoName));
        categories.append("]]\n");

        if (0 > currentText.indexOf("[[Categorie:")) {
            currentText.append(categories);
        } else {
            currentText.insert(currentText.indexOf("[[Categorie:"), categories);
        }

        executor.save(actualTitle, currentText.toString(), "Robot - creare/completare articol despre comuna ucraineană "
            + obtainActualRomanianName(com));

        for (String eachPossibleCommuneName : UAUtils.getPossibleCommuneNames(com, rowiki, 1 == countCommunesWithThisName,
            1 == countCommunesWithThisNameInRegion)) {
            if (!StringUtils.equals(eachPossibleCommuneName, actualTitle)) {
                if (!rowiki.exists(new String[] { eachPossibleCommuneName })[0]) {
                    executor.save(eachPossibleCommuneName, "#redirect[[" + actualTitle + "]]",
                        "Robot - creare redirecționare de la nume alternativ");
                }
            }
        }
        executor.link("rowiki", actualTitle, "ukwiki", getUkrainianCommuneArticleName(com));
    }

    private String generateCommuneIntro(final Commune com, final String actualTitle) {
        final STGroup stgroup = new STGroupFile("templates/ua/ucraina.stg");
        stgroup.getInstanceOf("introVillage");
        if (com.getTown() == 0) {
            return generateRuralCommuneIntro(com, actualTitle);
        } else if (com.getTown() == 2
            && (com.getRaion() == null || (com.getRaion().isMiskrada() && StringUtils.equals(com.getRaion()
                .getTransliteratedName(), com.getTransliteratedName())))) {
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

        final Raion raion = com.getRaion();
        if (null != raion) {
            if (1 == com.getTown()) {
                if (com.equals(raion.getCapital())) {
                    introTmpl.add("statut", "[[Așezare de tip urban|așezarea de tip urban]] de reședință a [["
                        + getArticleName(raion) + "|raionului " + obtainActualRomanianName(raion) + "]]");
                } else {
                    introTmpl.add("statut", "o [[așezare de tip urban]]");
                }
            } else {
                if (com.equals(raion.getCapital())) {
                    introTmpl.add("statut", "orașul raional de reședință al [[" + getArticleName(raion) + "|raionului "
                        + obtainActualRomanianName(raion) + "]]");
                } else {
                    introTmpl.add("statut", "un oraș raional");
                }
            }
        } else {
            introTmpl.add("statut", "un oraș regional");
        }

        final StringBuilder raionPart = new StringBuilder();
        if (null != raion && !raion.isMiskrada() && !com.equals(raion.getCapital())) {
            raionPart.append(makeLink(raion, true)).append(", ");
        } else if (null != raion && raion.isMiskrada()) {
            raionPart.append("orașul regional [[");
            String regTownActualName = obtainActualRomanianName(raion.getCapital());
            String regTownArticleName = getArticleName(raion.getCapital());
            raionPart.append(regTownArticleName);
            if (!StringUtils.equals(regTownActualName, regTownArticleName)) {
                raionPart.append('|').append(regTownActualName);
            }
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

        List<Settlement> settlementsOtherThanMain = getSettlementsOtherThanMain(com);

        if (settlementsOtherThanMain.size() > 1) {
            final List<String> villageNames = new ArrayList<String>();
            for (final Settlement eachVillage : settlementsOtherThanMain) {
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
            Collections.sort(villageNames, new CollatorComparator(ROMANIAN_COLLATOR));
            final StringBuilder villageEnumeration = new StringBuilder("mai cuprinde și satele ");
            for (int i = 0; i < villageNames.size() - 1; i++) {
                villageEnumeration.append(villageNames.get(i)).append(", ");
            }
            villageEnumeration.setLength(villageEnumeration.length() - 2);
            villageEnumeration.append(" și ").append(villageNames.get(villageNames.size() - 1));
            introTmpl.add("sate", villageEnumeration.toString());
        } else if (1 == settlementsOtherThanMain.size()) {
            StringBuilder villageEnumeration = new StringBuilder("mai cuprinde și satul ");
            String villageRoName = obtainActualRomanianName(settlementsOtherThanMain.get(0));
            String villageArticleName = getArticleName(settlementsOtherThanMain.get(0));
            villageEnumeration.append("[[").append(villageArticleName);
            if (!StringUtils.equals(villageRoName, villageArticleName)) {
                villageEnumeration.append('|').append(villageRoName);
            }
            villageEnumeration.append("]]");
            introTmpl.add("sate", villageEnumeration.toString());
        } else {
            introTmpl.add("sate", "nu cuprinde și alte sate");
        }
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

        Raion raion = com.getRaion();
        if (null == raion) {
            introTmpl.add("statut", "oraș regional în ");
        } else {
            if (com.equals(raion.getCapital())) {
                String raionLink = "[[" + getArticleName(raion) + "|" + "raionului "
                    + StringUtils.defaultIfEmpty(raion.getRomanianName(), raion.getTransliteratedName()) + "]]";
                introTmpl.add("statut", "orașul raional de reședință al " + raionLink + " din ");
            } else {
                introTmpl.add("statut", "oraș raional în " + makeLink(raion, true) + " din ");
            }
        }

        final StringBuilder regionPart = new StringBuilder("[[");
        final Region reg = com.computeRegion();
        regionPart.append(getArticleName(reg));
        regionPart.append('|');
        regionPart.append(StringUtils.equals(reg.getRomanianName(), "Crimeea") ? "Republica Autonomă " : "regiunea ");
        regionPart.append(obtainActualRomanianName(reg));
        regionPart.append("]]");
        introTmpl.add("regiune", regionPart.toString());

        List<Settlement> settlementsOtherThanMain = getSettlementsOtherThanMain(com);

        if (settlementsOtherThanMain.size() > 0) {
            final List<String> villageNames = new ArrayList<String>();
            for (final Settlement eachVillage : settlementsOtherThanMain) {
                final String villageRoName = obtainActualRomanianName(eachVillage);
                final String villageArticleName = getArticleName(eachVillage);
                final StringBuilder villageLinkBuilder = new StringBuilder("[[");
                villageLinkBuilder.append(villageArticleName);
                if (!StringUtils.equals(villageArticleName, villageRoName)) {
                    villageLinkBuilder.append('|').append(villageRoName);
                }
                villageLinkBuilder.append("]]");
                if (eachVillage.equals(com.getCapital())) {
                    if (com.getTown() > 0) {
                        continue;
                    }
                    villageLinkBuilder.append(" (reședința)");
                }
                villageNames.add(villageLinkBuilder.toString());
            }
            Collections.sort(villageNames, new CollatorComparator(ROMANIAN_COLLATOR));
            final StringBuilder villageEnumeration = new StringBuilder(
                "În afara localității principale, mai cuprinde și satele ");
            for (int i = 0; i < villageNames.size() - 1; i++) {
                villageEnumeration.append(villageNames.get(i)).append(", ");
            }
            villageEnumeration.setLength(villageEnumeration.length() - 2);
            villageEnumeration.append(" și ").append(villageNames.get(villageNames.size() - 1));
            villageEnumeration.append('.');
            introTmpl.add("are_sate", villageEnumeration.toString());
        } else if (1 == settlementsOtherThanMain.size()) {
            StringBuilder villageEnumeration = new StringBuilder("mai cuprinde și satul ");
            String villageRoName = obtainActualRomanianName(settlementsOtherThanMain.get(0));
            String villageArticleName = getArticleName(settlementsOtherThanMain.get(0));
            villageEnumeration.append("[[").append(villageArticleName);
            if (!StringUtils.equals(villageRoName, villageArticleName)) {
                villageEnumeration.append('|').append(villageRoName);
            }
            villageEnumeration.append("]]");
            introTmpl.add("sate", villageEnumeration.toString());
        } else {
            introTmpl.add("are_sate", "");
        }

        if (null == com.getRaion()) {
            List<Raion> raions = hib.findOuterRaionsForCity(com);
            StringBuilder sb = new StringBuilder();
            if (0 < raions.size()) {
                sb.append("Deși subordonat direct regiunii, orașul este și reședința");
                if (1 == raions.size()) {
                    sb.append(" [[").append(getArticleName(raions.get(0))).append("|raionului ")
                        .append(obtainActualRomanianName(raions.get(0))).append("]]");
                } else {
                    sb.append(" raioanelor ");
                    List<String> commaSeparatedRaions = new ArrayList<String>();
                    for (int i = 0; i < raions.size() - 1; i++) {
                        commaSeparatedRaions.add("[[" + getArticleName(raions.get(i)) + '|'
                            + obtainActualRomanianName(raions.get(i)));
                    }
                    sb.append(join(commaSeparatedRaions, ", "));
                    sb.append(" și ").append(" [[").append(getArticleName(raions.get(raions.size() - 1))).append('|')
                        .append(obtainActualRomanianName(raions.get(raions.size() - 1)));
                }
                sb.append('.');
            }
            introTmpl.add("are_raion", sb.toString());
        }

        return introTmpl.render();
    }

    private String makeLink(LanguageStructurable ls, boolean usenomtypename) {
        StringBuilder sb = new StringBuilder("[[");
        String articleName = getArticleName(ls);
        sb.append(articleName);
        StringBuilder textualDescription = new StringBuilder();
        if (usenomtypename) {
            textualDescription.append(ls.getNominative()).append(' ');
        }
        textualDescription.append(obtainActualRomanianName(ls));
        if (!StringUtils.equals(articleName, textualDescription.toString())) {
            sb.append('|').append(textualDescription);
        }
        sb.append("]]");
        return sb.toString();
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
            Collections.sort(villageNames, new CollatorComparator(ROMANIAN_COLLATOR));
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
        Raion raion = com.getRaion();
        switch (com.getTown()) {
        case 0:
            sb.append("Comună");
            break;
        case 1:
            sb.append("Așezare de tip urban");
            break;
        case 2:
            sb.append(null != raion ? "Oraș" : "Oraș regional");
            break;
        }
        sb.append("\n|nume=").append(villageRoName);
        sb.append("\n|nume_nativ=").append(com.getName());
        if (null != raion) {
            obtainActualRomanianName(raion);
        }
        final Region region = com.computeRegion();
        final String regionRoName = obtainActualRomanianName(region);

        sb.append("\n|tip_subdiviziune=[[Țările lumii|Țară]]");
        sb.append("\n|nume_subdiviziune={{UKR}}");
        sb.append("\n|tip_subdiviziune1=[[Regiunile Ucrainei|Regiune]]");
        sb.append("\n|nume_subdiviziune1=[[");
        sb.append(getArticleName(region));
        sb.append("|").append(regionRoName).append("]]");
        if (null != raion) {
            if (raion.isMiskrada() && !StringUtils.equals(raion.getTransliteratedName(), com.getTransliteratedName())) {
                sb.append("\n|tip_subdiviziune2=Oraș regional");
                sb.append("\n|nume_subdiviziune2=[[");
                String regTownArticleName = getArticleName(raion.getCapital());
                sb.append(regTownArticleName);
                String regTownActualName = obtainActualRomanianName(raion.getCapital());
                if (!StringUtils.equals(regTownArticleName, regTownActualName)) {
                    sb.append('|');
                    sb.append(regTownActualName);
                }
            } else if (!raion.isMiskrada()) {
                sb.append("\n|tip_subdiviziune2=[[Raioanele Ucrainei|Raion]]");
                sb.append("\n|nume_subdiviziune2=[[");
                sb.append(getArticleName(raion));
                sb.append('|');
                sb.append(obtainActualRomanianName(raion));
            }
            sb.append("]]");
        }

        if (com.getTown() == 0) {
            sb.append("\n|resedinta=");
            if (1 < com.getSettlements().size()) {
                sb.append(makeLink(com.getCapital(), false));
                Set<Settlement> allSettlements = new HashSet<Settlement>();
                allSettlements.addAll(com.getSettlements());
                if (null != com.getCapital()) {
                    allSettlements.add(com.getCapital());
                }
                List<Settlement> sate = new ArrayList<Settlement>();
                sate.addAll(allSettlements);
                Collections.sort(sate, new Comparator<Settlement>() {

                    public int compare(Settlement o1, Settlement o2) {
                        return ROMANIAN_COLLATOR.compare(obtainActualRomanianName(o1), obtainActualRomanianName(o2));
                    }

                });
                for (int i = 0; i < sate.size(); i++) {
                    sb.append("\n|p").append(1 + i).append('=');
                    sb.append(makeLink(sate.get(i), false));
                }
                sb.append("\n|componenta=").append(sate.size()).append(" sate");
                if (sate.size() < 3) {
                    sb.append("\n|componenta_stil=para");
                }
            } else {
                sb.append(obtainActualRomanianName(com.getCapital()));
                sb.append("\n|p1=").append(obtainActualRomanianName(com.getCapital()));
                sb.append("\n|componenta_stil=para");
            }
        } else {
            List<Settlement> sate = new ArrayList<Settlement>();
            sate.addAll(com.getSettlements());
            Set<Settlement> toRemove = new HashSet<Settlement>();
            for (Settlement village : sate) {
                if (StringUtils.equals(village.getTransliteratedName(), com.getTransliteratedName())) {
                    toRemove.add(village);
                }
            }
            sate.removeAll(toRemove);
            Collections.sort(sate, new Comparator<Settlement>() {

                public int compare(Settlement o1, Settlement o2) {
                    return ROMANIAN_COLLATOR.compare(obtainActualRomanianName(o1), obtainActualRomanianName(o2));
                }

            });
            if (0 < sate.size()) {
                for (int i = 0; i < sate.size(); i++) {
                    sb.append("\n|p").append(1 + i).append('=');
                    sb.append(makeLink(sate.get(i), false));
                }
                if (3 > sate.size()) {
                    sb.append("\n|componenta_stil=para");
                }
                if (1 < sate.size()) {
                    sb.append("\n|componenta=").append(sate.size()).append(" sate");
                }
            }
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
                final int indexOfIB = StringUtils.indexOfAny(uaText, "{{Село", "{{Смт", "{{Місто", "{{Сільська рада");
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

    private String getUkrainianRaionArticleName(final Raion raion) throws ConcurrentException {
        LazyInitializer<String> raionInitializer = uaArticleNames.get(raion);
        if (null == raionInitializer) {
            raionInitializer = new LazyInitializer<String>() {

                @Override
                protected String initialize() throws ConcurrentException {
                    final List<String> possibleUkrainianArticleNames = getPossibleUkrainianArticleNames(raion);
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
                            if (UAUtils.isInAnyCategoryTree(articleCandidateTitle, ukwiki, 2, "Райони областей України")) {
                                return articleCandidateTitle;
                            }
                        }
                    }
                    return possibleUkrainianArticleNames.get(0);
                }

            };
            uaArticleNames.put(raion, raionInitializer);
        }
        return raionInitializer.get();
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
                            List<String> allowedCategories = new ArrayList<String>();
                            if (com.getTown() == 0) {
                                allowedCategories.add("Сільські ради України");
                            } else if (com.getTown() == 1) {
                                allowedCategories.add("Селища міського типу України");
                            } else {
                                allowedCategories.add("Міста України");
                            }
                            if (com.getSettlements().size() == 1) {
                                allowedCategories.add("Села України");
                            }
                            if (UAUtils.isInAnyCategoryTree(articleCandidateTitle, ukwiki, 2,
                                allowedCategories.toArray(new String[allowedCategories.size()]))) {
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
        if (null == articleName) {
            return;
        }
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
                    return ROMANIAN_COLLATOR.compare(obtainActualRomanianName(o1), obtainActualRomanianName(o2));
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
        List<Settlement> settlementsOtherThanMain = getSettlementsOtherThanMain(s.getCommune());
        if (1 > settlementsOtherThanMain.size()) {
            return;
        }
        if (s.getCommune().getTown() > 0 && (s.equals(s.getCommune().getCapital()))) {
            return;
        }
        final int countVillagesWithThisName = hib.countSettlementsByRomanianOrTransliteratedName(villageRoName);
        int countVillagesWithThisNameInRaion = 1;
        Raion raion = s.getCommune().getRaion();
        if (null != raion) {
            countVillagesWithThisNameInRaion = hib.countSettlementsInRaionByRomanianOrTransliteratedName(villageRoName,
                raion);
        }
        final int countVillagesWithThisNameInRegion = hib.countSettlementsInRegionByRomanianOrTransliteratedName(
            villageRoName, s.computeRegion());

        final StringBuilder currentText = new StringBuilder();
        String actualTitle = getArticleName(s);
        if (null == actualTitle) {
            return;
        }
        final boolean[] titleExistance = rowiki.exists(new String[] { actualTitle });
        if (titleExistance[0]) {
            currentText.append(rowiki.getPageText(actualTitle));
        }
        if (StringUtils.contains(currentText, "de Andrebot -->")) {
            return;
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
            if (!StringUtils.contains(currentText, "<!-- Start secțiune generată de Andrebot -->")) {
                currentText.replace(indexOfCurrentDemography, currentText.indexOf("==", indexOfCurrentDemography + 2) + 2,
                    demografie);
            }
        } else {
            final int indexOfFutureDemographySection = locateFirstOf(currentText, "== Economie", "==Economie", "{{Ucraina",
                "==Legături externe", "== Legături externe", "== Vezi și", "==Vezi și", "{{Localități în ", "{{Comune în ");
            if (0 <= indexOfFutureDemographySection) {
                currentText.insert(indexOfFutureDemographySection, demografie + "\n{{clearleft}}\n");
            } else {
                currentText.append(demografie).append("\n{{clearleft}}\n");
            }
        }

        generateReferencesSection(currentText);

        if (0 > currentText.indexOf("{{Comuna ")) {
            currentText.append("\n{{");
            currentText.append(roArticleNames.get(s.getCommune()).get());
            currentText.append("}}\n");
        }

        String communeRoName = obtainActualRomanianName(s.getCommune());
        String villageKey = generateCategoryKey(villageRoName + ", " + communeRoName);
        String communeRaionKey = villageKey + ", " + generateCategoryKey(obtainActualRomanianName(raion));
        String regionRoName = obtainActualRomanianName(s.computeRegion());

        StringBuilder categories = new StringBuilder();
        categories.append("[[Categorie:Sate în ").append(raion.isMiskrada() ? "orașul regional " : "raionul ")
            .append(obtainActualRomanianName(raion)).append(", ").append(regionRoName);
        categories.append('|').append(villageKey);
        categories.append("]]\n");
        categories.append("[[Categorie:Sate în regiunea ");
        categories.append(regionRoName);
        categories.append('|').append(communeRaionKey);
        categories.append("]]\n");
        categories.append("[[Categorie:Sate în Ucraina");
        categories.append('|').append(communeRaionKey).append(", ").append(generateCategoryKey(regionRoName));
        categories.append("]]\n");

        if (0 > currentText.indexOf("[[Categorie:")) {
            currentText.append(categories);
        } else {
            currentText.insert(currentText.indexOf("[[Categorie:"), categories);
        }

        executor.save(actualTitle, currentText.toString(), "Robot - creare/completare articol despre satul ucrainean "
            + obtainActualRomanianName(s));
        for (String eachPossibleVillageName : UAUtils.getPossibleSettlementNames(s, rowiki, 1 == countVillagesWithThisName,
            1 == countVillagesWithThisNameInRegion, 1 == countVillagesWithThisNameInRaion)) {
            if (!StringUtils.equals(eachPossibleVillageName, actualTitle)) {
                if (!rowiki.exists(new String[] { eachPossibleVillageName })[0]) {
                    executor.save(eachPossibleVillageName, "#redirect[[" + actualTitle + "]]",
                        "Robot - creare redirecționare de la nume alternativ");
                }
            }
        }
        executor.link("rowiki", actualTitle, "ukwiki", getUkrainianVillageArticleName(s));
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
                } else {
                    communePart.append("raional ");
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

        sb.append("\n|tip_subdiviziune3=");
        switch (s.getCommune().getTown()) {
        case 0:
            sb.append("[[Comunele Ucrainei|Comună");
            break;
        case 1:
            sb.append("[[Așezare de tip urban");
            break;
        case 2:
            sb.append("[[Orașele Ucrainei|Oraș ");
            if (null != s.getCommune().getRaion() && ! s.getCommune().getRaion().isMiskrada()) {
                sb.append("raional");
            } else {
                sb.append("regional");
            }
            break;
        }
        sb.append("]]");
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
                return ROMANIAN_COLLATOR.compare(obtainActualRomanianName(o1), obtainActualRomanianName(o2));
            }
        });
        final List<Raion> raions = new ArrayList<Raion>(hib.getRaionsForRegion(region));
        Collections.sort(raions, new Comparator<Raion>() {

            public int compare(final Raion o1, final Raion o2) {
                return ROMANIAN_COLLATOR.compare(obtainActualRomanianName(o1), obtainActualRomanianName(o2));
            }
        });
        int section = 0;

        final String regionRomanianName = obtainActualRomanianName(region);
        boolean isCrimea = StringUtils.equals(regionRomanianName, "Crimeea");
        final StringBuilder navBox = new StringBuilder(
            "{{Casetă de navigare simplă\n|stare = {{{stare|autopliabilă}}}\n|titlu=Diviziuni administrative ale [[");
        navBox.append(isCrimea ? "Republica Autonomă " : "Regiunea ");
        navBox.append(regionRomanianName);
        navBox.append('|').append(isCrimea ? "Republicii Autonome " : "regiunii ");
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
                navBox.append("\n[[").append(regCityArticleName);
                if (!StringUtils.equals(regCityArticleName, regCityName)) {
                    navBox.append('|').append(regCityName);
                }
                navBox.append("]]{{~}}");
            }
            navBox.delete(navBox.length() - "{{~}}".length(), navBox.length());
            navBox.append("\n</div>");
        }
        if (null != raions && 0 < raions.size()) {
            section++;
            navBox.append("\n|grup").append(section).append("=[[Raioanele Ucrainei|Raioane]]");
            navBox.append("\n|listă").append(section).append("=<div>");
            for (final Raion raion : raions) {
                final String raionArticleName = getArticleName(raion);
                final String raionName = obtainActualRomanianName(raion);
                navBox.append("\n[[").append(raionArticleName).append('|').append(raionName).append("]]{{~}}");
            }
            navBox.delete(navBox.length() - "{{~}}".length(), navBox.length());
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
                if (!StringUtils.contains(currentText, "<!-- Start secțiune generată de Andrebot -->")) {
                    currentText.replace(indexOfCurrentDemography,
                        currentText.indexOf("==", indexOfCurrentDemography + 2) + 2, demografie);
                }
            } else {
                final int indexOfFutureDemographySection = locateFirstOf(currentText, "== Economie", "==Economie",
                    "{{Ucraina", "==Legături externe", "== Legături externe", "== Vezi și", "==Vezi și");
                if (0 <= indexOfFutureDemographySection) {
                    currentText.insert(indexOfFutureDemographySection, demografie + "\n{{clearleft}}\n");
                } else {
                    currentText.append(demografie).append("\n{{clearleft}}\n");
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
                "== Legături externe", "{{Localități în ", "{{Comune în ", "{{ciot");
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

    public List<String> getPossibleUkrainianArticleNames(Raion raion) {
        final List<String> ret = new ArrayList<String>();
        final Region reg = raion.computeRegion();
        final String regName = getUkrainianRegionName(reg);
        if (!raion.isMiskrada()) {
            String originalName = StringUtils.replace(raion.getOriginalName(), "`", "'");
            ret.add(originalName + " район (" + regName + ")");
            ret.add(originalName + " район ");
        }
        return ret;
    }

    public List<String> getPossibleUkrainianArticleNames(final Settlement s) {
        final List<String> ret = new ArrayList<String>();
        final Raion r = s.getCommune().getRaion();
        final Region reg = s.computeRegion();
        final String regName = getUkrainianRegionName(reg);
        String raionOriginalName = StringUtils.replace(r.getOriginalName(), "`", "'");

        String settlementName = StringUtils.replace(s.getName(), "`", "'");
        if (null != r && !r.isMiskrada()) {
            ret.add(settlementName + " (" + raionOriginalName + " район, " + regName + ")");
        }
        if (null != r && !r.isMiskrada()) {
            ret.add(settlementName + " (" + raionOriginalName + " район)");
        }
        ret.add(settlementName + " (село)");
        ret.add(settlementName);
        return ret;
    }

    public List<String> getPossibleUkrainianArticleNames(final Commune c) {
        final List<String> ret = new ArrayList<String>();
        final Raion r = c.getRaion();
        final Region reg = c.computeRegion();
        final String regName = getUkrainianRegionName(reg);
        String silradaName = trim(removeEnd(trim(replace(c.getOriginalName(), "`", "'")), "сілрада")) + " сільська рада";
        String communeName = replace(c.getName(), "`", "'");

        if (null != r && !r.isMiskrada()) {
            ret.add(communeName + " (" + r.getOriginalName() + " район, " + regName + ")");
            ret.add(silradaName + " (" + r.getOriginalName() + " район, " + regName + ")");
        }
        if (null != r && !r.isMiskrada()) {
            ret.add(communeName + " (" + r.getOriginalName() + " район)");
            ret.add(silradaName + " (" + r.getOriginalName() + " район)");
        }
        if (1 == c.getTown()) {
            ret.add(communeName + " (смт)");
        } else if (2 == c.getTown()) {
            ret.add(communeName + " (місто)");
        }
        ret.add(silradaName);
        ret.add(communeName);
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

class CollatorComparator implements Comparator<String> {

    private Collator collator;

    public CollatorComparator(Collator c) {
        collator = c;
    }

    public int compare(String arg0, String arg1) {
        return collator.compare(arg0, arg1);
    }

}