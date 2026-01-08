package org.wikipedia.ro.parser;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.wikipedia.ro.model.PlainText;

public class TestWikiTextParser {

    @Test
    public void testNonPretentiousWikiText() {
        WikiTextParser sut = new WikiTextParser();
        ParseResult<PlainText> parseResult = sut.parse("wiki text");

        Assertions.assertEquals("wiki text", parseResult.getParsedString());
        Assertions.assertEquals("", parseResult.getUnparsedString());
        Assertions.assertNotNull(parseResult.getIdentifiedPart());
        Assertions.assertEquals("wiki text", parseResult.getIdentifiedPart().getText());
    }
    
    @Test
    public void testWikiTextFollowedByLink() {
        WikiTextParser sut = new WikiTextParser();
        ParseResult<PlainText> parseResult = sut.parse("wiki text[[link]]");

        Assertions.assertEquals("wiki text", parseResult.getParsedString());
        Assertions.assertEquals("[[link]]", parseResult.getUnparsedString());
        Assertions.assertNotNull(parseResult.getIdentifiedPart());
        Assertions.assertEquals("wiki text", parseResult.getIdentifiedPart().getText());
        
    }

    @Test
    public void testWikiTextFollowedByTemplate() {
        WikiTextParser sut = new WikiTextParser();
        ParseResult<PlainText> parseResult = sut.parse("wiki text{{Template}}");

        Assertions.assertEquals("wiki text", parseResult.getParsedString());
        Assertions.assertEquals("{{Template}}", parseResult.getUnparsedString());
        Assertions.assertNotNull(parseResult.getIdentifiedPart());
        Assertions.assertEquals("wiki text", parseResult.getIdentifiedPart().getText());
        
    }

}
