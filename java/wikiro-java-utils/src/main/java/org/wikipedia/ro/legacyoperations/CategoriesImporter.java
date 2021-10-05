package org.wikipedia.ro.legacyoperations;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.prependIfMissing;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.substringBefore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.login.LoginException;

import org.wikibase.Wikibase;
import org.wikibase.WikibaseException;
import org.wikibase.data.Entity;
import org.wikibase.data.Sitelink;
import org.wikipedia.Wiki;

@Operation(useWikibase = true, labelKey = "operation.catimport.label")
public class CategoriesImporter implements WikiOperation {

    private Wiki sourceWiki, targetWiki;
    private Wikibase dataWiki;
    private String article, sourceWikiCode, targetWikiCode;
    private String[] status = new String[] { "status.not.inited" };
    private Pattern problemePattern = Pattern.compile(
        "(\\{\\{\\s*(?:P|p)robleme(?:articol)?(?:\\s*\\|[^\\|\\}]*)*)(\\s*\\|\\s*necat(?:egorizate)?\\s*=[^\\|\\}]*?)((?:\\s*\\|[^\\|\\}]*)*)\\}\\}");
    private static final Map<String, Set<String>> MAINTENANCE_CATEGORIES = new HashMap<String, Set<String>>() {
        {
            put("rowiki", new HashSet<String>() {
                {
                    add("Articole ");
                    add("Pagini ");
                    add("Pages ");
                    add("1911 Britannica");
                    add("Cioturi");
                }
            });
        }
    };

    public CategoriesImporter(Wiki targetWiki, Wiki sourceWiki, Wikibase dataWiki, String article) {
        this.targetWiki = targetWiki;
        this.sourceWiki = sourceWiki;
        this.dataWiki = dataWiki;
        this.article = article;
        this.sourceWikiCode = substringBefore(sourceWiki.getDomain(), ".") + "wiki";
        this.targetWikiCode = substringBefore(targetWiki.getDomain(), ".") + "wiki";
    }

    public String execute() throws IOException, LoginException {
        status = new String[] { "status.searching.wikibase.item.by.article", article, targetWikiCode };
        StringBuffer articleBuilder = new StringBuffer(targetWiki.getPageText(List.of(article)).stream().findFirst().orElse(""));
        Entity articleItem = null;
        try {
            articleItem = dataWiki.getWikibaseItemBySiteAndTitle(targetWikiCode, article);
        } catch (WikibaseException e) {
            return articleBuilder.toString();
        }
        if (null == articleItem) {
            return articleBuilder.toString();
        }
        Sitelink sitelink = articleItem.getSitelinks().get(sourceWikiCode);
        if (null == sitelink) {
            return articleBuilder.toString();
        }
        String sourceWikiArticle = sitelink.getPageName();
        status = new String[] { "status.reading.categories", sourceWikiArticle, sourceWikiCode };
        List<String> sourceCategories = sourceWiki.getCategories(List.of(sourceWikiArticle), sourceWiki.new RequestHelper(), false).stream().findFirst().orElse(List.of());
        status = new String[] { "status.reading.categories", article, targetWikiCode };
        List<String> targetCategoriesList = targetWiki.getCategories(List.of(article), targetWiki.new RequestHelper(), false).stream().findFirst().orElse(List.of());
        List<String> categoriesToAdd = new ArrayList<String>();
        for (String eachSourceCat : sourceCategories) {
            String sourceCatFullPageName =
                prependIfMissing(eachSourceCat, sourceWiki.namespaceIdentifier(Wiki.CATEGORY_NAMESPACE));
            Entity eachCatItem = null;

            status = new String[] { "status.searching.corresponding.category",
                removeStart(sourceCatFullPageName, sourceWiki.namespaceIdentifier(Wiki.CATEGORY_NAMESPACE)),
                sourceWikiCode };
            try {
                eachCatItem = dataWiki.getWikibaseItemBySiteAndTitle(sourceWikiCode, sourceCatFullPageName);
            } catch (WikibaseException wbe) {
            }
            if (null != eachCatItem) {
                Sitelink catTargetSitelink = eachCatItem.getSitelinks().get(targetWikiCode);
                if (null != catTargetSitelink) {
                    String catTargetPage = catTargetSitelink.getPageName();
                    if (!targetCategoriesList.contains(catTargetPage)) {
                        boolean maintCat = false;
                        if (MAINTENANCE_CATEGORIES.containsKey(targetWikiCode)) {
                            for (String eachMainPrefix : MAINTENANCE_CATEGORIES.get(targetWikiCode)) {
                                if (startsWith(removeStart(catTargetPage,
                                    targetWiki.namespaceIdentifier(Wiki.CATEGORY_NAMESPACE) + ":"), eachMainPrefix)) {
                                    maintCat = true;
                                    break;
                                }
                            }
                        }
                        if (!maintCat) {
                            categoriesToAdd.add(catTargetPage);
                        }
                    }
                }
            }
        }

        status = new String[] { "status.inserting.categories" };
        StringBuilder catBuilder = new StringBuilder();
        for (String eachCatToAdd : categoriesToAdd) {
            catBuilder.append("[[");
            catBuilder.append(eachCatToAdd);
            catBuilder.append("]]\n");
            // System.out.println("To add: " + eachCatToAdd);
        }

        if (0 < catBuilder.length()) {
            int locationOfCats = articleBuilder.indexOf("[[" + targetWiki.namespaceIdentifier(Wiki.CATEGORY_NAMESPACE));
            if (0 <= locationOfCats) {
                articleBuilder.insert(locationOfCats, catBuilder.toString());
            } else {
                articleBuilder.append(catBuilder.toString());
            }
            String newArticleText = articleBuilder.toString();
            newArticleText = newArticleText.replaceAll("\\{\\{\\s*(N|n)ecat(egorizate)?(\\|[^\\}]*)?\\}\\}", "");

            articleBuilder = new StringBuffer();
            Matcher problemeMatcher = problemePattern.matcher(newArticleText);
            while (problemeMatcher.find()) {
                problemeMatcher.appendReplacement(articleBuilder,
                    problemeMatcher.group(1) + defaultString(problemeMatcher.group(3)) + "}}");
            }
            problemeMatcher.appendTail(articleBuilder);
        }
        return articleBuilder.toString();
    }

    public String[] getStatus() {
        return status;
    }

}
