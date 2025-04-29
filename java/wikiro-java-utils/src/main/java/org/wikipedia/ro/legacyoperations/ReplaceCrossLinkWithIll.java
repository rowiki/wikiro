package org.wikipedia.ro.legacyoperations;

import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.equalsAny;
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
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.security.auth.login.LoginException;

import org.apache.commons.lang3.StringUtils;
import org.wikibase.Wikibase;
import org.wikibase.WikibaseException;
import org.wikibase.data.Entity;
import org.wikibase.data.Sitelink;
import org.wikipedia.Wiki;
import org.wikipedia.ro.model.WikiLink;
import org.wikipedia.ro.model.WikiTemplate;
import org.wikipedia.ro.utils.WikidataCacheManager;

@Operation(labelKey = "operation.insertill.label", useWikibase = true)
public class ReplaceCrossLinkWithIll implements WikiOperation {
    private static final Logger LOG = Logger.getLogger(ReplaceCrossLinkWithIll.class.getCanonicalName());
    private Wiki targetWiki;
    private Wiki sourceWiki;
    private Wikibase dataWiki;
    private String article;
    private String sourceWikiCode;
    private String targetWikiCode;
    private String[] status = new String[] { "status.not.inited" };

    private Map<String, String> roArticlesCache = new HashMap<>();

    public ReplaceCrossLinkWithIll(Wiki targetWiki, Wiki sourceWiki, Wikibase dataWiki, String article) {
        this.targetWiki = targetWiki;
        this.sourceWiki = sourceWiki;
        this.dataWiki = dataWiki;
        this.article = article;
        this.sourceWikiCode = StringUtils.substringBefore(sourceWiki.getDomain(), ".") + "wiki";
        this.targetWikiCode = StringUtils.substringBefore(targetWiki.getDomain(), ".") + "wiki";
    }

    public String execute() throws IOException, LoginException, WikibaseException {
        status = new String[] { "status.reading.text", article, targetWikiCode };
        LOG.log(Level.INFO, "Replacing cross links with Ill in article ''{0}''", article);
        String text = targetWiki.getPageText(List.of(article)).stream().findFirst().orElse("");
        Pattern namespacepattern = Pattern.compile("((?:Template|Wikipedia):)?(.*)");

        status = new String[] { "status.identifying.links" };
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

            status = new String[] { "status.analyzing.link", foreignTitle };

            String roTitle = roArticlesCache.get(lang + ":" + foreignTitle);
            Entity wbEntity = WikidataCacheManager.getWikidataEntitiesCache(dataWiki).getByArticle(lang + "wiki", foreignTitle);
            if (null == roTitle && null == wbEntity) {
                try {
                    if (null == wbEntity) {
                        if (equalsAny(lowerCase(lang), "s", "wikt")) {
                            continue;
                        } else if ("d".equals(lang)) {
                            wbEntity = dataWiki
                                .getWikibaseItemById(WikidataCacheManager.getCachedRedirect(dataWiki, foreignTitle));
                        } else {
                            String fullForeignTitle = substringBefore(foreignTitle, "#");
                            Matcher namespaceMatcher = namespacepattern.matcher(fullForeignTitle);
                            namespaceMatcher.matches();
                            String namespace = defaultString(namespaceMatcher.group(1));
                            String simpleForeignTitle = namespaceMatcher.group(2);
                            foreignTitle = capitalize(namespace) + capitalize(simpleForeignTitle);
                            String target = WikidataCacheManager.getCachedRedirect(sourceWiki, foreignTitle);
                            wbEntity = dataWiki.getWikibaseItemBySiteAndTitle(lang + "wiki", target);
                        }
                    }
                    roTitle = Optional.ofNullable(wbEntity).map(Entity::getLabels).map(m -> m.get("ro")).orElse(null);
                    if (!isBlank(roTitle)) {
                        roArticlesCache.put(lang + ":" + foreignTitle, roTitle);
                    }
                } catch (WikibaseException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            String replacedString = null;
            if (null != roTitle && StringUtils.equals(roTitle, localLabel)) {
                replacedString = new WikiLink(roTitle, localLabel).toString();
            } else if (null == wbEntity) {
                replacedString = new WikiTemplate().setTemplateTitle("Ill").setSingleLine(true).setParam("1", lang)
                    .setParam("2", roTitle).setParam("3", foreignTitle).setParam("4", localLabel).toString();
            } else {
                replacedString = new WikiTemplate().setTemplateTitle("Ill-wd").setSingleLine(true)
                    .setParam("1", prependIfMissing(wbEntity.getId(), "Q")).setParam("3", localLabel).toString();
            }

            if (null != replacedString) {
                extLinkMatcher.appendReplacement(newText,
                    startsWith(foreignTitle, "Special:") ? extLinkMatcher.group(0) : replacedString);
            }
            LOG.log(Level.INFO, "{0} ---> {1}", new Object[] { extLinkMatcher.group(0), replacedString});
            countMatches++;
        }
        LOG.log(Level.INFO, "{0} found", countMatches);
        extLinkMatcher.appendTail(newText);

        String wlAsExtLinkRegEx = "\\[(https?\\:)?//([^\\.]+)(?:\\.m)?\\.wikipedia.org/wiki/([^\\s]+)\\s+([^\\]]+)\\]";
        Pattern wlAsExtLinkPattern = Pattern.compile(wlAsExtLinkRegEx);
        Matcher wlAsExtLinkMatcher = wlAsExtLinkPattern.matcher(newText.toString());
        StringBuffer anotherNewText = new StringBuffer();
        while (wlAsExtLinkMatcher.find()) {
            String lang = wlAsExtLinkMatcher.group(2);
            String articleTitle = URLDecoder.decode(wlAsExtLinkMatcher.group(3), "UTF-8");
            String linkTitle = wlAsExtLinkMatcher.group(4);

            status = new String[] { "status.analyzing.link", articleTitle };

            Wiki srcWiki = Wiki.newSession(lang + ".wikipedia.org");
            String target = WikidataCacheManager.getCachedRedirect(srcWiki, articleTitle);
            String targetLang = removeEnd(targetWikiCode, "wiki");
            String sourceLang = defaultIfEmpty(lang, removeEnd(sourceWikiCode, "wiki"));

            String roLabel = null;
            String roArticle = roArticlesCache.get(sourceLang + ":" + target);
            Entity wbEntity = WikidataCacheManager.getWikidataEntitiesCache(dataWiki).getByArticle(sourceLang + "wiki", target);
            if (null == roArticle && null == wbEntity) {
                try {
                    wbEntity = dataWiki.getWikibaseItemBySiteAndTitle(sourceLang + "wiki", target);
                    if (null != wbEntity) {
                        roLabel = wbEntity.getLabels().get(targetLang);
                        Sitelink roSitelink = wbEntity.getSitelinks().get(targetWikiCode);
                        if (null != roSitelink) {
                            roArticle = roSitelink.getPageName();
                            roArticlesCache.put(sourceLang + ":" + target, roArticle);
                        }
                    }
                } catch (WikibaseException e) {
                    e.printStackTrace();
                }
            }
            String replacedString;
            if (null != roArticle) {
                replacedString = new WikiLink(roArticle, defaultString(linkTitle, articleTitle)).toString();
            } else if (null == wbEntity) {
                replacedString = new WikiTemplate().setSingleLine(true).setTemplateTitle("Ill").setParam("1", lang)
                    .setParam("2", roLabel).setParam("3", articleTitle).setParam("4", linkTitle).toString();
            } else {
                replacedString = new WikiTemplate().setSingleLine(true).setTemplateTitle("Ill-wd")
                    .setParam("1", prependIfMissing(wbEntity.getId(), "Q")).setParam("3", linkTitle).toString();
            }
            wlAsExtLinkMatcher.appendReplacement(anotherNewText,
                startsWith(articleTitle, "Special:") ? wlAsExtLinkMatcher.group(0) : replacedString);
            LOG.log(Level.INFO, "{0} ---> {1}", new Object[] {wlAsExtLinkMatcher.group(0), replacedString});
            countMatches++;
        }
        LOG.log(Level.INFO, "{0} found", countMatches);
        wlAsExtLinkMatcher.appendTail(anotherNewText);

        String innerLinkRegEx = "\\[\\[([^:][^\\|\\]\\[]+?)(\\|((?:([^\\|\\]\\[]*?)|(\\{\\{[^\\}]*\\}\\}))+))?\\]\\]";
        Pattern innerLinkPattern = Pattern.compile(innerLinkRegEx);
        Matcher innerLinkMatcher = innerLinkPattern.matcher(anotherNewText.toString());
        anotherNewText = new StringBuffer();
        List<String> localLinks = new ArrayList<>();
        Map<String, String> actualLocalTitleMap = new HashMap<>();
        Map<String, Boolean> localLinkExistenceMap = new HashMap<>();

        Map<String, String> actualForeignTitleMap = new HashMap<>();
        Map<String, Boolean> foreignLinkExistenceMap = new HashMap<>();
        while (innerLinkMatcher.find()) {
            // first pass - evaluate links existence

            String link = innerLinkMatcher.group(1);
            //link = URLDecoder.decode(link, StandardCharsets.UTF_8.name());'
            link = replace(link, "_", " ");
            LOG.log(Level.INFO, "First pass: Link: {0}", link);
            String articleLink = removeStart(trim(link), " ");
            String articleTitle = capitalize(substringBefore(articleLink, "#"));
            if (isBlank(articleTitle)) {
                LOG.log(Level.INFO, "Blank article title in link ''{0}''! skipping...", innerLinkMatcher.group(0));
                continue;
            }
            if (isNotReplaceableLink(articleLink)) {
                LOG.log(Level.INFO, "''{0}'' links to something else! Skipping...", articleLink);
                continue;
            }
            if (!localLinks.contains(articleTitle)) {
                localLinks.add(articleTitle);
            }
        }
        List<String> localResolvedRedirects = targetWiki.resolveRedirects(localLinks);
        for (int idx = 0; idx < localLinks.size(); idx++) {
            actualLocalTitleMap.put(localLinks.get(idx), defaultString(localResolvedRedirects.get(idx), localLinks.get(idx)));
        }
        localLinks = actualLocalTitleMap.values().stream().filter(x -> !isNotReplaceableLink(x)).collect(Collectors.toList());
        boolean[] localExistanceArray = targetWiki.exists(localLinks);
        for (int idx = 0; idx < localLinks.size(); idx++) {
            localLinkExistenceMap.put(localLinks.get(idx), Boolean.valueOf(localExistanceArray[idx]));
        }

        List<String> nonExistingLinks = localLinkExistenceMap.keySet().stream()
            .filter(key -> !localLinkExistenceMap.get(key)).collect(Collectors.toList());
        if (!nonExistingLinks.isEmpty()) {
            List<String> actualForeignTitles = sourceWiki.resolveRedirects(nonExistingLinks);
            for (int idx = 0; idx < nonExistingLinks.size(); idx++) {
                actualForeignTitleMap.put(nonExistingLinks.get(idx),
                    defaultString(actualForeignTitles.get(idx), nonExistingLinks.get(idx)));
            }
        }

        nonExistingLinks = actualForeignTitleMap.values().stream().collect(Collectors.toList());
        if (!nonExistingLinks.isEmpty()) {
            boolean[] foreignLinkExistenceArray = sourceWiki.exists(nonExistingLinks);
            for (int idx = 0; idx < foreignLinkExistenceArray.length; idx++) {
                foreignLinkExistenceMap.put(nonExistingLinks.get(idx), Boolean.valueOf(foreignLinkExistenceArray[idx]));
            }
        }
        innerLinkMatcher.reset();
        while (innerLinkMatcher.find()) {
            // second pass - actually perform changes with data already collected in an optimized way

            String link = innerLinkMatcher.group(1);
            //link = URLDecoder.decode(link, StandardCharsets.UTF_8.name());
            link = replace(link, "_", " ");
            LOG.log(Level.INFO, "Second pass: Link: {0}", link);
            String articleLink = removeStart(trim(link), " ");
            String articleTitle = capitalize(substringBefore(articleLink, "#"));
            String linkTitle = defaultString(innerLinkMatcher.group(3), articleTitle);
            if (isBlank(articleTitle)) {
                LOG.log(Level.INFO, "Article title blank in link ''{0}''! skipping...", innerLinkMatcher.group(0));
                continue;
            }
            if (isNotReplaceableLink(articleLink)) {
                LOG.log(Level.INFO, "''{0}'' links to something else! Skipping...", articleLink);
                continue;
            }
            String actualLocalArticleTitle = actualLocalTitleMap.get(articleTitle);
            status = new String[] { "status.analyzing.link", actualLocalArticleTitle };

            if (localLinkExistenceMap.get(actualLocalArticleTitle)) {
                LOG.log(Level.INFO, "''{0}'' already exists! skipping...", actualLocalArticleTitle);
                continue;
            }
            String foreignArticleTitle = actualForeignTitleMap.get(articleTitle);
            String replacedString = null;
            String sourceLang = removeEnd(sourceWikiCode, "wiki");

            String roLabel = null;
            String roArticle = roArticlesCache.get(sourceLang + ":" + foreignArticleTitle);

            if (Optional.ofNullable(foreignLinkExistenceMap.get(foreignArticleTitle)).orElse(false)) {
                Entity wbEntity = WikidataCacheManager.getWikidataEntitiesCache(dataWiki).getByArticle(sourceLang + "wiki", foreignArticleTitle);
                if (null == roArticle && null == wbEntity) {
                    try {
                        wbEntity = dataWiki.getWikibaseItemBySiteAndTitle(sourceWikiCode, foreignArticleTitle);
                    } catch (WikibaseException e) {
                        e.printStackTrace();
                    } catch (Throwable th) {
                        th.printStackTrace();
                    }
                }
                if (null != wbEntity) {
                    roLabel = wbEntity.getLabels().get("ro");
                    Sitelink roSitelink = wbEntity.getSitelinks().get(targetWikiCode);
                    if (null != roSitelink) {
                        roArticle = roSitelink.getPageName();
                        roArticlesCache.put(sourceLang + ":" + foreignArticleTitle, roArticle);
                    }
                }
                if (null != roArticle) {
                    replacedString = new WikiLink(roArticle, linkTitle).toString();
                } else if (null == wbEntity) {
                    replacedString =
                        new WikiTemplate().setTemplateTitle("Ill").setParam("1", sourceLang).setParam("2", roLabel)
                            .setParam("3", articleLink).setParam("4", linkTitle).setSingleLine(true).toString();
                } else {
                    replacedString =
                        new WikiTemplate().setTemplateTitle("Ill-wd").setParam("1", prependIfMissing(wbEntity.getId(), "Q"))
                            .setParam("3", linkTitle).setSingleLine(true).toString();
                }
            }
            replacedString = defaultString(replacedString, innerLinkMatcher.group(0));
            innerLinkMatcher.appendReplacement(anotherNewText, replacedString);

        }
        innerLinkMatcher.appendTail(anotherNewText);

        return anotherNewText.toString();

    }

    private boolean isNotReplaceableLink(String articleLink) {
        return startsWithAny(lowerCase(articleLink), "google:", "wiktionary:", "iarchive:", "file:", "fișier:", "image:", ":s:",
            "imagine:", "categorie:", "category:", "arxiv:", "openlibrary:", "s:", "imdbname:", "c:file:", "doi:", "issn:", "wp:",
            "bibcode:", "imdbtitle:", "foldoc:", "gutenberg:", "rfc:", "wikisource:", "oeis:", "special:", "wikt:", "wikiquote:");
    }

    public String[] getStatus() {
        return status;
    }

}
