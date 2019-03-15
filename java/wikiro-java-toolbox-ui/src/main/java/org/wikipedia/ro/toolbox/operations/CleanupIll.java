package org.wikipedia.ro.toolbox.operations;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.prependIfMissing;
import static org.apache.commons.lang3.StringUtils.substring;
import static org.apache.commons.lang3.StringUtils.substringBefore;

import java.io.IOException;
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
import org.wikipedia.ro.parser.ParseResult;
import org.wikipedia.ro.parser.WikiTemplateParser;
import static org.wikipedia.ro.utils.ParseUtils.*;

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

        String pageText = targetWiki.getPageText(article);

        Matcher illMatcher = PATTERN_ILL.matcher(pageText);
        StringBuilder replacedTextBuilder = new StringBuilder(pageText);
        int offset = 0;
        while (illMatcher.find()) {
            WikiTemplateParser wtp = new WikiTemplateParser();
            ParseResult<WikiTemplate> parsedIllTemplate = wtp.parse(substring(pageText, illMatcher.start()));
            WikiTemplate illTemplate = parsedIllTemplate.getIdentifiedPart();
            
            status = new String[] { "status.analyzing.link", illTemplate.getInitialText() };
            
            String newLinkText = null;
            if (StringUtils.equals(illMatcher.group(1), "-wd")) {
                String wdId = wikipartListToString(illTemplate.getParam("1"));
                String label = wikipartListToString(illTemplate.getParam("3"));

                Entity wdItem = dataWiki.getWikibaseItemById(prependIfMissing(wdId, "Q"));
                Sitelink targetSitelink = wdItem.getSitelinks().get(targetWikiCode);
                if (null != targetSitelink) {
                    WikiLink link = new WikiLink(targetSitelink.getPageName(), label);
                    newLinkText =link.toString();
                }

            } else if (null == illMatcher.group(1)) {
                String langId = wikipartListToString(illTemplate.getParam("1"));
                String targetPage = wikipartListToString(illTemplate.getParam("2"));
                String sourcePage = defaultString(wikipartListToString(illTemplate.getParam("3")), targetPage);
                String label = wikipartListToString(illTemplate.getParam("4"));
                
                if (targetWiki.exists(new String[] {targetPage})[0]) {
                    newLinkText = new WikiLink(targetPage, label).toString();
                } else {
                    Entity wdItem = dataWiki.getWikibaseItemBySiteAndTitle(langId + "wiki", sourcePage);
                    Sitelink targetSitelink = wdItem.getSitelinks().get(targetWikiCode);
                    if (null != targetSitelink) {
                        WikiLink link = new WikiLink(targetSitelink.getPageName(), label);
                        newLinkText =link.toString();
                    }
                }
            }
            
            if (null != newLinkText) {
                int initialTemplateLength = illTemplate.getInitialText().length();
                replacedTextBuilder.replace(offset + illMatcher.start(), offset + illMatcher.start() + initialTemplateLength, newLinkText);
                offset += newLinkText.length() - initialTemplateLength;
            }
        }

        return replacedTextBuilder.toString();
    }

    @Override
    public String[] getStatus() {
        return status;
    }
}
