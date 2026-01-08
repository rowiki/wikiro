package org.wikipedia.ro.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.wikipedia.ro.model.PlainText;
import org.wikipedia.ro.model.WikiLink;
import org.wikipedia.ro.model.WikiPart;

public class TestAggregatingParser {

    @Test
    public void testOneTypeOfPartLink() {
        String wikiText = "[[link]]";

        AggregatingParser sut = new AggregatingParser();

        List<ParseResult<WikiPart>> parseRes = sut.parse(wikiText);

        assertNotNull(parseRes);
        assertEquals(1, parseRes.size());
        ParseResult<? extends WikiPart> theResult = parseRes.get(0);
        assertTrue(theResult.getIdentifiedPart() instanceof WikiLink);
        WikiLink link = (WikiLink) theResult.getIdentifiedPart();
        assertEquals("link", link.getTarget());
    }
    
    @Test
    public void testLinkAndText() {
        String wikiText = "[[link]] and text";

        AggregatingParser sut = new AggregatingParser();

        List<ParseResult<WikiPart>> parseRes = sut.parse(wikiText);

        assertNotNull(parseRes);
        assertEquals(2, parseRes.size());
        ParseResult<WikiPart> firstResult = parseRes.get(0);
        assertTrue(firstResult.getIdentifiedPart() instanceof WikiLink);
        WikiLink link = (WikiLink) firstResult.getIdentifiedPart();
        assertEquals("link", link.getTarget());

        ParseResult<WikiPart> secondResult = parseRes.get(1);
        assertTrue(secondResult.getIdentifiedPart() instanceof PlainText);
        PlainText plainText = (PlainText) secondResult.getIdentifiedPart();
        assertEquals(" and text", plainText.getText());
    }
}
