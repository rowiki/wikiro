package org.wikipedia.ro.parser;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.wikipedia.ro.model.PlainText;
import org.wikipedia.ro.model.WikiLink;
import org.wikipedia.ro.model.WikiPart;

public class TestAggregatingParser {

    @Test
    public void testOneTypeOfPartLink() {
        String wikiText = "[[link]]";

        AggregatingParser sut = new AggregatingParser();

        List<ParseResult<WikiPart>> parseRes = sut.parse(wikiText);

        Assert.assertNotNull(parseRes);
        Assert.assertEquals(1, parseRes.size());
        ParseResult<? extends WikiPart> theResult = parseRes.get(0);
        Assert.assertTrue(theResult.getIdentifiedPart() instanceof WikiLink);
        WikiLink link = (WikiLink) theResult.getIdentifiedPart();
        Assert.assertEquals("link", link.getTarget());
    }
    
    @Test
    public void testLinkAndText() {
        String wikiText = "[[link]] and text";

        AggregatingParser sut = new AggregatingParser();

        List<ParseResult<WikiPart>> parseRes = sut.parse(wikiText);

        Assert.assertNotNull(parseRes);
        Assert.assertEquals(2, parseRes.size());
        ParseResult<WikiPart> firstResult = parseRes.get(0);
        Assert.assertTrue(firstResult.getIdentifiedPart() instanceof WikiLink);
        WikiLink link = (WikiLink) firstResult.getIdentifiedPart();
        Assert.assertEquals("link", link.getTarget());

        ParseResult<WikiPart> secondResult = parseRes.get(1);
        Assert.assertTrue(secondResult.getIdentifiedPart() instanceof PlainText);
        PlainText plainText = (PlainText) secondResult.getIdentifiedPart();
        Assert.assertEquals(" and text", plainText.getText());
    }
}
