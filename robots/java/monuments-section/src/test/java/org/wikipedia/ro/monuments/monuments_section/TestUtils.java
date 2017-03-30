package org.wikipedia.ro.monuments.monuments_section;

import org.junit.Assert;
import org.junit.Test;

public class TestUtils {
    @Test
    public void testCapitalize() {
        Assert.assertEquals("Capitalize should leave already capitalized strings alone", "George", Utils.capitalize("George"));
        Assert.assertEquals("Capitalize should only change the first letter to uppercase", "George", Utils.capitalize("george"));
        Assert.assertEquals("Capitalize should leave upper case and lower case letters alone if they are not the first", "GEoRgE", Utils.capitalize("gEoRgE"));
    }
}
