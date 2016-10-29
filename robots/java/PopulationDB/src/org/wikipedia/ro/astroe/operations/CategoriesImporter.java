package org.wikipedia.ro.astroe.operations;

import static org.apache.commons.lang3.StringUtils.defaultString;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

import org.apache.commons.lang3.StringUtils;
import org.wikibase.Wikibase;
import org.wikibase.WikibaseException;
import org.wikibase.data.Entity;
import org.wikibase.data.Sitelink;
import org.wikipedia.Wiki;
import org.wikipedia.ro.astroe.FixVillages;

@Operation(useWikibase = true, labelKey = "operation.catimport.label")
public class CategoriesImporter implements WikiOperation {

    private Wiki sourceWiki, targetWiki;
    private Wikibase dataWiki;
    private String article, sourceWikiCode, targetWikiCode;
    private String[] status = new String[] { "status.not.inited" };
    private Pattern problemePattern = Pattern.compile("(\\{\\{\\s*(?:P|p)robleme(?:articol)?(?:\\s*\\|[^\\|\\}]*)*)(\\s*\\|\\s*necat(?:egorizate)?\\s*=[^\\|\\}]*)(\\s*\\|[^\\|\\}]*)*\\}\\}");

    public CategoriesImporter(Wiki targetWiki, Wiki sourceWiki, Wikibase dataWiki, String article) {
        this.targetWiki = targetWiki;
        this.sourceWiki = sourceWiki;
        this.dataWiki = dataWiki;
        this.article = article;
        this.sourceWikiCode = StringUtils.substringBefore(sourceWiki.getDomain(), ".") + "wiki";
        this.targetWikiCode = StringUtils.substringBefore(targetWiki.getDomain(), ".") + "wiki";
    }

    public String execute() throws IOException, WikibaseException, LoginException {
        status = new String[] { "status.searching.wikibase.item.by.article", article, targetWikiCode };
        Entity articleItem = dataWiki.getWikibaseItemBySiteAndTitle(targetWikiCode, article);
        Sitelink sitelink = articleItem.getSitelinks().get(sourceWikiCode);
        if (null == sitelink) {
            throw new WikibaseException("Article in source wiki not found");
        }
        String sourceWikiArticle = sitelink.getPageName();
        status = new String[] { "status.reading.categories", sourceWikiArticle, sourceWikiCode };
        String[] sourceCategories = sourceWiki.getCategories(sourceWikiArticle);
        status = new String[] { "status.reading.categories", article, targetWikiCode };
        String[] targetCategories = targetWiki.getCategories(article);
        List<String> targetCategoriesList = Arrays.asList(targetCategories);
        List<String> categoriesToAdd = new ArrayList<String>();
        for (String eachSourceCat : sourceCategories) {
            String sourceCatFullPageName =
                StringUtils.prependIfMissing(eachSourceCat, sourceWiki.namespaceIdentifier(Wiki.CATEGORY_NAMESPACE));
            Entity eachCatItem = null;

            status = new String[] { "status.searching.corresponding.category",
                StringUtils.removeStart(sourceCatFullPageName, sourceWiki.namespaceIdentifier(Wiki.CATEGORY_NAMESPACE)),
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
                        categoriesToAdd.add(catTargetPage);
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
            StringBuilder articleBuilder = new StringBuilder(targetWiki.getPageText(article));
            int locationOfCats = articleBuilder.indexOf("[[" + targetWiki.namespaceIdentifier(Wiki.CATEGORY_NAMESPACE));
            if (0 <= locationOfCats) {
                articleBuilder.insert(locationOfCats, catBuilder.toString());
            } else {
                articleBuilder.append(catBuilder.toString());
            }
            String newArticleText = articleBuilder.toString();
            newArticleText = newArticleText.replaceAll("\\{\\{\\s*(N|n)ecat(egorizate)?(\\|[^\\}]*)?\\}\\}", "");
            
            StringBuffer sbuf = new StringBuffer();
            Matcher problemeMatcher = problemePattern.matcher(newArticleText);
            while (problemeMatcher.find()) {
                problemeMatcher.appendReplacement(sbuf, problemeMatcher.group(1) + defaultString(problemeMatcher.group(3)) + "}}");
            }
            problemeMatcher.appendTail(sbuf);
            return newArticleText;
        }
        return null;
    }

    public static void main(String[] args) {
        Wiki rowiki = new Wiki("ro.wikipedia.org");
        Wiki enwiki = new Wiki("en.wikipedia.org");
        Wikibase dwiki = new Wikibase("www.wikidata.org");
        if (args.length < 1) {
            System.err.println("Please specify article title");
            System.exit(1);
        }
        String article = args[0];

        try {
            final Properties credentials = new Properties();

            credentials.load(FixVillages.class.getClassLoader().getResourceAsStream("credentials.properties"));

            final String rowpusername = credentials.getProperty("rowiki.user");
            final String rowppassword = credentials.getProperty("rowiki.password");

            rowiki.login(rowpusername, rowppassword);
            rowiki.setMarkBot(true);

            CategoriesImporter importer = new CategoriesImporter(rowiki, enwiki, dwiki, article);
            String s = importer.execute();
            rowiki.edit(article, s, "Importat categorii de la en.wp");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (FailedLoginException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (WikibaseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (LoginException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            rowiki.logout();
        }
    }

    @Override
    public String[] getStatus() {
        return status;
    }

}
