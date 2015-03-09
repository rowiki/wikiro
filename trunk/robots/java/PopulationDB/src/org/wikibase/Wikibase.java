package org.wikibase;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.StringTokenizer;

import org.wikipedia.Wiki;

public class Wikibase extends Wiki {
    public Wikibase() {
        super("www.wikidata.org");
    }

    public String getWikibaseItem(final String site, final String pageName) throws IOException {
        final StringBuilder url = new StringBuilder(query);
        url.append("action=wbgetentities");
        url.append("&sites=" + site);
        url.append("&titles=" + URLEncoder.encode(pageName, "UTF-8"));
        url.append("&format=xmlfm");

        final String text = fetch(url.toString(), "getWikibaseItem");

        return text;
    }

    public String getTitleInLanguage(final String site, final String pageName, final String language) throws IOException {
        final StringBuilder url = new StringBuilder(query);
        url.append("action=wbgetentities");
        url.append("&sites=" + site);
        url.append("&titles=" + URLEncoder.encode(pageName, "UTF-8"));
        url.append("&format=xml");

        final String text = fetch(url.toString(), "getWikibaseItem");

        final String sitelinkText = "<sitelink site=\"" + language + "wiki\" title=";
        final int sitelinkStart = text.indexOf(sitelinkText);
        if (0 > sitelinkStart) {
            return null;
        }
        final int pageNameStart = text.indexOf("\"", sitelinkStart + sitelinkText.length());
        final int pageNameEnd = text.indexOf("\"", pageNameStart + 1);

        return text.substring(pageNameStart + 1, pageNameEnd);
    }

    public void linkPages(final String fromsite, final String fromtitle, final String tosite, final String totitle)
        throws IOException {
        final StringBuilder url1 = new StringBuilder(query);
        url1.append("action=wbgetentities");
        url1.append("&sites=" + tosite);
        url1.append("&titles=" + URLEncoder.encode(totitle, "UTF-8"));
        url1.append("&format=xml");
        final String text = fetch(url1.toString(), "linkPages");

        final int startindex = text.indexOf("<entity");
        final int endindex = text.indexOf(">", startindex);
        final String entityTag = text.substring(startindex, endindex);
        final StringTokenizer entityTok = new StringTokenizer(entityTag, " ", false);
        String q = null;
        while (entityTok.hasMoreTokens()) {
            final String entityAttr = entityTok.nextToken();
            if (!entityAttr.contains("=")) {
                continue;
            }
            final String[] entityParts = entityAttr.split("\\=");
            if (entityParts[0].trim().startsWith("title")) {
                q = entityParts[1].trim().replace("\"", "");
            }
        }

        if (null == q) {
            return;
        }

        final StringBuilder getTokenURL = new StringBuilder(query);
        getTokenURL.append("prop=info");
        getTokenURL.append("&intoken=edit");
        getTokenURL.append("&titles=" + URLEncoder.encode(q, "UTF-8"));
        getTokenURL.append("&format=xml");
        String res = fetch(getTokenURL.toString(), "linkPages");

        final int pagestartindex = res.indexOf("<page ");
        final int pageendindex = res.indexOf(">", pagestartindex);
        final String pageTag = res.substring(pagestartindex, pageendindex);

        String edittoken = null;
        final StringTokenizer pageTok = new StringTokenizer(pageTag, " ", false);
        while (pageTok.hasMoreTokens()) {
            final String pageAttr = pageTok.nextToken();
            if (!pageAttr.contains("=")) {
                continue;
            }
            final String[] entityParts = pageAttr.split("=");
            if (entityParts[0].trim().startsWith("edittoken")) {
                edittoken = entityParts[1].trim().replace("\"", "");
            }
        }
        final StringBuilder postdata = new StringBuilder();
        postdata.append("&tosite=" + tosite);
        postdata.append("&totitle=" + URLEncoder.encode(totitle, "UTF-8"));
        postdata.append("&fromtitle=" + URLEncoder.encode(fromtitle, "UTF-8"));
        postdata.append("&fromsite=" + fromsite);
        postdata.append("&token=" + URLEncoder.encode(edittoken, "UTF-8"));
        postdata.append("&format=xmlfm");

        res = post(query + "action=wblinktitles", postdata.toString(), "linkPages");
    }
}
