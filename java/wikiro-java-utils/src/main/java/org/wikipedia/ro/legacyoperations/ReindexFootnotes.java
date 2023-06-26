package org.wikipedia.ro.legacyoperations;

import static org.apache.commons.lang3.StringUtils.substringBefore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.login.LoginException;

import org.wikibase.Wikibase;
import org.wikibase.WikibaseException;
import org.wikipedia.Wiki;

@Operation(useWikibase = true, labelKey = "operation.reindexfn.label")
public class ReindexFootnotes implements WikiOperation {
    private static final Logger LOG = Logger.getLogger(ReindexFootnotes.class.getCanonicalName());
    private Wiki sourceWiki, targetWiki;
    private Wikibase dataWiki;
    private String article, sourceWikiCode, targetWikiCode;
    private String[] status = new String[] { "status.not.inited" };
    private Pattern ALL_REFS_PATTERN = Pattern.compile("<\\s*ref\\s+name=\"([^\"]+)\"\\s*>(.*?)</ref>");

    public ReindexFootnotes(Wiki targetWiki, Wiki sourceWiki, Wikibase dataWiki, String article) {
        this.targetWiki = targetWiki;
        this.sourceWiki = sourceWiki;
        this.dataWiki = dataWiki;
        this.article = article;
        this.sourceWikiCode = substringBefore(sourceWiki.getDomain(), ".") + "wiki";
        this.targetWikiCode = substringBefore(targetWiki.getDomain(), ".") + "wiki";
    }

    @Override
    public String execute() throws IOException, WikibaseException, LoginException {
        LOG.log(Level.INFO, "Reindexing footnotes of page {0}", article);
        Optional<String> pageTextOpt = targetWiki.getPageText(List.of(article)).stream().findFirst();
        if (pageTextOpt.isEmpty()) {
            throw new IOException("Page " + article + " could not be loaded");
        }
        String pageText = pageTextOpt.get();

        return processText(pageText);
    }

    public String processText(String pageText) {
        Map<String, List<String>> fnMap = new HashMap<>();
        Matcher matcher = ALL_REFS_PATTERN.matcher(pageText);
        while(matcher.find()) {
            List<String> fnNameList = fnMap.computeIfAbsent(matcher.group(1), s -> new ArrayList<>());
            fnNameList.add(matcher.group(2));
        }
        
        String crtText = pageText;
        for (Map.Entry<String, List<String>> fnEntry: fnMap.entrySet()) {
            if (fnEntry.getValue().isEmpty()) {
                continue;
            }
            status = new String[] {"status.reindexfn.editing", fnEntry.getKey()};
            int keeperIdx = 0;
            for (String fnContent: fnEntry.getValue()) {
                if (fnContent.contains("<cite ")) {
                    keeperIdx++;
                } else {
                    break;
                }
            }
            keeperIdx = keeperIdx >= fnEntry.getValue().size() ? 0 : keeperIdx;
                
            Pattern thisRefPattern = Pattern.compile("<\\s*ref\\s+name=\"" + Pattern.quote(fnEntry.getKey()) + "\"\\s*>(.*?)</ref>");
            StringBuilder sbuild = new StringBuilder();
            Matcher thisRefMatcher = thisRefPattern.matcher(crtText);
            int appearanceIdx = 0;
            while (thisRefMatcher.find()) {
                if (appearanceIdx != keeperIdx) {
                    thisRefMatcher.appendReplacement(sbuild, "<ref name=\"" + Matcher.quoteReplacement(fnEntry.getKey()) + "\"/>");
                }
                appearanceIdx++;
            }
            thisRefMatcher.appendTail(sbuild);
            crtText = sbuild.toString();
            LOG.log(Level.INFO, "Reindexed footnotes, changed size by {0} characters", Integer.valueOf(crtText.length() - pageText.length()));
        }
        return crtText;
    }

    @Override
    public String[] getStatus() {
        return status;
    }

}
