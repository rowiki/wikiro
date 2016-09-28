package org.wikipedia.ro.astroe;

import static org.apache.commons.lang3.StringUtils.appendIfMissing;
import static org.apache.commons.lang3.StringUtils.contains;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.endsWith;
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

import java.io.IOException;
import java.net.URLDecoder;
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
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
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
import org.wikibase.data.Snak;
import org.wikibase.data.WikibaseData;
import org.wikipedia.Wiki;
import org.wikipedia.ro.populationdb.util.WikiTemplate;

public class FixVillages {
    private static final String MOLDOVA_LINK = "[[Moldova Occidentală|Moldova]]";
    private static final String BUCOVINA_LINK = "[[Bucovina]]";
    private static String collationDescription = "<  0 < 1 < 2 < 3 < 4 < 5 < 6 < 7 < 8 < 9 "
        + "< a, A < ă, Ă < â, Â‚ < b, B < c, C < d, D < e, E < f, F < g, G < h, H < i, I"
        + "< î, Î < j, J < k, K < l, L < m, M < n, N < o, O < p, P < q, Q < r, R"
        + "< s, S < ș, Ș < t, T < ț, Ț < u, U < v, V < w, W < x, X < y, Y < z, Z";
    private static RuleBasedCollator collator = null;
    private static Exception exception;

    private static String countyStart = "Galați";
    private static String communeStart = "Cuza Vodă";

    // Pattern sentencePattern = Pattern.compile("((.*(\\(.*?\\)))*.*?)(\\.|$)\\s*");
    // Pattern sentencePattern = Pattern.compile(
    // "(([^\\.]*(\\(.*?\\)))*.*?)(((\\.|$)\\s*((\\<ref[^\\/]*?\\>.*?\\<\\/ref\\>)|(\\<ref[^>]*\\/\\>))*)|((\\<ref[^\\/]*?\\>.*?\\<\\/ref\\>)|(\\<ref[^>]*\\/\\>))*\\s*(\\.|$))\\s*");
    private static Pattern manuallyFormatedNumberPattern = Pattern.compile("\\d+(?:\\.\\s*\\d+)+");
    private static Pattern sentencePattern = Pattern.compile(
        "((?:'''.*?''')?(?:(?:\\[\\[.*?\\]\\])|(\\(.*?\\))|[^\\[\\]\\.])*+(?:(?:\\<ref[^\\>]*+\\>(?:.*?\\</ref\\>)?)|(?:\\[\\[.*?\\]\\])|[^\\[\\]\\.])*?)(((\\.|$)\\s*((\\<ref[^\\/]*?\\>.*?\\<\\/ref\\>)|(\\<ref[^>]*\\/\\>))*)|((\\<ref[^\\/]*?\\>.*?\\<\\/ref\\>)|(\\<ref[^>]*\\/\\>))*+\\s*(\\.|$))\\s*");
    private static Pattern fullLocationPattern =
        Pattern.compile("\\s*în \\[\\[județul\\s+.*?(\\|.*?)?\\]\\], \\[\\[.*?\\]\\], \\[\\[România\\]\\]\\,?");
    private static Pattern qualifierPattern = Pattern.compile("'''.*?'''(.*?)\\s+este");
    private static Pattern commentedEol = Pattern.compile("<!\\-\\-[\\r\\n]+\\-\\->");
    private static Pattern imageInInfoboxPattern =
        Pattern.compile("(\\[\\[)?(((Fișier)|(File)|(Imagine)|(Image))\\:)?([^\\|\\]]*)((\\|.*?)*\\]\\])?");
    private static Pattern roFormattedNumberStringRegex = Pattern.compile("\\d+(?:\\.\\d+)*,\\d+");
    private static Pattern simpleRefPattern = Pattern.compile("\\<ref[^\\/\\>]*\\>([^\\<]+)\\</ref\\>");
    private static Pattern numberRangePattern = Pattern.compile("(\\d+)((?:[–\\-]|(?:&ndash;))(\\d+))?");

    public static void main(String[] args) throws IOException {
        Wiki rowiki = new Wiki("ro.wikipedia.org");
        Wiki commonsWiki = new Wiki("commons.wikimedia.org");

        try {
            collator = new RuleBasedCollator(collationDescription);
        } catch (Exception e) {
            exception = e;
        }

        Wikibase dwiki = new Wikibase();
        String[] countyCategoryMembers = rowiki.getCategoryMembers("Category:Județe în România", Wiki.CATEGORY_NAMESPACE);
        List<String> countyCategoryMembersList = Arrays.asList(countyCategoryMembers);
        List<String> countyNames = new ArrayList<String>();
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

        Entity meterEnt = new Entity("Q11573");
        Entity squareKmEnt = new Entity("Q712226");

        Set<Snak> roWpReference = new HashSet<Snak>();
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
            final Properties credentials = new Properties();

            credentials.load(FixVillages.class.getClassLoader().getResourceAsStream("credentials.properties"));

            final String rowpusername = credentials.getProperty("rowiki.user");
            final String rowppassword = credentials.getProperty("rowiki.password");
            final String datausername = credentials.getProperty("wd.user");
            final String datapassword = credentials.getProperty("wd.password");

            dwiki.login(datausername, datapassword.toCharArray());
            rowiki.login(rowpusername, rowppassword.toCharArray());
            rowiki.setMarkBot(true);
            boolean countyTouched = false;
            for (String eachCounty : countyNames) {
                if (!countyTouched && !eachCounty.equals(countyStart)) {
                    continue;
                }
                countyTouched = true;
                String[] categoryMembers =
                    rowiki.getCategoryMembers("Category:Orașe în județul " + eachCounty, Wiki.MAIN_NAMESPACE);
                List<String> categoryMembersList = new ArrayList<String>();
                categoryMembersList.addAll(Arrays.asList(categoryMembers));
                categoryMembersList.addAll(Arrays
                    .asList(rowiki.getCategoryMembers("Category:Comune în județul " + eachCounty, Wiki.MAIN_NAMESPACE)));

                String[] subcategories =
                    rowiki.getCategoryMembers("Category:Orașe în județul " + eachCounty, Wiki.CATEGORY_NAMESPACE);
                for (String eachSubcat : subcategories) {
                    String catTitle = removeStart(eachSubcat, "Categorie:");
                    String[] subarticles = rowiki.getCategoryMembers(eachSubcat, Wiki.MAIN_NAMESPACE);
                    for (String eachSubarticle : subarticles) {
                        if (StringUtils.equals(eachSubarticle, catTitle)) {
                            categoryMembersList.add(eachSubarticle);
                        }
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
                boolean communeTouched = false;
                for (String eachCommuneArticle : new LinkedHashSet<String>(categoryMembersList)) {
                    Entity communeWikibaseItem = dwiki.getWikibaseItemBySiteAndTitle("rowiki", eachCommuneArticle);

                    String communeName = trim(substringBefore(
                        removeStart(removeEnd(removeEnd(eachCommuneArticle, ", " + eachCounty), ", România"), "Comuna "),
                        "("));
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

                    String communeDescr = communeType + " în județul " + eachCounty + ", România";
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
                    Map<String, String> villages = new HashMap<String, String>();
                    Map<String, String> urbanSettlements = new HashMap<String, String>();

                    Set<Claim> capitalClaims = communeWikibaseItem.getClaims().get(new Property("P36"));
                    Claim capitalClaim = new ArrayList<Claim>(capitalClaims).get(0);
                    Entity capitalEntity = dwiki.getWikibaseItemById(((Item) capitalClaim.getMainsnak().getData()).getEnt());

                    Pattern communeLinkPattern =
                        Pattern.compile("\\[\\[\\s*((C|c)omuna )?" + communeName + "(, " + eachCounty + ")?\\s*(\\||\\])");
                    Pattern countyCategoryPattern = Pattern.compile(
                        "\\[\\[Categor(y|(ie))\\:Orașe în județul " + replace(eachCounty, "-", "\\-") + ".*?\\]\\]");

                    for (Claim eachCompositeVillageClaim : compositeVillagesClaims) {
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

                        String villageName =
                            trim(removeStart(removeEnd(removeEnd(villageEntity.getLabels().get("ro"), ", " + eachCounty),
                                "(" + communeName + ")"), "Comuna "));

                        String villageRelationWithCommune = "în comuna";
                        if (!"comună".equalsIgnoreCase(communeType)) {
                            if ("localitate componentă".equals(villageType)) {
                                villageRelationWithCommune = "a " + appendIfMissing(communeType, "u") + "lui";
                            } else {
                                villageRelationWithCommune = "al " + appendIfMissing(communeType, "u") + "lui";
                            }
                        }

                        String villageDescr = villageType + " " + villageRelationWithCommune + " " + communeName
                            + ", județul " + eachCounty + ", România";

                        boolean villageChanged = false;
                        if (!StringUtils.equals(villageEntity.getLabels().get("ro"), villageName)) {
                            dwiki.setLabel(villageEntity.getId(), "ro", villageName);
                            communeChanged = villageChanged = true;
                        }
                        if (!StringUtils.equals(villageEntity.getDescriptions().get("ro"), villageDescr)) {
                            dwiki.setDescription(villageEntity.getId(), "ro", villageDescr);
                            communeChanged = villageChanged = true;
                        }

                        Map<String, String> mapToAdd =
                            "localitate componentă".equals(villageType) ? urbanSettlements : villages;

                        if (null == villageEntity.getSitelinks().get("rowiki")) {
                            mapToAdd.put(villageName, null);
                        } else {
                            String rovillagearticle = villageEntity.getSitelinks().get("rowiki").getPageName();
                            mapToAdd.put(villageName, rovillagearticle);

                            String pageText = rowiki.getPageText(rovillagearticle);
                            String initialPageText = pageText;
                            pageText = commentedEol.matcher(pageText).replaceAll("");

                            WikiTemplate initialTemplate = null;
                            int templateAnalysisStart = 0;

                            int previousTemplateAnalysisStart = -1;
                            do {
                                initialTemplate = new WikiTemplate();
                                initialTemplate.fromString(pageText.substring(templateAnalysisStart));
                                previousTemplateAnalysisStart = templateAnalysisStart;
                                templateAnalysisStart = templateAnalysisStart + initialTemplate.getTemplateLength();
                            } while (!startsWithAny(lowerCase(initialTemplate.getTemplateTitle()), "cutie", "casetă",
                                "infobox", "infocaseta") && templateAnalysisStart < pageText.length()
                                && previousTemplateAnalysisStart != templateAnalysisStart);

                            if (startsWithAny(lowerCase(initialTemplate.getTemplateTitle()), "cutie", "casetă", "caseta",
                                "infobox", "infocaseta")) {
                                initialTemplate.removeParam("nume");
                                initialTemplate.removeParam("județ");
                                initialTemplate.removeParam("resedinta");
                                initialTemplate.removeParam("reședința");
                                if (StringUtils.equalsIgnoreCase("Infocaseta Așezare", initialTemplate.getTemplateTitle())) {
                                    if (isBlank(initialTemplate.getParams().get("nume_nativ"))
                                        && isNotBlank(initialTemplate.getParams().get("alt_nume"))) {
                                        initialTemplate.setParam("nume_nativ", initialTemplate.getParams().get("alt_nume"));
                                        initialTemplate.removeParam("alt_nume");
                                    }
                                }
                                for (String eachParam : initialTemplate.getParamNames()) {
                                    String paramValue = initialTemplate.getParams().get(eachParam);
                                    if (startsWithAny(eachParam, "comună", "oraș", "tip_subdiviziune", "nume_subdiviziune")
                                        || eachParam.matches("p?\\d+")
                                        || isBlank(paramValue)) {
                                        initialTemplate.removeParam(eachParam);
                                    }
                                    if (startsWith(eachParam, "suprafață_totală_km2")) {
                                        if (isNotBlank(paramValue) && (!villageEntity.getClaims().containsKey(areaProp)
                                            || null == villageEntity.getClaims().get(areaProp))) {
                                            Claim areaClaim = extractWdAreaDataFromParam(areaProp, squareKmEnt,
                                                paramValue, roWpReference);
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
                                            Claim altClaim = extractWdAltitudeDataFromParam(altProp, meterEnt,
                                                paramValue, roWpReference);
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
                                    articleAfterInfobox = trim(substring(articleAfterInfobox,
                                        new WikiTemplate(articleAfterInfobox).getTemplateLength()));
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
                                    if (commonsWiki.exists(new String[] { "File:" + imageName })[0]) {
                                        String imageClaimId = null;
                                        boolean captionQualifierFound = false;
                                        if (null == villageEntity.getClaims().get(imageWikidataProperty)) {
                                            imageClaimId = dwiki.addClaim(villageEntity.getId(),
                                                new Claim(imageWikidataProperty, new CommonsMedia(imageName)));
                                            initialTemplate.removeParam("imagine");
                                        } else if (StringUtils.equals(
                                            replace(((CommonsMedia) villageEntity.getClaims().get(imageWikidataProperty)
                                                .iterator().next().getValue()).getFileName(), " ", "_"),
                                            replace(imageName, " ", "_"))) {
                                            initialTemplate.removeParam("imagine");
                                            Claim imageClaim =
                                                villageEntity.getClaims().get(imageWikidataProperty).iterator().next();
                                            imageClaimId = imageClaim.getId();
                                            Set<Snak> captions = imageClaim.getQualifiers().get(captionProp);
                                            if (null != captions) {
                                                captionQualifierFound = true;
                                            }

                                        }
                                        String villageImageCaption =
                                            defaultIfEmpty(initialTemplate.getParams().get("imagine_descriere"),
                                                initialTemplate.getParams().get("descriere"));
                                        villageImageCaption = replace(villageImageCaption, "'''", "");
                                        if (isNotEmpty(villageImageCaption) && null != imageClaimId
                                            && !captionQualifierFound) {
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
                            if (villageEntity.getClaims().containsKey(captionProp)
                                && null != villageEntity.getClaims().get(captionProp)) {
                                Set<Claim> imagesClaims = villageEntity.getClaims().get(imageProp);
                                if (imagesClaims.size() == 1) {
                                    Claim imageClaim = imagesClaims.iterator().next();
                                    String imageClaimId = imageClaim.getId();
                                    if (null != imageClaimId) {
                                        Set<Claim> imageDescriptionClaims = villageEntity.getClaims().get(captionProp);
                                        for (Claim eachImageDescriptionClaim : imageDescriptionClaims) {
                                            WikibaseData imageDescriptionLangString = eachImageDescriptionClaim.getValue();
                                            dwiki.addQualifier(imageClaimId, captionProp.getId(),
                                                imageDescriptionLangString);
                                            dwiki.removeClaim(eachImageDescriptionClaim.getId());
                                            villageChanged = true;
                                        }
                                    }
                                }
                            }

                            if (null != initialTemplate.getInitialTemplateText() && null != initialTemplate.toString()) {
                                pageText =
                                    pageText.replace(initialTemplate.getInitialTemplateText(), initialTemplate.toString());
                            }

                            String firstParagraph = substringBefore(articleAfterInfobox, "\n");
                            String restOfArticle = substringAfter(articleAfterInfobox, "\n");
                            if (isEmpty(firstParagraph)) {
                                firstParagraph = initialTemplate.getBeforeText();
                            }
                            while (startsWith(trim(firstParagraph), "{{")) {
                                WikiTemplate parsedExtraTemplate = new WikiTemplate();
                                parsedExtraTemplate.fromString(trim(restOfArticle));
                                firstParagraph = trim(substringBefore(
                                    substring(trim(restOfArticle), parsedExtraTemplate.getInitialTemplateText().length()),
                                    "\n"));
                                restOfArticle = substringAfter(
                                    substring(trim(restOfArticle), parsedExtraTemplate.getInitialTemplateText().length()),
                                    "\n");
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
                                    "{{formatnum:" + numberMatcher.group(0).replaceAll("[\\.|\\s]", "") + "}}");
                            }
                            numberMatcher.appendTail(formattedNumberBuf);
                            workingFirstParagraph = formattedNumberBuf.toString();

                            Matcher sentenceMatcher = sentencePattern.matcher(workingFirstParagraph);
                            List<String> firstParagraphSentences = new ArrayList<String>();
                            List<String> firstParagraphRefs = new ArrayList<String>();
                            while (sentenceMatcher.find()) {
                                firstParagraphSentences.add(sentenceMatcher.group(1));
                                firstParagraphRefs.add(defaultIfBlank(defaultIfBlank(sentenceMatcher.group(8), sentenceMatcher.group(10)), sentenceMatcher.group(6)));
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
                            String villageIntro =
                                String.format("este %s %s %s [[%s|%s %s]] din [[județul %s]], %s, [[România]]",
                                    villageIndefArticle, villageType, in, communeLink, articulatedCommuneType,
                                    StringUtils.equals(communeName, villageName) ? "cu același nume" : communeName,
                                    eachCounty, getHistoricalRegionLink(villageName, communeName, eachCounty));

                            String l = "sat".equalsIgnoreCase(villageType) ? "l" : "";
                            String articulatedVillageType = "sat".equals(villageType) ? "satul" : "localitatea componentă";
                            String residenceIntro =
                                String.format("este %s de reședință a%s [[%s|%s %s]] din [[județul %s]], %s, [[România]]",
                                    articulatedVillageType, l, communeLink, genitiveCommuneType,
                                    StringUtils.equals(communeName, villageName) ? "cu același nume" : communeName,
                                    eachCounty, getHistoricalRegionLink(villageName, communeName, eachCounty));

                            String intro = StringUtils.equals(capitalEntity.getId(), villageEntity.getId()) ? residenceIntro
                                : villageIntro;

                            newFirstParagraphSentences.add("'''" + villageName + "'''" + qualifier + " " + intro);
                            newFirstParagraphRefs.add("");

                            for (int sentenceIdx = 0; sentenceIdx < firstParagraphSentences.size(); sentenceIdx++) {
                                String eachOldParagraphSentence = firstParagraphSentences.get(sentenceIdx);
                                String eachOldParagraphRef = firstParagraphRefs.get(sentenceIdx);
                                if (countyLinkPattern.matcher(eachOldParagraphSentence).find()
                                    || communeLinkPattern.matcher(eachOldParagraphSentence).find()
                                    || eachOldParagraphSentence.contains("'''" + villageName + "'''")
                                    || isEmpty(eachOldParagraphSentence)) {

                                    String[] locationPart = eachOldParagraphSentence.split("(aflat|situat)ă?");
                                    if (locationPart.length > 1) {
                                        Pattern[] locationTransformerPatterns = new Pattern[] {
                                            Pattern.compile(
                                                "((\\[\\[județ\\]\\])|județ)ului\\s+\\[\\[(J|j)udețul\\s+.*?(\\|.*?)?\\]\\]"),
                                            Pattern.compile("\\[\\[(J|j)udețul\\s+.*?\\|județului\\s+.*?\\]\\]") };
                                        String transformedLocation = "Se află " + removeEnd(trim(locationPart[1]), ".");
                                        for (Pattern eachLocationTransformerPattern : locationTransformerPatterns) {
                                            Matcher locationTransformationMatcher =
                                                eachLocationTransformerPattern.matcher(transformedLocation);
                                            transformedLocation = locationTransformationMatcher.replaceAll("județului");
                                        }
                                        transformedLocation =
                                            fullLocationPattern.matcher(transformedLocation).replaceAll("");
                                        newFirstParagraphSentences.add(transformedLocation);
                                        newFirstParagraphRefs.add("");
                                    }
                                    continue;
                                }
                                eachOldParagraphSentence = removeEnd(trim(eachOldParagraphSentence), ".");
                                eachOldParagraphSentence =
                                    fullLocationPattern.matcher(eachOldParagraphSentence).replaceAll("");
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

                            if (!StringUtils.equals(pageText, initialPageText)) {
                                rowiki.edit(rovillagearticle, pageText,
                                    "Eliminare din infocasetă parametri migrați la Wikidata sau fără valoare, revizitat introducere standard.");
                                villageChanged = true;
                            }
                        }

                        if (villageChanged) {
                            long sleeptime = 10 + Math.round(20 * Math.random());
                            System.out.println("Sleeping " + sleeptime + "s");
                            Thread.sleep(1000l * sleeptime);
                        }
                    }

                    WikiTemplate initialTemplate = null;
                    int templateAnalysisStart = 0;

                    String rocommunearticle = communeWikibaseItem.getSitelinks().get("rowiki").getPageName();
                    String pageText = rowiki.getPageText(rocommunearticle);
                    String initialPageText = pageText;

                    int previousTemplateAnalysisStart = -1;
                    do {
                        initialTemplate = new WikiTemplate();
                        initialTemplate.fromString(pageText.substring(templateAnalysisStart));
                        previousTemplateAnalysisStart = templateAnalysisStart;
                        templateAnalysisStart = templateAnalysisStart + initialTemplate.getTemplateLength();
                    } while (!startsWithAny(lowerCase(initialTemplate.getTemplateTitle()), "cutie", "casetă", "infobox",
                        "infocaseta") && templateAnalysisStart < pageText.length()
                        && previousTemplateAnalysisStart != templateAnalysisStart);

                    if (startsWithAny(lowerCase(initialTemplate.getTemplateTitle()), "cutie", "casetă", "caseta", "infobox",
                        "infocaseta")) {
                        initialTemplate.removeParam("nume");
                        initialTemplate.removeParam("județ");
                        initialTemplate.removeParam("resedinta");
                        initialTemplate.removeParam("reședința");
                        initialTemplate.removeParam("sate");
                        initialTemplate.removeParam("componență");
                        initialTemplate.removeParam("componenta");
                        if (StringUtils.equalsIgnoreCase("Infocaseta Așezare", initialTemplate.getTemplateTitle())) {
                            if (isBlank(initialTemplate.getParams().get("nume_nativ"))
                                && isNotBlank(initialTemplate.getParams().get("alt_nume"))) {
                                initialTemplate.setParam("nume_nativ", initialTemplate.getParams().get("alt_nume"));
                                initialTemplate.removeParam("alt_nume");
                            }
                        }
                        for (String eachParam : initialTemplate.getParamNames()) {
                            String paramValue = initialTemplate.getParams().get(eachParam);
                            if (startsWithAny(eachParam, "comună", "oraș", "tip_subdiviziune", "nume_subdiviziune")
                                || eachParam.matches("p?\\d+") || isBlank(paramValue)) {
                                initialTemplate.removeParam(eachParam);
                            }
                            if (startsWith(eachParam, "suprafață_totală_km2")) {
                                if (isNotBlank(paramValue) && (!communeWikibaseItem.getClaims().containsKey(areaProp)
                                    || null == communeWikibaseItem.getClaims().get(areaProp))) {
                                    Claim areaClaim = extractWdAreaDataFromParam(areaProp, squareKmEnt,
                                        paramValue, roWpReference);
                                    String claimId = dwiki.addClaim(communeWikibaseItem.getId(), areaClaim);
                                    if (null != claimId) {
                                        for (Set<Snak> eachRef : areaClaim.getReferences()) {
                                            dwiki.addReference(claimId, new ArrayList<Snak>(eachRef));
                                        }
                                    }
                                }
                                initialTemplate.removeParam(eachParam);
                            }
                            if (StringUtils.equals(eachParam, "altitudine")) {
                                if (isNotBlank(paramValue) && (!communeWikibaseItem.getClaims().containsKey(altProp)
                                    || null == communeWikibaseItem.getClaims().get(altProp))) {
                                    Claim altClaim = extractWdAltitudeDataFromParam(altProp, meterEnt,
                                        paramValue, roWpReference);
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
                            if (commonsWiki.exists(new String[] { "File:" + coatOfArmsImageName })[0]
                                && !ArrayUtils.contains(commonsWiki.getCategories("File:" + coatOfArmsImageName),
                                    "Category:Coat of arms placeholders")) {
                                Set<Claim> coatOfArmsFromWd = communeClaims.get(coatOfArmsImageWikidataProperty);
                                if (null == coatOfArmsFromWd || 0 == coatOfArmsFromWd.size()) {
                                    dwiki.addClaim(communeWikibaseItem.getId(),
                                        new Claim(coatOfArmsImageWikidataProperty, new CommonsMedia(coatOfArmsImageName)));
                                } else {
                                    List<Claim> coatOfArmsFromWdList = new ArrayList<Claim>(coatOfArmsFromWd);
                                    while (coatOfArmsFromWdList.size() > 1) {
                                        coatOfArmsFromWdList.remove(1);
                                    }
                                    coatOfArmsFromWdList.get(0).setValue(new CommonsMedia(coatOfArmsImageName));
                                    coatOfArmsFromWd = new HashSet<Claim>(coatOfArmsFromWdList);
                                    dwiki.editClaim(coatOfArmsFromWdList.get(0));
                                }
                                initialTemplate.removeParam("stemă");
                            }
                        }
                    } else {
                        initialTemplate.removeParam("stemă");
                    }

                    String communeImage = initialTemplate.getParams().get("imagine");
                    if (isNotEmpty(communeImage)) {
                        Matcher imageMatcher = imageInInfoboxPattern.matcher(communeImage);
                        if (contains(communeImage, "{{#property:P18}}")) {
                            initialTemplate.removeParam("imagine");
                        } else if (imageMatcher.matches()) {
                            String imageName = imageMatcher.group(8);
                            Property imageWikidataProperty = new Property("P18");
                            if (commonsWiki.exists(new String[] { "File:" + imageName })[0]) {
                                String imageClaimId = null;
                                boolean captionQualifierFound = false;
                                if (null == communeClaims.get(imageWikidataProperty)) {
                                    imageClaimId = dwiki.addClaim(communeWikibaseItem.getId(),
                                        new Claim(imageWikidataProperty, new CommonsMedia(imageName)));
                                    initialTemplate.removeParam("imagine");
                                } else if (StringUtils.equals(replace(
                                    ((CommonsMedia) communeClaims.get(imageWikidataProperty).iterator().next().getValue())
                                        .getFileName(),
                                    " ", "_"), replace(imageName, " ", "_"))) {
                                    Claim imageClaim = communeClaims.get(imageWikidataProperty).iterator().next();
                                    imageClaimId = imageClaim.getId();
                                    captionQualifierFound = (null != imageClaim.getQualifiers().get(captionProp));
                                    initialTemplate.removeParam("imagine");
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
                    } else {
                        initialTemplate.removeParam("imagine");
                        initialTemplate.removeParam("imagine_descriere");
                        initialTemplate.removeParam("descriere");
                        initialTemplate.removeParam("imagine_dimensiune");
                        initialTemplate.removeParam("dimensiune_imagine");
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
                    if (null != initialTemplate.getInitialTemplateText() && null != initialTemplate.toString()) {
                        pageText = pageText.replace(initialTemplate.getInitialTemplateText(), initialTemplate.toString());
                    }
                    String firstParagraph = substringBefore(articleAfterInfobox, "\n");
                    String workingFirstParagraph = firstParagraph;

                    Matcher numberMatcher = manuallyFormatedNumberPattern.matcher(workingFirstParagraph);
                    StringBuffer formattedNumberBuf = new StringBuffer();
                    while (numberMatcher.find()) {
                        numberMatcher.appendReplacement(formattedNumberBuf,
                            "{{formatnum:" + numberMatcher.group(0).replaceAll("[\\.|\\s]", "") + "}}");
                    }
                    numberMatcher.appendTail(formattedNumberBuf);
                    workingFirstParagraph = formattedNumberBuf.toString();

                    Matcher sentenceMatcher = sentencePattern.matcher(workingFirstParagraph);
                    List<String> firstParagraphSentences = new ArrayList<String>();
                    List<String> firstParagraphRefs = new ArrayList<String>();
                    while (sentenceMatcher.find()) {
                        firstParagraphSentences.add(sentenceMatcher.group(1));
                        firstParagraphRefs.add(defaultIfBlank(defaultIfBlank(sentenceMatcher.group(8), sentenceMatcher.group(10)), sentenceMatcher.group(6)));
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

                    List<Entry<String, String>> urbanSettlementsEntryList =
                        new ArrayList<Map.Entry<String, String>>(urbanSettlements.entrySet());
                    List<Entry<String, String>> villagesEntryList =
                        new ArrayList<Map.Entry<String, String>>(villages.entrySet());
                    if (urbanSettlementsEntryList.size() + villagesEntryList.size() > 0) {
                        String intro = null;

                        if (villagesEntryList.size() == 1 && 0 == urbanSettlementsEntryList.size()) {
                            intro = String.format("'''%s'''%s este %s %s, %s, [[România]]", communeName, qualifier,
                                communeTypeLink, countyLink, getHistoricalRegionLink(null, communeName, eachCounty));
                            if ("comună".equals(communeType)) {
                                intro = intro + String.format(", formată numai din satul de reședință%s",
                                    StringUtils.equals(communeName, villagesEntryList.get(0).getKey()) ? " cu același nume"
                                        : (", [[" + villagesEntryList.get(0).getValue() + '|'
                                            + villagesEntryList.get(0).getKey()) + "]]");
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
                                    villagesList.append("[[").append(urbanSettlementsEntryList.get(0).getValue()).append('|')
                                        .append(urbanSettlementsEntryList.get(0).getKey()).append("]]");
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
                                        villagesList.append("[[").append(eachSettlementEntry.getValue()).append('|')
                                            .append(eachSettlementEntry.getKey()).append("]]");
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
                        List<String> newFirstParagraphSentences = new ArrayList<String>();
                        List<String> newFirstParagraphRefs = new ArrayList<String>();
                        newFirstParagraphSentences.add(intro);
                        newFirstParagraphRefs.add("");

                        List<String> villageLinksRegexList = new ArrayList<String>();
                        Set<String> allVillagesAndSettlements = new HashSet<String>();
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
                        String villageLinksRegexExpression = 0 < villageLinksRegexList.size()
                            ? join(villageLinksRegexList, '|') : "nothing should match this regex";
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
                                    String transformedLocation = "Se află " + removeEnd(trim(locationPart[1]), ".");
                                    transformedLocation.replaceAll("județului\\s+\\[\\[(J|j)udețul\\s+.*?(\\|.*?)?\\]\\]",
                                        "județului");
                                    transformedLocation.replaceAll("\\[\\[(J|j)udețul\\s+.*?\\|județului\\s+.*?\\]\\]",
                                        "județului");
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
                            newFirstParagraphBuilder.append(newFirstParagraphSentences.get(sentenceIdx)).append('.')
                                .append(defaultString(newFirstParagraphRefs.get(sentenceIdx))).append(' ');
                        }
                        String newFirstParagraph = trim(newFirstParagraphBuilder.toString());
                        pageText = pageText.replace(firstParagraph, newFirstParagraph);

                        String sortingKey = replaceEach(communeName, new String[] { "ă", "â", "ș", "ț", "î" },
                            new String[] { "aă", "aăâ", "sș", "tț", "iî" });
                        Matcher countyCategoryMatcher = countyCategoryPattern.matcher(pageText);
                        if (countyCategoryMatcher.find()) {
                            pageText = countyCategoryMatcher
                                .replaceAll("[[Categorie:" + (communeType.equals("comună") ? "Comune" : "Orașe")
                                    + " în județul " + eachCounty + '|' + sortingKey + "]]");
                        }

                        if (!StringUtils.equals(pageText, initialPageText)) {
                            rowiki.edit(rocommunearticle, pageText,
                                "Eliminare din infocasetă parametri migrați la Wikidata sau fără valoare, revizitat introducere standard.");
                            communeChanged = true;
                        }

                    }
                    if (communeChanged) {
                        long sleeptime = 10 + Math.round(30 * Math.random());
                        System.out.println("Sleeping " + sleeptime + "s");
                        Thread.sleep(1000l * sleeptime);
                    }
                }
            }
        } catch (

        WikibaseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (FailedLoginException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (LoginException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if (null != dwiki) {
                dwiki.logout();
            }
        }
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
        String areaString = trim(removeStart(removeEnd(trim(data), "}}"), "{{formatnum:"));
        double area = 0d;
        Matcher roFormatedNumberMatcher = roFormattedNumberStringRegex.matcher(areaString);
        Matcher simpleRefMatcher = simpleRefPattern.matcher(areaString);
        String ref = null;
        if (roFormatedNumberMatcher.find()) {
            NumberFormat nf = NumberFormat.getNumberInstance(new Locale("ro"));
            area = nf.parse(roFormatedNumberMatcher.group(0)).doubleValue();
        } else if (simpleRefMatcher.find()) {
            ref = simpleRefMatcher.group(1);
            areaString = replace(areaString, simpleRefMatcher.group(0), "");
            area = Double.parseDouble(areaString);
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
        if (Arrays.asList("Iași", "Vaslui", "Galați").contains(trim(county))) {
            return MOLDOVA_LINK;
        }
        if (Arrays.asList("Constanța", "Tulcea").contains(trim(county))) {
            return "[[Dobrogea de Nord|Dobrogea]]";
        }
        if (Arrays.asList("Brăila", "Buzău", "Ialomița", "Călărași", "Prahova", "Ilfov", "Giurgiu", "Dâmbovița", "Argeș")
            .contains(trim(county))) {
            return "[[Muntenia]]";
        }
        if (Arrays.asList("Dolj", "Gorj").contains(trim(county))) {
            return "[[Oltenia]]";
        }
        if (Arrays.asList("Timiș").contains(trim(county))) {
            return "[[Banat]]";
        }
        if (Arrays.asList("Covasna", "Harghita", "Mureș", "Bistrița-Năsăud", "Sibiu", "Alba", "Cluj")
            .contains(trim(county))) {
            return "[[Transilvania]]";
        }
        if ("Bihor".equalsIgnoreCase(trim(county))) {
            return "[[Crișana]]";
        }

        // counties broken between regions:
        // Moldova-Transilvania
        if ("Neamț".equalsIgnoreCase(trim(county))) {
            if (Arrays.asList("Bicazu Ardelean", "Bicaz-Chei", "Dămuc").contains(commune)) {
                return "[[Transilvania]]";
            } else {
                return MOLDOVA_LINK;
            }
        }

        if ("Bacău".equalsIgnoreCase(trim(county))) {
            if (Arrays.asList("Ghimeș-Făget").contains(commune) || Arrays.asList("Poiana Sărată").contains(settlement)) {
                return "[[Transilvania]]";
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
                return "[[Muntenia]]";
            } else if (Arrays.asList("Mera", "Vulturu").contains(trim(commune))) {
                if (Arrays.asList("Vulcăneasa").contains(trim(settlement))) {
                    return "[[Muntenia]]";
                }
                if (Arrays.asList("Hângulești", "Maluri").contains(trim(settlement))) {
                    return "[[Muntenia]]";
                }
            }
            return MOLDOVA_LINK;
        }
        // Muntenia-Oltenia
        if ("Teleorman".equalsIgnoreCase(trim(county))) {
            if ("Islaz".equalsIgnoreCase(commune)) {
                return "[[Oltenia]]";
            }
            return "[[Muntenia]]";
        }
        if ("Vâlcea".equalsIgnoreCase(trim(county))) {
            if (Arrays.asList("Berislăvești", "Boișoara", "Budești", "Dăești", "Dănicei", "Drăgoești", "Galicea", "Golești",
                "Milcoiu", "Nicolae Bălcescu", "Olanu", "Perișani", "Racovița", "Runcu", "Sălătrucel", "Stoilești",
                "Titești").contains(trim(commune))) {
                return "[[Muntenia]]";
            } else if (Arrays.asList("Câineni").contains(trim(commune))) {
                if (Arrays.asList("Câinenii Mici", "Greblești", "Priloage").contains(trim(settlement))) {
                    return "[[Muntenia]]";
                }
            }
            return "[[Oltenia]]";
        }
        if ("Olt".equalsIgnoreCase(trim(county))) {
            if (Arrays
                .asList("Bălteni", "Bărăști", "Brebeni", "Colonești", "Corbu", "Coteana", "Crâmpoia", "Cungrea",
                    "Curtișoara", "Dăneasa", "Dobroteasa", "Făgețelu", "Ghimpețeni", "Icoana", "Ipotești", "Izbiceni",
                    "Izvoarele", "Leleasca", "Mărunței", "Mihăești", "Milcov", "Movileni", "Nicolae Titulescu", "Oporelu",
                    "Optași-Măgura", "Perieți", "Poboru", "Priseaca", "Radomirești", "Sâmburești", "Sârbii-Măgura", "Schitu",
                    "Seaca", "Spineni", "Sprâncenata", "Stoicănești", "Șerbănești", "Tătulești", "Teslui", "Topana",
                    "Tufeni", "Vâlcele", "Valea Mare", "Văleni", "Verguleasa", "Vitomirești", "Vulturești")
                .contains(trim(commune))) {
                return "[[Muntenia]]";
            } else if (Arrays.asList("Câineni").contains(trim(commune))) {
                if (Arrays.asList("Câinenii Mici", "Greblești", "Priloage").contains(trim(settlement))) {
                    return "[[Muntenia]]";
                }
            }
            return "[[Oltenia]]";
        }
        // Oltenia-Banat
        if ("Mehedinți".equalsIgnoreCase(trim(county))) {
            if (Arrays.asList("Dubova", "Eșelnița", "Svinița").contains(trim(commune))) {
                return "[[Banat]]";
            }
            return "[[Oltenia]]";
        }
        // Banat-Transilvania
        if ("Caraș-Severin".equalsIgnoreCase(trim(county))) {
            if (Arrays.asList("Băuțar").contains(trim(commune))) {
                return "[[Transilvania]]";
            }
            return "[[Banat]]";
        }
        if ("Hunedoara".equalsIgnoreCase(trim(county))) {
            if ("Zam".equalsIgnoreCase(trim(commune))) {
                if (Arrays.asList("Sălciva", "Pojoga").contains(settlement)) {
                    return "[[Banat]]";
                }
            }
            return "[[Transilvania]]";
        }
        // Banat-Crișana
        if ("Arad".equalsIgnoreCase(trim(county))) {
            if (Arrays.asList("Bata", "Birchiș", "Fântânele", "Felnac", "Frumușeni", "Șagu", "Secusigiu", "Șiștarovăț",
                "Ususău", "Vinga", "Zăbrani", "Zădăreni").contains(trim(commune))) {
                return "[[Banat]]";
            } else if (Arrays.asList("Bârzava", "Conop", "Săvârșin").contains(trim(commune))) {
                if (Arrays.asList("Lalașinț", "Belotinț", "Căprioara", "Chelmac", "Valea Mare").contains(settlement)) {
                    return "[[Banat]]";
                }
            }
            return "[[Crișana]]";
        }
        // Crișana-Sătmar/Maramureș
        if ("Satu Mare".equalsIgnoreCase(trim(county))) {
            return "[[Transilvania]]";
        }
        if ("Maramureș".equalsIgnoreCase(trim(county))) {
            return "[[Transilvania]]";
        }
        if ("Sălaj".equalsIgnoreCase(trim(county))) {
            return "[[Transilvania]]";
        }
        // Transilvania-Muntenia
        if ("Brașov".equalsIgnoreCase(trim(county))) {
            if (null == settlement && "Predeal".equalsIgnoreCase(commune)) {
                return "[[Muntenia]]";
            }
            return "[[Transilvania]]";
        }
        // Bucovina - Moldova
        if ("Botoșani".equalsIgnoreCase(trim(county))) {
            if ("Mihăileni".equalsIgnoreCase(trim(commune))) {
                if (Arrays.asList("Rogojești").contains(trim(settlement)) || null == settlement) {
                    return BUCOVINA_LINK;
                }
            }
            if ("Cândești".equalsIgnoreCase(trim(commune))
                && (null == settlement || "Cândești".equalsIgnoreCase(trim(settlement)))) {
                return BUCOVINA_LINK;
            }
            return MOLDOVA_LINK;
        }
        if ("Suceava".equalsIgnoreCase(trim(county))) {
            if (Arrays.asList("Adâncata", "Baia", "Bogdănești", "Boroaia", "Broșteni", "Crucea", "Dolhești", "Drăgușeni",
                "Dumbrăveni", "Fântâna Mare", "Fântânele", "Forăști", "Grămești", "Hănțești", "Hârtop", "Horodniceni",
                "Mălini", "Panaci", "Preutești", "Rădășeni", "Râșca", "Simimicea", "Slatina", "Vadu Moldovei", "Verești",
                "Vulturești", "Zamostea", "Zvoriștea", "Dolhasca", "Fălticeni").contains(trim(commune))) {
                return MOLDOVA_LINK;
            }

            if ("Bunești".equalsIgnoreCase(trim(commune))) {
                if (Arrays.asList("Uncești", "Petia").contains(trim(settlement))) {
                    return MOLDOVA_LINK;
                }
            }
            if ("Cornu Luncii".equalsIgnoreCase(trim(commune))) {
                if (Arrays.asList("Dumbrava", "Păiseni", "Sasca Mare", "Sasca Mică", "Sasca Nouă", "Șinca")
                    .contains(trim(settlement))) {
                    return MOLDOVA_LINK;
                }
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
            if ("Udești".equalsIgnoreCase(trim(commune))
                && Arrays.asList("Mănăstioara", "Racova", "Știrbăț").contains(trim(settlement))) {
                return MOLDOVA_LINK;
            }

            return BUCOVINA_LINK;
        }
        return null;
    }
}
