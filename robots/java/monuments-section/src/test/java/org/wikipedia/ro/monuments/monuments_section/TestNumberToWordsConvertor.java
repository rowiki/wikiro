package org.wikipedia.ro.monuments.monuments_section;

import org.junit.Test;

import junit.framework.Assert;

public class TestNumberToWordsConvertor {
    @Test
    public void testOneDigitNumber() {
        Assert.assertEquals(new NumberToWordsConvertor(5).convert(), "cinci");
        Assert.assertEquals(new NumberToWordsConvertor(1).convert(), "un");
    }
    @Test
    public void testTeenNumber() {
        Assert.assertEquals(new NumberToWordsConvertor(14).convert(), "paisprezece");
        Assert.assertEquals(new NumberToWordsConvertor(15).convert(), "cincisprezece");
        Assert.assertEquals(new NumberToWordsConvertor(16).convert(), "șaisprezece");
        Assert.assertEquals(new NumberToWordsConvertor(11).convert(), "unsprezece");
    }

    @Test
    public void testRoundTwoDigitsNumber() {
        Assert.assertEquals(new NumberToWordsConvertor(20).convert(), "douăzeci");
        Assert.assertEquals(new NumberToWordsConvertor(30).convert(), "treizeci");
        Assert.assertEquals(new NumberToWordsConvertor(50).convert(), "cincizeci");
    }

    @Test
    public void testNonRoundTwoDigitsNumber() {
        Assert.assertEquals(new NumberToWordsConvertor(21).convert(), "douăzeci și unu");
        Assert.assertEquals(new NumberToWordsConvertor(35).convert(), "treizeci și cinci");
        Assert.assertEquals(new NumberToWordsConvertor(56).convert(), "cincizeci și șase");
    }

    @Test
    public void testMultipleDigitsNumbers() {
        Assert.assertEquals(new NumberToWordsConvertor(124).convert(), "124");
        Assert.assertEquals(new NumberToWordsConvertor(4888).convert(), "4888");
        Assert.assertEquals(new NumberToWordsConvertor(300).convert(), "300");
    }

}
