package org.wikipedia.ro.astroe.operations;

import java.io.IOException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.login.LoginException;

import org.apache.commons.lang3.StringUtils;
import org.wikibase.Wikibase;
import org.wikibase.WikibaseException;
import org.wikibase.data.Entity;
import org.wikipedia.Wiki;
import org.wikipedia.ro.astroe.FixVillages;

public class CXCleanup {

    private static Wiki wiki = new Wiki("ro.wikipedia.org");
    private static Wikibase dwiki = new Wikibase();

    public static void main(String[] args) {
        try {
            final Properties credentials = new Properties();
            
            credentials.load(FixVillages.class.getClassLoader().getResourceAsStream("credentials.properties"));
            
            final String rowpusername = credentials.getProperty("rowiki.user");
            final String rowppassword = credentials.getProperty("rowiki.password");
            wiki.login(rowpusername, rowppassword.toCharArray());
            for (int i = 0; i < args.length; i += 2) {
                String eachArticle = args[i];
                Entity ent = dwiki.getWikibaseItemBySiteAndTitle("rowiki", eachArticle);
                String lang = "en";
                if (args.length > i + 1) {
                    lang = args[i + 1];
                }
                String translation = ent.getSitelinks().get(lang + "wiki").getPageName();
                doCleanContentEditableSpans(wiki, eachArticle, lang + ":" + translation);
            }
        } catch (IOException | LoginException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (WikibaseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void doCleanContentEditableSpans(Wiki w, String article, String translation)
        throws IOException, LoginException {
        String text = w.getPageText(article);

        text = StringUtils.replace(text, "Wikipedia:WikiProject_Medicine/Translation_task_force/RTT/Simple_", "");
        text = StringUtils.replace(text, "&#x20;", " ");

        Pattern spanContentEditablePattern = Pattern.compile(
            "<span(\\s+([a-z]|\\-)+=\"[^>]*\")*\\s+((contenteditable=\"false\")|(abp=\"\\d+\"))(\\s+([a-z]|\\-)+=\"[^>]*\")*\\s*>(.*?)</span>",
            Pattern.DOTALL);
        Matcher spanContentEditableMatcher = spanContentEditablePattern.matcher(text);

        StringBuffer newText = new StringBuffer();
        while (spanContentEditableMatcher.find()) {
            spanContentEditableMatcher.appendReplacement(newText, "$8");
        }
        spanContentEditableMatcher.appendTail(newText);

        Pattern coloredFontEditablePattern = Pattern.compile("<font.*?>(.*?)</font>", Pattern.DOTALL);
        Matcher coloredFontEditableMatcher = coloredFontEditablePattern.matcher(newText.toString());
        newText = new StringBuffer();
        while (coloredFontEditableMatcher.find()) {
            coloredFontEditableMatcher.appendReplacement(newText, "$1");
        }
        coloredFontEditableMatcher.appendTail(newText);

        Pattern strongPattern = Pattern.compile("<strong.*?>(.*?)</strong>", Pattern.DOTALL);
        Matcher strongMatcher = strongPattern.matcher(newText.toString());
        newText = new StringBuffer();
        while (strongMatcher.find()) {
            strongMatcher.appendReplacement(newText, "$1");
        }
        strongMatcher.appendTail(newText);

        Pattern uPattern = Pattern.compile("<u.*?>(.*?)</u>", Pattern.DOTALL);
        Matcher uMatcher = uPattern.matcher(newText.toString());
        newText = new StringBuffer();
        while (uMatcher.find()) {
            uMatcher.appendReplacement(newText, "$1");
        }
        uMatcher.appendTail(newText);

        Pattern spanPattern = Pattern.compile("<span\\s*>(.*?)</span>", Pattern.DOTALL);
        Matcher spanMatcher = spanPattern.matcher(newText.toString());
        newText = new StringBuffer();
        while (spanMatcher.find()) {
            spanMatcher.appendReplacement(newText, "$1");
        }
        spanMatcher.appendTail(newText);

        Pattern emptyCitesPattern = Pattern.compile("<((cite)|(span))[^>]*>(\\s*)</\\1>", Pattern.DOTALL);
        Matcher emptyCitesMatcher = emptyCitesPattern.matcher(newText.toString());
        newText = new StringBuffer();
        while (emptyCitesMatcher.find()) {
            emptyCitesMatcher.appendReplacement(newText, emptyCitesMatcher.group(4));
        }
        emptyCitesMatcher.appendTail(newText);

        Pattern fontChangingSpanPattern = Pattern.compile(
            "<span(\\s+([a-z]|\\-)+=\"[^>]*\")*\\s+style=\"[^>]*?((font-family)|(background)|(color)|(mso-spacerun))\\s*:\\s*[^>]*?\">(.*?)</span>",
            Pattern.DOTALL);
        Matcher fontChangingSpanMatcher = fontChangingSpanPattern.matcher(newText.toString());
        newText = new StringBuffer();
        while (fontChangingSpanMatcher.find()) {
            fontChangingSpanMatcher.appendReplacement(newText, "$8");
        }
        fontChangingSpanMatcher.appendTail(newText);

        Pattern msoHyperlinkSpanPattern = Pattern.compile(
            "<span(\\s+([a-z]|\\-)+=\"[^>]*\")*\\s+class=\"[^>]*?((MsoHyperlink)|(apple\\-converted\\-space)).*?\">([^>]*?)</span>",
            Pattern.DOTALL);
        Matcher msoHyperlinkSpanMatcher = msoHyperlinkSpanPattern.matcher(newText.toString());
        newText = new StringBuffer();
        while (msoHyperlinkSpanMatcher.find()) {
            msoHyperlinkSpanMatcher.appendReplacement(newText, "$6");
        }
        msoHyperlinkSpanMatcher.appendTail(newText);

        String citationNeededRegEx =
            "\\<sup\\s*class=\"noprint Inline-Template Template-Fact\"[^>]*?\\>.*?citation needed.*?\\</sup\\>";
        Pattern citationNeededPattern = Pattern.compile(citationNeededRegEx);
        Matcher citationNeededMatcher = citationNeededPattern.matcher(newText.toString());
        newText = new StringBuffer();
        while (citationNeededMatcher.find()) {
            citationNeededMatcher.appendReplacement(newText, "{{nc}}");
        }
        citationNeededMatcher.appendTail(newText);

        String mathSpanRegEx = "\\<span\\s+class=\"texhtml\"\\s*\\>(.*?)\\</span\\s*\\>";
        Pattern mathSpanPattern = Pattern.compile(mathSpanRegEx);
        Matcher mathSpanMatcher = mathSpanPattern.matcher(newText.toString());
        newText = new StringBuffer();
        while (mathSpanMatcher.find()) {
            String mathTemplateParam = mathSpanMatcher.group(1);
            mathSpanMatcher.appendReplacement(newText,
                "{{math|" + (mathTemplateParam.contains("=") ? "1=" : "") + mathTemplateParam + "}}");
        }
        mathSpanMatcher.appendTail(newText);

        String selfLinkRegEx = StringUtils.replace(translation, "(", "\\(");
        selfLinkRegEx = StringUtils.replace(selfLinkRegEx, ")", "\\)");
        selfLinkRegEx = StringUtils.replace(selfLinkRegEx, ":", "\\:");
        selfLinkRegEx = StringUtils.replace(selfLinkRegEx, StringUtils.substringBefore(selfLinkRegEx, ":") + ":",
            "(" + StringUtils.substringBefore(selfLinkRegEx, ":") + ":)?");
        selfLinkRegEx = StringUtils.replace(selfLinkRegEx, "-", "\\-");
        selfLinkRegEx = StringUtils.replace(selfLinkRegEx, " ", "(\\s+|_)");
        StringUtils.replace(translation, " ", "(\\s+|_)");
        String regEx = "\\[\\[\\:?" + selfLinkRegEx + "#(?<anchor>.*?)\\]\\]";
        System.out.println(regEx);
        Pattern selfLinkPattern = Pattern.compile(regEx);
        Matcher selfLinkMatcher = selfLinkPattern.matcher(newText.toString());

        newText = new StringBuffer();
        int matchesCount = 0;
        while (selfLinkMatcher.find()) {
            matchesCount++;
            selfLinkMatcher.appendReplacement(newText, "[[#${anchor}]]");
        }
        selfLinkMatcher.appendTail(newText);

        Pattern footnoteBySupPattern = Pattern.compile(
            "\\[\\[#cite_note\\-.*?\\|<sup>(<span(\\s+lang=\".*?\")?\\s+style=\".*?\"\\s*?>\\s*?)?\\[\\d+\\](\\s*</span>\\s*)?</sup>\\]\\]",
            Pattern.DOTALL);
        Matcher footnoteBySupMatcher = footnoteBySupPattern.matcher(newText.toString());
        newText = new StringBuffer();
        while (footnoteBySupMatcher.find()) {
            footnoteBySupMatcher.appendReplacement(newText, "");
        }
        footnoteBySupMatcher.appendTail(newText);
        
        Pattern frenchCommaBetweenRefsPattern = Pattern.compile("\\<sup\\s*class=\"reference cite_virgule\"\\>,\\</sup\\>");
        Matcher frenchCommaBetweenRefsMatcher = frenchCommaBetweenRefsPattern.matcher(newText.toString());
        newText = new StringBuffer();
        while (frenchCommaBetweenRefsMatcher.find()) {
            frenchCommaBetweenRefsMatcher.appendReplacement(newText, "");
        }
        frenchCommaBetweenRefsMatcher.appendTail(newText);

        String newTextStr = newText.toString();
        newTextStr = StringUtils.replace(newTextStr, "''''''", "'''");
        newTextStr = StringUtils.replace(newTextStr, "''''", "''");
        System.out.println(matchesCount + " matches found.");

        // System.out.println(newText);
        w.setMarkBot(true);
        w.edit(article, newText.toString(), "Curățenie gunoi CX");

    }

}
