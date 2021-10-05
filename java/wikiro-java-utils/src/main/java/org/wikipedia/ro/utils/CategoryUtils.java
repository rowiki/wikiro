package org.wikipedia.ro.utils;

import static org.apache.commons.lang3.StringUtils.substringAfter;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.wikipedia.Wiki;

public class CategoryUtils {
    private CategoryUtils() {

    }

    public static boolean isInCategoryTree(final String pageTitle, final Wiki wiki, final int depth, final String category) {
        List<String> cats = null;
        boolean categoriesRead = false;

        do {
            try {
                cats = wiki.getCategories(List.of(pageTitle), wiki.new RequestHelper(), false).stream().findFirst().orElse(List.of());
                categoriesRead = true;
            } catch (final IOException e) {
                e.printStackTrace();
                System.err.println("Retrying");
                categoriesRead = false;
            }
        } while (!categoriesRead);

        for (final String eachCat : cats) {
            if (StringUtils.equals(category, substringAfter(eachCat, ":"))) {
                return true;
            }
            if (0 < depth && isInCategoryTree(eachCat, wiki, depth - 1, category)) {
                return true;
            }
        }
        return false;

    }

    public static boolean isInAnyCategoryTree(final String pageTitle, final Wiki wiki, final int depth,
                                              final String... categories) {
        List<String> cats = null;
        boolean categoriesRead = false;

        do {
            try {
                cats = wiki.getCategories(List.of(pageTitle), wiki.new RequestHelper(), false).stream().findFirst().orElse(List.of());
                categoriesRead = true;
            } catch (final IOException e) {
                e.printStackTrace();
                System.err.println("Retrying");
                categoriesRead = false;
            }
        } while (!categoriesRead);

        for (final String eachCat : cats) {
            if (ArrayUtils.contains(categories, substringAfter(eachCat, ":"))) {
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
