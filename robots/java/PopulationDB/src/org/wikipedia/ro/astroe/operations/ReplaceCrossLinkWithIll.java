package org.wikipedia.ro.astroe.operations;

import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.prependIfMissing;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.apache.commons.lang3.StringUtils.replace;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.startsWithAny;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.apache.commons.lang3.StringUtils.trim;

import java.io.IOException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.login.LoginException;

import org.apache.commons.lang3.StringUtils;
import org.wikibase.Wikibase;
import org.wikibase.WikibaseException;
import org.wikibase.data.Entity;
import org.wikibase.data.Sitelink;
import org.wikipedia.Wiki;
import org.wikipedia.ro.astroe.FixVillages;

@Operation(labelKey = "operation.insertill.label", useWikibase = true)
public class ReplaceCrossLinkWithIll implements WikiOperation {

    public static void main(String[] args) {
        Wiki wiki = new Wiki("ro.wikipedia.org");
        Wiki enwiki = null;
        try {
            final Properties credentials = new Properties();

            credentials.load(FixVillages.class.getClassLoader().getResourceAsStream("credentials.properties"));

            final String rowpusername = credentials.getProperty("rowiki.user");
            final String rowppassword = credentials.getProperty("rowiki.password");
            wiki.login(rowpusername, rowppassword.toCharArray());
            wiki.setMarkBot(true);
            for (int i = 0; i < args.length; i += 2) {
                String eachArticle = args[i];
                String lang = "en";
                if (i + 1 < args.length) {
                    lang = args[i + 1];
                }
                enwiki = new Wiki(lang + ".wikipedia.org");
                Wikibase wb = new Wikibase("www.wikidata.org");
                ReplaceCrossLinkWithIll replacer = new ReplaceCrossLinkWithIll(wiki, enwiki, wb, eachArticle);
                String s = replacer.execute();
                wiki.edit(eachArticle, s, "Înlocuit linkuri interne și externe către alt wiki generate de CX cu format Ill");
            }
        } catch (LoginException | IOException | WikibaseException e) {
            e.printStackTrace();
        }
    }

    private Wiki targetWiki;
    private Wiki sourceWiki;
    private Wikibase dataWiki;
    private String article;
    private String sourceWikiCode;
    private String targetWikiCode;

    public ReplaceCrossLinkWithIll(Wiki targetWiki, Wiki sourceWiki, Wikibase dataWiki, String article) {
        this.targetWiki = targetWiki;
        this.sourceWiki = sourceWiki;
        this.dataWiki = dataWiki;
        this.article = article;
        this.sourceWikiCode = StringUtils.substringBefore(sourceWiki.getDomain(), ".") + "wiki";
        this.targetWikiCode = StringUtils.substringBefore(targetWiki.getDomain(), ".") + "wiki";
    }

    public String execute() throws IOException, LoginException, WikibaseException {
        String text = targetWiki.getPageText(article);

        String extLinkRegEx = "\\[\\[\\:(?<lang>.*?)\\:(?<foreigntitle>.*?)(\\|(?<locallabel>.*?))?\\]]";
        Pattern extLinkPattern = Pattern.compile(extLinkRegEx, Pattern.DOTALL);
        Matcher extLinkMatcher = extLinkPattern.matcher(text);
        StringBuffer newText = new StringBuffer();
        int countMatches = 0;

        while (extLinkMatcher.find()) {
            String lang = extLinkMatcher.group("lang");
            if (3 < lang.length()) {
                continue;
            }
            String foreignTitle = replace(extLinkMatcher.group("foreigntitle"), "_", " ");
            String localLabel = extLinkMatcher.group("locallabel");

            String roLabel = null;
            Entity wbEntity = null;
            try {
                if ("d".equals(lang)) {
                    wbEntity = dataWiki.getWikibaseItemById(defaultString(dataWiki.resolveRedirect(foreignTitle), foreignTitle));
                } else {
                    Wiki sourceWiki = new Wiki(lang + ".wikipedia.org");
                    String target = defaultString(sourceWiki.resolveRedirect(foreignTitle), foreignTitle);
                    wbEntity = dataWiki.getWikibaseItemBySiteAndTitle(lang + "wiki", target);
                }
                roLabel = wbEntity.getLabels().get("ro");
            } catch (WikibaseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            String replacedString = null;
            if (null == wbEntity) {
                replacedString = "{{Ill|" + lang + (roLabel != null ? ("|" + roLabel) : "") + "|" + foreignTitle
                    + (null != localLabel ? ("|4=" + localLabel) : "") + "}}";
            } else {
                replacedString = "{{Ill-wd|" + prependIfMissing(wbEntity.getId(), "Q")
                    + (null != localLabel ? ("|3=" + localLabel) : "") + "}}";
            }

            extLinkMatcher.appendReplacement(newText,
                startsWith(foreignTitle, "Special:") ? extLinkMatcher.group(0) : replacedString);
            System.out.println(extLinkMatcher.group(0) + " ---> " + replacedString);
            countMatches++;
        }
        System.out.println(countMatches + " found");
        extLinkMatcher.appendTail(newText);

        String wlAsExtLinkRegEx = "\\[(https?\\:)?//([^\\.]+)\\.wikipedia.org/wiki/([^\\s]+)\\s+([^\\]]+)\\]";
        Pattern wlAsExtLinkPattern = Pattern.compile(wlAsExtLinkRegEx);
        Matcher wlAsExtLinkMatcher = wlAsExtLinkPattern.matcher(newText.toString());
        StringBuffer anotherNewText = new StringBuffer();
        while (wlAsExtLinkMatcher.find()) {
            String lang = wlAsExtLinkMatcher.group(2);
            String articleTitle = wlAsExtLinkMatcher.group(3);
            String linkTitle = wlAsExtLinkMatcher.group(4);

            Wiki sourceWiki = new Wiki(lang + ".wikipedia.org");
            String target = defaultString(sourceWiki.resolveRedirect(articleTitle), articleTitle);
            Entity wbEntity = null;
            String targetLang = removeEnd(targetWikiCode, "wiki");
            String sourceLang = removeEnd(sourceWikiCode, "wiki");
            String roLabel = null;
            String roArticle = null;
            try {
                wbEntity = dataWiki.getWikibaseItemBySiteAndTitle(lang + "wiki", target);
                if (null != wbEntity) {
                    roLabel = wbEntity.getLabels().get(targetLang);
                    Sitelink roSitelink = wbEntity.getSitelinks().get(targetWikiCode);
                    if (null != roSitelink) {
                        roArticle = roSitelink.getPageName();
                    }
                }
            } catch (WikibaseException e) {
                e.printStackTrace();
            }
            String replacedString;
            if (null == wbEntity) {
                replacedString = "{{Ill|" + lang + (roLabel != null ? ("|" + roLabel) : "") + "|" + articleTitle
                    + (null != linkTitle ? ("|4=" + linkTitle) : "") + "}}";
            } else if (null != roArticle) {
                replacedString = "[[" + roArticle + (roArticle.equals(linkTitle) ? "" : ("|" + linkTitle)) + "]]";
            } else {
                replacedString = "{{Ill-wd|" + prependIfMissing(wbEntity.getId(), "Q")
                    + (null != linkTitle ? ("|3=" + linkTitle) : "") + "}}";
            }
            wlAsExtLinkMatcher.appendReplacement(anotherNewText,
                startsWith(articleTitle, "Special:") ? wlAsExtLinkMatcher.group(0) : replacedString);
            System.out.println(wlAsExtLinkMatcher.group(0) + " ---> " + replacedString);
            countMatches++;
        }
        System.out.println(countMatches + " found");
        wlAsExtLinkMatcher.appendTail(anotherNewText);

        String innerLinkRegEx = "\\[\\[([^:][^|\\]]+?)(\\|(.*?))?\\]\\]";
        Pattern innerLinkPattern = Pattern.compile(innerLinkRegEx);
        Matcher innerLinkMatcher = innerLinkPattern.matcher(anotherNewText.toString());
        anotherNewText = new StringBuffer();
        while (innerLinkMatcher.find()) {

            System.out.println("Link:" + innerLinkMatcher.group(1));
            String articleLink = removeStart(trim(innerLinkMatcher.group(1)), " ");
            String articleTitle = capitalize(substringBefore(articleLink, "#"));
            String linkTitle = innerLinkMatcher.group(3);
            if (isBlank(articleTitle)) {
                continue;
            }
            if (startsWithAny(lowerCase(articleLink), "google:", "wiktionary:", "iarchive:", "file:", "fișier:", "image:",
                "imagine:", "categorie:", "category:", "arxiv:", "openlibrary:", "s:", "imdbname:", "c:file:", "doi:",
                "bibcode:", "imdbtitle:")) {
                continue;
            }
            articleTitle = defaultString(targetWiki.resolveRedirect(articleTitle), articleTitle);
            System.out.println("Link to article: " + articleTitle);

            if (targetWiki.exists(new String[] { articleTitle })[0]) {
                continue;
            }
            String enArticleTitle = defaultString(sourceWiki.resolveRedirect(articleTitle), articleTitle);
            String replacedString = innerLinkMatcher.group(0);
            if (sourceWiki.exists(new String[] { enArticleTitle })[0]) {
                Entity wbEntity = null;
                String roLabel = null;
                try {
                    wbEntity = dataWiki.getWikibaseItemBySiteAndTitle(sourceWikiCode, enArticleTitle);
                    if (null != wbEntity) {
                        roLabel = wbEntity.getLabels().get("ro");
                    }
                } catch (WikibaseException e) {
                    e.printStackTrace();
                }
                String sourceLang = removeEnd(sourceWikiCode, "wiki");
                if (null == wbEntity) {
                    replacedString = "{{Ill|" + sourceLang + (roLabel != null ? ("|" + roLabel) : "") + "|" + articleLink
                        + (null != linkTitle ? ("|4=" + linkTitle) : "") + "}}";
                } else {
                    replacedString = "{{Ill-wd|" + prependIfMissing(wbEntity.getId(), "Q")
                        + (null != linkTitle ? ("|3=" + linkTitle) : "") + "}}";
                }
            }
            innerLinkMatcher.appendReplacement(anotherNewText, replacedString);

        }
        innerLinkMatcher.appendTail(anotherNewText);

        return anotherNewText.toString();
    }

}
