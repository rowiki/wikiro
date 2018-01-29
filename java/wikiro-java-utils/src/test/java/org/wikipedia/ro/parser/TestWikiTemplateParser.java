package org.wikipedia.ro.parser;

import org.junit.Assert;
import org.junit.Test;
import org.wikipedia.ro.model.WikiTemplate;

public class TestWikiTemplateParser {

    @Test
    public void testOnlyTemplateNoParams() {
        String text = "{{TemplateNoArgs}}";
        
        WikiTemplateParser sut = new WikiTemplateParser();
        
        sut.parse(text, (parsedPart, parsedText, unparsedText) -> {
            
            Assert.assertEquals(parsedText, text);
            Assert.assertEquals(unparsedText, "");
            Assert.assertNotNull(parsedPart);
            Assert.assertTrue(parsedPart instanceof WikiTemplate);
            WikiTemplate parsedTemplate = (WikiTemplate) parsedPart;
            Assert.assertEquals(0, parsedTemplate.getParams().size());
            Assert.assertEquals("TemplateNoArgs", parsedTemplate.getTemplateTitle());
            
            return null;
        });
    }

    @Test
    public void testOnlyTemplateNamedParams() {
        String text = "{{TemplateNamedArg|param = param1}}";
        
        WikiTemplateParser sut = new WikiTemplateParser();
        
        sut.parse(text, (parsedPart, parsedText, unparsedText) -> {
            
            Assert.assertEquals(parsedText, text);
            Assert.assertEquals(unparsedText, "");
            Assert.assertNotNull(parsedPart);
            Assert.assertTrue(parsedPart instanceof WikiTemplate);
            WikiTemplate parsedTemplate = (WikiTemplate) parsedPart;
            Assert.assertEquals("TemplateNamedArg", parsedTemplate.getTemplateTitle());
            Assert.assertEquals(1, parsedTemplate.getParams().size());
            Assert.assertTrue(parsedTemplate.getParamNames().contains("param"));
            Assert.assertEquals("param1", parsedTemplate.getParams().get("param"));
            return null;
        });
    }
    
    @Test
    public void testTemplateNoParamsWithExtraText() {
        String text = "{{TemplateNoArgs}}AndSomeMore";
        
        WikiTemplateParser sut = new WikiTemplateParser();
        
        sut.parse(text, (parsedPart, parsedText, unparsedText) -> {
            
            Assert.assertEquals("{{TemplateNoArgs}}", parsedText);
            Assert.assertEquals(unparsedText, "AndSomeMore");
            Assert.assertNotNull(parsedPart);
            Assert.assertTrue(parsedPart instanceof WikiTemplate);
            WikiTemplate parsedTemplate = (WikiTemplate) parsedPart;
            Assert.assertEquals(0, parsedTemplate.getParams().size());
            Assert.assertEquals("TemplateNoArgs", parsedTemplate.getTemplateTitle());
      
            return null;
        });
    }

    @Test
    public void testTemplateNamedAndUnnamedParamsWithExtraText() {
        String text = "{{TemplateManyArgs|p1=v1|vanon}}AndSomeMore";
        
        WikiTemplateParser sut = new WikiTemplateParser();
        
        sut.parse(text, (parsedPart, parsedText, unparsedText) -> {
            
            Assert.assertEquals("{{TemplateManyArgs|p1=v1|vanon}}", parsedText);
            Assert.assertEquals(unparsedText, "AndSomeMore");
            Assert.assertNotNull(parsedPart);
            Assert.assertTrue(parsedPart instanceof WikiTemplate);
            WikiTemplate parsedTemplate = (WikiTemplate) parsedPart;
            Assert.assertEquals("TemplateManyArgs", parsedTemplate.getTemplateTitle());
            Assert.assertEquals(2, parsedTemplate.getParams().size());
            Assert.assertTrue(parsedTemplate.getParamNames().contains("p1"));
            Assert.assertEquals("v1", parsedTemplate.getParams().get("p1"));
            Assert.assertEquals("vanon", parsedTemplate.getParams().get("1"));
            Assert.assertNull(parsedTemplate.getParams().get("2"));
            Assert.assertNull(parsedTemplate.getParams().get("p2"));
            
            return null;
        });
    }

}
