package org.wikipedia.ro.utils;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class TestPageUtils {
    @Test
    public void testFindsTwoTemplates() {
        List<WikiTemplate> templatesInText =
            PageUtils.getTemplatesInText("Gigi {{are}} {{ouă|2}} în {{locație|nume=cuibar}}");
        Assert.assertEquals(3, templatesInText.size());
    }
}
