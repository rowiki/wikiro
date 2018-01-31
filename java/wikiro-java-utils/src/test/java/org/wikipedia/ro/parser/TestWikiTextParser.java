package org.wikipedia.ro.parser;

import org.junit.Assert;
import org.junit.Test;
import org.wikipedia.ro.model.PlainText;

public class TestWikiTextParser {

    @Test
    public void testNonPretentiousWikiText() {
        WikiTextParser sut = new WikiTextParser();
        ParseResult<PlainText> parseResult = sut.parse("wiki text");

        Assert.assertEquals("wiki text", parseResult.getParsedString());
        Assert.assertEquals("", parseResult.getUnparsedString());
        Assert.assertNotNull(parseResult.getIdentifiedPart());
        Assert.assertEquals("wiki text", parseResult.getIdentifiedPart().getText());
    }
    
    @Test
    public void testWikiTextFollowedByLink() {
        WikiTextParser sut = new WikiTextParser();
        ParseResult<PlainText> parseResult = sut.parse("wiki text[[link]]");

        Assert.assertEquals("wiki text", parseResult.getParsedString());
        Assert.assertEquals("[[link]]", parseResult.getUnparsedString());
        Assert.assertNotNull(parseResult.getIdentifiedPart());
        Assert.assertEquals("wiki text", parseResult.getIdentifiedPart().getText());
        
    }

    @Test
    public void testWikiTextFollowedByTemplate() {
        WikiTextParser sut = new WikiTextParser();
        ParseResult<PlainText> parseResult = sut.parse("wiki text{{Template}}");

        Assert.assertEquals("wiki text", parseResult.getParsedString());
        Assert.assertEquals("{{Template}}", parseResult.getUnparsedString());
        Assert.assertNotNull(parseResult.getIdentifiedPart());
        Assert.assertEquals("wiki text", parseResult.getIdentifiedPart().getText());
        
    }

}
