package org.wikipedia.ro.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestWikiLink {
    @Test
    public void testLinkWithNoLabel() {
        WikiLink link = new WikiLink("Target");
        assertEquals("[[Target]]", link.toString());
    }

    @Test
    public void testLinkWithSameLabel() {
        WikiLink link = new WikiLink("Target", "Target");
        assertEquals("[[Target]]", link.toString());
    }

    @Test
    public void testLinkWithDifferentLabel() {
        WikiLink link = new WikiLink("Target", "label");
        assertEquals("[[Target|label]]", link.toString());
    }

    @Test
    public void testLinkWithNonCapitalizedLabel() {
        WikiLink link = new WikiLink("Target", "target");
        assertEquals("[[target]]", link.toString());
    }
}
