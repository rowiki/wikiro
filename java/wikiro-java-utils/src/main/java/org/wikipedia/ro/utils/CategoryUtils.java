package org.wikipedia.ro.utils;

import java.io.IOException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.wikipedia.Wiki;

public class CategoryUtils {
    public static boolean isInCategoryTree(final String pageTitle, final Wiki wiki, final int depth, final String category) {
        String[] cats = null;
        boolean categoriesRead = false;

        do {
            try {
                cats = wiki.getCategories(pageTitle);
                categoriesRead = true;
            } catch (final IOException e) {
                e.printStackTrace();
                System.err.println("Retrying");
                categoriesRead = false;
            }
        } while (!categoriesRead);

        for (final String eachCat : cats) {
            if (StringUtils.equals(category, StringUtils.substringAfter(eachCat, ":"))) {
                return true;
            }
            if (0 < depth) {
                if (isInCategoryTree(eachCat, wiki, depth - 1, category)) {
                    return true;
                }
            }
        }
        return false;

    }

    public static boolean isInAnyCategoryTree(final String pageTitle, final Wiki wiki, final int depth,
                                              final String... categories) {
        String[] cats = null;
        boolean categoriesRead = false;

        do {
            try {
                cats = wiki.getCategories(pageTitle);
                categoriesRead = true;
            } catch (final IOException e) {
                e.printStackTrace();
                System.err.println("Retrying");
                categoriesRead = false;
            }
        } while (!categoriesRead);

        for (final String eachCat : cats) {
            if (ArrayUtils.contains(categories, StringUtils.substringAfter(eachCat, ":"))) {
                return true;
            }
            if (0 < depth) {
                if (isInAnyCategoryTree(eachCat, wiki, depth - 1, categories)) {
                    return true;
                }
            }
        }
        return false;

    }

}
