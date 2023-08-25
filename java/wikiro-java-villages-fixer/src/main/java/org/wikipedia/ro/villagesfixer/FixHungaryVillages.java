package org.wikipedia.ro.villagesfixer;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.prependIfMissing;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.substring;
import static org.apache.commons.lang3.StringUtils.trim;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.io.Console;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.login.LoginException;

import org.apache.commons.lang3.StringUtils;
import org.wikibase.Wikibase;
import org.wikibase.WikibaseException;
import org.wikibase.WikibasePropertyFactory;
import org.wikibase.data.Claim;
import org.wikibase.data.Entity;
import org.wikibase.data.Item;
import org.wikibase.data.Property;
import org.wikipedia.Wiki;
import org.wikipedia.ro.model.WikiLink;
import org.wikipedia.ro.model.WikiPart;
import org.wikipedia.ro.model.WikiTemplate;
import org.wikipedia.ro.parser.ParseResult;
import org.wikipedia.ro.parser.WikiTemplateParser;

public class FixHungaryVillages {

    public static void main(String[] args) throws IOException {
        Wiki rowiki = Wiki.newSession("ro.wikipedia.org");
        Wikibase dwiki = new Wikibase();

        List<String> countyCategoryMembersList =
            rowiki.getCategoryMembers("Category:Județele Ungariei", Wiki.CATEGORY_NAMESPACE);
        List<String> countyNames = new ArrayList<>();
        for (String eachArticleName : countyCategoryMembersList) {
            String countyNameNoCat = removeStart(eachArticleName, "Categorie:");
            if (startsWith(countyNameNoCat, "Județul")) {
                countyNames.add(trim(removeStart(countyNameNoCat, "Județul")));
            }
        }

        try {
            String rowpusername = System.getenv("WP_USER");
            // String datausername = System.getenv("WD_USER");

            char[] rowppassword = null == System.getenv("WP_PASS") ? null : System.getenv("WP_PASS").toCharArray();
            // char[] datapassword = null == System.getenv("WD_PASS") ? null : System.getenv("WD_PASS").toCharArray();
            Console sysConsole = System.console();
            if (null == rowpusername) {
                sysConsole.printf("Wiki username:");
                rowpusername = sysConsole.readLine();
            }
            if (null == rowppassword) {
                sysConsole.printf("Wiki password:");
                rowppassword = sysConsole.readPassword();
            }

            rowiki.login(rowpusername, rowppassword);
            rowiki.setMarkBot(true);
            Pattern infoboxPattern = Pattern.compile("\\{\\{\\s*[Ii]nfocaseta Așezare");
            Property coAProperty = WikibasePropertyFactory.getWikibaseProperty("P94");
            Property flagProperty = WikibasePropertyFactory.getWikibaseProperty("P41");
            Property coordProperty = WikibasePropertyFactory.getWikibaseProperty("P625");
            Property adminTerrProperty = WikibasePropertyFactory.getWikibaseProperty("P131");
            Property imageProperty = WikibasePropertyFactory.getWikibaseProperty("P18");

            String countyStart = null;
            boolean countyTouched = args.length < 1;
            if (args.length > 0) {
                countyStart = args[0];
            }
            for (String eachCounty : countyNames) {
                if (!countyTouched && !eachCounty.equals(countyStart)) {
                    continue;
                }
                countyTouched = true;
                List<String> categoryMembers =
                    rowiki.getCategoryMembers("Category:Orașe în județul " + eachCounty, Wiki.MAIN_NAMESPACE);
                List<String> categoryMembersList = new ArrayList<>();
                categoryMembersList.addAll(categoryMembers);
                categoryMembersList
                    .addAll(rowiki.getCategoryMembers("Category:Sate în județul " + eachCounty, Wiki.MAIN_NAMESPACE));

                List<String> subcategories =
                    rowiki.getCategoryMembers("Category:Orașe în județul " + eachCounty, Wiki.CATEGORY_NAMESPACE);
                List<String> subcategoriesList = new ArrayList<>();
                subcategoriesList.addAll(subcategories);
                subcategoriesList
                    .addAll(rowiki.getCategoryMembers("Category:Sate în județul " + eachCounty, Wiki.CATEGORY_NAMESPACE));
                for (String eachSubcat : subcategoriesList) {
                    String catTitle = removeStart(eachSubcat, "Categorie:");
                    List<String> subarticles = rowiki.getCategoryMembers(eachSubcat, Wiki.MAIN_NAMESPACE);
                    for (String eachSubarticle : subarticles) {
                        if (StringUtils.equals(eachSubarticle, catTitle)) {
                            categoryMembersList.add(eachSubarticle);
                        }
                    }
                }

                for (String eachSettlement : categoryMembersList) {
                    String articleText = rowiki.getPageText(List.of(eachSettlement)).stream().findFirst().orElse("");
                    Matcher infoboxMatcher = infoboxPattern.matcher(articleText);
                    if (infoboxMatcher.find()) {
                        int infoboxLocation = infoboxMatcher.start();

                        WikiTemplateParser infoboxParser = new WikiTemplateParser();
                        ParseResult<WikiTemplate> infoboxParseResult =
                            infoboxParser.parse(substring(articleText, infoboxLocation));
                        WikiTemplate infobox = infoboxParseResult.getIdentifiedPart();

                        Entity settlementEntity = dwiki.getWikibaseItemBySiteAndTitle("rowiki", eachSettlement);
                        Set<Claim> coAClaims = settlementEntity.getBestClaims(coAProperty);
                        if (null != coAClaims) {
                            for (Claim eachCoAClaim : coAClaims) {
                                if ("statement".equals(eachCoAClaim.getType())
                                    && "value".equals(eachCoAClaim.getMainsnak().getSnaktype())) {
                                    infobox.removeParam("stemă");
                                    break;
                                }
                            }
                        }
                        Set<Claim> imageClaims = settlementEntity.getBestClaims(imageProperty);
                        if (null != imageClaims) {
                            for (Claim eachImageClaim : imageClaims) {
                                if ("statement".equals(eachImageClaim.getType())
                                    && "value".equals(eachImageClaim.getMainsnak().getSnaktype())) {
                                    infobox.removeParam("imagine");
                                    infobox.removeParam("imagine_descriere");
                                    infobox.removeParam("image");
                                    infobox.removeParam("caption");
                                    break;
                                }
                            }
                        }
                        Set<Claim> flagClaims = settlementEntity.getBestClaims(flagProperty);
                        if (null != flagClaims) {
                            for (Claim eachFlagClaim : flagClaims) {
                                if ("statement".equals(eachFlagClaim.getType())
                                    && "value".equals(eachFlagClaim.getMainsnak().getSnaktype())) {
                                    infobox.removeParam("steag");
                                    infobox.removeParam("drapel");
                                    break;
                                }
                            }
                        }
                        Set<Claim> coordClaims = settlementEntity.getBestClaims(coordProperty);
                        if (null != coordClaims) {
                            for (Claim eachCoordClaim : coordClaims) {
                                if ("statement".equals(eachCoordClaim.getType())
                                    && "value".equals(eachCoordClaim.getMainsnak().getSnaktype())) {
                                    infobox.removeParam("latd");
                                    infobox.removeParam("latm");
                                    infobox.removeParam("lats");
                                    infobox.removeParam("latNS");
                                    infobox.removeParam("lat");
                                    infobox.removeParam("longd");
                                    infobox.removeParam("longm");
                                    infobox.removeParam("longs");
                                    infobox.removeParam("longEV");
                                    infobox.removeParam("long");
                                    infobox.removeParam("longEW");
                                    infobox.removeParam("coordonate");
                                    break;
                                }
                            }
                        }

                        Set<Claim> adminTerrClaims = settlementEntity.getBestClaims(adminTerrProperty);
                        if (null != adminTerrClaims) {
                            for (Claim eachAdminTerrClaim : adminTerrClaims) {
                                if ("statement".equals(eachAdminTerrClaim.getType())
                                    && "value".equals(eachAdminTerrClaim.getMainsnak().getSnaktype())) {

                                    Item adminTerrItem = (Item) eachAdminTerrClaim.getMainsnak().getData();
                                    Optional<WikiPart> optLink =
                                        defaultIfNull(infobox.getParam("nume_subdiviziune2"), Collections.EMPTY_LIST)
                                            .stream().filter(wpart -> wpart instanceof WikiLink).findFirst();
                                    if (optLink.isPresent()) {
                                        WikiLink adminTerrLink = (WikiLink) optLink.get();
                                        String adminTerrArticle =
                                            defaultString(
                                                rowiki.resolveRedirects(List.of(adminTerrLink.getTarget())).stream()
                                                    .findFirst().orElse(adminTerrLink.getTarget()),
                                                adminTerrLink.getTarget());
                                        Entity adminTerrItemFromInfobox =
                                            dwiki.getWikibaseItemBySiteAndTitle("rowiki", adminTerrArticle);
                                        if (null != adminTerrItem && null != adminTerrItemFromInfobox
                                            && prependIfMissing(adminTerrItem.getEnt().getId(), "Q")
                                                .equals(prependIfMissing(adminTerrItemFromInfobox.getId(), "Q"))) {
                                            infobox.removeParam("tip_subdiviziune");
                                            infobox.removeParam("tip_subdiviziune1");
                                            infobox.removeParam("tip_subdiviziune2");
                                            infobox.removeParam("nume_subdiviziune");
                                            infobox.removeParam("nume_subdiviziune1");
                                            infobox.removeParam("nume_subdiviziune2");
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                        String newInfoboxText = infobox.toString();
                        StringBuilder articleBuilder = new StringBuilder(articleText);
                        articleBuilder.replace(infoboxLocation, infoboxLocation + infobox.getTemplateLength(),
                            newInfoboxText);

                        if (!StringUtils.equals(articleBuilder.toString(), articleText)) {
                            rowiki.edit(eachSettlement, articleBuilder.toString(), "Robot: curățenie infocasetă");
                        }
                    }
                }
            }
        } catch (WikibaseException | LoginException e) {
            e.printStackTrace();
        } finally {
            if (null != dwiki) {
                dwiki.logout();
            }
        }
    }

}
