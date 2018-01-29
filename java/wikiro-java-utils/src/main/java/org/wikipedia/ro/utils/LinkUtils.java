package org.wikipedia.ro.utils;

public class LinkUtils {
    
    public static String createLink(String articleTitle, String label) {
        if (null == articleTitle) {
            return label;
        }
        StringBuilder sb = new StringBuilder("[[");
        if (null != label && !articleTitle.equals(label)) {
            sb.append(TextUtils.capitalizeName(articleTitle));
            sb.append('|').append(label);
        } else {
            sb.append(articleTitle);
        }
        sb.append("]]");
        return sb.toString();
    }

    
}
