package org.wikipedia.ro.model;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestWikiTag {

    @Test
    public void testToStringOpeningTagWithAttributes() {
        WikiTag sut = new WikiTag();
        sut.setTagName("tag");
        sut.setAttribute("attr1", Arrays.asList(new PlainText("value1")));
        sut.setAttribute("attr2", Arrays.asList(new PlainText("value2")));
        
        Assertions.assertEquals("<tag attr1=\"value1\" attr2=\"value2\">", sut.toString());
    }

    @Test
    public void testToStringOpeningTagNoAttributes() {
        WikiTag sut = new WikiTag();
        sut.setTagName("tag");
        
        Assertions.assertEquals("<tag>", sut.toString());
    }

    @Test
    public void testToStringSelfClosingTagWithAttributes() {
        WikiTag sut = new WikiTag();
        sut.setTagName("tag");
        sut.setAttribute("attr1", Arrays.asList(new PlainText("value1")));
        sut.setAttribute("attr2", Arrays.asList(new PlainText("value2")));
        sut.setSelfClosing(true);
        
        Assertions.assertEquals("<tag attr1=\"value1\" attr2=\"value2\" />", sut.toString());
    }

    @Test
    public void testToStringClosingTag() {
        WikiTag sut = new WikiTag();
        sut.setTagName("tag");
        sut.setClosing(true);
        
        Assertions.assertEquals("</tag>", sut.toString());
    }
}
