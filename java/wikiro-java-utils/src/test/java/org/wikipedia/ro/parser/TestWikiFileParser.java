package org.wikipedia.ro.parser;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
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
        
        Assert.assertEquals(wikiText, parseRes.getParsedString());
        Assert.assertEquals("", parseRes.getUnparsedString());

        Assert.assertNotNull(parseRes);
        Assert.assertNotNull(parseRes.getIdentifiedPart());

        WikiFile parsedFile = parseRes.getIdentifiedPart();
        Assert.assertEquals("Fișier", parsedFile.getNamespace());
        Assert.assertEquals("gigibobo.jpg", parsedFile.getName());
        Assert.assertNull(parsedFile.getDisplayType());
        Assert.assertNull(parsedFile.getAlignment());
        Assert.assertNull(parsedFile.getAlt());
        Assert.assertNull(parsedFile.getBorder());
        Assert.assertNull(parsedFile.getLang());
        Assert.assertNull(parsedFile.getLink());
        Assert.assertNull(parsedFile.getLocation());
        Assert.assertNull(parsedFile.getSize());
        Assert.assertEquals(0, parsedFile.getCaptions().size());
    }

    @Test
    public void testParseImageWithCaption() {
        String wikiText = "[[Fișier:gigibobo.jpg|[[Gigi bobo]] cu nana]]";

        WikiFileParser fileParser = new WikiFileParser();
        ParseResult<WikiFile> parseRes = fileParser.parse(wikiText);
        Assert.assertEquals(wikiText, parseRes.getParsedString());
        Assert.assertEquals("", parseRes.getUnparsedString());

        Assert.assertNotNull(parseRes);
        Assert.assertNotNull(parseRes.getIdentifiedPart());

        WikiFile parsedFile = parseRes.getIdentifiedPart();
        Assert.assertEquals("Fișier", parsedFile.getNamespace());
        Assert.assertEquals("gigibobo.jpg", parsedFile.getName());
        Assert.assertNull(parsedFile.getAlignment());
        Assert.assertNull(parsedFile.getDisplayType());
        Assert.assertNull(parsedFile.getAlt());
        Assert.assertNull(parsedFile.getBorder());
        Assert.assertNull(parsedFile.getLang());
        Assert.assertNull(parsedFile.getLink());
        Assert.assertNull(parsedFile.getLocation());
        Assert.assertNull(parsedFile.getSize());
        Assert.assertEquals(1, parsedFile.getCaptions().size());
        List<WikiPart> captionParts = parsedFile.getCaption();
        Assert.assertEquals(2, captionParts.size());
        Assert.assertTrue(captionParts.get(0) instanceof WikiLink);
        Assert.assertEquals("Gigi bobo", ((WikiLink) captionParts.get(0)).getTarget());
        Assert.assertTrue(captionParts.get(1) instanceof PlainText);
        Assert.assertEquals(" cu nana", ((PlainText) captionParts.get(1)).getText());
    }

    @Test
    public void testParseImageUprightWithCaption() {
        String wikiText = "[[Fișier:gigibobo.jpg|upright|[[Gigi bobo]] cu nana]]";

        WikiFileParser fileParser = new WikiFileParser();
        ParseResult<WikiFile> parseRes = fileParser.parse(wikiText);
        Assert.assertEquals(wikiText, parseRes.getParsedString());
        Assert.assertEquals("", parseRes.getUnparsedString());

        Assert.assertNotNull(parseRes);
        Assert.assertNotNull(parseRes.getIdentifiedPart());

        WikiFile parsedFile = parseRes.getIdentifiedPart();
        Assert.assertEquals("Fișier", parsedFile.getNamespace());
        Assert.assertEquals("gigibobo.jpg", parsedFile.getName());
        Assert.assertNull(parsedFile.getDisplayType());
        Assert.assertNull(parsedFile.getAlignment());
        Assert.assertNull(parsedFile.getAlt());
        Assert.assertNull(parsedFile.getBorder());
        Assert.assertNull(parsedFile.getLang());
        Assert.assertNull(parsedFile.getLink());
        Assert.assertNull(parsedFile.getLocation());
        Assert.assertEquals("upright", parsedFile.getSize());
        Assert.assertEquals(1, parsedFile.getCaptions().size());
        List<WikiPart> captionParts = parsedFile.getCaption();
        Assert.assertEquals(2, captionParts.size());
        Assert.assertTrue(captionParts.get(0) instanceof WikiLink);
        Assert.assertEquals("Gigi bobo", ((WikiLink) captionParts.get(0)).getTarget());
        Assert.assertTrue(captionParts.get(1) instanceof PlainText);
        Assert.assertEquals(" cu nana", ((PlainText) captionParts.get(1)).getText());
    }

    @Test
    public void testParseImageWithUprightFactorAndCaption() {
        String wikiText = "[[Fișier:gigibobo.jpg|upright=0.7|[[Gigi bobo]] cu nana]]";

        WikiFileParser fileParser = new WikiFileParser();
        ParseResult<WikiFile> parseRes = fileParser.parse(wikiText);
        Assert.assertEquals(wikiText, parseRes.getParsedString());
        Assert.assertEquals("", parseRes.getUnparsedString());

        Assert.assertNotNull(parseRes);
        Assert.assertNotNull(parseRes.getIdentifiedPart());

        WikiFile parsedFile = parseRes.getIdentifiedPart();
        Assert.assertEquals("Fișier", parsedFile.getNamespace());
        Assert.assertEquals("gigibobo.jpg", parsedFile.getName());
        Assert.assertNull(parsedFile.getDisplayType());
        Assert.assertNull(parsedFile.getAlignment());
        Assert.assertNull(parsedFile.getAlt());
        Assert.assertNull(parsedFile.getBorder());
        Assert.assertNull(parsedFile.getLang());
        Assert.assertNull(parsedFile.getLink());
        Assert.assertNull(parsedFile.getLocation());
        Assert.assertEquals("upright=0.7", parsedFile.getSize());
        Assert.assertEquals(1, parsedFile.getCaptions().size());
        List<WikiPart> captionParts = parsedFile.getCaption();
        Assert.assertEquals(2, captionParts.size());
        Assert.assertTrue(captionParts.get(0) instanceof WikiLink);
        Assert.assertEquals("Gigi bobo", ((WikiLink) captionParts.get(0)).getTarget());
        Assert.assertTrue(captionParts.get(1) instanceof PlainText);
        Assert.assertEquals(" cu nana", ((PlainText) captionParts.get(1)).getText());
    }

    @Test
    public void testParseImageWithFixedSizeAndCaption() {
        String wikiText = "[[Fișier:gigibobo.jpg|216px|[[Gigi bobo]] cu nana]]";

        WikiFileParser fileParser = new WikiFileParser();
        ParseResult<WikiFile> parseRes = fileParser.parse(wikiText);
        Assert.assertEquals(wikiText, parseRes.getParsedString());
        Assert.assertEquals("", parseRes.getUnparsedString());

        Assert.assertNotNull(parseRes);
        Assert.assertNotNull(parseRes.getIdentifiedPart());

        WikiFile parsedFile = parseRes.getIdentifiedPart();
        Assert.assertEquals("Fișier", parsedFile.getNamespace());
        Assert.assertEquals("gigibobo.jpg", parsedFile.getName());
        Assert.assertNull(parsedFile.getAlignment());
        Assert.assertNull(parsedFile.getDisplayType());
        Assert.assertNull(parsedFile.getAlt());
        Assert.assertNull(parsedFile.getBorder());
        Assert.assertNull(parsedFile.getLang());
        Assert.assertNull(parsedFile.getLink());
        Assert.assertNull(parsedFile.getLocation());
        Assert.assertEquals("216px", parsedFile.getSize());
        Assert.assertEquals(1, parsedFile.getCaptions().size());
        List<WikiPart> captionParts = parsedFile.getCaption();
        Assert.assertEquals(2, captionParts.size());
        Assert.assertTrue(captionParts.get(0) instanceof WikiLink);
        Assert.assertEquals("Gigi bobo", ((WikiLink) captionParts.get(0)).getTarget());
        Assert.assertTrue(captionParts.get(1) instanceof PlainText);
        Assert.assertEquals(" cu nana", ((PlainText) captionParts.get(1)).getText());
    }

    @Test
    public void testParseImageWithFixedSizeFramedAndCaption() {
        String wikiText = "[[Fișier:gigibobo.jpg|200x200px|[[Gigi bobo]] cu nana]]";

        WikiFileParser fileParser = new WikiFileParser();
        ParseResult<WikiFile> parseRes = fileParser.parse(wikiText);
        Assert.assertEquals(wikiText, parseRes.getParsedString());
        Assert.assertEquals("", parseRes.getUnparsedString());

        Assert.assertNotNull(parseRes);
        Assert.assertNotNull(parseRes.getIdentifiedPart());

        WikiFile parsedFile = parseRes.getIdentifiedPart();
        Assert.assertEquals("Fișier", parsedFile.getNamespace());
        Assert.assertEquals("gigibobo.jpg", parsedFile.getName());
        Assert.assertNull(parsedFile.getAlignment());
        Assert.assertNull(parsedFile.getDisplayType());
        Assert.assertNull(parsedFile.getAlt());
        Assert.assertNull(parsedFile.getBorder());
        Assert.assertNull(parsedFile.getLang());
        Assert.assertNull(parsedFile.getLink());
        Assert.assertNull(parsedFile.getLocation());
        Assert.assertEquals("200x200px", parsedFile.getSize());
        Assert.assertEquals(1, parsedFile.getCaptions().size());
        List<WikiPart> captionParts = parsedFile.getCaption();
        Assert.assertEquals(2, captionParts.size());
        Assert.assertTrue(captionParts.get(0) instanceof WikiLink);
        Assert.assertEquals("Gigi bobo", ((WikiLink) captionParts.get(0)).getTarget());
        Assert.assertTrue(captionParts.get(1) instanceof PlainText);
        Assert.assertEquals(" cu nana", ((PlainText) captionParts.get(1)).getText());
    }

    @Test
    public void testParseImageWithThumbBorderSizeAndCaption() {
        String wikiText = "[[Fișier:gigibobo.jpg|thumb|border|200x200px|[[Gigi bobo]] cu nana]]";

        WikiFileParser fileParser = new WikiFileParser();
        ParseResult<WikiFile> parseRes = fileParser.parse(wikiText);
        Assert.assertEquals(wikiText, parseRes.getParsedString());
        Assert.assertEquals("", parseRes.getUnparsedString());

        Assert.assertNotNull(parseRes);
        Assert.assertNotNull(parseRes.getIdentifiedPart());

        WikiFile parsedFile = parseRes.getIdentifiedPart();
        Assert.assertEquals("Fișier", parsedFile.getNamespace());
        Assert.assertEquals("gigibobo.jpg", parsedFile.getName());
        Assert.assertNull(parsedFile.getAlignment());
        Assert.assertNull(parsedFile.getAlt());
        Assert.assertEquals("thumb", parsedFile.getDisplayType());
        Assert.assertEquals("border", parsedFile.getBorder());
        Assert.assertNull(parsedFile.getLang());
        Assert.assertNull(parsedFile.getLink());
        Assert.assertNull(parsedFile.getLocation());
        Assert.assertEquals("200x200px", parsedFile.getSize());
        Assert.assertEquals(1, parsedFile.getCaptions().size());
        List<WikiPart> captionParts = parsedFile.getCaption();
        Assert.assertEquals(2, captionParts.size());
        Assert.assertTrue(captionParts.get(0) instanceof WikiLink);
        Assert.assertEquals("Gigi bobo", ((WikiLink) captionParts.get(0)).getTarget());
        Assert.assertTrue(captionParts.get(1) instanceof PlainText);
        Assert.assertEquals(" cu nana", ((PlainText) captionParts.get(1)).getText());
    }
    
    
    @Test
    public void testParseImageWithLangAndCaption() {
        String wikiText = "[[Fișier:gigibobo.jpg|lang=ro|[[Gigi bobo]] cu nana]]";

        WikiFileParser fileParser = new WikiFileParser();
        ParseResult<WikiFile> parseRes = fileParser.parse(wikiText);
        Assert.assertEquals(wikiText, parseRes.getParsedString());
        Assert.assertEquals("", parseRes.getUnparsedString());

        Assert.assertNotNull(parseRes);
        Assert.assertNotNull(parseRes.getIdentifiedPart());

        WikiFile parsedFile = parseRes.getIdentifiedPart();
        Assert.assertEquals("Fișier", parsedFile.getNamespace());
        Assert.assertEquals("gigibobo.jpg", parsedFile.getName());
        Assert.assertNull(parsedFile.getDisplayType());
        Assert.assertNull(parsedFile.getAlignment());
        Assert.assertNull(parsedFile.getAlt());
        Assert.assertNull(parsedFile.getBorder());
        Assert.assertEquals("ro", parsedFile.getLang());
        Assert.assertNull(parsedFile.getLink());
        Assert.assertNull(parsedFile.getLocation());
        Assert.assertNull(parsedFile.getSize());
        Assert.assertEquals(1, parsedFile.getCaptions().size());
        List<WikiPart> captionParts = parsedFile.getCaption();
        Assert.assertEquals(2, captionParts.size());
        Assert.assertTrue(captionParts.get(0) instanceof WikiLink);
        Assert.assertEquals("Gigi bobo", ((WikiLink) captionParts.get(0)).getTarget());
        Assert.assertTrue(captionParts.get(1) instanceof PlainText);
        Assert.assertEquals(" cu nana", ((PlainText) captionParts.get(1)).getText());
    }

    @Test
    public void testParseImageWithThumbLocationSizeAndCaption() {
        String wikiText = "[[Fișier:gigibobo.jpg|thumb|left|upright|[[Gigi bobo]] cu nana]]";

        WikiFileParser fileParser = new WikiFileParser();
        ParseResult<WikiFile> parseRes = fileParser.parse(wikiText);
        Assert.assertEquals(wikiText, parseRes.getParsedString());
        Assert.assertEquals("", parseRes.getUnparsedString());

        Assert.assertNotNull(parseRes);
        Assert.assertNotNull(parseRes.getIdentifiedPart());

        WikiFile parsedFile = parseRes.getIdentifiedPart();
        Assert.assertEquals("Fișier", parsedFile.getNamespace());
        Assert.assertEquals("gigibobo.jpg", parsedFile.getName());
        Assert.assertNull(parsedFile.getAlignment());
        Assert.assertNull(parsedFile.getAlt());
        Assert.assertEquals("thumb", parsedFile.getDisplayType());
        Assert.assertNull(parsedFile.getBorder());
        Assert.assertNull(parsedFile.getLang());
        Assert.assertNull(parsedFile.getLink());
        Assert.assertEquals("left", parsedFile.getLocation());
        Assert.assertEquals("upright", parsedFile.getSize());
        Assert.assertEquals(1, parsedFile.getCaptions().size());
        List<WikiPart> captionParts = parsedFile.getCaption();
        Assert.assertEquals(2, captionParts.size());
        Assert.assertTrue(captionParts.get(0) instanceof WikiLink);
        Assert.assertEquals("Gigi bobo", ((WikiLink) captionParts.get(0)).getTarget());
        Assert.assertTrue(captionParts.get(1) instanceof PlainText);
        Assert.assertEquals(" cu nana", ((PlainText) captionParts.get(1)).getText());
    }
}
