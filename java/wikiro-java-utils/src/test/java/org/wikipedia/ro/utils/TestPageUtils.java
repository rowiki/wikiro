package org.wikipedia.ro.utils;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.wikipedia.ro.model.WikiTemplate;

public class TestPageUtils {
    @Test
    public void testFindsTwoTemplates() {
        List<WikiTemplate> templatesInText =
            PageUtils.getTemplatesInText("Gigi {{are}} {{ouă|2}} în {{locație|nume=cuibar}}");
        Assertions.assertEquals(3, templatesInText.size());
    }
}
