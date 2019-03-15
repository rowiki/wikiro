package org.wikipedia.ro.toolbox.operations;

import static org.apache.commons.lang3.StringUtils.*;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.prependIfMissing;
import static org.apache.commons.lang3.StringUtils.substring;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.wikipedia.ro.utils.ParseUtils.wikipartListToString;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.login.LoginException;

import org.apache.commons.lang3.StringUtils;
import org.wikibase.Wikibase;
import org.wikibase.WikibaseException;
import org.wikibase.data.Entity;
import org.wikibase.data.Sitelink;
import org.wikipedia.Wiki;
import org.wikipedia.ro.model.WikiLink;
import org.wikipedia.ro.model.WikiTemplate;
import org.wikipedia.ro.parser.ParseResult;
import org.wikipedia.ro.parser.WikiTemplateParser;

@Operation(useWikibase = true, labelKey = "operation.cleanupIll.label")
public class CleanupIll implements WikiOperation {

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

        String pageText = targetWiki.getPageText(article);

        Matcher illMatcher = PATTERN_ILL.matcher(pageText);
        StringBuilder replacedTextBuilder = new StringBuilder(pageText);
        int offset = 0;
        int changesCount = 0;
        int instancesFound = 0;
        while (illMatcher.find()) {
            WikiTemplateParser wtp = new WikiTemplateParser();
            ParseResult<WikiTemplate> parsedIllTemplate = wtp.parse(substring(pageText, illMatcher.start()));
            WikiTemplate illTemplate = parsedIllTemplate.getIdentifiedPart();

            status = new String[] { "status.changes.todo.inarticle", article, String.valueOf(changesCount), String.valueOf(instancesFound) };

            String newLinkText = null;
            if (StringUtils.equals(illMatcher.group(1), "-wd")) {
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

    private String extractLinkTextFromOtherWiki(WikiTemplate illTemplate)
        throws IOException, WikibaseException {
        String langId = wikipartListToString(illTemplate.getParam("1"));
        String baseTargetPage = wikipartListToString(illTemplate.getParam("2"));
        String label;
        if (contains(baseTargetPage, "{{!}}")) {
            baseTargetPage = substringBefore(baseTargetPage, "{{!}}");
            label = substringAfter(baseTargetPage, "{{!}}");
        }
        String targetPage = defaultString(targetWiki.resolveRedirect(baseTargetPage), baseTargetPage);
        String sourcePage = defaultString(wikipartListToString(illTemplate.getParam("3")), baseTargetPage);
        label = defaultString(wikipartListToString(illTemplate.getParam("4")), baseTargetPage);
        
        if ("d".equals(langId)) {
            WikiTemplate prospectiveIllWdTemplate = new WikiTemplate();
            prospectiveIllWdTemplate.setTemplateTitle("Ill-wd");
            prospectiveIllWdTemplate.setParam("1", sourcePage);
            prospectiveIllWdTemplate.setParam("2", targetPage);
            prospectiveIllWdTemplate.setParam("3", label);
            
            String wikidataReplacementLink = extractLinkTextFromWikidata(prospectiveIllWdTemplate);
            return defaultString(wikidataReplacementLink, prospectiveIllWdTemplate.toString());
        }

        if (targetWiki.exists(new String[] { targetPage })[0]) {
            return new WikiLink(targetPage, label).toString();
        } else {
            Wiki linkSourceWiki = Wiki.newSession(langId + ".wikipedia.org");
            Entity wdItem = dataWiki.getWikibaseItemBySiteAndTitle(langId + "wiki",
                defaultString(linkSourceWiki.resolveRedirect(sourcePage), sourcePage));
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

    private String extractLinkTextFromWikidata(WikiTemplate illTemplate)
        throws IOException, WikibaseException {
        String wdId = wikipartListToString(illTemplate.getParam("1"));
        String label = wikipartListToString(illTemplate.getParam("3"));

        Entity wdItem = dataWiki.getWikibaseItemById(prependIfMissing(wdId, "Q"));
        Sitelink targetSitelink = wdItem.getSitelinks().get(targetWikiCode);
        if (null != targetSitelink) {
            WikiLink link = new WikiLink(targetSitelink.getPageName(), label);
            return link.toString();
        }
        return null;
    }

    @Override
    public String[] getStatus() {
        return status;
    }
}
