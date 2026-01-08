package org.wikipedia.ro.parser;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.wikipedia.ro.model.PlainText;
import org.wikipedia.ro.model.WikiLink;

public class TestWikiLinkParser {

    @Test
    public void testParseOnlyLinkNoLabel() {
        String text = "[[Link]]";

        WikiLinkParser sut = new WikiLinkParser();
        ParseResult<WikiLink> parseResult = sut.parse(text);

        Assertions.assertEquals("[[Link]]", parseResult.getParsedString());
        Assertions.assertEquals("", parseResult.getUnparsedString());
        WikiLink identifiedPart = parseResult.getIdentifiedPart();
        Assertions.assertNotNull(identifiedPart);
        Assertions.assertTrue(identifiedPart instanceof WikiLink);

        Assertions.assertEquals("Link", identifiedPart.getTarget());
        Assertions.assertEquals(0, identifiedPart.getLabelStructure().size());
    }

    @Test
    public void testParseOnlyWithLabel() {
        String text = "[[Link|Target]]";

        WikiLinkParser sut = new WikiLinkParser();

        ParseResult<WikiLink> parseResult = sut.parse(text);

        Assertions.assertEquals("[[Link|Target]]", parseResult.getParsedString());
        Assertions.assertEquals("", parseResult.getUnparsedString());
        Assertions.assertNotNull(parseResult.getIdentifiedPart());
        WikiLink parsedPart = parseResult.getIdentifiedPart();
        Assertions.assertTrue(parsedPart instanceof WikiLink);

        Assertions.assertEquals("Link", parsedPart.getTarget());
        Assertions.assertEquals(1, parsedPart.getLabelStructure().size());
        Assertions.assertTrue(parsedPart.getLabelStructure().get(0) instanceof PlainText);
        Assertions.assertEquals("Target", ((PlainText) parsedPart.getLabelStructure().get(0)).getText());

    }

    @Test
    public void testParseLinkWithLabelWithText() {
        String text = "[[Link|Target]] and more";

        WikiLinkParser sut = new WikiLinkParser();

        ParseResult<WikiLink> parseResult = sut.parse(text);

        Assertions.assertEquals("[[Link|Target]]", parseResult.getParsedString());
        Assertions.assertEquals(" and more", parseResult.getUnparsedString());
        
        WikiLink parsedPart = parseResult.getIdentifiedPart();
        Assertions.assertNotNull(parsedPart);
        Assertions.assertEquals("Link", parsedPart.getTarget());
        Assertions.assertEquals(1, parsedPart.getLabelStructure().size());
        Assertions.assertTrue(parsedPart.getLabelStructure().get(0) instanceof PlainText);
        Assertions.assertEquals("Target", ((PlainText) parsedPart.getLabelStructure().get(0)).getText());

    }
    
}
