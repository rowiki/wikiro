package org.wikipedia.ro.parser;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.wikipedia.ro.model.PlainText;
import org.wikipedia.ro.model.WikiLink;
import org.wikipedia.ro.model.WikiPart;

public class TestWikiLinkParser {

    @Test
    public void testParseOnlyLinkNoLabel() {
        String text = "[[Link]]";

        WikiLinkParser sut = new WikiLinkParser();
        ParseResult<WikiLink> parseResult = sut.parse(text);

        Assert.assertEquals("[[Link]]", parseResult.getParsedString());
        Assert.assertEquals("", parseResult.getUnparsedString());
        WikiLink identifiedPart = parseResult.getIdentifiedPart();
        Assert.assertNotNull(identifiedPart);
        Assert.assertTrue(identifiedPart instanceof WikiLink);

        Assert.assertEquals("Link", identifiedPart.getTarget());
        Assert.assertEquals(0, identifiedPart.getLabelStructure().size());
    }

    @Test
    public void testParseOnlyWithLabel() {
        String text = "[[Link|Target]]";

        WikiLinkParser sut = new WikiLinkParser();

        ParseResult<WikiLink> parseResult = sut.parse(text);

        Assert.assertEquals("[[Link|Target]]", parseResult.getParsedString());
        Assert.assertEquals("", parseResult.getUnparsedString());
        Assert.assertNotNull(parseResult.getIdentifiedPart());
        WikiLink parsedPart = parseResult.getIdentifiedPart();
        Assert.assertTrue(parsedPart instanceof WikiLink);

        Assert.assertEquals("Link", parsedPart.getTarget());
        Assert.assertEquals(1, parsedPart.getLabelStructure().size());
        Assert.assertTrue(parsedPart.getLabelStructure().get(0) instanceof PlainText);
        Assert.assertEquals("Target", ((PlainText) parsedPart.getLabelStructure().get(0)).getText());

    }

    @Test
    public void testParseLinkWithLabelWithText() {
        String text = "[[Link|Target]] and more";

        WikiLinkParser sut = new WikiLinkParser();

        ParseResult<WikiLink> parseResult = sut.parse(text);

        Assert.assertEquals("[[Link|Target]]", parseResult.getParsedString());
        Assert.assertEquals(" and more", parseResult.getUnparsedString());
        
        WikiLink parsedPart = parseResult.getIdentifiedPart();
        Assert.assertNotNull(parsedPart);
        Assert.assertEquals("Link", parsedPart.getTarget());
        Assert.assertEquals(1, parsedPart.getLabelStructure().size());
        Assert.assertTrue(parsedPart.getLabelStructure().get(0) instanceof PlainText);
        Assert.assertEquals("Target", ((PlainText) parsedPart.getLabelStructure().get(0)).getText());

    }
    
}
