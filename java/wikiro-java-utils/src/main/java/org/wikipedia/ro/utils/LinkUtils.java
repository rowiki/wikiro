package org.wikipedia.ro.utils;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.wikibase.data.Entity;
import org.wikibase.data.Sitelink;

public class LinkUtils {

    public static String createLink(String articleTitle, String label) {
        return createLink(null, null, articleTitle, label);
    }
    public static String createLink(String wiki, String namespace, String articleTitle, String label) {
        if (null == articleTitle) {
            return label;
        }
        StringBuilder sb = new StringBuilder("[[");
        if (null != wiki) {
            sb.append(':').append(wiki).append(':');
        }
        if (null != namespace) {
            sb.append(StringUtils.capitalize(namespace)).append(':');
        }
        if (null != label && !articleTitle.equals(label)) {
            sb.append(StringUtils.capitalize(articleTitle));
            sb.append('|').append(label);
        } else {
            sb.append(articleTitle);
        }
        sb.append("]]");
        return sb.toString();
    }

    public static String createLinkViaWikidata(Entity ent, String label) {

        label = ArrayUtils.removeAllOccurences(new String[] {label, ent.getLabels().get("ro"), ent.getLabels().get("en"), ent.getId()}, null)[0];
        if (ent.getSitelinks().containsKey("rowiki")) {
            Sitelink roSitelink = ent.getSitelinks().get("rowiki");
            return createLink(roSitelink.getPageName(), label);
        } else {
            return createLink("d", null, ent.getId(), label);
        }
    }
}
