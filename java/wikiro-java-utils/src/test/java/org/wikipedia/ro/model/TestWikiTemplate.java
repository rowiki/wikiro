package org.wikipedia.ro.model;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestWikiTemplate {

    @Test
    public void testToStringInlineNoParams() {
        WikiTemplate sut = new WikiTemplate();
        sut.setTemplateTitle("Template");
        
        Assertions.assertEquals("{{Template}}", sut.toString());
    }

    @Test
    public void testToStringInlineAnonParamsNoGap() {
        WikiTemplate sut = new WikiTemplate();
        sut.setTemplateTitle("Template");
        sut.setParam("1", Arrays.asList(new PlainText("t1")));
        sut.setParam("2", Arrays.asList(new PlainText("t2")));
        
        Assertions.assertEquals("{{Template|t1|t2}}", sut.toString());
    }

    @Test
    public void testToStringInlineAnonParamsWithGap() {
        WikiTemplate sut = new WikiTemplate();
        sut.setTemplateTitle("Template");
        sut.setParam("1", Arrays.asList(new PlainText("t1")));
        sut.setParam("2", Arrays.asList(new PlainText("t2")));
        sut.setParam("4", Arrays.asList(new PlainText("t4")));
        
        Assertions.assertEquals("{{Template|t1|t2|4=t4}}", sut.toString());
    }
    
    @Test
    public void testToStringInlineAnonParamsWithGapAndNamed() {
        WikiTemplate sut = new WikiTemplate();
        sut.setTemplateTitle("Template");
        sut.setParam("1", Arrays.asList(new PlainText("t1")));
        sut.setParam("2", Arrays.asList(new PlainText("t2")));
        sut.setParam("4", Arrays.asList(new PlainText("t4")));
        sut.setParam("key", Arrays.asList(new PlainText("value")));
        
        Assertions.assertEquals("{{Template|t1|t2|4=t4|key=value}}", sut.toString());
    }
    
    @Test
    public void testToStringBlockAnonParamsWithGap() {
        WikiTemplate sut = new WikiTemplate();
        sut.setTemplateTitle("Template");
        sut.setParam("1", Arrays.asList(new PlainText("t1")));
        sut.setParam("2", Arrays.asList(new PlainText("t2")));
        sut.setParam("4", Arrays.asList(new PlainText("t4")));
        sut.setSingleLine(false);
        
        Assertions.assertEquals("{{Template\n|t1\n|t2\n| 4 = t4\n}}", sut.toString());
    }
    
    @Test
    public void testToStringBlockAnonParamsWithGapAndNamed() {
        WikiTemplate sut = new WikiTemplate();
        sut.setTemplateTitle("Template");
        sut.setParam("1", Arrays.asList(new PlainText("t1")));
        sut.setParam("2", Arrays.asList(new PlainText("t2")));
        sut.setParam("4", Arrays.asList(new PlainText("t4")));
        sut.setParam("key", Arrays.asList(new PlainText("value")));
        sut.setSingleLine(false);
        
        Assertions.assertEquals("{{Template\n|t1\n|t2\n| 4 = t4\n| key = value\n}}", sut.toString());
    }

}
