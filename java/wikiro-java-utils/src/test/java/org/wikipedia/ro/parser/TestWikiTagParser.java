package org.wikipedia.ro.parser;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.wikipedia.ro.model.PlainText;
import org.wikipedia.ro.model.WikiTag;
import org.wikipedia.ro.model.WikiTemplate;

public class TestWikiTagParser {

    @Test
    public void testSingleTagNoClosing() {
        String wikiText = "<span lang=\"tr\">";

        WikiTagParser sut = new WikiTagParser();

        ParseResult<WikiTag> parseRes = sut.parse(wikiText);

        Assertions.assertNotNull(parseRes);
        Assertions.assertEquals(wikiText, parseRes.getParsedString());
        Assertions.assertEquals("", parseRes.getUnparsedString());
        Assertions.assertEquals("span", parseRes.getIdentifiedPart().getTagName());
        Assertions.assertEquals(1, parseRes.getIdentifiedPart().getAttributes().size());
        Assertions.assertEquals("tr", parseRes.getIdentifiedPart().getAttributes().get("lang").stream().map(Object::toString)
            .collect(Collectors.joining("")));
        Assertions.assertFalse(parseRes.getIdentifiedPart().isClosing());
        Assertions.assertFalse(parseRes.getIdentifiedPart().isSelfClosing());
    }

    @Test
    public void testSingleTagClosing() {
        String wikiText = "</span>";

        WikiTagParser sut = new WikiTagParser();

        ParseResult<WikiTag> parseRes = sut.parse(wikiText);

        Assertions.assertNotNull(parseRes);
        Assertions.assertEquals(wikiText, parseRes.getParsedString());
        Assertions.assertEquals("", parseRes.getUnparsedString());
        Assertions.assertEquals("span", parseRes.getIdentifiedPart().getTagName());
        Assertions.assertEquals(0, parseRes.getIdentifiedPart().getAttributes().size());
        Assertions.assertTrue(parseRes.getIdentifiedPart().isClosing());
        Assertions.assertFalse(parseRes.getIdentifiedPart().isSelfClosing());
    }

    @Test
    public void testSingleTagSelfClosing() {
        String wikiText = "<br />";

        WikiTagParser sut = new WikiTagParser();

        ParseResult<WikiTag> parseRes = sut.parse(wikiText);

        Assertions.assertNotNull(parseRes);
        Assertions.assertEquals(wikiText, parseRes.getParsedString());
        Assertions.assertEquals("", parseRes.getUnparsedString());
        Assertions.assertEquals("br", parseRes.getIdentifiedPart().getTagName());
        Assertions.assertEquals(0, parseRes.getIdentifiedPart().getAttributes().size());
        Assertions.assertFalse(parseRes.getIdentifiedPart().isClosing());
        Assertions.assertTrue(parseRes.getIdentifiedPart().isSelfClosing());
    }

    @Test
    public void testTagNoClosingWithText() {
        String wikiText = "<test key='val'>and another thing";

        WikiTagParser sut = new WikiTagParser();
        sut.setValidTagNames(Arrays.asList("test"));

        ParseResult<WikiTag> parseRes = sut.parse(wikiText);

        Assertions.assertNotNull(parseRes);
        Assertions.assertEquals("<test key='val'>", parseRes.getParsedString());
        Assertions.assertEquals("and another thing", parseRes.getUnparsedString());
        Assertions.assertEquals("test", parseRes.getIdentifiedPart().getTagName());
        Assertions.assertEquals(1, parseRes.getIdentifiedPart().getAttributes().size());
        Assertions.assertEquals("val", parseRes.getIdentifiedPart().getAttributes().get("key").stream().map(Object::toString)
            .collect(Collectors.joining("")));
        Assertions.assertFalse(parseRes.getIdentifiedPart().isClosing());
        Assertions.assertFalse(parseRes.getIdentifiedPart().isSelfClosing());
    }

    @Test
    public void testSingleTagSelfClosingWithTemplateInAttrValue() {
        String wikiText = "<span lang=\"en_{{Abbreviation|country=USA}}\">";

        WikiTagParser sut = new WikiTagParser();

        ParseResult<WikiTag> parseRes = sut.parse(wikiText);

        Assertions.assertNotNull(parseRes);
        Assertions.assertEquals(wikiText, parseRes.getParsedString());
        Assertions.assertEquals("", parseRes.getUnparsedString());
        Assertions.assertEquals("span", parseRes.getIdentifiedPart().getTagName());
        Assertions.assertEquals(1, parseRes.getIdentifiedPart().getAttributes().size());
        Assertions.assertEquals(2, parseRes.getIdentifiedPart().getAttributes().get("lang").size());
        Assertions.assertEquals("en_{{Abbreviation|country=USA}}", parseRes.getIdentifiedPart().getAttributes().get("lang")
            .stream().map(Object::toString).collect(Collectors.joining("")));
        Assertions.assertTrue(parseRes.getIdentifiedPart().getAttributes().get("lang").get(0) instanceof PlainText);
        Assertions.assertEquals("en_", ((PlainText) parseRes.getIdentifiedPart().getAttributes().get("lang").get(0)).getText());

        Assertions.assertTrue(parseRes.getIdentifiedPart().getAttributes().get("lang").get(1) instanceof WikiTemplate);
        WikiTemplate tmpl = (WikiTemplate) parseRes.getIdentifiedPart().getAttributes().get("lang").get(1);
        Assertions.assertEquals("Abbreviation", tmpl.getTemplateTitle());
        Assertions.assertEquals(1, tmpl.getParamNames().size());
        Assertions.assertNotNull(tmpl.getParam("country"));
        Assertions.assertEquals(1, tmpl.getParam("country").size());
        Assertions.assertEquals("USA", tmpl.getParam("country").get(0).toString());

        Assertions.assertFalse(parseRes.getIdentifiedPart().isClosing());
        Assertions.assertFalse(parseRes.getIdentifiedPart().isSelfClosing());
    }

    @Test
    public void testTagWithParamsWithoutQuotes() {
        String wikiText = "<font color=white>Out of tag";

        WikiTagParser sut = new WikiTagParser();
        ParseResult<WikiTag> parseRes = sut.parse(wikiText);

        Assertions.assertEquals("<font color=white>", parseRes.getParsedString());
        Assertions.assertEquals("Out of tag", parseRes.getUnparsedString());

        WikiTag tag = parseRes.getIdentifiedPart();
        Assertions.assertEquals("font", tag.getTagName());
        Assertions.assertEquals(1, tag.getAttributes().size());
        Assertions.assertNotNull(tag.getAttributes().get("color"));
        Assertions.assertEquals("white",
            tag.getAttributes().get("color").stream().map(Object::toString).collect(Collectors.joining()));
    }
    
    @Test
    public void testSelfClosingTagWithParamsWithoutQuotes() {
        String wikiText = "<font color=white/>Out of tag";

        WikiTagParser sut = new WikiTagParser();
        ParseResult<WikiTag> parseRes = sut.parse(wikiText);

        Assertions.assertEquals("<font color=white/>", parseRes.getParsedString());
        Assertions.assertEquals("Out of tag", parseRes.getUnparsedString());

        WikiTag tag = parseRes.getIdentifiedPart();
        Assertions.assertEquals("font", tag.getTagName());
        Assertions.assertEquals(1, tag.getAttributes().size());
        Assertions.assertNotNull(tag.getAttributes().get("color"));
        Assertions.assertEquals("white",
            tag.getAttributes().get("color").stream().map(Object::toString).collect(Collectors.joining()));
        Assertions.assertTrue(tag.isSelfClosing());
    }

    @Test
    public void testTagWithSpacesInAttr() {
        String wikiText =
            "<div class=\"NavHead\" style=\"background-color:#025ad0;border:1px solid #000000;padding: 0px 0px 0px 4px; font-size: 100%; text-align:left;margin:0px;color:#330000\">";

        WikiTagParser sut = new WikiTagParser();
        ParseResult<WikiTag> parseRes = sut.parse(wikiText);

        WikiTag tag = parseRes.getIdentifiedPart();
        Assertions.assertEquals("div", tag.getTagName());
        Assertions.assertEquals("NavHead",
            tag.getAttributes().get("class").stream().map(Object::toString).collect(Collectors.joining()));
        Assertions.assertEquals(
            "background-color:#025ad0;border:1px solid #000000;padding: 0px 0px 0px 4px; font-size: 100%; text-align:left;margin:0px;color:#330000",
            tag.getAttributes().get("style").stream().map(Object::toString).collect(Collectors.joining()));
    }
 
}
