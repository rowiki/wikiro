package org.wikipedia.ro.astroe.operations;

import static org.apache.commons.lang3.StringUtils.*;
import static org.apache.commons.lang3.StringUtils.substringBefore;

import java.io.FileNotFoundException;
import java.io.IOException;

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

    @Override
    public String execute() throws IOException, WikibaseException, LoginException {
        if (Wiki.TALK_NAMESPACE != targetWiki.namespace(article)) {
            return null;
        }

        status = new String[] { "status.reading.talkpage" };
        String talkPageText = "";
        try {
            talkPageText = trim(defaultString(targetWiki.getPageText(article)));
        } catch (FileNotFoundException e) {
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
        String lastRevId = sourceWiki.getPageInfo(sourceArticle).get("lastrevid").toString();
        status = new String[] { "status.last.revision.target" };
        String targetLastRevId = targetWiki.getPageInfo(articlePage).get("lastrevid").toString();
        StringBuilder template = new StringBuilder("{{Pagină tradusă|");
        template.append(substringBefore(sourceWiki.getDomain(), "."));
        template.append('|');
        template.append(sourceArticle);
        if (!small) {
            template.append("|small=no");
        }
        template.append('|');
        template.append("|version=").append(lastRevId);
        template.append("|insertversion=").append(targetLastRevId);
        template.append("}}\n");
        template.append(talkPageText);

        return template.toString();
    }

    @Override
    public String[] getStatus() {
        return null;
    }

}
