package org.wikipedia.ro.model;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

public class TestWikiTemplate {

    @Test
    public void testToStringInlineNoParams() {
        WikiTemplate sut = new WikiTemplate();
        sut.setTemplateTitle("Template");
        
        Assert.assertEquals("{{Template}}", sut.toString());
    }

    @Test
    public void testToStringInlineAnonParamsNoGap() {
        WikiTemplate sut = new WikiTemplate();
        sut.setTemplateTitle("Template");
        sut.setParam("1", Arrays.asList(new PlainText("t1")));
        sut.setParam("2", Arrays.asList(new PlainText("t2")));
        
        Assert.assertEquals("{{Template|t1|t2}}", sut.toString());
    }

    @Test
    public void testToStringInlineAnonParamsWithGap() {
        WikiTemplate sut = new WikiTemplate();
        sut.setTemplateTitle("Template");
        sut.setParam("1", Arrays.asList(new PlainText("t1")));
        sut.setParam("2", Arrays.asList(new PlainText("t2")));
        sut.setParam("4", Arrays.asList(new PlainText("t4")));
        
        Assert.assertEquals("{{Template|t1|t2|4=t4}}", sut.toString());
    }
    
    @Test
    public void testToStringInlineAnonParamsWithGapAndNamed() {
        WikiTemplate sut = new WikiTemplate();
        sut.setTemplateTitle("Template");
        sut.setParam("1", Arrays.asList(new PlainText("t1")));
        sut.setParam("2", Arrays.asList(new PlainText("t2")));
        sut.setParam("4", Arrays.asList(new PlainText("t4")));
        sut.setParam("key", Arrays.asList(new PlainText("value")));
        
        Assert.assertEquals("{{Template|t1|t2|4=t4|key=value}}", sut.toString());
    }
    
    @Test
    public void testToStringBlockAnonParamsWithGap() {
        WikiTemplate sut = new WikiTemplate();
        sut.setTemplateTitle("Template");
        sut.setParam("1", Arrays.asList(new PlainText("t1")));
        sut.setParam("2", Arrays.asList(new PlainText("t2")));
        sut.setParam("4", Arrays.asList(new PlainText("t4")));
        sut.setSingleLine(false);
        
        Assert.assertEquals("{{Template\n|t1\n|t2\n| 4 = t4\n}}", sut.toString());
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
        
        Assert.assertEquals("{{Template\n|t1\n|t2\n| 4 = t4\n| key = value\n}}", sut.toString());
    }

}
