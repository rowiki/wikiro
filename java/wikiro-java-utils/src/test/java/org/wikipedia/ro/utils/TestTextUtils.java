package org.wikipedia.ro.utils;

import org.junit.Assert;
import org.junit.Test;

public class TestTextUtils {
    @Test
    public void testDe() {
        Assert.assertEquals("vacă", TextUtils.de(1, "vacă", "vaci"));
        Assert.assertEquals("vaci", TextUtils.de(2, "vacă", "vaci"));
        Assert.assertEquals("de&nbsp;vaci", TextUtils.de(20, "vacă", "vaci"));
        Assert.assertEquals("vaci", TextUtils.de(0, "vacă", "vaci"));
        Assert.assertEquals("vaci", TextUtils.de(107, "vacă", "vaci"));
        Assert.assertEquals("de&nbsp;vaci", TextUtils.de(200, "vacă", "vaci"));
    }
}
