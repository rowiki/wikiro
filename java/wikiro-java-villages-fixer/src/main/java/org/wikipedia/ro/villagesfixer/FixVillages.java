package org.wikipedia.ro.villagesfixer;

import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.appendIfMissing;
import static org.apache.commons.lang3.StringUtils.contains;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.endsWith;
import static org.apache.commons.lang3.StringUtils.endsWithAny;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.prependIfMissing;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.apache.commons.lang3.StringUtils.removeStartIgnoreCase;
import static org.apache.commons.lang3.StringUtils.replace;
import static org.apache.commons.lang3.StringUtils.replaceEach;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.startsWithAny;
import static org.apache.commons.lang3.StringUtils.substring;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.apache.commons.lang3.StringUtils.trim;

import java.io.Console;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.RuleBasedCollator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.wikibase.Wikibase;
import org.wikibase.WikibaseException;
import org.wikibase.WikibasePropertyFactory;
import org.wikibase.data.Claim;
import org.wikibase.data.CommonsMedia;
import org.wikibase.data.Entity;
import org.wikibase.data.Item;
import org.wikibase.data.LanguageString;
import org.wikibase.data.Property;
import org.wikibase.data.Quantity;
import org.wikibase.data.Rank;
import org.wikibase.data.Snak;
import org.wikibase.data.StringData;
import org.wikibase.data.Time;
import org.wikibase.data.WikibaseData;
import org.wikipedia.Wiki;
import org.wikipedia.ro.InitializableComparator;
import org.wikipedia.ro.cache.WikidataEntitiesCache;
import org.wikipedia.ro.model.PlainText;
import org.wikipedia.ro.model.WikiPart;
import org.wikipedia.ro.model.WikiTemplate;
import org.wikipedia.ro.parser.ParseResult;
import org.wikipedia.ro.parser.WikiTemplateParser;
import org.wikipedia.ro.parser.WikiTextParser;
import org.wikipedia.ro.utils.LinkUtils;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.jul.LevelChangePropagator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.core.ConsoleAppender;

public class FixVillages {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(FixVillages.class);
    private static final String CRISANA_LINK = "[[Crișana]]";
    private static final String BANAT_LINK = "[[Banat]]";
    private static final String TRANSYLVANIA_LINK = "[[Transilvania]]";
    private static final String MUNTENIA_LINK = "[[Muntenia]]";
    private static final String MOLDOVA_LINK = "[[Moldova Occidentală|Moldova]]";
    private static final String BUCOVINA_LINK = "[[Bucovina]]";
    private static String collationDescription = "<  0 < 1 < 2 < 3 < 4 < 5 < 6 < 7 < 8 < 9 "
        + "< a, A < ă, Ă < â, Â‚ < b, B < c, C < d, D < e, E < f, F < g, G < h, H < i, I"
        + "< î, Î < j, J < k, K < l, L < m, M < n, N < o, O < p, P < q, Q < r, R"
        + "< s, S < ș, Ș < t, T < ț, Ț < u, U < v, V < w, W < x, X < y, Y < z, Z";
    private static RuleBasedCollator collator = null;
    private static Exception exception;

    // Pattern sentencePattern = Pattern.compile("((.*(\\(.*?\\)))*.*?)(\\.|$)\\s*");
    // Pattern sentencePattern = Pattern.compile(
    // "(([^\\.]*(\\(.*?\\)))*.*?)(((\\.|$)\\s*((\\<ref[^\\/]*?\\>.*?\\<\\/ref\\>)|(\\<ref[^>]*\\/\\>))*)|((\\<ref[^\\/]*?\\>.*?\\<\\/ref\\>)|(\\<ref[^>]*\\/\\>))*\\s*(\\.|$))\\s*");
    private static Pattern manuallyFormatedNumberPattern =
        Pattern.compile("(?<!(?:ormatnum[\\:\\|]\\d{0,9}))\\d+(?:\\.\\s*\\d+)+");
    private static Pattern sentencePattern = Pattern.compile(
        "((?:'''.*?''')?(?:(?:\\[\\[.*?\\]\\])|(\\(.*?\\))|(?:\\<ref[^\\>]*(?:(?:\\/\\>)|(?:\\>.*?\\<\\/ref\\>)))|[^\\[\\]\\.])*+(?:(?:\\<ref[^\\>]*+\\>(?:.*?\\</ref\\>)?)|(?:\\[\\[.*?\\]\\])|[^\\[\\]\\.])*?)(((\\.|$)\\s*((\\<ref[^\\/]*?\\>.*?\\<\\/ref\\>)|(\\<ref[^>]*\\/\\>))*)|((\\<ref[^\\/]*?\\>.*?\\<\\/ref\\>)|(\\<ref[^>]*\\/\\>))*+\\s*(\\.|$))\\s*");
    private static Pattern fullLocationPattern =
        Pattern.compile("\\s*(?:î|di)n \\[\\[județul\\s+.*?(\\|.*?)?\\]\\], \\[\\[.*?\\]\\], \\[\\[România\\]\\]\\,?");
    private static Pattern qualifierPattern = Pattern.compile("'''.*?'''(.*?)\\s+este");
    private static Pattern commentedEol = Pattern.compile("<!\\-\\-[\\r\\n]+\\-\\->");
    private static Pattern imageInInfoboxPattern =
        Pattern.compile("(\\[\\[)?(((Fișier)|(File)|(Imagine)|(Image))\\:)?([^\\|\\]]*)((\\|.*?)*\\]\\])?");
    private static Pattern roFormattedNumberStringRegex = Pattern.compile("\\d+(?:\\.\\d+)*,\\d+");
    private static Pattern simpleRefPattern = Pattern.compile("\\<ref[^\\/\\>]*\\>([^\\<]+)\\</ref\\>");
    private static Pattern numberRangePattern = Pattern.compile("(\\d+)((?:[–\\-]|(?:&ndash;))(\\d+))?");
    private static Pattern ifPattern = Pattern.compile("\\{\\{\\s*#if");

    private static String crtSettlementName = null, crtCommuneName = null, crtCountyName = null;

    public static void main(String[] args) throws IOException {
        initLogging();
        Wiki rowiki = Wiki.newSession("ro.wikipedia.org");
        Wiki commonsWiki = Wiki.newSession("commons.wikimedia.org");

        try {
            collator = new RuleBasedCollator(collationDescription);
        } catch (Exception e) {
            exception = e;
        }

        Wikibase dwiki = new Wikibase();
        List<String> countyCategoryMembersList =
            rowiki.getCategoryMembers("Category:Județe din România", Wiki.CATEGORY_NAMESPACE);
        List<String> countyNames = new ArrayList<>();
        for (String eachArticleName : countyCategoryMembersList) {
            String countyNameNoCat = removeStart(eachArticleName, "Categorie:");
            if (startsWith(countyNameNoCat, "Județul")) {
                countyNames.add(trim(removeStart(countyNameNoCat, "Județul")));
            }
        }

        Property captionProp = WikibasePropertyFactory.getWikibaseProperty("P2096");
        Property imageProp = WikibasePropertyFactory.getWikibaseProperty("P18");
        Property instanceOfProp = WikibasePropertyFactory.getWikibaseProperty("P31");
        Property capitalProp = WikibasePropertyFactory.getWikibaseProperty("P36");
        Property areaProp = WikibasePropertyFactory.getWikibaseProperty("P2046");
        Property altProp = WikibasePropertyFactory.getWikibaseProperty("P2044");
        Property mayorProp = WikibasePropertyFactory.getWikibaseProperty("P6");
        Property webAddrProp = WikibasePropertyFactory.getWikibaseProperty("P856");
        Property popProp = WikibasePropertyFactory.getWikibaseProperty("P1082");
        Property pointInTimeProp = WikibasePropertyFactory.getWikibaseProperty("P585");
        Property sirutaProp = WikibasePropertyFactory.getWikibaseProperty("P843");
        Property nativeLabelProp = WikibasePropertyFactory.getWikibaseProperty("P1705");
        Property regPlateProp = WikibasePropertyFactory.getWikibaseProperty("P395");

        Entity meterEnt = new Entity("Q11573");
        Entity squareKmEnt = new Entity("Q712226");

        Set<Snak> roWpReference = new HashSet<>();
        Snak statedInRoWp = new Snak();
        statedInRoWp.setProperty(WikibasePropertyFactory.getWikibaseProperty("P143"));
        statedInRoWp.setData(new Item(new Entity("Q199864")));
        roWpReference.add(statedInRoWp);

        InitializableComparator<String> roComp = new InitializableComparator<String>() {

            private String prefix;

            public InitializableComparator<String> init(String prefix) {
                this.prefix = prefix;
                return this;
            }

            public int compare(String o1, String o2) {
                return collator.compare(removeStart(o1, prefix), removeStart(o2, prefix));
            }
        };
        Collections.sort(countyNames, roComp.init("Județul "));

        try {
            String rowpusername = System.getenv("WP_USER");
            String datausername = System.getenv("WD_USER");

            char[] rowppassword = null == System.getenv("WP_PASS") ? null : System.getenv("WP_PASS").toCharArray();
            char[] datapassword = null == System.getenv("WD_PASS") ? null : System.getenv("WD_PASS").toCharArray();
            Console sysConsole = System.console();
            if (null == rowpusername) {
                sysConsole.printf("Wiki username:");
                rowpusername = sysConsole.readLine();
            }
            if (null == rowppassword) {
                sysConsole.printf("Wiki password:");
                rowppassword = sysConsole.readPassword();
            }
            if (null == datausername) {
                sysConsole.printf("Data username:");
                datausername = sysConsole.readLine();
            }
            if (null == datapassword) {
                sysConsole.printf("Data password:");
                datapassword = sysConsole.readPassword();
            }

            LOG.info("Logging in to ro wiki with username: {}", rowpusername);
            rowiki.login(rowpusername, rowppassword);
            LOG.info("Logging in to wikibase with username: {}", datausername);
            dwiki.login(datausername, datapassword);
            
            LOG.info("Login successful");
            
            WikidataEntitiesCache wdcache = new WikidataEntitiesCache(dwiki);
            rowiki.setMarkBot(true);
            dwiki.setMarkBot(true);

            String countyStart = null;
            boolean countyTouched = args.length < 1;
            if (args.length > 0) {
                countyStart = args[0];
            }
            for (String eachCounty : countyNames) {
                if (!countyTouched && !eachCounty.equals(countyStart)) {
                    continue;
                }
                crtCountyName = eachCounty;
                LOG.info("Starting up county {}", crtCountyName);

                Entity countyEnt = wdcache.getByArticle("rowiki", StringUtils.prependIfMissing(crtCountyName, "Județul "));
                Set<Claim> regPlate = countyEnt.getBestClaims(regPlateProp);
                String countySymbol = regPlate.stream().filter(c -> "statement".equals(c.getType())).map(Claim::getMainsnak)
                    .map(Snak::getData).filter(d -> d instanceof StringData).map(StringData.class::cast).findFirst()
                    .map(StringData::getValue).orElse("");

                countyTouched = true;
                List<String> categoryMembers =
                    rowiki.getCategoryMembers("Category:Orașe din județul " + eachCounty, Wiki.MAIN_NAMESPACE);
                List<String> categoryMembersList = new ArrayList<>();
                categoryMembersList.addAll(categoryMembers);
                categoryMembersList
                    .addAll(rowiki.getCategoryMembers("Category:Comune din județul " + eachCounty, Wiki.MAIN_NAMESPACE));

                List<String> subcategories =
                    rowiki.getCategoryMembers("Category:Orașe din județul " + eachCounty, Wiki.CATEGORY_NAMESPACE);
                List<String> subcategoriesList = new ArrayList<>();
                subcategoriesList.addAll(subcategories);
                subcategoriesList
                    .addAll(rowiki.getCategoryMembers("Category:Comune din județul " + eachCounty, Wiki.CATEGORY_NAMESPACE));
                for (String eachSubcat : subcategoriesList) {
                    String catTitle = removeStart(eachSubcat, "Categorie:");
                    List<String> subarticles = rowiki.getCategoryMembers(eachSubcat, Wiki.MAIN_NAMESPACE);
                    for (String eachSubarticle : subarticles) {
                        if (StringUtils.equals(eachSubarticle, catTitle)) {
                            categoryMembersList.add(eachSubarticle);
                        }
                    }
                }

                List<String> settlementCategories = List.of("Categorie:Localități urbane din județul " + eachCounty,
                    "Categorie:Sate din județul " + eachCounty,
                    "Categorie:Subunități administrative ale județului " + eachCounty);
                String[] settlementCategoriesContent = new String[] {
                    "[[Categorie:Localități din județul " + eachCounty + "]]\n[[Categorie:Localități urbane din România|"
                        + eachCounty + "]]",
                    "[[Categorie:Localități din județul " + eachCounty + "]]\n[[Categorie:Sate din România după județ|"
                        + eachCounty + "]]",
                    "[[Categorie:Geografia județului " + eachCounty
                        + "]]\n[[Categorie:Subunități administrative ale județelor României|" + eachCounty + "]]", };
                boolean[] categories = rowiki.exists(settlementCategories);

                for (int catIdx = 0; catIdx < settlementCategories.size(); catIdx++) {
                    if (!categories[catIdx]) {
                        rowiki.edit(settlementCategories.get(catIdx), settlementCategoriesContent[catIdx],
                            "Creare categorii cu localități și subunități administrative pentru județul " + eachCounty);
                    }
                }

                String countyCapitalId = null;
                Entity countyEntity =
                    dwiki.getWikibaseItemBySiteAndTitle("rowiki", prependIfMissing(eachCounty, "Județul "));
                Set<Claim> countyCapitalClaims = countyEntity.getClaims().get(capitalProp);
                for (Claim eachCountyCapitalClaim : countyCapitalClaims) {
                    countyCapitalId = ((Item) eachCountyCapitalClaim.getMainsnak().getData()).getEnt().getId();
                }

                Collections.sort(categoryMembersList, roComp.init("Comuna "));
                Pattern countyLinkPattern = Pattern.compile("\\[\\[\\s*(J|j)udețul\\s" + eachCounty + "\\s*(\\||\\])");
                boolean communeTouched = args.length < 2;
                String communeStart = null;
                if (args.length > 1) {
                    communeStart = args[1];
                }
                for (String eachCommuneArticle : new LinkedHashSet<String>(categoryMembersList)) {
                    crtSettlementName = null;
                    Entity communeWikibaseItem = wdcache.getByArticle("rowiki", eachCommuneArticle);

                    String communeName = trim(substringBefore(
                        removeStart(removeEnd(removeEnd(eachCommuneArticle, ", " + eachCounty), ", România"), "Comuna "),
                        "("));
                    LOG.info("Working on commune {}, county {}", communeName, crtCountyName);
                    crtCommuneName = communeName;
                    if (!communeTouched && countyStart.equals(eachCounty) && !communeName.equals(communeStart)) {
                        continue;
                    }
                    communeTouched = true;
                    String communeType = null;
                    Set<Claim> instanceOfClaims = communeWikibaseItem.getClaims().get(instanceOfProp);
                    if (null == instanceOfClaims) {
                        continue;
                    }
                    for (Claim eachInstanceOfClaim : instanceOfClaims) {
                        Item instanceType = (Item) eachInstanceOfClaim.getMainsnak().getData();
                        if (removeStartIgnoreCase(instanceType.getEnt().getId(), "Q").equals("640364")) {
                            communeType = "municipiu";
                            break;
                        } else if (removeStartIgnoreCase(instanceType.getEnt().getId(), "Q").equals("16858213")) {
                            communeType = "oraș";
                            break;
                        } else if (removeStartIgnoreCase(instanceType.getEnt().getId(), "Q").equals("659103")) {
                            communeType = "comună";
                            break;
                        }
                    }

                    if (null == communeType) {
                        continue;
                    }

                    String communeDescr = String.format("%s din județul %s, România", communeType, eachCounty);
                    boolean communeChanged = false;
                    if (!StringUtils.equals(communeWikibaseItem.getDescriptions().get("ro"), communeDescr)) {
                        dwiki.setDescription(communeWikibaseItem.getId(), "ro", communeDescr);
                        communeChanged = true;
                    }
                    if (!StringUtils.equals(communeWikibaseItem.getLabels().get("ro"), communeName)) {
                        dwiki.setLabel(communeWikibaseItem.getId(), "ro", communeName);
                        communeChanged = true;
                    }

                    Map<Property, Set<Claim>> communeClaims = communeWikibaseItem.getClaims();
                    Set<Claim> compositeVillagesClaims = communeClaims.get(new Property("P1383"));
                    Set<Rank> availableRanks =
                        compositeVillagesClaims.stream().map(Claim::getRank).collect(Collectors.toSet());
                    Rank biggestRank = availableRanks.contains(Rank.PREFERRED) ? Rank.PREFERRED : Rank.NORMAL;
                    compositeVillagesClaims =
                        compositeVillagesClaims.stream().filter(c -> c.getRank() == biggestRank).collect(Collectors.toSet());
                    Map<String, String> villages = new HashMap<>();
                    Map<String, String> urbanSettlements = new HashMap<>();

                    Set<Claim> capitalClaims = communeWikibaseItem.getClaims().get(new Property("P36"));
                    Claim capitalClaim = capitalClaims.stream().findFirst().get();
                    Entity capitalEntity = dwiki.getWikibaseItemById(((Item) capitalClaim.getMainsnak().getData()).getEnt());

                    Pattern communeLinkPattern =
                        Pattern.compile("\\[\\[\\s*((C|c)omuna )?" + communeName + "(, " + eachCounty + ")?\\s*(\\||\\])");
                    Pattern countyCategoryPattern = Pattern.compile(
                        "\\[\\[Categor(y|(ie))\\:Orașe din județul " + replace(eachCounty, "-", "\\-") + ".*?\\]\\]");

                    for (Claim eachCompositeVillageClaim : compositeVillagesClaims) {
                        communeChanged = processSettlement(rowiki, commonsWiki, dwiki, captionProp, imageProp,
                            instanceOfProp, areaProp, altProp, popProp, pointInTimeProp, sirutaProp, nativeLabelProp,
                            meterEnt, squareKmEnt, roWpReference, eachCounty, countyLinkPattern, communeWikibaseItem,
                            communeName, communeType, communeChanged, villages, urbanSettlements, capitalEntity,
                            communeLinkPattern, eachCompositeVillageClaim);
                        crtSettlementName = null;
                    }

                    WikiTemplate initialTemplate = null;
                    String initialTemplateText = null;
                    int templateAnalysisStart = 0;

                    String rocommunearticle = communeWikibaseItem.getSitelinks().get("rowiki").getPageName();
                    String pageText = rowiki.getPageText(List.of(rocommunearticle)).stream().findFirst().orElse("");
                    String initialPageText = pageText;

                    String sirutaFromWd = null;
                    Set<Claim> sirutaClaims = communeClaims.get(sirutaProp);
                    if (null != sirutaClaims && !sirutaClaims.isEmpty()) {
                        for (Claim eachSirutaClaim : sirutaClaims) {
                            if ("statement".equals(eachSirutaClaim.getType())) {
                                sirutaFromWd = ((StringData) eachSirutaClaim.getMainsnak().getData()).getValue();
                            }
                        }
                    }

                    int previousTemplateAnalysisStart = -1;
                    do {
                        WikiTextParser initTextParser = new WikiTextParser();
                        ParseResult<PlainText> initTextParseRes =
                            initTextParser.parse(pageText.substring(templateAnalysisStart));

                        WikiTemplateParser templateParser = new WikiTemplateParser();
                        ParseResult<WikiTemplate> templateParseRes =
                            templateParser.parse(initTextParseRes.getUnparsedString());
                        initialTemplate = templateParseRes.getIdentifiedPart();
                        initialTemplateText = templateParseRes.getParsedString();

                        previousTemplateAnalysisStart = templateAnalysisStart;
                        templateAnalysisStart = templateAnalysisStart + initialTemplateText.length()
                            + initTextParseRes.getParsedString().length();
                    } while (!startsWithAny(lowerCase(initialTemplate.getTemplateTitle()), "cutie", "casetă", "infobox",
                        "infocaseta") && templateAnalysisStart < pageText.length()
                        && previousTemplateAnalysisStart != templateAnalysisStart);

                    if (startsWithAny(lowerCase(initialTemplate.getTemplateTitle()), "cutie", "casetă", "caseta", "infobox",
                        "infocaseta")) {
                        initialTemplate.setSingleLine(false);
                        initialTemplate.removeParam("nume");
                        initialTemplate.removeParam("județ");
                        initialTemplate.removeParam("resedinta");
                        initialTemplate.removeParam("reședința");
                        initialTemplate.removeParam("sate");
                        initialTemplate.removeParam("componență");
                        initialTemplate.removeParam("tip_așezare");
                        initialTemplate.removeParam("tip_asezare");
                        initialTemplate.removeParam("componenta");
                        if (StringUtils.equalsIgnoreCase("Infocaseta Așezare", initialTemplate.getTemplateTitle())) {
                            if (isBlank(initialTemplate.getParams().get("nume_nativ"))
                                && isNotBlank(initialTemplate.getParams().get("alt_nume"))) {
                                initialTemplate.setParam("nume_nativ", initialTemplate.getParams().get("alt_nume"));
                                initialTemplate.removeParam("alt_nume");
                            }
                        }
                        if (initialTemplate.getParamNames().contains("infodoc")) {
                            initialTemplate.setParam("infodoc", initialTemplate.getTemplateTitle());
                        }
                        for (String eachParam : initialTemplate.getParamNames()) {
                            String paramValue = initialTemplate.getParams().get(eachParam);
                            if (startsWithAny(eachParam, "comună", "oraș", "tip_subdiviziune", "nume_subdiviziune")
                                || eachParam.matches("p?\\d+") || isBlank(paramValue)) {
                                initialTemplate.removeParam(eachParam);
                            } else if (equalsIgnoreCase("Infocaseta Așezare", initialTemplate.getTemplateTitle())) {
                                Matcher ifMatcher = ifPattern.matcher(paramValue);
                                StringBuffer sbuf = new StringBuffer();
                                while (ifMatcher.find()) {
                                    ifMatcher.appendReplacement(sbuf, "{{subst:#if");
                                }
                                ifMatcher.appendTail(sbuf);
                                if (isNotBlank(sbuf)) {
                                    initialTemplate.setParam(eachParam, sbuf.toString());
                                }
                            }
                            if (startsWith(eachParam, "suprafață_totală_km2")) {
                                if (isNotBlank(paramValue) && (!communeWikibaseItem.getClaims().containsKey(areaProp)
                                    || null == communeWikibaseItem.getClaims().get(areaProp))) {
                                    Claim areaClaim =
                                        extractWdAreaDataFromParam(areaProp, squareKmEnt, paramValue, roWpReference);
                                    String claimId = dwiki.addClaim(communeWikibaseItem.getId(), areaClaim);
                                    if (null != claimId) {
                                        for (Set<Snak> eachRef : areaClaim.getReferences()) {
                                            dwiki.addReference(claimId, new ArrayList<Snak>(eachRef));
                                        }
                                    }
                                }
                                initialTemplate.removeParam(eachParam);
                            }
                            if (startsWith(eachParam, "sit-")) {
                                if (isNotBlank(paramValue) && (communeWikibaseItem.getClaims().containsKey(webAddrProp))
                                    || (!StringUtils.equals("sit-adresă", eachParam)
                                        && !communeWikibaseItem.getClaims().containsKey(webAddrProp)
                                        && !initialTemplate.getParams().containsKey("sit-adresă"))) {
                                    initialTemplate.removeParam(eachParam);
                                }
                            }
                            if (StringUtils.equals(eachParam, "altitudine")) {
                                if (isNotBlank(paramValue) && (!communeWikibaseItem.getClaims().containsKey(altProp)
                                    || null == communeWikibaseItem.getClaims().get(altProp))) {
                                    Claim altClaim =
                                        extractWdAltitudeDataFromParam(altProp, meterEnt, paramValue, roWpReference);
                                    String claimId = dwiki.addClaim(communeWikibaseItem.getId(), altClaim);
                                    if (null != claimId) {
                                        for (Set<Snak> eachRef : altClaim.getReferences()) {
                                            dwiki.addReference(claimId, new ArrayList<Snak>(eachRef));
                                        }
                                    }
                                }
                                initialTemplate.removeParam(eachParam);
                            }
                            if (startsWith(eachParam, "lider_") && !endsWith(eachParam, "_titlu")) {
                                Set<Claim> mayorClaim = communeWikibaseItem.getClaims().get(mayorProp);
                                if (null != mayorClaim && 0 < mayorClaim.size()) {
                                    initialTemplate.removeParam(eachParam);
                                }
                            }

                        }
                        removePopBlankParams(initialTemplate);

                    } else {
                        pageText = "{{Infocaseta Așezare}}" + pageText;
                        templateAnalysisStart = "{{Infocaseta Așezare}}".length();
                    }

                    String coatOfArms = initialTemplate.getParams().get("stemă");
                    if (isNotEmpty(coatOfArms)) {
                        Matcher coatOfArmsMatcher = imageInInfoboxPattern.matcher(coatOfArms);
                        if (contains(coatOfArms, "{{#property:P94}}")) {
                            initialTemplate.removeParam("stemă");
                        } else if (coatOfArmsMatcher.matches()) {
                            String coatOfArmsImageName = coatOfArmsMatcher.group(8);
                            Property coatOfArmsImageWikidataProperty = new Property("P94");
                            if (commonsWiki.exists(List.of("File:" + coatOfArmsImageName))[0] && !commonsWiki
                                .getCategories(List.of("File:" + coatOfArmsImageName), commonsWiki.new RequestHelper(),
                                    false)
                                .stream().map(l -> l.contains("Category:Coat of arms placeholders")).findFirst()
                                .orElse(false)) {
                                Set<Claim> coatOfArmsFromWd = communeClaims.get(coatOfArmsImageWikidataProperty);
                                if (null == coatOfArmsFromWd || coatOfArmsFromWd.isEmpty()) {
                                    dwiki.addClaim(communeWikibaseItem.getId(),
                                        new Claim(coatOfArmsImageWikidataProperty, new CommonsMedia(coatOfArmsImageName)));
                                } else {
                                    List<Claim> coatOfArmsFromWdList = new ArrayList<>(coatOfArmsFromWd);
                                    while (coatOfArmsFromWdList.size() > 1) {
                                        coatOfArmsFromWdList.remove(1);
                                    }
                                    coatOfArmsFromWdList.get(0).setValue(new CommonsMedia(coatOfArmsImageName));
                                    coatOfArmsFromWd = new HashSet<>(coatOfArmsFromWdList);
                                    dwiki.editClaim(coatOfArmsFromWdList.get(0));
                                }
                                initialTemplate.removeParam("stemă");
                            }
                        }
                    } else {
                        initialTemplate.removeParam("stemă");
                    }

                    Set<Claim> communeNativeLabelClaims = communeWikibaseItem.getClaims(nativeLabelProp);
                    if (null != communeNativeLabelClaims && !communeNativeLabelClaims.isEmpty()) {
                        List<WikiPart> nativeNameInTemplateParts = initialTemplate.getParam("nume_nativ");
                        if (null != nativeNameInTemplateParts && !nativeNameInTemplateParts.isEmpty()) {
                            String nativeNameInTemplateString =
                                nativeNameInTemplateParts.stream().map(WikiPart::toString).collect(joining());
                            String[] nativeNamesArray = nativeNameInTemplateString.split("<br\\s*/?\\s*>");
                            int matchedNativeNames = 0;
                            for (String eachNativeName : nativeNamesArray) {
                                for (Claim eachCommuneNativeLabelClaim : communeNativeLabelClaims) {
                                    if (StringUtils
                                        .equals(
                                            trim(LanguageString.class
                                                .cast(eachCommuneNativeLabelClaim.getMainsnak().getData()).getText()),
                                            trim(eachNativeName))) {
                                        matchedNativeNames++;
                                        break;
                                    }
                                }
                            }
                            if (matchedNativeNames == nativeNamesArray.length) {
                                initialTemplate.removeParam("nume_nativ");
                            }
                        }
                    }

                    String communeImage = initialTemplate.getParams().get("imagine");
                    if (isNotEmpty(communeImage)) {
                        Matcher imageMatcher = imageInInfoboxPattern.matcher(communeImage);
                        if (contains(communeImage, "{{#property:P18}}")) {
                            initialTemplate.removeParam("imagine");
                        } else if (imageMatcher.matches()) {
                            String imageName = URLDecoder.decode(imageMatcher.group(8), StandardCharsets.UTF_8.name());
                            if (endsWithAny(lowerCase(imageName), "jpg", "jpeg", "png")) {
                                Property imageWikidataProperty = new Property("P18");
                                if (commonsWiki.exists(List.of("File:" + imageName))[0]) {
                                    String imageClaimId = null;
                                    boolean captionQualifierFound = false;
                                    if (!communeClaims.containsKey(imageWikidataProperty)) {
                                        imageClaimId = dwiki.addClaim(communeWikibaseItem.getId(),
                                            new Claim(imageWikidataProperty, new CommonsMedia(imageName)));
                                        initialTemplate.removeParam("imagine");
                                    } else {
                                        Optional<Claim> possibleImageClaim =
                                            communeClaims.get(imageWikidataProperty).stream().findFirst();
                                        if (possibleImageClaim.isPresent() && StringUtils.equals(
                                            replace(possibleImageClaim.map(Claim::getValue).map(CommonsMedia.class::cast)
                                                .map(CommonsMedia::getFileName).get(), " ", "_"),
                                            replace(imageName, " ", "_"))) {
                                            Claim imageClaim = possibleImageClaim.get();
                                            imageClaimId = imageClaim.getId();
                                            captionQualifierFound = (null != imageClaim.getQualifiers().get(captionProp));
                                            initialTemplate.removeParam("imagine");
                                        }
                                    }
                                    String communeImageCaption =
                                        defaultIfEmpty(initialTemplate.getParams().get("imagine_descriere"),
                                            initialTemplate.getParams().get("descriere"));
                                    communeImageCaption = replace(communeImageCaption, "'''", "");

                                    if (isNotEmpty(communeImageCaption) && null != imageClaimId && !captionQualifierFound) {
                                        dwiki.addQualifier(imageClaimId, captionProp.getId(),
                                            new LanguageString("ro", communeImageCaption));
                                    }
                                    initialTemplate.removeParam("descriere");
                                    initialTemplate.removeParam("imagine_descriere");
                                }
                            }
                        }
                    } else {
                        initialTemplate.removeParam("imagine");
                        initialTemplate.removeParam("imagine_descriere");
                        initialTemplate.removeParam("descriere");
                        initialTemplate.removeParam("imagine_dimensiune");
                        initialTemplate.removeParam("dimensiune_imagine");

                        if (null != sirutaFromWd) {
                            initialTemplate.removeParam("cod_clasificare");
                            initialTemplate.removeParam("tip_cod_clasificare");
                            initialTemplate.removeParam("siruta");
                        }

                    }
                    if (communeClaims.containsKey(captionProp) && null != communeClaims.get(captionProp)) {
                        Set<Claim> imagesClaims = communeClaims.get(imageProp);
                        if (imagesClaims.size() == 1) {
                            Claim imageClaim = imagesClaims.iterator().next();
                            String imageClaimId = imageClaim.getId();
                            if (null != imageClaimId) {
                                Set<Claim> imageDescriptionClaims = communeClaims.get(captionProp);
                                for (Claim eachImageDescriptionClaim : imageDescriptionClaims) {
                                    dwiki.addQualifier(imageClaimId, captionProp.getId(),
                                        eachImageDescriptionClaim.getValue());
                                    dwiki.removeClaim(eachImageDescriptionClaim.getId());
                                    communeChanged = true;
                                }
                            }
                        }
                    }

                    String articleAfterInfobox = trim(pageText.substring(templateAnalysisStart));

                    while (startsWithAny(trim(articleAfterInfobox), "[[Fișier", "[[File", "[[Imagine", "[[Image",
                        "[[Categorie", "{{")) {
                        if (startsWithAny(trim(articleAfterInfobox), "[[Fișier", "[[File", "[[Imagine", "[[Image",
                            "[[Categorie")) {
                            int imgIdx = 0;
                            int depth = 0;
                            do {
                                if (substring(articleAfterInfobox, imgIdx, imgIdx + 2).equals("[[")) {
                                    depth++;
                                }
                                if (substring(articleAfterInfobox, imgIdx, imgIdx + 2).equals("]]")) {
                                    depth--;
                                }
                                imgIdx++;
                            } while (depth > 0 && imgIdx < articleAfterInfobox.length() - 1);
                            articleAfterInfobox = trim(substring(articleAfterInfobox, imgIdx + 2));
                        } else if (startsWith(trim(articleAfterInfobox), "{{")) {
                            articleAfterInfobox = trim(
                                substring(articleAfterInfobox, new WikiTemplate(articleAfterInfobox).getTemplateLength()));
                        }
                    }
                    if (null != initialTemplateText && null != initialTemplate.toString()) {
                        pageText = pageText.replace(initialTemplateText, initialTemplate.toString());
                    }
                    String firstParagraph = substringBefore(articleAfterInfobox, "\n");
                    String workingFirstParagraph = firstParagraph;

                    Matcher numberMatcher = manuallyFormatedNumberPattern.matcher(workingFirstParagraph);
                    StringBuffer formattedNumberBuf = new StringBuffer();
                    while (numberMatcher.find()) {
                        numberMatcher.appendReplacement(formattedNumberBuf,
                            "{{formatnum|" + numberMatcher.group(0).replaceAll("[\\.|\\s]", "") + "}}");
                    }
                    numberMatcher.appendTail(formattedNumberBuf);
                    workingFirstParagraph = formattedNumberBuf.toString();

                    Matcher sentenceMatcher = sentencePattern.matcher(workingFirstParagraph);
                    List<String> firstParagraphSentences = new ArrayList<>();
                    List<String> firstParagraphRefs = new ArrayList<>();
                    while (sentenceMatcher.find()) {
                        firstParagraphSentences.add(sentenceMatcher.group(1));
                        firstParagraphRefs.add(defaultIfBlank(
                            defaultIfBlank(sentenceMatcher.group(8), sentenceMatcher.group(10)), sentenceMatcher.group(6)));
                    }

                    String qualifier = "";
                    Matcher qualifierMatcher = qualifierPattern.matcher(firstParagraphSentences.get(0));
                    if (qualifierMatcher.find()) {
                        qualifier = qualifierMatcher.group(1);
                    }
                    String communeTypeLink = "";
                    if ("comună".equals(communeType)) {
                        communeTypeLink = "o [[Comunele României|comună]]";
                    } else if ("oraș".equals(communeType)) {
                        communeTypeLink = "un [[Lista orașelor din România|oraș]]";
                    } else if ("municipiu".equals(communeType)) {
                        communeTypeLink = "un [[Municipiile României|municipiu]]";
                        if (prependIfMissing(countyCapitalId, "Q")
                            .equals(prependIfMissing(communeWikibaseItem.getId(), "Q"))) {
                            communeTypeLink = "[[Municipiile României|municipiul]] de reședință";
                        }
                    }
                    String countyLink = "în [[județul " + eachCounty + "]]";
                    if (prependIfMissing(countyCapitalId, "Q").equals(prependIfMissing(communeWikibaseItem.getId(), "Q"))) {
                        countyLink = "al [[" + countyEntity.getSitelinks().get("rowiki").getPageName() + "|județului "
                            + (eachCounty.equalsIgnoreCase(communeName) ? "cu același nume" : eachCounty) + "]]";
                    }

                    List<Entry<String, String>> urbanSettlementsEntryList = new ArrayList<>(urbanSettlements.entrySet());
                    List<Entry<String, String>> villagesEntryList = new ArrayList<>(villages.entrySet());
                    if (!urbanSettlementsEntryList.isEmpty() || !villagesEntryList.isEmpty()) {
                        String intro = null;

                        if (1 == villagesEntryList.size() && urbanSettlementsEntryList.isEmpty()) {
                            intro = String.format("'''%s'''%s este %s %s, %s, [[România]]", communeName, qualifier,
                                communeTypeLink, countyLink, getHistoricalRegionLink(null, communeName, eachCounty));
                            if ("comună".equals(communeType)) {
                                intro = intro + String.format(", formată numai din satul de reședință%s",
                                    StringUtils.equals(communeName, villagesEntryList.get(0).getKey()) ? " cu același nume"
                                        : String.format(", [[%s|%s]]", villagesEntryList.get(0).getValue(),
                                            villagesEntryList.get(0).getKey()));
                            }
                        } else {
                            StringBuilder villagesList = new StringBuilder();
                            Collections.sort(urbanSettlementsEntryList, new Comparator<Entry<String, String>>() {

                                public int compare(Entry<String, String> o1, Entry<String, String> o2) {
                                    return collator.compare(o1.getKey() + defaultString(o1.getValue()),
                                        o2.getKey() + defaultString(o2.getValue()));
                                }
                            });
                            if (1 == urbanSettlements.size() && 0 < villages.size()) {
                                villagesList.append(", format din localitatea componentă ");
                                if (null == urbanSettlementsEntryList.get(0).getValue()) {
                                    villagesList.append(urbanSettlementsEntryList.get(0).getKey()).append(" (reședința)");
                                } else {
                                    villagesList.append(LinkUtils.createLink(urbanSettlementsEntryList.get(0).getValue(),
                                        urbanSettlementsEntryList.get(0).getKey()));
                                }
                            } else if (1 < urbanSettlements.size()) {
                                villagesList.append(", format din localitățile componente ");
                                int idx = 0;
                                for (Map.Entry<String, String> eachSettlementEntry : urbanSettlementsEntryList) {
                                    if (idx != 0 && idx < urbanSettlementsEntryList.size() - 1) {
                                        villagesList.append(", ");
                                    } else if (idx != 0 && idx == urbanSettlementsEntryList.size() - 1) {
                                        villagesList.append(" și ");
                                    }
                                    if (null == eachSettlementEntry.getValue()) {
                                        villagesList.append(eachSettlementEntry.getKey());
                                    } else {
                                        villagesList.append(LinkUtils.createLink(eachSettlementEntry.getValue(),
                                            eachSettlementEntry.getKey()));
                                    }
                                    if (null == eachSettlementEntry.getValue() || (null != capitalEntity.getSitelinks()
                                        && null != capitalEntity.getSitelinks().get("rowiki")
                                        && StringUtils.equals(eachSettlementEntry.getValue(),
                                            capitalEntity.getSitelinks().get("rowiki").getPageName()))) {
                                        villagesList.append(" (reședința)");
                                    }
                                    idx++;
                                }
                            }

                            if (0 < villages.size()) {
                                if (0 < urbanSettlements.size()) {
                                    villagesList.append(", și ");
                                } else if (0 == urbanSettlements.size()) {
                                    villagesList.append(", formată ");
                                }
                                villagesList.append("din sat");
                                villagesList.append(1 == villages.size() ? "ul " : "ele ");

                                Collections.sort(villagesEntryList, new Comparator<Entry<String, String>>() {

                                    public int compare(Entry<String, String> o1, Entry<String, String> o2) {
                                        return collator.compare(o1.getKey() + o1.getValue(), o2.getKey() + o2.getValue());
                                    }
                                });

                                int idx = 0;
                                for (Map.Entry<String, String> eachVillageEntry : villagesEntryList) {
                                    if (idx != 0 && idx < villagesEntryList.size() - 1) {
                                        villagesList.append(", ");
                                    } else if (idx != 0 && idx == villagesEntryList.size() - 1) {
                                        villagesList.append(" și ");
                                    }
                                    villagesList.append("[[").append(eachVillageEntry.getValue()).append('|')
                                        .append(eachVillageEntry.getKey()).append("]]");
                                    if (null == eachVillageEntry.getValue() || (null != capitalEntity.getSitelinks()
                                        && null != capitalEntity.getSitelinks().get("rowiki")
                                        && StringUtils.equals(eachVillageEntry.getValue(),
                                            capitalEntity.getSitelinks().get("rowiki").getPageName()))) {
                                        villagesList.append(" (reședința)");
                                    }
                                    idx++;
                                }
                            }
                            intro = String.format("'''%s'''%s este %s %s, %s, [[România]]%s", communeName, qualifier,
                                communeTypeLink, countyLink, getHistoricalRegionLink(null, communeName, eachCounty),
                                villagesList);
                        }
                        List<String> newFirstParagraphSentences = new ArrayList<>();
                        List<String> newFirstParagraphRefs = new ArrayList<>();
                        newFirstParagraphSentences.add(intro);
                        newFirstParagraphRefs.add("");

                        List<String> villageLinksRegexList = new ArrayList<>();
                        Set<String> allVillagesAndSettlements = new HashSet<>();
                        allVillagesAndSettlements.addAll(villages.values());
                        allVillagesAndSettlements.addAll(urbanSettlements.values());
                        for (String eachVillageLink : allVillagesAndSettlements) {
                            if (null == eachVillageLink) {
                                continue;
                            }
                            StringBuilder villageLinkRegexBuilder = new StringBuilder();
                            villageLinkRegexBuilder.append("(\\[\\[").append(replaceEach(eachVillageLink,
                                new String[] { "(", ")", "-", "." }, new String[] { "\\(", "\\)", "\\-", "\\." }));
                            villageLinkRegexBuilder.append("((\\|.*?)|(\\]\\])))");
                            villageLinksRegexList.add(villageLinkRegexBuilder.toString());
                        }
                        String villageLinksRegexExpression =
                            0 < villageLinksRegexList.size() ? join(villageLinksRegexList, '|')
                                : "nothing should match this regex";
                        Pattern villageLinksRegex = Pattern.compile(villageLinksRegexExpression);

                        for (int sentenceIdx = 0; sentenceIdx < firstParagraphSentences.size(); sentenceIdx++) {
                            String eachOldParagraphSetence = firstParagraphSentences.get(sentenceIdx);
                            String eachOldParagraphRefs = firstParagraphRefs.get(sentenceIdx);
                            if (countyLinkPattern.matcher(eachOldParagraphSetence).find()
                                || communeLinkPattern.matcher(eachOldParagraphSetence).find()
                                || villageLinksRegex.matcher(eachOldParagraphSetence).find()
                                || eachOldParagraphSetence.contains("'''" + communeName + "'''")
                                || isEmpty(eachOldParagraphSetence)) {

                                String[] locationPart = eachOldParagraphSetence.split("(situat|aflat)ă?");
                                if (locationPart.length > 1) {
                                    String transformedLocation =
                                        String.format("Se află %s", removeEnd(trim(locationPart[1]), "."));
                                    transformedLocation = transformedLocation
                                        .replaceAll("județului\\s+\\[\\[(J|j)udețul\\s+.*?(\\|.*?)?\\]\\]", "județului");
                                    transformedLocation = transformedLocation
                                        .replaceAll("\\[\\[(J|j)udețul\\s+.*?\\|județului\\s+.*?\\]\\]", "județului");
                                    transformedLocation = fullLocationPattern.matcher(transformedLocation).replaceAll("");
                                    newFirstParagraphSentences.add(transformedLocation);
                                    newFirstParagraphRefs.add("");
                                }

                                continue;
                            }
                            newFirstParagraphSentences.add(removeEnd(trim(eachOldParagraphSetence), "."));
                            newFirstParagraphRefs.add(eachOldParagraphRefs);
                        }

                        StringBuilder newFirstParagraphBuilder = new StringBuilder();
                        for (int sentenceIdx = 0; sentenceIdx < newFirstParagraphSentences.size(); sentenceIdx++) {
                            String aSentence = newFirstParagraphSentences.get(sentenceIdx);
                            newFirstParagraphBuilder.append(aSentence);
                            if (!startsWith(aSentence, "{{") || !endsWith(aSentence, "}}")) {
                                newFirstParagraphBuilder.append('.')
                                    .append(defaultString(newFirstParagraphRefs.get(sentenceIdx)));
                            }
                            newFirstParagraphBuilder.append(' ');
                        }
                        String newFirstParagraph = trim(newFirstParagraphBuilder.toString());
                        pageText = pageText.replace(firstParagraph, newFirstParagraph);

                        String sortingKey = replaceEach(communeName, new String[] { "ă", "â", "ș", "ț", "î" },
                            new String[] { "aă", "aăâ", "sș", "tț", "iî" });
                        Matcher countyCategoryMatcher = countyCategoryPattern.matcher(pageText);
                        if (countyCategoryMatcher.find()) {
                            pageText = countyCategoryMatcher
                                .replaceAll("[[Categorie:" + (communeType.equals("comună") ? "Comune" : "Orașe")
                                    + " din județul " + eachCounty + '|' + sortingKey + "]]");
                        }

                        List<String> desiredCommuneCategories = new ArrayList<>();
                        if (communeType.equals("oraș")) {
                            desiredCommuneCategories.add("Orașe din județul " + eachCounty);
                            if (urbanSettlements.containsKey(communeName)) {
                                desiredCommuneCategories.add("Localități urbane din județul " + eachCounty);
                            }
                        } else if (communeType.equals("municipiu")) {
                            desiredCommuneCategories.add("Municipii din România");
                            if (urbanSettlements.containsKey(communeName)) {
                                desiredCommuneCategories.add("Localități urbane din județul " + eachCounty);
                            }
                        } else {
                            desiredCommuneCategories.add("Comune din județul " + eachCounty);
                            if (1 == villages.size() && villages.containsKey(communeName)) {
                                desiredCommuneCategories.add("Sate din județul " + eachCounty);
                            }
                        }

                        pageText =
                            recategorize(pageText, new String[] { communeName }, rocommunearticle, desiredCommuneCategories);

                        pageText = rewritePoliticsAndAdministrationSection(pageText, countySymbol, communeName, communeType,
                            sirutaFromWd, communeWikibaseItem, wdcache);

                        communeNativeLabelClaims = communeWikibaseItem.getClaims(nativeLabelProp);
                        if (null == communeNativeLabelClaims || communeNativeLabelClaims.isEmpty()) {
                            LanguageString roNativeName = new LanguageString("ro", communeName);
                            Claim roNativeNameClaim = new Claim(nativeLabelProp, roNativeName);
                            dwiki.addClaim(communeWikibaseItem.getId(), roNativeNameClaim);
                        }

                        if (!StringUtils.equals(pageText, initialPageText)) {
                            rowiki.edit(rocommunearticle, pageText,
                                "Eliminare din infocasetă parametri migrați la Wikidata sau fără valoare, recategorisit, standardizat introducerea și secțiunile Demografie și Administrație. Greșit? Raportați [[Discuție Utilizator:Andrei Stroe|aici]].");
                            communeChanged = true;
                        }

                    }
                    if (communeChanged) {
                        long sleeptime = 10 + Math.round(20 * Math.random());
                        LOG.info("Sleeping {0}s", sleeptime);
                        Thread.sleep(1000l * sleeptime);
                    }
                }
            }
        } catch (WikibaseException | LoginException | ParseException e) {
            LOG.error("Error filling in villages", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            LOG.info("Stopped at {0}",
                Stream.of(crtSettlementName, crtCommuneName, crtCountyName).filter(Objects::nonNull).collect(joining(", ")));
            if (null != dwiki) {
                dwiki.logout();
            }
        }
    }

    private static void removePopBlankParams(WikiTemplate initialTemplate) {
        Pattern popBlankNumberExtractor = Pattern.compile("population_blank(\\d+)_title");
        List<String> blankPopParamNames = initialTemplate.getParamNames().stream()
            .filter(name -> name.startsWith("population_blank")).collect(Collectors.toList());
        Set<Integer> blankPopIndexesToRemove = blankPopParamNames.stream().filter(name -> name.endsWith("_title"))
            .filter(name -> initialTemplate.getParam(name).toString().contains("Recensământul anterior")).map(name -> {
                Matcher m = popBlankNumberExtractor.matcher(name);
                if (m.matches()) {
                    return Integer.parseInt(m.group(1));
                }
                return 0;
            }).collect(Collectors.toSet());
        blankPopIndexesToRemove.stream().forEach(idx -> {
            initialTemplate.removeParam(String.format("population_blank%d", idx));
            initialTemplate.removeParam(String.format("population_blank%d_title", idx));
        });
    }

    private static boolean processSettlement(Wiki rowiki, Wiki commonsWiki, Wikibase dwiki, Property captionProp,
                                             Property imageProp, Property instanceOfProp, Property areaProp,
                                             Property altProp, Property popProp, Property pointInTimeProp,
                                             Property sirutaProp, Property nativeLabelProp, Entity meterEnt,
                                             Entity squareKmEnt, Set<Snak> roWpReference, String eachCounty,
                                             Pattern countyLinkPattern, Entity communeWikibaseItem, String communeName,
                                             String communeType, boolean communeChanged, Map<String, String> villages,
                                             Map<String, String> urbanSettlements, Entity capitalEntity,
                                             Pattern communeLinkPattern, Claim eachCompositeVillageClaim)
        throws IOException, WikibaseException, ParseException, UnsupportedEncodingException, LoginException,
        InterruptedException {
        Item compositeVillage = (Item) eachCompositeVillageClaim.getMainsnak().getData();

        Entity villageEntity = dwiki.getWikibaseItemById(compositeVillage.getEnt());

        String villageType = "sat";
        Set<Claim> villageInstanceOfClaims = villageEntity.getClaims().get(instanceOfProp);
        for (Claim eachVillageInstanceOfClaim : villageInstanceOfClaims) {
            Item villageTypeItem = (Item) eachVillageInstanceOfClaim.getMainsnak().getData();
            if (removeStart(villageTypeItem.getEnt().getId(), "Q").equals("15921247")) {
                villageType = "localitate componentă";
                break;
            }
        }

        String villageName = trim(removeStart(
            removeEnd(removeEnd(villageEntity.getLabels().get("ro"), ", " + eachCounty), "(" + communeName + ")"),
            "Comuna "));
        crtSettlementName = villageName;
        LOG.info("Processing settlement {}, UAT {}, county{}; settlement type: {}", crtSettlementName, crtCommuneName, crtCountyName, villageType);

        String villageRelationWithCommune = "în comuna";
        if (!"comună".equalsIgnoreCase(communeType)) {
            villageRelationWithCommune = String.format("a%s %slui", "localitate componentă".equals(villageType) ? "" : "l",
                appendIfMissing(communeType, "u"));
        }

        String villageDescr =
            String.format("%s %s %s, județul %s, România", villageType, villageRelationWithCommune, communeName, eachCounty);

        boolean villageChanged = false;
        if (!StringUtils.equals(villageEntity.getLabels().get("ro"), villageName)) {
            dwiki.setLabel(villageEntity.getId(), "ro", villageName);
            communeChanged = villageChanged = true;
        }
        if (!StringUtils.equals(villageEntity.getDescriptions().get("ro"), villageDescr)) {
            dwiki.setDescription(villageEntity.getId(), "ro", villageDescr);
            communeChanged = villageChanged = true;
        }

        Map<String, String> mapToAdd = "localitate componentă".equals(villageType) ? urbanSettlements : villages;

        if (null == villageEntity.getSitelinks().get("rowiki")) {
            mapToAdd.put(villageName, null);
        } else {
            String rovillagearticle = villageEntity.getSitelinks().get("rowiki").getPageName();
            mapToAdd.put(villageName, rovillagearticle);

            String pageText = rowiki.getPageText(List.of(rovillagearticle)).stream().findFirst().orElse("");
            String initialPageText = pageText;
            pageText = commentedEol.matcher(pageText).replaceAll("");

            WikiTemplate initialTemplate = null;
            String initialTemplateText = null;
            int templateAnalysisStart = 0;

            int previousTemplateAnalysisStart = -1;
            do {
                WikiTextParser textParser = new WikiTextParser();
                ParseResult<PlainText> textParseRes = textParser.parse(pageText.substring(templateAnalysisStart));

                WikiTemplateParser templateParser = new WikiTemplateParser();
                ParseResult<WikiTemplate> templateParseRes = templateParser.parse(textParseRes.getUnparsedString());

                initialTemplate = templateParseRes.getIdentifiedPart();
                initialTemplateText = templateParseRes.getParsedString();

                previousTemplateAnalysisStart = templateAnalysisStart;
                templateAnalysisStart =
                    templateAnalysisStart + textParseRes.getParsedString().length() + initialTemplate.getTemplateLength();
            } while (!startsWithAny(lowerCase(initialTemplate.getTemplateTitle()), "cutie", "casetă", "infobox",
                "infocaseta") && templateAnalysisStart < pageText.length()
                && previousTemplateAnalysisStart != templateAnalysisStart);

            if (startsWithAny(lowerCase(initialTemplate.getTemplateTitle()), "cutie", "casetă", "caseta", "infobox",
                "infocaseta")) {
                initialTemplate.setSingleLine(false);
                initialTemplate.removeParam("nume");
                initialTemplate.removeParam("județ");
                initialTemplate.removeParam("resedinta");
                initialTemplate.removeParam("tip_așezare");
                initialTemplate.removeParam("tip_asezare");
                initialTemplate.removeParam("reședința");
                if (null != villageEntity.getClaims()) {
                    Set<Claim> popClaims = villageEntity.getClaims().get(popProp);
                    if (null != popClaims && 0 < popClaims.size()) {
                        for (Claim eachPopClaim : popClaims) {
                            Set<Snak> pointsInTime = eachPopClaim.getQualifiers().get(pointInTimeProp);
                            if (null != pointsInTime && 0 < pointsInTime.size()) {
                                for (Snak eachPointInTimeSnak : pointsInTime) {
                                    Time pointInTimeData = (Time) eachPointInTimeSnak.getData();
                                    if (null != pointInTimeData && pointInTimeData.getYear() >= 2011) {
                                        initialTemplate.removeParam("populație");
                                        initialTemplate.removeParam("populatie");
                                        initialTemplate.removeParam("recensământ");
                                        initialTemplate.removeParam("recensamant");
                                    }
                                }
                            }
                        }
                    }

                    Set<Claim> villageNativeLabelClaims = villageEntity.getClaims(nativeLabelProp);
                    if (null != villageNativeLabelClaims && !villageNativeLabelClaims.isEmpty()) {
                        List<WikiPart> nativeNameInTemplateParts = initialTemplate.getParam("nume_nativ");
                        if (null != nativeNameInTemplateParts && !nativeNameInTemplateParts.isEmpty()) {
                            String nativeNameInTemplateString =
                                nativeNameInTemplateParts.stream().map(x -> x.toString()).collect(joining());
                            String[] nativeNamesArray = nativeNameInTemplateString.split("<br\\s*/?\\s*>");
                            int matchedNativeNames = 0;
                            for (String eachNativeName : nativeNamesArray) {
                                eachNativeName = removeStart(removeEnd(eachNativeName, "''"), "''");
                                for (Claim eachVillageNativeLabelClaim : villageNativeLabelClaims) {
                                    if (StringUtils
                                        .equals(
                                            trim(LanguageString.class
                                                .cast(eachVillageNativeLabelClaim.getMainsnak().getData()).getText()),
                                            trim(eachNativeName))) {
                                        matchedNativeNames++;
                                        break;
                                    }
                                }
                            }
                            if (matchedNativeNames == nativeNamesArray.length) {
                                initialTemplate.removeParam("nume_nativ");
                            }
                        }
                    }

                    Set<Claim> sirutaClaims = villageEntity.getClaims().get(sirutaProp);
                    if (null != sirutaClaims && !sirutaClaims.isEmpty()) {
                        for (Claim eachSirutaClaim : sirutaClaims) {
                            if ("statement".equals(eachSirutaClaim.getType())) {
                                initialTemplate.removeParam("cod_clasificare");
                                initialTemplate.removeParam("tip_cod_clasificare");
                            }
                        }
                    }
                }
                if (StringUtils.equalsIgnoreCase("Infocaseta Așezare", initialTemplate.getTemplateTitle())) {
                    if (isBlank(initialTemplate.getParams().get("nume_nativ"))
                        && isNotBlank(initialTemplate.getParams().get("alt_nume"))) {
                        initialTemplate.setParam("nume_nativ", initialTemplate.getParams().get("alt_nume"));
                        initialTemplate.removeParam("alt_nume");
                    }
                }
                if (initialTemplate.getParamNames().contains("infodoc")) {
                    initialTemplate.setParam("infodoc", initialTemplate.getTemplateTitle());
                }
                for (String eachParam : initialTemplate.getParamNames()) {
                    String paramValue = initialTemplate.getParams().get(eachParam);
                    if (startsWithAny(eachParam, "comună", "oraș", "tip_subdiviziune", "nume_subdiviziune")
                        || eachParam.matches("p?\\d+") || isBlank(paramValue)) {
                        initialTemplate.removeParam(eachParam);
                    } else if (equalsIgnoreCase("Infocaseta Așezare", initialTemplate.getTemplateTitle())) {
                        Matcher ifMatcher = ifPattern.matcher(paramValue);
                        StringBuffer sbuf = new StringBuffer();
                        while (ifMatcher.find()) {
                            ifMatcher.appendReplacement(sbuf, "{{subst:#if");
                        }
                        ifMatcher.appendTail(sbuf);
                        if (isNotBlank(sbuf)) {
                            initialTemplate.setParam(eachParam, sbuf.toString());
                        }
                    }
                    if (startsWith(eachParam, "suprafață_totală_km2")) {
                        if (isNotBlank(paramValue) && (!villageEntity.getClaims().containsKey(areaProp)
                            || null == villageEntity.getClaims().get(areaProp))) {
                            Claim areaClaim = extractWdAreaDataFromParam(areaProp, squareKmEnt, paramValue, roWpReference);
                            String claimId = dwiki.addClaim(villageEntity.getId(), areaClaim);
                            if (null != claimId) {
                                for (Set<Snak> eachRef : areaClaim.getReferences()) {
                                    dwiki.addReference(claimId, new ArrayList<Snak>(eachRef));
                                }
                            }
                        }
                        initialTemplate.removeParam(eachParam);
                    }
                    if (StringUtils.equals(eachParam, "altitudine")) {
                        if (isNotBlank(paramValue) && (!villageEntity.getClaims().containsKey(altProp)
                            || null == villageEntity.getClaims().get(altProp))) {
                            Claim altClaim = extractWdAltitudeDataFromParam(altProp, meterEnt, paramValue, roWpReference);
                            String claimId = dwiki.addClaim(villageEntity.getId(), altClaim);
                            if (null != claimId) {
                                for (Set<Snak> eachRef : altClaim.getReferences()) {
                                    dwiki.addReference(claimId, new ArrayList<Snak>(eachRef));
                                }
                            }
                        }
                        initialTemplate.removeParam(eachParam);
                    }

                }
            } else {
                pageText = "{{Infocaseta Așezare}}" + pageText;
                templateAnalysisStart = "{{Infocaseta Așezare}}".length();
            }

            String articleAfterInfobox = trim(pageText.substring(templateAnalysisStart));

            while (startsWithAny(trim(articleAfterInfobox), "[[Fișier", "[[File", "[[Imagine", "[[Image", "[[Categorie",
                "{{")) {
                if (startsWithAny(trim(articleAfterInfobox), "[[Fișier", "[[File", "[[Imagine", "[[Image", "[[Categorie")) {
                    int imgIdx = 0;
                    int depth = 0;
                    do {
                        if (substring(articleAfterInfobox, imgIdx, imgIdx + 2).equals("[[")) {
                            depth++;
                        }
                        if (substring(articleAfterInfobox, imgIdx, imgIdx + 2).equals("]]")) {
                            depth--;
                        }
                        imgIdx++;
                    } while (depth > 0 && imgIdx < articleAfterInfobox.length() - 1);
                    articleAfterInfobox = trim(substring(articleAfterInfobox, imgIdx + 2));
                } else if (startsWith(trim(articleAfterInfobox), "{{")) {
                    articleAfterInfobox =
                        trim(substring(articleAfterInfobox, new WikiTemplate(articleAfterInfobox).getTemplateLength()));
                }
            }
            String villageImage = initialTemplate.getParams().get("imagine");
            if (isNotEmpty(villageImage)) {
                Matcher imageMatcher = imageInInfoboxPattern.matcher(villageImage);
                if (contains(villageImage, "{{#property:P18}}")) {
                    initialTemplate.removeParam("imagine");
                } else if (imageMatcher.matches()) {
                    String imageName = imageMatcher.group(8);
                    imageName = URLDecoder.decode(imageName, "UTF-8");
                    Property imageWikidataProperty = new Property("P18");
                    if (commonsWiki.exists(List.of("File:" + imageName))[0]) {
                        String imageClaimId = null;
                        boolean captionQualifierFound = false;
                        if (!villageEntity.getClaims().containsKey(imageWikidataProperty)) {
                            imageClaimId = dwiki.addClaim(villageEntity.getId(),
                                new Claim(imageWikidataProperty, new CommonsMedia(imageName)));
                            initialTemplate.removeParam("imagine");
                        } else {
                            Optional<Claim> possibleImageClaim =
                                villageEntity.getClaims().get(imageWikidataProperty).stream().findFirst();
                            if (possibleImageClaim.isPresent()
                                && StringUtils
                                    .equals(
                                        replace(possibleImageClaim.map(Claim::getValue).map(CommonsMedia.class::cast)
                                            .map(CommonsMedia::getFileName).get(), " ", "_"),
                                        replace(imageName, " ", "_"))) {
                                Claim imageClaim = possibleImageClaim.get();
                                imageClaimId = imageClaim.getId();
                                Set<Snak> captions = imageClaim.getQualifiers().get(captionProp);
                                if (null != captions) {
                                    captionQualifierFound = true;
                                }
                                initialTemplate.removeParam("imagine");
                            }
                        }
                        String villageImageCaption = defaultIfEmpty(initialTemplate.getParams().get("imagine_descriere"),
                            initialTemplate.getParams().get("descriere"));
                        villageImageCaption = replace(villageImageCaption, "'''", "");
                        if (isNotEmpty(villageImageCaption) && null != imageClaimId && !captionQualifierFound) {
                            dwiki.addQualifier(imageClaimId, captionProp.getId(),
                                new LanguageString("ro", villageImageCaption));
                        }
                        initialTemplate.removeParam("descriere");
                        initialTemplate.removeParam("imagine_descriere");
                    }
                }
            } else {
                initialTemplate.removeParam("imagine");
                initialTemplate.removeParam("imagine_descriere");
                initialTemplate.removeParam("descriere");
                initialTemplate.removeParam("imagine_dimensiune");
                initialTemplate.removeParam("dimensiune_imagine");
            }
            if (villageEntity.getClaims().containsKey(captionProp) && null != villageEntity.getClaims().get(captionProp)) {
                Set<Claim> imagesClaims = villageEntity.getClaims().get(imageProp);
                if (imagesClaims.size() == 1) {
                    Claim imageClaim = imagesClaims.iterator().next();
                    String imageClaimId = imageClaim.getId();
                    if (null != imageClaimId) {
                        Set<Claim> imageDescriptionClaims = villageEntity.getClaims().get(captionProp);
                        for (Claim eachImageDescriptionClaim : imageDescriptionClaims) {
                            WikibaseData imageDescriptionLangString = eachImageDescriptionClaim.getValue();
                            dwiki.addQualifier(imageClaimId, captionProp.getId(), imageDescriptionLangString);
                            dwiki.removeClaim(eachImageDescriptionClaim.getId());
                            villageChanged = true;
                        }
                    }
                }
            }

            if (null != initialTemplateText && null != initialTemplate.toString()) {
                pageText = pageText.replace(initialTemplateText, initialTemplate.toString());
            }

            String firstParagraph = substringBefore(articleAfterInfobox, "\n");
            String restOfArticle = substringAfter(articleAfterInfobox, "\n");
            if (isEmpty(firstParagraph)) {
                firstParagraph = initialTemplate.getBeforeText();
            }
            while (startsWith(trim(firstParagraph), "{{")) {
                WikiTemplateParser extraTemplateParser = new WikiTemplateParser();
                ParseResult<WikiTemplate> extraTemplateParseRes = extraTemplateParser.parse(trim(restOfArticle));

                firstParagraph = trim(substringBefore(extraTemplateParseRes.getUnparsedString(), "\n"));
                restOfArticle =
                    substringAfter(substring(trim(restOfArticle), extraTemplateParseRes.getParsedString().length()), "\n");
            }
            while (startsWith(trim(firstParagraph), "[[")) {
                int inLink = 1;
                String imageSpace = trim(firstParagraph + restOfArticle);
                int idx;
                for (idx = 1; idx < imageSpace.length() - 1 && inLink > 0; idx++) {
                    if (imageSpace.charAt(idx) == ']' && imageSpace.charAt(idx + 1) == ']') {
                        inLink--;
                        continue;
                    }
                    if (imageSpace.charAt(idx) == '[' && imageSpace.charAt(idx + 1) == '[') {
                        inLink++;
                        continue;
                    }
                }
                restOfArticle = trim(substring(imageSpace, idx + 1));
                firstParagraph = substringBefore(restOfArticle, "\n");
                restOfArticle = substringAfter(restOfArticle, "\n");
            }
            String workingFirstParagraph = firstParagraph;
            Matcher numberMatcher = manuallyFormatedNumberPattern.matcher(workingFirstParagraph);
            StringBuffer formattedNumberBuf = new StringBuffer();
            while (numberMatcher.find()) {
                numberMatcher.appendReplacement(formattedNumberBuf,
                    "{{formatnum|" + numberMatcher.group(0).replaceAll("[\\.|\\s]", "") + "}}");
            }
            numberMatcher.appendTail(formattedNumberBuf);
            workingFirstParagraph = formattedNumberBuf.toString();

            Matcher sentenceMatcher = sentencePattern.matcher(workingFirstParagraph);
            List<String> firstParagraphSentences = new ArrayList<String>();
            List<String> firstParagraphRefs = new ArrayList<String>();
            while (sentenceMatcher.find()) {
                firstParagraphSentences.add(sentenceMatcher.group(1));
                firstParagraphRefs.add(defaultIfBlank(defaultIfBlank(sentenceMatcher.group(8), sentenceMatcher.group(10)),
                    sentenceMatcher.group(6)));
            }

            String qualifier = "";
            Matcher qualifierMatcher = qualifierPattern.matcher(firstParagraphSentences.get(0));
            if (qualifierMatcher.find()) {
                qualifier = qualifierMatcher.group(1);
            }

            List<String> newFirstParagraphSentences = new ArrayList<String>();
            List<String> newFirstParagraphRefs = new ArrayList<String>();

            String villageIndefArticle = startsWith(villageType, "localitate") ? "o" : "un";
            String communeLink = communeWikibaseItem.getSitelinks().get("rowiki").getPageName();
            String articulatedCommuneType = "comuna";
            String genitiveCommuneType = "comunei";
            String in = "în";
            if (!"comună".equals(communeType)) {
                genitiveCommuneType = appendIfMissing(communeType, "u") + "lui";
                articulatedCommuneType = genitiveCommuneType;
                if ("sat".equalsIgnoreCase(villageType)) {
                    in = "ce aparține";
                } else {
                    in = "a";
                }
            }
            String villageIntro = String.format("este %s %s %s [[%s|%s %s]] din [[județul %s]], %s, [[România]]",
                villageIndefArticle, villageType, in, communeLink, articulatedCommuneType,
                StringUtils.equals(communeName, villageName) ? "cu același nume" : communeName, eachCounty,
                getHistoricalRegionLink(villageName, communeName, eachCounty));

            String l = "sat".equalsIgnoreCase(villageType) ? "l" : "";
            String articulatedVillageType = "sat".equals(villageType) ? "satul" : "localitatea componentă";
            String residenceIntro =
                String.format("este %s de reședință a%s [[%s|%s %s]] din [[județul %s]], %s, [[România]]",
                    articulatedVillageType, l, communeLink, genitiveCommuneType,
                    StringUtils.equals(communeName, villageName) ? "cu același nume" : communeName, eachCounty,
                    getHistoricalRegionLink(villageName, communeName, eachCounty));

            String intro = StringUtils.equals(capitalEntity.getId(), villageEntity.getId()) ? residenceIntro : villageIntro;

            newFirstParagraphSentences.add("'''" + villageName + "'''" + qualifier + " " + intro);
            newFirstParagraphRefs.add("");

            for (int sentenceIdx = 0; sentenceIdx < firstParagraphSentences.size(); sentenceIdx++) {
                String eachOldParagraphSentence = firstParagraphSentences.get(sentenceIdx);
                String eachOldParagraphRef = firstParagraphRefs.get(sentenceIdx);
                if (countyLinkPattern.matcher(eachOldParagraphSentence).find()
                    || communeLinkPattern.matcher(eachOldParagraphSentence).find()
                    || eachOldParagraphSentence.contains("'''" + villageName + "'''") || isEmpty(eachOldParagraphSentence)) {

                    String[] locationPart = eachOldParagraphSentence.split("(aflat|situat)ă?");
                    if (locationPart.length > 1) {
                        Pattern[] locationTransformerPatterns = new Pattern[] {
                            Pattern.compile("((\\[\\[județ\\]\\])|județ)ului\\s+\\[\\[(J|j)udețul\\s+.*?(\\|.*?)?\\]\\]"),
                            Pattern.compile("\\[\\[(J|j)udețul\\s+.*?\\|județului\\s+.*?\\]\\]") };
                        String transformedLocation = "Se află " + removeEnd(trim(locationPart[1]), ".");
                        for (Pattern eachLocationTransformerPattern : locationTransformerPatterns) {
                            Matcher locationTransformationMatcher =
                                eachLocationTransformerPattern.matcher(transformedLocation);
                            transformedLocation = locationTransformationMatcher.replaceAll("județului");
                        }
                        transformedLocation = fullLocationPattern.matcher(transformedLocation).replaceAll("");
                        newFirstParagraphSentences.add(transformedLocation);
                        newFirstParagraphRefs.add("");
                    }
                    continue;
                }
                eachOldParagraphSentence = removeEnd(trim(eachOldParagraphSentence), ".");
                eachOldParagraphSentence = fullLocationPattern.matcher(eachOldParagraphSentence).replaceAll("");
                newFirstParagraphSentences.add(eachOldParagraphSentence);
                newFirstParagraphRefs.add(eachOldParagraphRef);
            }

            StringBuilder newFirstParagraphBuilder = new StringBuilder();
            for (int sentenceIdx = 0; sentenceIdx < newFirstParagraphSentences.size(); sentenceIdx++) {
                newFirstParagraphBuilder.append(newFirstParagraphSentences.get(sentenceIdx)).append('.')
                    .append(defaultString(newFirstParagraphRefs.get(sentenceIdx))).append(' ');
            }
            String newFirstParagraph = trim(newFirstParagraphBuilder.toString());
            pageText = pageText.replace(firstParagraph, newFirstParagraph);

            List<String> desiredCategories = new ArrayList<>();
            if (startsWith(villageType, "localitate")) {
                desiredCategories.add(String.format("Localități urbane din județul %s", eachCounty));
            } else {
                desiredCategories.add(String.format("Sate din județul %s", eachCounty));
            }

            pageText =
                recategorize(pageText, new String[] { villageName, communeName }, rovillagearticle, desiredCategories);

            if (!StringUtils.equals(pageText, initialPageText)) {
                rowiki.edit(rovillagearticle, pageText,
                    "Eliminare din infocasetă parametri migrați la Wikidata sau fără valoare, revizitat introducere standard, recategorisit. Greșit? Raportați [[Discuție Utilizator:Andrei Stroe|aici]].");
                villageChanged = true;
            }
        }

        Set<Claim> villageNativeLabelClaims = villageEntity.getClaims(nativeLabelProp);
        if (null == villageNativeLabelClaims || 0 == villageNativeLabelClaims.size()) {
            LanguageString roNativeName = new LanguageString("ro", villageName);
            Claim roNativeNameClaim = new Claim(nativeLabelProp, roNativeName);
            dwiki.addClaim(compositeVillage.getEnt().getId(), roNativeNameClaim);
        }

        if (villageChanged) {
            long sleeptime = 10 + Math.round(10 * Math.random());
            LOG.info("Sleeping {}s", sleeptime);
            Thread.sleep(1000l * sleeptime);
        }
        return communeChanged;
    }

    private static final Pattern POLITICS_SECTION_PATTERN = Pattern.compile(
        "==\\s*(Politică|Politică și administrație|Administrație|Administrație și politică|Administrați(e|a) locală)\\s*==");
    private static final Pattern DEMOGRAPHY_SECTION_PATTERN =
        Pattern.compile("==\\s*(Populați(?:e|a)|Demografie)\\s*==.*?==", Pattern.DOTALL);
    private static final Pattern ALREADY_GENERATED_SECTION_PATTERN = Pattern.compile(
        "\\s?<!-- secțiune administrație -->.*?<!--sfârșit secțiune administrație-->\\s*(\\{\\{Componență politică\\s*\\|.*?\\}\\}|\\{\\|.*?\\|\\})",
        Pattern.DOTALL);
    private static final Pattern ALREADY_GENERATED_MODIFIED_SECTION_PATTERN = Pattern.compile(
        "\\s?<!--\\s*secțiune administrație modificată manual\\s*-->.*?<!--sfârșit secțiune administrație-->\\s*\\{\\{Componență politică\\s*\\|.*?\\}\\}",
        Pattern.DOTALL);
    private static final Pattern CATEGORY_PATTERN =
        Pattern.compile("\\[\\[(?:C|c)ategor(?:y|ie):([^\\]\\|]*)(?:\\|(.*?))?\\]\\]");

    private static String recategorize(String pageText, String[] namingChain, String articleTitle,
                                       List<String> desiredCategories) {
        StringBuffer sbuf = new StringBuffer();
        Matcher categoryMatcher = CATEGORY_PATTERN.matcher(pageText);

        int catLocation = -1;
        while (categoryMatcher.find()) {
            String catName = categoryMatcher.group(1);
            String catKey = categoryMatcher.group(2);

            if (catLocation < 0) {
                catLocation = categoryMatcher.start();
            }
            if (startsWith(catName, "Localități din județul ")) {
                categoryMatcher.appendReplacement(sbuf, "");
                continue;
            }
            String newCatName = catName;
            String newCatKey = catKey;
            if (articleTitle.equals(catName)) {
                newCatKey = " ";
            } else {
                newCatKey = join(namingChain, ", ");
            }
            desiredCategories.remove(newCatName);

            StringBuilder newCatCode = new StringBuilder("[[Categorie:");
            newCatCode.append(newCatName);
            newCatCode.append('|');
            newCatCode.append(newCatKey);
            newCatCode.append("]]");

            categoryMatcher.appendReplacement(sbuf, newCatCode.toString());
        }
        categoryMatcher.appendTail(sbuf);
        for (String eachNewCat : desiredCategories) {
            StringBuilder newCatCode = new StringBuilder("[[Categorie:");
            newCatCode.append(eachNewCat);
            newCatCode.append('|');
            newCatCode.append(join(namingChain, ", "));
            newCatCode.append("]]\n");
            if (0 <= catLocation) {
                sbuf.insert(catLocation, newCatCode.toString());
            } else {
                sbuf.append(newCatCode.toString());
            }
        }

        return sbuf.toString();
    }

    private static String rewritePoliticsAndAdministrationSection(String pageText, String countySymbol, String communeName,
                                                                  String communeType, String siruta, Entity communeItem,
                                                                  WikidataEntitiesCache wdcache)
        throws IOException, WikibaseException {
        String communeNameRegexForMongo = replace(communeName, "ș", "(ș|ş)");
        communeNameRegexForMongo = replace(communeNameRegexForMongo, "ț", "[țţ]");
        communeNameRegexForMongo = replace(communeNameRegexForMongo, "Ș", "[ȘŞ]");
        communeNameRegexForMongo = replace(communeNameRegexForMongo, "Ț", "[ȚŢ]");
        communeNameRegexForMongo = replace(communeNameRegexForMongo, "â", "[âî]");
        communeNameRegexForMongo =
            replaceEach(communeNameRegexForMongo, new String[] { " ", "-" }, new String[] { "( |\\-)", "( |\\-)" });
        communeNameRegexForMongo = "^" + communeNameRegexForMongo + "$";

        int replacePosition = -1, insertPosition = -1;
        Matcher politicsSectionMatcher = POLITICS_SECTION_PATTERN.matcher(pageText);
        String politicsSectionTitle = "== Politică și administrație ==";
        if (politicsSectionMatcher.find()) {
            replacePosition = politicsSectionMatcher.start();
            politicsSectionTitle = politicsSectionMatcher.group();
        }

        if (replacePosition < 0) {
            Matcher demographySectionMatcher = DEMOGRAPHY_SECTION_PATTERN.matcher(pageText);
            if (demographySectionMatcher.find()) {
                insertPosition = demographySectionMatcher.end() - "==".length();
            }
        }

        communeType = endsWith(communeType, "ă") ? appendIfMissing(removeEnd(communeType, "ă"), "a")
            : appendIfMissing(appendIfMissing(communeType, "u"), "l");

        MongoClient mongoClient = new MongoClient("localhost", 57017);
        MongoDatabase electionsDb = mongoClient.getDatabase("elections2020");
        MongoCollection<Document> electionsColl = electionsDb.getCollection("cl");
        FindIterable<Document> electionResultsItrble = electionsColl.find(Filters.eq("siruta", siruta));
        electionResultsItrble.sort(new BasicDBObject("seats", -1));

        final List<Object[]> electionResults = new ArrayList<Object[]>();
        electionResultsItrble.forEach(new Block<Document>() {

            @Override
            public void apply(Document t) {
                Object[] crtResult = new Object[3];
                crtResult[0] = t.getString("shortName");
                crtResult[1] = t.getString("fullName");
                crtResult[2] = t.get("seats");
                electionResults.add(crtResult);
            }
        });

        mongoClient.close();

        WikiTemplate councillorsTemplate = new WikiTemplate();
        councillorsTemplate.setSingleLine(false);
        councillorsTemplate.setTemplateTitle("Componență politică");
        councillorsTemplate.setParam("eticheta_compoziție", "Componența Consiliului");
        councillorsTemplate.setParam("eticheta_mandate", "Consilieri");
        int mandatesCount = 0;
        for (int i = 0; i < electionResults.size(); i++) {
            Object[] eachResult = electionResults.get(i);
            mandatesCount += (Integer) eachResult[2];
            councillorsTemplate.setParam("nume_scurt" + String.valueOf(1 + i), eachResult[0].toString());
            councillorsTemplate.setParam("nume_complet" + String.valueOf(1 + i), eachResult[1].toString());
            councillorsTemplate.setParam("mandate" + String.valueOf(1 + i), eachResult[2].toString());
        }

        String partyIntro = "";
        Set<Claim> mayors = communeItem.getClaims().get(WikibasePropertyFactory.getWikibaseProperty("P6"));
        Claim mayorClaim = null;
        for (Claim eachMayorClaim : mayors) {
            if (null == mayorClaim || Rank.PREFERRED.equals(eachMayorClaim.getRank())) {
                mayorClaim = eachMayorClaim;
            }
        }
        if (null != mayorClaim) {
            Item mayorItem = (Item) mayorClaim.getValue();
            Entity mayorEnt = wdcache.get(mayorItem.getEnt());
            Set<Claim> partyClaims = mayorEnt.getClaims().get(WikibasePropertyFactory.getWikibaseProperty("P102"));
            Claim partyClaim = null;
            for (Claim eachPartyClaim : partyClaims) {
                if (null == partyClaim || Rank.PREFERRED.equals(eachPartyClaim.getRank())) {
                    partyClaim = eachPartyClaim;
                }
            }
            if (null != partyClaim) {
                Entity partyEnt = ((Item) partyClaim.getValue()).getEnt();
                if (null != partyEnt && !partyEnt.getId().endsWith("327591")) {
                    partyIntro = "de la";
                }
            }
        }
        String section = String.format(
            "%n<!-- secțiune administrație -->%s %s este administrat%s de un primar și un consiliu local compus din %d consilieri. Primarul, {{Date înlănțuite de la Wikidata|P6}}, %s {{Date înlănțuite de la Wikidata|P6|P102}}, este în funcție din {{Date înlănțuite de la Wikidata|P6|_P580}}. Începând cu [[Alegeri locale în România, 2020|alegerile locale din 2020]], consiliul local are următoarea componență pe partide politice:<ref>{{Citat web|url=https://prezenta.roaep.ro/locale27092020/data/json/sicpv/pv/pv_%s_final.json|format=Json|titlu=Rezultatele finale ale alegerilor locale din 2020 |publisher=Autoritatea Electorală Permanentă|accessdate=2020-11-02}}</ref><!--sfârșit secțiune administrație-->%n%s",
            StringUtils.capitalize(communeType), communeName, "Comuna".equalsIgnoreCase(communeType) ? "ă" : "",
            mandatesCount, partyIntro, StringUtils.lowerCase(countySymbol), councillorsTemplate);

        if (replacePosition >= 0) {
            Matcher alreadyGeneratedAndManuallyModifiedSectionMatcher =
                ALREADY_GENERATED_MODIFIED_SECTION_PATTERN.matcher(pageText);
            if (alreadyGeneratedAndManuallyModifiedSectionMatcher.find(replacePosition)) {
                return pageText;
            }
            Matcher alreadyGeneratedSectionMatcher = ALREADY_GENERATED_SECTION_PATTERN.matcher(pageText);
            StringBuffer sbuf = new StringBuffer();
            boolean foundOldSection = false;
            if (alreadyGeneratedSectionMatcher.find(replacePosition)) {
                alreadyGeneratedSectionMatcher.appendReplacement(sbuf, section);
                foundOldSection = true;
            }
            alreadyGeneratedSectionMatcher.appendTail(sbuf);
            pageText = sbuf.toString();
            if (!foundOldSection) {
                sbuf = new StringBuffer(pageText);
                sbuf.insert(politicsSectionMatcher.end(), section);
            }
            pageText = sbuf.toString().trim();
        } else if (insertPosition > 0) {
            StringBuilder sbuf = new StringBuilder(pageText);
            sbuf.insert(insertPosition, politicsSectionTitle + section + '\n');
            pageText = sbuf.toString().trim();
        } else {
            StringBuilder sbuf = new StringBuilder(pageText);
            sbuf.append(politicsSectionTitle).append(section).append('\n');
            pageText = sbuf.toString().trim();
        }

        return pageText;
    }

    private static Claim extractWdAltitudeDataFromParam(Property prop, Entity meterEnt, String data,
                                                        Set<Snak> baseReferences) {
        Matcher rangeMatcher = numberRangePattern.matcher(data);
        List<String> numbersIdenfifiedList = new ArrayList<String>();
        if (rangeMatcher.find()) {
            numbersIdenfifiedList.add(rangeMatcher.group(1));
            if (isNotBlank(rangeMatcher.group(2))) {
                numbersIdenfifiedList.add(rangeMatcher.group(3));
            }
        }

        String ref = null;
        Matcher simpleRefMatcher = simpleRefPattern.matcher(data);
        if (simpleRefMatcher.find()) {
            ref = simpleRefMatcher.group(1);
            data = replace(data, simpleRefMatcher.group(0), "");
        }

        Quantity altQty = new Quantity();
        double[] alt = new double[numbersIdenfifiedList.size()];
        for (int i = 0; i < numbersIdenfifiedList.size(); i++) {
            alt[i] = Double.parseDouble(trim(numbersIdenfifiedList.get(i)));
        }
        if (alt.length == 0) {
            return null;
        }
        double avgAlt = 0.0d;
        avgAlt = (alt.length > 1) ? ((alt[0] + alt[1]) / 2.0d) : alt[0];
        altQty.setAmount(avgAlt);
        altQty.setLowerBound((alt.length > 1) ? alt[0] : alt[0] - 1.0d);
        altQty.setUpperBound(alt.length == 1 ? (alt[0] + 1.0d) : alt[1]);
        altQty.setUnit(new Item(meterEnt));
        Claim altClaim = new Claim(prop, altQty);
        altClaim.getMainsnak().setDatatype("quantity");

        Set<Set<Snak>> refs = new HashSet<Set<Snak>>();
        refs.add(baseReferences);
        if (null != ref) {
            if (startsWith(lowerCase(trim(ref)), "google earth")) {
                Set<Snak> newRef = new HashSet<Snak>();
                Snak statedInGgErth = new Snak();
                statedInGgErth.setProperty(WikibasePropertyFactory.getWikibaseProperty("P143"));
                statedInGgErth.setData(new Item(new Entity("Q42274")));
                newRef.add(statedInGgErth);
                refs.add(newRef);
            }
        }
        for (Set<Snak> eachRef : refs) {
            altClaim.addReference(eachRef);
        }

        return altClaim;
    }

    private static Claim extractWdAreaDataFromParam(Property areaProp, Entity squareKmEnt, String data,
                                                    Set<Snak> baseReferences)
        throws ParseException {
        Quantity areaQty = new Quantity();
        String areaString = trim(removeStart(removeEnd(trim(lowerCase(data)), "}}"), "{{formatnum:"));
        double area = 0d;
        Matcher roFormatedNumberMatcher = roFormattedNumberStringRegex.matcher(areaString);
        Matcher simpleRefMatcher = simpleRefPattern.matcher(areaString);
        Matcher numberRangeMatcher = numberRangePattern.matcher(areaString);
        String ref = null;
        if (roFormatedNumberMatcher.find()) {
            NumberFormat nf = NumberFormat.getNumberInstance(new Locale("ro"));
            area = nf.parse(roFormatedNumberMatcher.group(0)).doubleValue();
        } else if (simpleRefMatcher.find()) {
            ref = simpleRefMatcher.group(1);
            areaString = replace(areaString, simpleRefMatcher.group(0), "");
            area = Double.parseDouble(areaString);
        } else if (numberRangeMatcher.find()) {
            area = Double.parseDouble(numberRangeMatcher.group(1));
        } else {
            area = Double.parseDouble(areaString);
        }
        areaQty.setAmount(area);
        areaQty.setUpperBound(area + 0.01d);
        areaQty.setLowerBound(area - 0.01d);
        areaQty.setUnit(new Item(squareKmEnt));
        Claim areaClaim = new Claim(areaProp, areaQty);
        areaClaim.getMainsnak().setDatatype("quantity");

        Set<Set<Snak>> refs = new HashSet<Set<Snak>>();
        refs.add(baseReferences);
        if (null != ref) {
            if (startsWith(lowerCase(trim(ref)), "google earth")) {
                Set<Snak> newRef = new HashSet<Snak>();
                Snak statedInGgErth = new Snak();
                statedInGgErth.setProperty(WikibasePropertyFactory.getWikibaseProperty("P143"));
                statedInGgErth.setData(new Item(new Entity("Q42274")));
                newRef.add(statedInGgErth);
                refs.add(newRef);
            }
        }
        for (Set<Snak> eachRef : refs) {
            areaClaim.addReference(eachRef);
        }
        return areaClaim;
    }

    private static String getHistoricalRegionLink(String settlement, String commune, String county) {
        if (Arrays.asList("Iași", "Vaslui").contains(trim(county))) {
            return MOLDOVA_LINK;
        }
        if (Arrays.asList("Constanța", "Tulcea").contains(trim(county))) {
            return "[[Dobrogea de Nord|Dobrogea]]";
        }
        if (Arrays.asList("Brăila", "Buzău", "Ialomița", "Călărași", "Prahova", "Ilfov", "Giurgiu", "Dâmbovița", "Argeș")
            .contains(trim(county))) {
            return MUNTENIA_LINK;
        }
        if (Arrays.asList("Dolj", "Gorj").contains(trim(county))) {
            return "[[Oltenia]]";
        }
        if (Arrays.asList("Timiș").contains(trim(county))) {
            return BANAT_LINK;
        }
        if (Arrays.asList("Covasna", "Harghita", "Mureș", "Bistrița-Năsăud", "Sibiu", "Alba", "Cluj")
            .contains(trim(county))) {
            return TRANSYLVANIA_LINK;
        }
        if ("Bihor".equalsIgnoreCase(trim(county))) {
            return CRISANA_LINK;
        }

        // counties broken between regions:
        // Moldova-Transilvania
        if ("Neamț".equalsIgnoreCase(trim(county))) {
            if (Arrays.asList("Bicazu Ardelean", "Bicaz-Chei", "Dămuc").contains(commune)) {
                return TRANSYLVANIA_LINK;
            } else {
                return MOLDOVA_LINK;
            }
        }

        if ("Bacău".equalsIgnoreCase(trim(county))) {
            if (Arrays.asList("Ghimeș-Făget").contains(commune) || Arrays.asList("Poiana Sărată").contains(settlement)
                || "Agăș".equals(commune) && "Coșnea".equals(settlement)
                || "Palanca".equals(commune) && Arrays.asList("Pajiștea", "Cădărești").contains(settlement)) {
                return TRANSYLVANIA_LINK;
            } else {
                return MOLDOVA_LINK;
            }
        }
        // Moldova-Muntenia
        if ("Vrancea".equalsIgnoreCase(trim(county))) {
            if (Arrays.asList("Andreiașu de Jos", "Bălești", "Bordești", "Broșteni", "Câmpineanca", "Cârligele", "Chiojdeni",
                "Ciorăști", "Cotești", "Dumbrăveni", "Dumitrești", "Golești", "Gologanu", "Gugești", "Gura Caliței", "Jitia",
                "Măicănești", "Milcovul", "Obrejița", "Poiana Cristei", "Popești", "Sihlea", "Slobozia Bradului",
                "Slobozia Ciorăști", "Tâmboești", "Tătăranu", "Urechești", "Vârteșcoiu", "Vintileasca")
                .contains(trim(commune))) {
                return MUNTENIA_LINK;
            } else if (Arrays.asList("Mera").contains(trim(commune))) {
                if (Arrays.asList("Vulcăneasa").contains(trim(settlement))) {
                    return MUNTENIA_LINK;
                }
            } else if (Arrays.asList("Focșani", "Vulturu").contains(trim(commune))) {
                if (null == settlement) {
                    return "la limita între regiunile istorice " + join(Arrays.asList(MOLDOVA_LINK, MUNTENIA_LINK), " și ");
                } else if (settlement.contains("Munteni") || Arrays.asList("Hângulești", "Maluri").contains(settlement)) {
                    return MUNTENIA_LINK;
                }
            }
            return MOLDOVA_LINK;
        }
        if ("Galați".equalsIgnoreCase(trim(county))) {
            if ("Nămoloasa".equalsIgnoreCase(trim(commune))) {
                if (null == settlement) {
                    return "la limita între regiunile istorice " + join(Arrays.asList(MOLDOVA_LINK, MUNTENIA_LINK), " și ");
                } else if ("Crângeni".equals(settlement)) {
                    return MUNTENIA_LINK;
                }
            }
            return MOLDOVA_LINK;
        }

        // Muntenia-Oltenia
        if ("Teleorman".equalsIgnoreCase(trim(county))) {
            if ("Islaz".equalsIgnoreCase(commune)) {
                return "[[Oltenia]]";
            }
            return MUNTENIA_LINK;
        }
        if ("Vâlcea".equalsIgnoreCase(trim(county))) {
            if (Arrays.asList("Berislăvești", "Boișoara", "Budești", "Dăești", "Dănicei", "Drăgoești", "Galicea", "Golești",
                "Milcoiu", "Nicolae Bălcescu", "Olanu", "Perișani", "Racovița", "Runcu", "Sălătrucel", "Stoilești",
                "Titești").contains(trim(commune))) {
                return MUNTENIA_LINK;
            } else if (Arrays.asList("Câineni").contains(trim(commune))) {
                if (Arrays.asList("Câinenii Mici", "Greblești", "Priloage").contains(trim(settlement))) {
                    return MUNTENIA_LINK;
                }
            }
            return "[[Oltenia]]";
        }
        if ("Olt".equalsIgnoreCase(trim(county))) {
            if (Arrays.asList("Bălteni", "Bărăști", "Brebeni", "Colonești", "Corbu", "Coteana", "Crâmpoia", "Cungrea",
                "Curtișoara", "Dăneasa", "Dobroteasa", "Făgețelu", "Ghimpețeni", "Icoana", "Ipotești", "Izvoarele",
                "Leleasca", "Mărunței", "Mihăești", "Milcov", "Movileni", "Nicolae Titulescu", "Oporelu", "Optași-Măgura",
                "Perieți", "Poboru", "Potcoava", "Priseaca", "Radomirești", "Sâmburești", "Sârbii-Măgura", "Schitu",
                "Scornicești", "Seaca", "Slatina", "Spineni", "Sprâncenata", "Stoicănești", "Șerbănești", "Tătulești",
                "Teslui", "Topana", "Tufeni", "Vâlcele", "Valea Mare", "Văleni", "Verguleasa", "Vitomirești", "Vulturești",
                "Drăgănești-Olt").contains(trim(commune))) {
                return MUNTENIA_LINK;
            } else if (Arrays.asList("Câineni").contains(trim(commune))) {
                if (Arrays.asList("Câinenii Mici", "Greblești", "Priloage").contains(trim(settlement))) {
                    return MUNTENIA_LINK;
                }
            }
            return "[[Oltenia]]";
        }
        // Oltenia-Banat
        if ("Mehedinți".equalsIgnoreCase(trim(county))) {
            if (Arrays.asList("Dubova", "Eșelnița", "Svinița", "Orșova").contains(trim(commune))) {
                return BANAT_LINK;
            }
            return "[[Oltenia]]";
        }
        // Banat-Transilvania
        if ("Caraș-Severin".equalsIgnoreCase(trim(county))) {
            if (Arrays.asList("Băuțar").contains(trim(commune))) {
                return TRANSYLVANIA_LINK;
            }
            return BANAT_LINK;
        }
        if ("Hunedoara".equalsIgnoreCase(trim(county))) {
            if ("Zam".equalsIgnoreCase(trim(commune))) {
                if (Arrays.asList("Sălciva", "Pojoga").contains(settlement)) {
                    return BANAT_LINK;
                }
            }
            return TRANSYLVANIA_LINK;
        }
        // Banat-Crișana
        if ("Arad".equalsIgnoreCase(trim(county))) {
            if (Arrays.asList("Bata", "Birchiș", "Fântânele", "Felnac", "Frumușeni", "Șagu", "Secusigiu", "Șiștarovăț",
                "Ususău", "Vinga", "Zăbrani", "Zădăreni").contains(trim(commune))) {
                return BANAT_LINK;
            } else if (Arrays.asList("Bârzava", "Conop", "Săvârșin").contains(trim(commune))) {
                if (Arrays.asList("Lalașinț", "Belotinț", "Căprioara", "Chelmac", "Valea Mare").contains(settlement)) {
                    return BANAT_LINK;
                }
                return "la limita între regiunile istorice " + join(Arrays.asList(BANAT_LINK, CRISANA_LINK), " și ");
            } else if ("Arad".equalsIgnoreCase(trim(commune))) {
                return "la limita între regiunile istorice " + join(Arrays.asList(BANAT_LINK, CRISANA_LINK), " și ");
            }
            return CRISANA_LINK;
        }
        // Crișana-Sătmar/Maramureș
        if ("Satu Mare".equalsIgnoreCase(trim(county))) {
            return TRANSYLVANIA_LINK;
        }
        if ("Maramureș".equalsIgnoreCase(trim(county))) {
            return TRANSYLVANIA_LINK;
        }
        if ("Sălaj".equalsIgnoreCase(trim(county))) {
            return TRANSYLVANIA_LINK;
        }
        // Transilvania-Muntenia
        if ("Brașov".equalsIgnoreCase(trim(county))) {
            if (null == settlement && "Predeal".equalsIgnoreCase(commune)) {
                return "la limita între regiunile istorice " + join(Arrays.asList(MUNTENIA_LINK, TRANSYLVANIA_LINK), " și ");
            }
            return TRANSYLVANIA_LINK;
        }
        // Bucovina - Moldova
        if ("Botoșani".equalsIgnoreCase(trim(county))) {
            if ("Mihăileni".equalsIgnoreCase(trim(commune))) {
                if (Arrays.asList("Rogojești").contains(trim(settlement))) {
                    return BUCOVINA_LINK;
                }
                if (null == settlement) {
                    return "la limita între regiunile istorice " + join(Arrays.asList(MOLDOVA_LINK, BUCOVINA_LINK), " și ");
                }
            }
            if ("Cândești".equalsIgnoreCase(trim(commune))) {
                if ("Cândești".equalsIgnoreCase(trim(settlement))) {
                    return BUCOVINA_LINK;
                }
                if (null == settlement) {
                    return "la limita între regiunile istorice " + join(Arrays.asList(MOLDOVA_LINK, BUCOVINA_LINK), " și ");
                }
            }
            return MOLDOVA_LINK;
        }
        if ("Suceava".equalsIgnoreCase(trim(county))) {
            if (Arrays
                .asList("Adâncata", "Baia", "Bogdănești", "Boroaia", "Broșteni", "Crucea", "Dolhești", "Drăgușeni",
                    "Dumbrăveni", "Fântâna Mare", "Fântânele", "Forăști", "Grămești", "Hănțești", "Hârtop", "Horodniceni",
                    "Mălini", "Panaci", "Preutești", "Rădășeni", "Râșca", "Siminicea", "Slatina", "Vadu Moldovei", "Verești",
                    "Vulturești", "Zamostea", "Zvoriștea", "Dolhasca", "Fălticeni", "Liteni", "Salcea")
                .contains(trim(commune))) {
                return MOLDOVA_LINK;
            }

            if ("Bunești".equalsIgnoreCase(trim(commune))) {
                if (Arrays.asList("Uncești", "Petia").contains(trim(settlement))) {
                    return MOLDOVA_LINK;
                }
            }
            if ("Cârlibaba".equalsIgnoreCase(trim(commune))) {
                if (null == settlement) {
                    return "la limita între regiunile istorice "
                        + Stream.of(BUCOVINA_LINK, TRANSYLVANIA_LINK).collect(joining(" și "));
                }
                if (Arrays.asList("Șesuri", "Cârlibaba Nouă").contains(trim(settlement))) {
                    return TRANSYLVANIA_LINK;
                }
                return BUCOVINA_LINK;
            }
            if ("Cornu Luncii".equalsIgnoreCase(trim(commune))) {
                if (Arrays.asList("Dumbrava", "Păiseni", "Sasca Mare", "Sasca Mică", "Sasca Nouă", "Șinca")
                    .contains(trim(settlement))) {
                    return MOLDOVA_LINK;
                }
            }
            if ("Coșna".equalsIgnoreCase(trim(commune))) {
                if (Arrays.asList("Podu Coșnei", "Românești", "Valea Bancului").contains(trim(settlement))) {
                    return BUCOVINA_LINK;
                }
                if (null == settlement) {
                    return "la limita între regiunile istorice "
                        + Stream.of(BUCOVINA_LINK, TRANSYLVANIA_LINK).collect(joining(" și "));
                }
                return TRANSYLVANIA_LINK;
            }
            if ("Dorna-Arini".equalsIgnoreCase(trim(commune))) {
                if (!"Gheorghițeni".equalsIgnoreCase(trim(settlement))) {
                    return MOLDOVA_LINK;
                }
            }
            if ("Mitocu Dragomirnei".equalsIgnoreCase(trim(commune)) && "Mitocași".equalsIgnoreCase(trim(settlement))) {
                return MOLDOVA_LINK;
            }
            if ("Șaru Dornei".equalsIgnoreCase(trim(commune)) && !"Șaru Bucovinei".equalsIgnoreCase(trim(settlement))) {
                return MOLDOVA_LINK;
            }
            if ("Udești".equalsIgnoreCase(trim(commune)) && Arrays.asList("Racova", "Știrbăț").contains(trim(settlement))) {
                return MOLDOVA_LINK;
            }
            if ("Siret".equalsIgnoreCase(trim(commune)) && "Pădureni".equalsIgnoreCase(trim(settlement))) {
                return MOLDOVA_LINK;
            }

            return BUCOVINA_LINK;
        }
        return null;
    }

    private static void initLogging() {
        LoggerContext logbackContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        PatternLayoutEncoder ple = new PatternLayoutEncoder();

        ple.setPattern("%date %level %logger{10} [%file:%line] %msg%n");
        ple.setContext(logbackContext);
        ple.start();

        ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
        appender.setContext(logbackContext);
        appender.setName("console");
        appender.setEncoder(ple);
        appender.start();

        Logger roWikiLog = logbackContext.getLogger("org.wikipedia.ro");
        roWikiLog.setAdditive(false);
        roWikiLog.setLevel(Level.INFO);
        roWikiLog.addAppender(appender);

        Logger wikiLog = logbackContext.getLogger("wiki");
        wikiLog.setAdditive(false);
        wikiLog.setLevel(Level.WARN);
        wikiLog.addAppender(appender);

        Logger mongoLog = logbackContext.getLogger("org.mongodb");
        mongoLog.setAdditive(false);
        mongoLog.setLevel(Level.WARN);
        mongoLog.addAppender(appender);

        LevelChangePropagator levelChangePropagator = new LevelChangePropagator();
        levelChangePropagator.setResetJUL(true);
        logbackContext.addListener(levelChangePropagator);
        
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }
}
