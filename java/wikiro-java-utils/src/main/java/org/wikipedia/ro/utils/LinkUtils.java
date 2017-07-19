package org.wikipedia.ro.utils;

public class LinkUtils {
    
    public static String createLink(String articleTitle, String label) {
        if (null == articleTitle) {
            return label;
        }
        StringBuilder sb = new StringBuilder("[[");
        sb.append(articleTitle);
        if (null != label && !articleTitle.equals(label)) {
            sb.append('|').append(label);
        }
        sb.append("]]");
        return sb.toString();
    }
}
