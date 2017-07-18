package org.wikipedia.ro.monuments.monuments_section;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

public class TestUtils {
    
    private String[] drumGrammarSet = new String[] {"drum", "drumul", "drumuri", "drumurile"};
    
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
    public void testQualifyinglyPluralizeEOne() {
        Assert.assertEquals("un singur obiectiv", Utils.qualifyinglyPluralizeE(1, "obiectiv"));
    }

    @Test
    public void testQualifyinglyPluralizeEThree() {
        Assert.assertEquals("trei obiective", Utils.qualifyinglyPluralizeE(3, "obiectiv"));
    }

    @Test
    public void testQualifyinglyPluralizeENty() {
        Assert.assertEquals(Utils.qualifyinglyPluralizeE(36, "obiectiv"), "treizeci și șase de obiective");
    }

    @Test
    public void testQualifyinglyPluralizeEHundredsAndAFew() {
        Assert.assertEquals(Utils.qualifyinglyPluralizeE(106, "obiectiv"), "106 obiective");
    }

    @Test
    public void testQualifyinglyPluralizeEHundredsAndNty() {
        Assert.assertEquals("156 de obiective", Utils.qualifyinglyPluralizeE(156, "obiectiv"));
    }
    
    @Test
    public void testSimplyPluralizeByGrammarSetOne() {
        Assert.assertEquals("drum", Utils.simplyPluralizeByGrammarSet(1, drumGrammarSet));
    }

    @Test
    public void testSimplyPluralizeByGrammarSetMore() {
        Assert.assertEquals("drumuri", Utils.simplyPluralizeByGrammarSet(3, drumGrammarSet));
    }
    
    
    @Test
    public void testQualifyinglyPluralizeByGrammarSetOne() {
        Assert.assertEquals("un singur drum", Utils.qualifyinglyPluralizeByGrammarSet(1, drumGrammarSet));
    }

    @Test
    public void testQualifyinglyPluralizeByGrammarSetThree() {
        Assert.assertEquals("trei drumuri", Utils.qualifyinglyPluralizeByGrammarSet(3, drumGrammarSet));
    }

    @Test
    public void testQualifyinglyPluralizeByGrammarSetNty() {
        Assert.assertEquals("treizeci și șase de drumuri", Utils.qualifyinglyPluralizeByGrammarSet(36, drumGrammarSet));
    }

    @Test
    public void testQualifyinglyPluralizeByGrammarSetHundredsAndAFew() {
        Assert.assertEquals("106 drumuri", Utils.qualifyinglyPluralizeByGrammarSet(106, drumGrammarSet));
    }

    @Test
    public void testQualifyinglyPluralizeByGrammarSetHundredsAndNty() {
        Assert.assertEquals("156 de drumuri", Utils.qualifyinglyPluralizeByGrammarSet(156, drumGrammarSet));
    }
}
