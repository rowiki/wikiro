package org.wikipedia.ro.legacyoperations;

import static org.apache.commons.lang3.StringUtils.substring;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.apache.commons.lang3.Strings.CI;
import static org.apache.commons.lang3.Strings.CS;
import static org.wikipedia.ro.utils.ParseUtils.wikipartListToString;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.login.LoginException;

import org.apache.commons.lang3.ObjectUtils;
import org.wikibase.Wikibase;
import org.wikibase.WikibaseException;
import org.wikibase.data.Entity;
import org.wikibase.data.Sitelink;
import org.wikipedia.Wiki;
import org.wikipedia.ro.model.WikiLink;
import org.wikipedia.ro.model.WikiTemplate;
import org.wikipedia.ro.parser.ParseResult;
import org.wikipedia.ro.parser.WikiTemplateParser;
import org.wikipedia.ro.utils.WikidataCacheManager;
import org.wikipedia.ro.utils.WikipediaPageCache;

@Operation(useWikibase = true, labelKey = "operation.cleanupIll.label")
public class CleanupIll implements WikiOperation {
    private static final Logger LOG = Logger.getLogger(CleanupIll.class.getCanonicalName());

    private String[] status = new String[] { "status.not.inited" };
    private Wiki targetWiki;
    private Wiki sourceWiki;
    private Wikibase dataWiki;
    private String article;
    private String sourceWikiCode;
    private String targetWikiCode;

    public CleanupIll(Wiki targetWiki, Wiki sourceWiki, Wikibase dataWiki, String article) {
        this.targetWiki = targetWiki;
        this.sourceWiki = sourceWiki;
        this.dataWiki = dataWiki;
        this.article = article;
        this.sourceWikiCode = substringBefore(sourceWiki.getDomain(), ".") + "wiki";
        this.targetWikiCode = substringBefore(targetWiki.getDomain(), ".") + "wiki";
    }

    private static final Pattern PATTERN_ILL = Pattern.compile("\\{\\{\\s*[Ii]ll(-wd)?\\s*\\|");

    @Override
    public String execute() throws IOException, WikibaseException, LoginException {
        status = new String[] { "status.changes.todo.inarticle", article, String.valueOf(0), "?" };
        String pageText = WikipediaPageCache.getInstance().getPageText(targetWiki, article);
        
        return this.executeWithInitialText(pageText);
    }

    public String executeWithInitialText(String pageText) throws IOException, WikibaseException, LoginException {
        LOG.log(Level.INFO, "Cleaning up Ill templates in article ''{0}''", article);
        Matcher illMatcher = PATTERN_ILL.matcher(pageText);
        StringBuilder replacedTextBuilder = new StringBuilder(pageText);
        int offset = 0;
        int changesCount = 0;
        int instancesFound = 0;
        while (illMatcher.find()) {
            WikiTemplateParser wtp = new WikiTemplateParser();
            ParseResult<WikiTemplate> parsedIllTemplate = wtp.parse(substring(pageText, illMatcher.start()));
            WikiTemplate illTemplate = parsedIllTemplate.getIdentifiedPart();

            status = new String[] { "status.changes.todo.inarticle", article, String.valueOf(changesCount),
                String.valueOf(instancesFound) };

            String newLinkText = null;
            if (CS.equals(illMatcher.group(1), "-wd")) {
                newLinkText = extractLinkTextFromWikidata(illTemplate);

            } else if (null == illMatcher.group(1)) {
                newLinkText = extractLinkTextFromOtherWiki(illTemplate);
            }

            if (null != newLinkText) {
                int initialTemplateLength = illTemplate.getInitialText().length();
                replacedTextBuilder.replace(offset + illMatcher.start(), offset + illMatcher.start() + initialTemplateLength,
                    newLinkText);
                offset += newLinkText.length() - initialTemplateLength;
                changesCount++;
            }
            instancesFound++;
        }

        return replacedTextBuilder.toString();
    }

    private String extractLinkTextFromOtherWiki(WikiTemplate illTemplate) throws IOException, WikibaseException {
        String langId = wikipartListToString(illTemplate.getParam("1"));
        String baseTargetPage = wikipartListToString(illTemplate.getParam("2"));
        String label = null;
        if (CS.contains(baseTargetPage, "{{!}}")) {
            baseTargetPage = substringBefore(baseTargetPage, "{{!}}");
            label = substringAfter(baseTargetPage, "{{!}}");
        }
        String sourcePage = Objects.toString(wikipartListToString(illTemplate.getParam("3")), baseTargetPage);
        String targetPage = null != baseTargetPage ? WikidataCacheManager.getCachedRedirect(targetWiki, baseTargetPage): sourcePage; 
        label = Objects.toString(label, Objects.toString(wikipartListToString(illTemplate.getParam("4")), baseTargetPage));
        targetPage = Objects.toString(targetPage, label);

        if ("d".equals(langId)) {
            WikiTemplate prospectiveIllWdTemplate = new WikiTemplate();
            prospectiveIllWdTemplate.setTemplateTitle("Ill-wd");
            prospectiveIllWdTemplate.setParam("1", sourcePage);
            prospectiveIllWdTemplate.setParam("2", targetPage);
            prospectiveIllWdTemplate.setParam("3", label);

            String wikidataReplacementLink = extractLinkTextFromWikidata(prospectiveIllWdTemplate);
            return Objects.toString(wikidataReplacementLink, prospectiveIllWdTemplate.toString());
        }

        if (WikipediaPageCache.getInstance().pageExists(targetWiki, targetPage)) {
            return new WikiLink(targetPage, label).toString();
        } else {
            Wiki linkSourceWiki = Wiki.newSession(langId + ".wikipedia.org");
            Entity wdItem = WikidataCacheManager.getWikidataEntitiesCache(dataWiki).getByArticle(langId + "wiki",
                WikidataCacheManager.getCachedRedirect(linkSourceWiki, sourcePage));
            if (null != wdItem) {
                Sitelink targetSitelink = wdItem.getSitelinks().get(targetWikiCode);
                if (null != targetSitelink) {
                    WikiLink link = new WikiLink(targetSitelink.getPageName(), label);
                    return link.toString();
                }
            }
        }
        return null;
    }

    private String extractLinkTextFromWikidata(WikiTemplate illTemplate) throws IOException, WikibaseException {
        String wdId = wikipartListToString(illTemplate.getParam("1"));
        String label = wikipartListToString(illTemplate.getParam("3"));
        
        String qId = CI.prependIfMissing(wdId, "Q");
        qId = WikidataCacheManager.getCachedRedirect(dataWiki, qId);

        try {
            Entity wdItem = WikidataCacheManager.getWikidataEntitiesCache(dataWiki).get(qId);
            if (null != wdItem) {
                Map<String, Sitelink> sitelinks = ObjectUtils.getIfNull(wdItem.getSitelinks(), Collections.emptyMap());
                Sitelink targetSitelink = sitelinks.get(targetWikiCode);
                if (null != targetSitelink) {
                    if (null == label && null != wdItem.getLabels()) {
                        label = wdItem.getLabels().get(CI.removeEnd(targetWikiCode, "wiki"));
                    }
                    
                    WikiLink link = new WikiLink(targetSitelink.getPageName(), label);
                    return link.toString();
                }
            }
        } catch (WikibaseException e) {
            LOG.log(Level.WARNING, "Cannot find text for link to item ''{0}'' and label ''{1}''", new Object[] { wdId, label });
            LOG.log(Level.WARNING, "Stack trace:", e);
        }

        return null;
    }

    @Override
    public String[] getStatus() {
        return status;
    }
}
