package org.wikipedia.ro.parser;

import org.junit.Assert;
import org.junit.Test;
import org.wikipedia.ro.model.WikiTemplate;

public class TestWikiTemplateParser {

    @Test
    public void testOnlyTemplateNoParams() {
        String text = "{{TemplateNoArgs}}";

        WikiTemplateParser sut = new WikiTemplateParser();

        ParseResult<WikiTemplate> parseResult = sut.parse(text);

        Assert.assertEquals(parseResult.getParsedString(), text);
        Assert.assertEquals("", parseResult.getUnparsedString());
        WikiTemplate parsedPart = parseResult.getIdentifiedPart();
        Assert.assertNotNull(parsedPart);
        Assert.assertEquals(0, parsedPart.getParams().size());
        Assert.assertEquals("TemplateNoArgs", parsedPart.getTemplateTitle());

    }

    @Test
    public void testOnlyTemplateNamedParams() {
        String text = "{{TemplateNamedArg|param = param1}}";

        WikiTemplateParser sut = new WikiTemplateParser();

        ParseResult<WikiTemplate> parseResult = sut.parse(text);

        Assert.assertEquals(parseResult.getParsedString(), text);
        Assert.assertEquals("", parseResult.getUnparsedString());
        WikiTemplate parsedPart = parseResult.getIdentifiedPart();
        Assert.assertNotNull(parsedPart);
        Assert.assertEquals("TemplateNamedArg", parsedPart.getTemplateTitle());
        Assert.assertEquals(1, parsedPart.getParams().size());
        Assert.assertTrue(parsedPart.getParamNames().contains("param"));
        Assert.assertEquals("param1", parsedPart.getParams().get("param"));
    }

    @Test
    public void testTemplateNoParamsWithExtraText() {
        String text = "{{TemplateNoArgs}}AndSomeMore";

        WikiTemplateParser sut = new WikiTemplateParser();

        ParseResult<WikiTemplate> parseResult = sut.parse(text);

        Assert.assertEquals("{{TemplateNoArgs}}", parseResult.getParsedString());
        Assert.assertEquals("AndSomeMore", parseResult.getUnparsedString());
        WikiTemplate parsedPart = parseResult.getIdentifiedPart();
        Assert.assertNotNull(parsedPart);
        Assert.assertEquals(0, parsedPart.getParams().size());
        Assert.assertEquals("TemplateNoArgs", parsedPart.getTemplateTitle());
    }

    @Test
    public void testTemplateNamedAndUnnamedParamsWithExtraText() {
        String text = "{{TemplateManyArgs|p1=v1|vanon}}AndSomeMore";

        WikiTemplateParser sut = new WikiTemplateParser();

        ParseResult<WikiTemplate> parseResult = sut.parse(text);

        Assert.assertEquals("{{TemplateManyArgs|p1=v1|vanon}}", parseResult.getParsedString());
        Assert.assertEquals("AndSomeMore", parseResult.getUnparsedString());
        WikiTemplate parsedPart = parseResult.getIdentifiedPart();
        Assert.assertNotNull(parsedPart);
        Assert.assertEquals("TemplateManyArgs", parsedPart.getTemplateTitle());
        Assert.assertEquals(2, parsedPart.getParams().size());
        Assert.assertTrue(parsedPart.getParamNames().contains("p1"));
        Assert.assertEquals("v1", parsedPart.getParams().get("p1"));
        Assert.assertEquals("vanon", parsedPart.getParams().get("1"));
        Assert.assertNull(parsedPart.getParams().get("2"));
        Assert.assertNull(parsedPart.getParams().get("p2"));

    }

}
