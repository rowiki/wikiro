package org.wikipedia.ro.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PageUtils {
    private static Pattern templateStartPattern = Pattern.compile("\\{\\{");

    public static List<WikiTemplate> getTemplatesInText(String text) {
        List<WikiTemplate> ret = new ArrayList<WikiTemplate>();

        Matcher templateStartMatcher = templateStartPattern.matcher(text);
        while (templateStartMatcher.find()) {
            WikiTemplate template = new WikiTemplate(text.substring(templateStartMatcher.start()));
            ret.add(template);
        }
        return ret;
    }
}
