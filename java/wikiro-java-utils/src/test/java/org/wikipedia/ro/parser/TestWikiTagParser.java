package org.wikipedia.ro.parser;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.wikipedia.ro.model.PlainText;
import org.wikipedia.ro.model.WikiTag;
import org.wikipedia.ro.model.WikiTemplate;

public class TestWikiTagParser {

    @Test
    public void testSingleTagNoClosing() {
        String wikiText = "<span lang=\"tr\">";

        WikiTagParser sut = new WikiTagParser();

        ParseResult<WikiTag> parseRes = sut.parse(wikiText);

        Assert.assertNotNull(parseRes);
        Assert.assertEquals(wikiText, parseRes.getParsedString());
        Assert.assertEquals("", parseRes.getUnparsedString());
        Assert.assertEquals("span", parseRes.getIdentifiedPart().getTagName());
        Assert.assertEquals(1, parseRes.getIdentifiedPart().getAttributes().size());
        Assert.assertEquals("tr", parseRes.getIdentifiedPart().getAttributes().get("lang").stream().map(Object::toString)
            .collect(Collectors.joining("")));
        Assert.assertFalse(parseRes.getIdentifiedPart().isClosing());
        Assert.assertFalse(parseRes.getIdentifiedPart().isSelfClosing());
    }

    @Test
    public void testSingleTagClosing() {
        String wikiText = "</span>";

        WikiTagParser sut = new WikiTagParser();

        ParseResult<WikiTag> parseRes = sut.parse(wikiText);

        Assert.assertNotNull(parseRes);
        Assert.assertEquals(wikiText, parseRes.getParsedString());
        Assert.assertEquals("", parseRes.getUnparsedString());
        Assert.assertEquals("span", parseRes.getIdentifiedPart().getTagName());
        Assert.assertEquals(0, parseRes.getIdentifiedPart().getAttributes().size());
        Assert.assertTrue(parseRes.getIdentifiedPart().isClosing());
        Assert.assertFalse(parseRes.getIdentifiedPart().isSelfClosing());
    }

    @Test
    public void testSingleTagSelfClosing() {
        String wikiText = "<br />";

        WikiTagParser sut = new WikiTagParser();

        ParseResult<WikiTag> parseRes = sut.parse(wikiText);

        Assert.assertNotNull(parseRes);
        Assert.assertEquals(wikiText, parseRes.getParsedString());
        Assert.assertEquals("", parseRes.getUnparsedString());
        Assert.assertEquals("br", parseRes.getIdentifiedPart().getTagName());
        Assert.assertEquals(0, parseRes.getIdentifiedPart().getAttributes().size());
        Assert.assertFalse(parseRes.getIdentifiedPart().isClosing());
        Assert.assertTrue(parseRes.getIdentifiedPart().isSelfClosing());
    }
    
    @Test
    public void testTagNoClosingWithText() {
        String wikiText = "<test key='val'>and another thing";

        WikiTagParser sut = new WikiTagParser();
        sut.setValidTagNames(Arrays.asList("test"));

        ParseResult<WikiTag> parseRes = sut.parse(wikiText);

        Assert.assertNotNull(parseRes);
        Assert.assertEquals("<test key='val'>", parseRes.getParsedString());
        Assert.assertEquals("and another thing", parseRes.getUnparsedString());
        Assert.assertEquals("test", parseRes.getIdentifiedPart().getTagName());
        Assert.assertEquals(1, parseRes.getIdentifiedPart().getAttributes().size());
        Assert.assertEquals("val", parseRes.getIdentifiedPart().getAttributes().get("key").stream().map(Object::toString)
            .collect(Collectors.joining("")));
        Assert.assertFalse(parseRes.getIdentifiedPart().isClosing());
        Assert.assertFalse(parseRes.getIdentifiedPart().isSelfClosing());
    }

    @Test
    public void testSingleTagSelfClosingWithTemplateInAttrValue() {
        String wikiText = "<span lang=\"en_{{Abbreviation|country=USA}}\">";

        WikiTagParser sut = new WikiTagParser();

        ParseResult<WikiTag> parseRes = sut.parse(wikiText);

        Assert.assertNotNull(parseRes);
        Assert.assertEquals(wikiText, parseRes.getParsedString());
        Assert.assertEquals("", parseRes.getUnparsedString());
        Assert.assertEquals("span", parseRes.getIdentifiedPart().getTagName());
        Assert.assertEquals(1, parseRes.getIdentifiedPart().getAttributes().size());
        Assert.assertEquals(2, parseRes.getIdentifiedPart().getAttributes().get("lang").size());
        Assert.assertEquals("en_{{Abbreviation|country=USA}}", parseRes.getIdentifiedPart().getAttributes().get("lang").stream().map(Object::toString)
            .collect(Collectors.joining("")));
        Assert.assertTrue(parseRes.getIdentifiedPart().getAttributes().get("lang").get(0) instanceof PlainText);
        Assert.assertEquals("en_", ((PlainText) parseRes.getIdentifiedPart().getAttributes().get("lang").get(0)).getText());

        Assert.assertTrue(parseRes.getIdentifiedPart().getAttributes().get("lang").get(1) instanceof WikiTemplate);
        WikiTemplate tmpl = (WikiTemplate) parseRes.getIdentifiedPart().getAttributes().get("lang").get(1);
        Assert.assertEquals("Abbreviation", tmpl.getTemplateTitle());
        Assert.assertEquals(1, tmpl.getParamNames().size());
        Assert.assertNotNull(tmpl.getParam("country"));
        Assert.assertEquals(1, tmpl.getParam("country").size());
        Assert.assertEquals("USA", tmpl.getParam("country").get(0).toString());
        
        
        Assert.assertFalse(parseRes.getIdentifiedPart().isClosing());
        Assert.assertFalse(parseRes.getIdentifiedPart().isSelfClosing());
    }

    @Test
    public void testTagWithParamsWithoutQuotes() {
        String wikiText = "<font color=white>Out of tag";
        
        WikiTagParser sut = new WikiTagParser();
        ParseResult<WikiTag> parseRes = sut.parse(wikiText);

        Assert.assertEquals("<font color=white>", parseRes.getParsedString());
        Assert.assertEquals("Out of tag", parseRes.getUnparsedString());
        
        WikiTag tag = parseRes.getIdentifiedPart();
        Assert.assertEquals("font", tag.getTagName());
        Assert.assertEquals(1, tag.getAttributes().size());
        Assert.assertNotNull(tag.getAttributes().get("color"));
        Assert.assertEquals("white", tag.getAttributes().get("color").stream().map(Object::toString).collect(Collectors.joining()));
    }
}
