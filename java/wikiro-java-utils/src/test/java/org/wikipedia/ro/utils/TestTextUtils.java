package org.wikipedia.ro.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestTextUtils {
    @Test
    public void testDe() {
        Assertions.assertEquals("vacă", TextUtils.de(1, "vacă", "vaci"));
        Assertions.assertEquals("vaci", TextUtils.de(2, "vacă", "vaci"));
        Assertions.assertEquals("de&nbsp;vaci", TextUtils.de(20, "vacă", "vaci"));
        Assertions.assertEquals("vaci", TextUtils.de(0, "vacă", "vaci"));
        Assertions.assertEquals("vaci", TextUtils.de(107, "vacă", "vaci"));
        Assertions.assertEquals("de&nbsp;vaci", TextUtils.de(200, "vacă", "vaci"));
    }
    
    @Test
    public void testCapitalizeName() {
        Assertions.assertEquals("Poienile de sub Munte", TextUtils.capitalizeName("poiEnile de SUB muNte"));
        Assertions.assertEquals("Gigi Gheorghescu", TextUtils.capitalizeName("gigi gheorghescu"));
        Assertions.assertEquals("Gigi Gheorghescu", TextUtils.capitalizeName("GIGI GHEORGHESCU"));
    }
}
