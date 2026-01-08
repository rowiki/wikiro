package org.wikipedia.ro.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.wikipedia.ro.model.PlainText;
import org.wikipedia.ro.model.WikiFile;
import org.wikipedia.ro.model.WikiLink;
import org.wikipedia.ro.model.WikiPart;

public class TestWikiFileParser {
    @Test
    public void testParseImageNoCaption() {
        String wikiText = "[[Fișier:gigibobo.jpg]]";

        WikiFileParser fileParser = new WikiFileParser();
        ParseResult<WikiFile> parseRes = fileParser.parse(wikiText);
        
        assertEquals(wikiText, parseRes.getParsedString());
        assertEquals("", parseRes.getUnparsedString());

        assertNotNull(parseRes);
        assertNotNull(parseRes.getIdentifiedPart());

        WikiFile parsedFile = parseRes.getIdentifiedPart();
        assertEquals("Fișier", parsedFile.getNamespace());
        assertEquals("gigibobo.jpg", parsedFile.getName());
        assertNull(parsedFile.getDisplayType());
        assertNull(parsedFile.getAlignment());
        assertNull(parsedFile.getAlt());
        assertNull(parsedFile.getBorder());
        assertNull(parsedFile.getLang());
        assertNull(parsedFile.getLink());
        assertNull(parsedFile.getLocation());
        assertNull(parsedFile.getSize());
        assertEquals(0, parsedFile.getCaptions().size());
    }

    @Test
    public void testParseImageWithCaption() {
        String wikiText = "[[Fișier:gigibobo.jpg|[[Gigi bobo]] cu nana]]";

        WikiFileParser fileParser = new WikiFileParser();
        ParseResult<WikiFile> parseRes = fileParser.parse(wikiText);
        assertEquals(wikiText, parseRes.getParsedString());
        assertEquals("", parseRes.getUnparsedString());

        assertNotNull(parseRes);
        assertNotNull(parseRes.getIdentifiedPart());

        WikiFile parsedFile = parseRes.getIdentifiedPart();
        assertEquals("Fișier", parsedFile.getNamespace());
        assertEquals("gigibobo.jpg", parsedFile.getName());
        assertNull(parsedFile.getAlignment());
        assertNull(parsedFile.getDisplayType());
        assertNull(parsedFile.getAlt());
        assertNull(parsedFile.getBorder());
        assertNull(parsedFile.getLang());
        assertNull(parsedFile.getLink());
        assertNull(parsedFile.getLocation());
        assertNull(parsedFile.getSize());
        assertEquals(1, parsedFile.getCaptions().size());
        List<WikiPart> captionParts = parsedFile.getCaption();
        assertEquals(2, captionParts.size());
        assertTrue(captionParts.get(0) instanceof WikiLink);
        assertEquals("Gigi bobo", ((WikiLink) captionParts.get(0)).getTarget());
        assertTrue(captionParts.get(1) instanceof PlainText);
        assertEquals(" cu nana", ((PlainText) captionParts.get(1)).getText());
    }

    @Test
    public void testParseImageUprightWithCaption() {
        String wikiText = "[[Fișier:gigibobo.jpg|upright|[[Gigi bobo]] cu nana]]";

        WikiFileParser fileParser = new WikiFileParser();
        ParseResult<WikiFile> parseRes = fileParser.parse(wikiText);
        assertEquals(wikiText, parseRes.getParsedString());
        assertEquals("", parseRes.getUnparsedString());

        assertNotNull(parseRes);
        assertNotNull(parseRes.getIdentifiedPart());

        WikiFile parsedFile = parseRes.getIdentifiedPart();
        assertEquals("Fișier", parsedFile.getNamespace());
        assertEquals("gigibobo.jpg", parsedFile.getName());
        assertNull(parsedFile.getDisplayType());
        assertNull(parsedFile.getAlignment());
        assertNull(parsedFile.getAlt());
        assertNull(parsedFile.getBorder());
        assertNull(parsedFile.getLang());
        assertNull(parsedFile.getLink());
        assertNull(parsedFile.getLocation());
        assertEquals("upright", parsedFile.getSize());
        assertEquals(1, parsedFile.getCaptions().size());
        List<WikiPart> captionParts = parsedFile.getCaption();
        assertEquals(2, captionParts.size());
        assertTrue(captionParts.get(0) instanceof WikiLink);
        assertEquals("Gigi bobo", ((WikiLink) captionParts.get(0)).getTarget());
        assertTrue(captionParts.get(1) instanceof PlainText);
        assertEquals(" cu nana", ((PlainText) captionParts.get(1)).getText());
    }

    @Test
    public void testParseImageWithUprightFactorAndCaption() {
        String wikiText = "[[Fișier:gigibobo.jpg|upright=0.7|[[Gigi bobo]] cu nana]]";

        WikiFileParser fileParser = new WikiFileParser();
        ParseResult<WikiFile> parseRes = fileParser.parse(wikiText);
        assertEquals(wikiText, parseRes.getParsedString());
        assertEquals("", parseRes.getUnparsedString());

        assertNotNull(parseRes);
        assertNotNull(parseRes.getIdentifiedPart());

        WikiFile parsedFile = parseRes.getIdentifiedPart();
        assertEquals("Fișier", parsedFile.getNamespace());
        assertEquals("gigibobo.jpg", parsedFile.getName());
        assertNull(parsedFile.getDisplayType());
        assertNull(parsedFile.getAlignment());
        assertNull(parsedFile.getAlt());
        assertNull(parsedFile.getBorder());
        assertNull(parsedFile.getLang());
        assertNull(parsedFile.getLink());
        assertNull(parsedFile.getLocation());
        assertEquals("upright=0.7", parsedFile.getSize());
        assertEquals(1, parsedFile.getCaptions().size());
        List<WikiPart> captionParts = parsedFile.getCaption();
        assertEquals(2, captionParts.size());
        assertTrue(captionParts.get(0) instanceof WikiLink);
        assertEquals("Gigi bobo", ((WikiLink) captionParts.get(0)).getTarget());
        assertTrue(captionParts.get(1) instanceof PlainText);
        assertEquals(" cu nana", ((PlainText) captionParts.get(1)).getText());
    }

    @Test
    public void testParseImageWithFixedSizeAndCaption() {
        String wikiText = "[[Fișier:gigibobo.jpg|216px|[[Gigi bobo]] cu nana]]";

        WikiFileParser fileParser = new WikiFileParser();
        ParseResult<WikiFile> parseRes = fileParser.parse(wikiText);
        assertEquals(wikiText, parseRes.getParsedString());
        assertEquals("", parseRes.getUnparsedString());

        assertNotNull(parseRes);
        assertNotNull(parseRes.getIdentifiedPart());

        WikiFile parsedFile = parseRes.getIdentifiedPart();
        assertEquals("Fișier", parsedFile.getNamespace());
        assertEquals("gigibobo.jpg", parsedFile.getName());
        assertNull(parsedFile.getAlignment());
        assertNull(parsedFile.getDisplayType());
        assertNull(parsedFile.getAlt());
        assertNull(parsedFile.getBorder());
        assertNull(parsedFile.getLang());
        assertNull(parsedFile.getLink());
        assertNull(parsedFile.getLocation());
        assertEquals("216px", parsedFile.getSize());
        assertEquals(1, parsedFile.getCaptions().size());
        List<WikiPart> captionParts = parsedFile.getCaption();
        assertEquals(2, captionParts.size());
        assertTrue(captionParts.get(0) instanceof WikiLink);
        assertEquals("Gigi bobo", ((WikiLink) captionParts.get(0)).getTarget());
        assertTrue(captionParts.get(1) instanceof PlainText);
        assertEquals(" cu nana", ((PlainText) captionParts.get(1)).getText());
    }

    @Test
    public void testParseImageWithFixedSizeFramedAndCaption() {
        String wikiText = "[[Fișier:gigibobo.jpg|200x200px|[[Gigi bobo]] cu nana]]";

        WikiFileParser fileParser = new WikiFileParser();
        ParseResult<WikiFile> parseRes = fileParser.parse(wikiText);
        assertEquals(wikiText, parseRes.getParsedString());
        assertEquals("", parseRes.getUnparsedString());

        assertNotNull(parseRes);
        assertNotNull(parseRes.getIdentifiedPart());

        WikiFile parsedFile = parseRes.getIdentifiedPart();
        assertEquals("Fișier", parsedFile.getNamespace());
        assertEquals("gigibobo.jpg", parsedFile.getName());
        assertNull(parsedFile.getAlignment());
        assertNull(parsedFile.getDisplayType());
        assertNull(parsedFile.getAlt());
        assertNull(parsedFile.getBorder());
        assertNull(parsedFile.getLang());
        assertNull(parsedFile.getLink());
        assertNull(parsedFile.getLocation());
        assertEquals("200x200px", parsedFile.getSize());
        assertEquals(1, parsedFile.getCaptions().size());
        List<WikiPart> captionParts = parsedFile.getCaption();
        assertEquals(2, captionParts.size());
        assertTrue(captionParts.get(0) instanceof WikiLink);
        assertEquals("Gigi bobo", ((WikiLink) captionParts.get(0)).getTarget());
        assertTrue(captionParts.get(1) instanceof PlainText);
        assertEquals(" cu nana", ((PlainText) captionParts.get(1)).getText());
    }

    @Test
    public void testParseImageWithThumbBorderSizeAndCaption() {
        String wikiText = "[[Fișier:gigibobo.jpg|thumb|border|200x200px|[[Gigi bobo]] cu nana]]";

        WikiFileParser fileParser = new WikiFileParser();
        ParseResult<WikiFile> parseRes = fileParser.parse(wikiText);
        assertEquals(wikiText, parseRes.getParsedString());
        assertEquals("", parseRes.getUnparsedString());

        assertNotNull(parseRes);
        assertNotNull(parseRes.getIdentifiedPart());

        WikiFile parsedFile = parseRes.getIdentifiedPart();
        assertEquals("Fișier", parsedFile.getNamespace());
        assertEquals("gigibobo.jpg", parsedFile.getName());
        assertNull(parsedFile.getAlignment());
        assertNull(parsedFile.getAlt());
        assertEquals("thumb", parsedFile.getDisplayType());
        assertEquals("border", parsedFile.getBorder());
        assertNull(parsedFile.getLang());
        assertNull(parsedFile.getLink());
        assertNull(parsedFile.getLocation());
        assertEquals("200x200px", parsedFile.getSize());
        assertEquals(1, parsedFile.getCaptions().size());
        List<WikiPart> captionParts = parsedFile.getCaption();
        assertEquals(2, captionParts.size());
        assertTrue(captionParts.get(0) instanceof WikiLink);
        assertEquals("Gigi bobo", ((WikiLink) captionParts.get(0)).getTarget());
        assertTrue(captionParts.get(1) instanceof PlainText);
        assertEquals(" cu nana", ((PlainText) captionParts.get(1)).getText());
    }
    
    
    @Test
    public void testParseImageWithLangAndCaption() {
        String wikiText = "[[Fișier:gigibobo.jpg|lang=ro|[[Gigi bobo]] cu nana]]";

        WikiFileParser fileParser = new WikiFileParser();
        ParseResult<WikiFile> parseRes = fileParser.parse(wikiText);
        assertEquals(wikiText, parseRes.getParsedString());
        assertEquals("", parseRes.getUnparsedString());

        assertNotNull(parseRes);
        assertNotNull(parseRes.getIdentifiedPart());

        WikiFile parsedFile = parseRes.getIdentifiedPart();
        assertEquals("Fișier", parsedFile.getNamespace());
        assertEquals("gigibobo.jpg", parsedFile.getName());
        assertNull(parsedFile.getDisplayType());
        assertNull(parsedFile.getAlignment());
        assertNull(parsedFile.getAlt());
        assertNull(parsedFile.getBorder());
        assertEquals("ro", parsedFile.getLang());
        assertNull(parsedFile.getLink());
        assertNull(parsedFile.getLocation());
        assertNull(parsedFile.getSize());
        assertEquals(1, parsedFile.getCaptions().size());
        List<WikiPart> captionParts = parsedFile.getCaption();
        assertEquals(2, captionParts.size());
        assertTrue(captionParts.get(0) instanceof WikiLink);
        assertEquals("Gigi bobo", ((WikiLink) captionParts.get(0)).getTarget());
        assertTrue(captionParts.get(1) instanceof PlainText);
        assertEquals(" cu nana", ((PlainText) captionParts.get(1)).getText());
    }

    @Test
    public void testParseImageWithThumbLocationSizeAndCaption() {
        String wikiText = "[[Fișier:gigibobo.jpg|thumb|left|upright|[[Gigi bobo]] cu nana]]";

        WikiFileParser fileParser = new WikiFileParser();
        ParseResult<WikiFile> parseRes = fileParser.parse(wikiText);
        assertEquals(wikiText, parseRes.getParsedString());
        assertEquals("", parseRes.getUnparsedString());

        assertNotNull(parseRes);
        assertNotNull(parseRes.getIdentifiedPart());

        WikiFile parsedFile = parseRes.getIdentifiedPart();
        assertEquals("Fișier", parsedFile.getNamespace());
        assertEquals("gigibobo.jpg", parsedFile.getName());
        assertNull(parsedFile.getAlignment());
        assertNull(parsedFile.getAlt());
        assertEquals("thumb", parsedFile.getDisplayType());
        assertNull(parsedFile.getBorder());
        assertNull(parsedFile.getLang());
        assertNull(parsedFile.getLink());
        assertEquals("left", parsedFile.getLocation());
        assertEquals("upright", parsedFile.getSize());
        assertEquals(1, parsedFile.getCaptions().size());
        List<WikiPart> captionParts = parsedFile.getCaption();
        assertEquals(2, captionParts.size());
        assertTrue(captionParts.get(0) instanceof WikiLink);
        assertEquals("Gigi bobo", ((WikiLink) captionParts.get(0)).getTarget());
        assertTrue(captionParts.get(1) instanceof PlainText);
        assertEquals(" cu nana", ((PlainText) captionParts.get(1)).getText());
    }
}
