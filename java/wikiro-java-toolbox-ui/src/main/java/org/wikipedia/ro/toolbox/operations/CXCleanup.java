package org.wikipedia.ro.toolbox.operations;

import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.replace;
import static org.apache.commons.lang3.StringUtils.substringBefore;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.login.LoginException;

import org.wikibase.Wikibase;
import org.wikibase.WikibaseException;
import org.wikibase.data.Entity;
import org.wikipedia.Wiki;

@Operation(useWikibase = true, labelKey = "operation.cxcleanup.label")
public class CXCleanup implements WikiOperation {

    private Wiki targetWiki;
    private Wiki sourceWiki;
    private Wikibase dataWiki;
    private String article;
    private String sourceWikiCode;
    private String targetWikiCode;
    private String[] status = new String[] { "status.not.inited" };

    public CXCleanup(Wiki targetWiki, Wiki sourceWiki, Wikibase dataWiki, String article) {
        this.targetWiki = targetWiki;
        this.sourceWiki = sourceWiki;
        this.dataWiki = dataWiki;
        this.article = article;
        this.sourceWikiCode = substringBefore(sourceWiki.getDomain(), ".") + "wiki";
        this.targetWikiCode = substringBefore(targetWiki.getDomain(), ".") + "wiki";
    }

    private static Pattern spanContentEditablePattern = Pattern.compile(
        "<span(\\s+([a-z]|\\-)+=\"[^>]*\")*\\s+((contenteditable=\"false\")|(abp=\"\\d+\"))(\\s+([a-z]|\\-)+=\"[^>]*\")*\\s*>(.*?)</span>",
        Pattern.DOTALL);
    private static Pattern coloredFontEditablePattern = Pattern.compile("<font.*?>(.*?)</font>", Pattern.DOTALL);
    private static Pattern strongPattern = Pattern.compile("<strong.*?>(.*?)</strong>", Pattern.DOTALL);
    private static Pattern uPattern = Pattern.compile("<u.*?>(.*?)</u>", Pattern.DOTALL);
    private static Pattern fontChangingSpanPattern = Pattern.compile(
        "<span(\\s+([a-z]|\\-)+=\"[^>]*\")*\\s+style=\"[^>]*?((font-family)|(background)|(color)|(mso-spacerun))\\s*:\\s*[^>]*?\">(.*?)</span>",
        Pattern.DOTALL);
    private static Pattern spanPattern = Pattern.compile("<span\\s*>(.*?)</span>", Pattern.DOTALL);
    private static Pattern msoHyperlinkSpanPattern = Pattern.compile(
        "<span(\\s+([a-z]|\\-)+=\"[^>]*\")*\\s+class=\"[^>]*?((MsoHyperlink)|(apple\\-converted\\-space)).*?\">([^>]*?)</span>",
        Pattern.DOTALL);
    private static Pattern emptyCitesPattern = Pattern.compile("<((cite)|(span))[^>]*>(\\s*)</\\1>", Pattern.DOTALL);
    private static String mathSpanRegEx = "\\<span\\s+class=\"texhtml\"\\s*\\>(.*?)\\</span\\s*\\>";

    public String execute() throws IOException, LoginException, WikibaseException {
        status = new String[] { "status.searching.wikibase.item.by.article", targetWikiCode, article };
        Entity ent = dataWiki.getWikibaseItemBySiteAndTitle(targetWikiCode, article);
        String translation = removeEnd(sourceWikiCode, "wiki") + ":" + ent.getSitelinks().get(sourceWikiCode).getPageName();
        status = new String[] { "status.reading.text", article, targetWikiCode };
        String text = targetWiki.getPageText(article);

        text = replace(text, "Wikipedia:WikiProject_Medicine/Translation_task_force/RTT/Simple_", "");
        text = replace(text, "&#x20;", " ");

        status = new String[] { "status.removing.contenteditable" };
        Matcher spanContentEditableMatcher = spanContentEditablePattern.matcher(text);

        StringBuffer newText = new StringBuffer();
        while (spanContentEditableMatcher.find()) {
            spanContentEditableMatcher.appendReplacement(newText, "$8");
        }
        spanContentEditableMatcher.appendTail(newText);

        status = new String[] { "status.removing.coloredfont" };
        Matcher coloredFontEditableMatcher = coloredFontEditablePattern.matcher(newText.toString());
        newText = new StringBuffer();
        while (coloredFontEditableMatcher.find()) {
            coloredFontEditableMatcher.appendReplacement(newText, "$1");
        }
        coloredFontEditableMatcher.appendTail(newText);

        status = new String[] { "status.removing.strong" };
        Matcher strongMatcher = strongPattern.matcher(newText.toString());
        newText = new StringBuffer();
        while (strongMatcher.find()) {
            strongMatcher.appendReplacement(newText, "$1");
        }
        strongMatcher.appendTail(newText);

        status = new String[] { "status.removing.u" };
        Matcher uMatcher = uPattern.matcher(newText.toString());
        newText = new StringBuffer();
        while (uMatcher.find()) {
            uMatcher.appendReplacement(newText, "$1");
        }
        uMatcher.appendTail(newText);

        status = new String[] { "status.removing.plainspans" };
        Matcher spanMatcher = spanPattern.matcher(newText.toString());
        newText = new StringBuffer();
        while (spanMatcher.find()) {
            spanMatcher.appendReplacement(newText, "$1");
        }
        spanMatcher.appendTail(newText);

        status = new String[] { "status.removing.emptycites" };
        Matcher emptyCitesMatcher = emptyCitesPattern.matcher(newText.toString());
        newText = new StringBuffer();
        while (emptyCitesMatcher.find()) {
            emptyCitesMatcher.appendReplacement(newText, emptyCitesMatcher.group(4));
        }
        emptyCitesMatcher.appendTail(newText);

        status = new String[] { "status.removing.fontchangingspans" };
        Matcher fontChangingSpanMatcher = fontChangingSpanPattern.matcher(newText.toString());
        newText = new StringBuffer();
        while (fontChangingSpanMatcher.find()) {
            fontChangingSpanMatcher.appendReplacement(newText, "$8");
        }
        fontChangingSpanMatcher.appendTail(newText);

        status = new String[] { "status.removing.applejunk" };
        Matcher msoHyperlinkSpanMatcher = msoHyperlinkSpanPattern.matcher(newText.toString());
        newText = new StringBuffer();
        while (msoHyperlinkSpanMatcher.find()) {
            msoHyperlinkSpanMatcher.appendReplacement(newText, "$6");
        }
        msoHyperlinkSpanMatcher.appendTail(newText);

        status = new String[] { "status.replacing.cn" };
        String citationNeededRegEx =
            "\\<sup\\s*class=\"noprint Inline-Template Template-Fact\"[^>]*?\\>.*?citation needed.*?\\</sup\\>";
        Pattern citationNeededPattern = Pattern.compile(citationNeededRegEx);
        Matcher citationNeededMatcher = citationNeededPattern.matcher(newText.toString());
        newText = new StringBuffer();
        while (citationNeededMatcher.find()) {
            citationNeededMatcher.appendReplacement(newText, "{{nc}}");
        }
        citationNeededMatcher.appendTail(newText);

        status = new String[] { "status.removing.mathspan" };
        Pattern mathSpanPattern = Pattern.compile(mathSpanRegEx);
        Matcher mathSpanMatcher = mathSpanPattern.matcher(newText.toString());
        newText = new StringBuffer();
        while (mathSpanMatcher.find()) {
            String mathTemplateParam = mathSpanMatcher.group(1);
            mathSpanMatcher.appendReplacement(newText,
                "{{math|" + (mathTemplateParam.contains("=") ? "1=" : "") + mathTemplateParam + "}}");
        }
        mathSpanMatcher.appendTail(newText);

        status = new String[] { "status.replacing.selflinks" };
        String selfLinkRegEx = replace(translation, "(", "\\(");
        selfLinkRegEx = replace(selfLinkRegEx, ")", "\\)");
        selfLinkRegEx = replace(selfLinkRegEx, ":", "\\:");
        selfLinkRegEx = replace(selfLinkRegEx, substringBefore(selfLinkRegEx, ":") + ":",
            "(" + substringBefore(selfLinkRegEx, ":") + ":)?");
        selfLinkRegEx = replace(selfLinkRegEx, "-", "\\-");
        selfLinkRegEx = replace(selfLinkRegEx, " ", "(\\s+|_)");
        replace(translation, " ", "(\\s+|_)");
        String regEx = "\\[\\[\\:?" + selfLinkRegEx + "#(?<anchor>.*?)\\]\\]";
        Pattern selfLinkPattern = Pattern.compile(regEx);
        Matcher selfLinkMatcher = selfLinkPattern.matcher(newText.toString());

        
        newText = new StringBuffer();
        int matchesCount = 0;
        while (selfLinkMatcher.find()) {
            matchesCount++;
            selfLinkMatcher.appendReplacement(newText, "[[#${anchor}]]");
        }
        selfLinkMatcher.appendTail(newText);

        String selfLinkRegEx2 = "\\[//" + removeEnd(sourceWikiCode, "wiki") + ".wikipedia.org/wiki/" + replace(ent.getSitelinks().get(sourceWikiCode).getPageName(), " ", "_") + "#(?<anchor>[^\\s]*?)\\s+(?<label>[^\\]]*?)\\]";
        Pattern selfLinkPattern2 = Pattern.compile(selfLinkRegEx2);
        Matcher selfLinkMatcher2 = selfLinkPattern2.matcher(newText.toString());

        newText = new StringBuffer();
        while (selfLinkMatcher2.find()) {
            selfLinkMatcher2.appendReplacement(newText, "[[#${anchor}|${label}]]");
        }
        selfLinkMatcher2.appendTail(newText);
        
        
        status = new String[] { "status.removing.supfootnotes" };
        Pattern footnoteBySupPattern = Pattern.compile(
            "\\[\\[#cite_note\\-.*?\\|<sup>(<span(\\s+lang=\".*?\")?\\s+style=\".*?\"\\s*?>\\s*?)?\\[\\d+\\](\\s*</span>\\s*)?</sup>\\]\\]",
            Pattern.DOTALL);
        Matcher footnoteBySupMatcher = footnoteBySupPattern.matcher(newText.toString());
        newText = new StringBuffer();
        while (footnoteBySupMatcher.find()) {
            footnoteBySupMatcher.appendReplacement(newText, "");
        }
        footnoteBySupMatcher.appendTail(newText);
        
        status = new String[] { "status.harvnbizing" };
        Pattern footnoteHarvnbPattern = Pattern.compile(
            "<ref(?:\\s+name=\"([^\"]*)\")?>\\s*\\[\\[#CITEREF(\\p{Alpha}*?)(\\d+)\\|[^\\]]*\\]\\](?:,\\s*(pp?)\\.(?:\\s+|&nbsp;)([\\d–\\-]+))?\\s*</ref>",
            Pattern.DOTALL);
        Matcher footnoteHarvnbMatcher = footnoteHarvnbPattern.matcher(newText.toString());
        newText = new StringBuffer();
        while (footnoteHarvnbMatcher.find()) {
            StringBuilder thisHarvnb = new StringBuilder("<ref");
            if (null != footnoteHarvnbMatcher.group(1)) {
                thisHarvnb.append(" name=\"").append(footnoteHarvnbMatcher.group(1)).append("\"");
            }
            thisHarvnb.append(">{{Harvnb|").append(footnoteHarvnbMatcher.group(2)).append('|').append(footnoteHarvnbMatcher.group(3));
            if (null != footnoteHarvnbMatcher.group(4) && null != footnoteHarvnbMatcher.group(5)) {
                thisHarvnb.append('|').append(footnoteHarvnbMatcher.group(4)).append('=').append(footnoteHarvnbMatcher.group(5));
            }
            thisHarvnb.append("}}</ref>");
            footnoteHarvnbMatcher.appendReplacement(newText, thisHarvnb.toString());
        }
        footnoteHarvnbMatcher.appendTail(newText);
        

        Pattern frenchCommaBetweenRefsPattern = Pattern.compile("\\<sup\\s*class=\"reference cite_virgule\"\\>,\\</sup\\>");
        Matcher frenchCommaBetweenRefsMatcher = frenchCommaBetweenRefsPattern.matcher(newText.toString());
        newText = new StringBuffer();
        while (frenchCommaBetweenRefsMatcher.find()) {
            frenchCommaBetweenRefsMatcher.appendReplacement(newText, "");
        }
        frenchCommaBetweenRefsMatcher.appendTail(newText);

        String newTextStr = newText.toString();
        newTextStr = replace(newTextStr, "''''''", "'''");
        newTextStr = replace(newTextStr, "''''", "''");

        return newText.toString();
    }

    public String[] getStatus() {
        return status;
    }

}
