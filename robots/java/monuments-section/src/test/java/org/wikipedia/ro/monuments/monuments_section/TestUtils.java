package org.wikipedia.ro.monuments.monuments_section;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

public class TestUtils {
    @Test
    public void testCapitalize() {
        Assert.assertEquals("Capitalize should leave already capitalized strings alone", "George",
            Utils.capitalize("George"));
        Assert.assertEquals("Capitalize should only change the first letter to uppercase", "George",
            Utils.capitalize("george"));
        Assert.assertEquals("Capitalize should leave upper case and lower case letters alone if they are not the first",
            "GEoRgE", Utils.capitalize("gEoRgE"));
    }

    @Test
    public void testJoinWithConjunction() {
        Assert.assertEquals("Conjunction should be between the last two and separator between others",
            Utils.joinWithConjunction(Arrays.asList("1", "2", "3", "4"), ", ", " și "), "1, 2, 3 și 4");
        Assert.assertEquals("Empty list should join to empty string",
            Utils.joinWithConjunction(new ArrayList<String>(), ", ", " și "), "");
        Assert.assertNull("Null list should join to null string",
            Utils.joinWithConjunction(null, ", ", " și "));
        Assert.assertEquals("List with one element should join to that element",
            Utils.joinWithConjunction(Arrays.asList("1"), ", ", " și "), "1");
        Assert.assertEquals("List with two elements should join them only with the conjunction",
            Utils.joinWithConjunction(Arrays.asList("1", "2"), ", ", " și "), "1 și 2");
    }
    
    @Test
    public void testSimplyPluralizeOne() {
        Assert.assertEquals("obiectiv", Utils.simplyPluralizeE(1, "obiectiv"));
    }

    @Test
    public void testSimplyPluralizeMore() {
        Assert.assertEquals(Utils.simplyPluralizeE(3, "obiectiv"), "obiective");
    }
    
    
    @Test
    public void testQualifyinglyPluralizeOne() {
        Assert.assertEquals("un singur obiectiv", Utils.qualifyinglyPluralizeE(1, "obiectiv"));
    }

    @Test
    public void testQualifyinglyPluralizeThree() {
        Assert.assertEquals("trei obiective", Utils.qualifyinglyPluralizeE(3, "obiectiv"));
    }

    @Test
    public void testQualifyinglyPluralizeNty() {
        Assert.assertEquals(Utils.qualifyinglyPluralizeE(36, "obiectiv"), "treizeci și șase de obiective");
    }

    @Test
    public void testQualifyinglyPluralizeHundredsAndAFew() {
        Assert.assertEquals(Utils.qualifyinglyPluralizeE(106, "obiectiv"), "106 obiective");
    }

    @Test
    public void testQualifyinglyPluralizeHundredsAndNty() {
        Assert.assertEquals(Utils.qualifyinglyPluralizeE(156, "obiectiv"), "156 de obiective");
    }
}
