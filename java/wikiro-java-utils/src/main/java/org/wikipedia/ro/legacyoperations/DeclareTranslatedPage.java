package org.wikipedia.ro.legacyoperations;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.apache.commons.lang3.StringUtils.trim;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.security.auth.login.LoginException;

import org.wikibase.Wikibase;
import org.wikibase.WikibaseException;
import org.wikibase.data.Entity;
import org.wikipedia.Wiki;

@Operation(useWikibase = true, labelKey = "operation.declaretranslate.label")
public class DeclareTranslatedPage implements WikiOperation {

    private Wiki targetWiki;
    private Wiki sourceWiki;
    private Wikibase dataWiki;
    private String article;
    private String sourceWikiCode;
    private String targetWikiCode;
    private String[] status = new String[] { "status.searching.wikibase.item.by.article", article, targetWikiCode };

    public DeclareTranslatedPage(Wiki targetWiki, Wiki sourceWiki, Wikibase dataWiki, String article) {
        this.targetWiki = targetWiki;
        this.sourceWiki = sourceWiki;
        this.dataWiki = dataWiki;
        this.article = article;
        this.sourceWikiCode = substringBefore(sourceWiki.getDomain(), ".") + "wiki";
        this.targetWikiCode = substringBefore(targetWiki.getDomain(), ".") + "wiki";
    }

    public String execute() throws IOException, WikibaseException, LoginException {
        if (Wiki.TALK_NAMESPACE != targetWiki.namespace(article)) {
            return null;
        }

        status = new String[] { "status.reading.talkpage" };
        String talkPageText = "";
        try {
            if (targetWiki.exists(List.of(article))[0]) {
                talkPageText = trim(defaultString(targetWiki.getPageText(List.of(article)).stream().findFirst().orElse("")));
            }
        } catch (FileNotFoundException e) {
            System.out.println("Page not found: " + article + " - " + e.getMessage());
        } 
        boolean small = false;
        if (talkPageText.length() > 0) {
            small = true;
        }
        if (startsWith(talkPageText, "{{")) {
            small = false;
        }

        String articlePage = substringAfter(article, ":");

        status = new String[] { "status.identifying.item" };
        Entity wbentity = dataWiki.getWikibaseItemBySiteAndTitle(targetWikiCode, articlePage);
        String sourceArticle = wbentity.getSitelinks().get(sourceWikiCode).getPageName();

        status = new String[] { "status.last.revision.src" };
        Optional<String> lastRevId = sourceWiki.getPageInfo(List.of(sourceArticle)).stream().findFirst().map(m -> m.get("lastrevid")).map(Objects::toString);
        status = new String[] { "status.last.revision.target" };
        Optional<String> targetLastRevId = targetWiki.getPageInfo(List.of(articlePage)).stream().findFirst().map(m -> m.get("lastrevid")).map(Objects::toString);
        StringBuilder template = new StringBuilder("{{Pagină tradusă|");
        template.append(substringBefore(sourceWiki.getDomain(), "."));
        template.append('|');
        template.append(sourceArticle);
        if (!small) {
            template.append("|small=no");
        }
        template.append('|');
        if (lastRevId.isPresent()) {
            template.append("|version=").append(lastRevId.get());
        }
        if (targetLastRevId.isPresent()) {
            template.append("|insertversion=").append(targetLastRevId.get());
        }
        template.append("}}\n");
        template.append(talkPageText);

        return template.toString();
    }

    public String[] getStatus() {
        return null;
    }

}
