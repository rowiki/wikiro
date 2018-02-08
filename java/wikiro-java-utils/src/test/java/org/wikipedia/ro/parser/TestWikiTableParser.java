package org.wikipedia.ro.parser;

import org.junit.Assert;
import org.junit.Test;
import org.wikipedia.ro.model.PlainText;
import org.wikipedia.ro.model.table.WikiTable;
import org.wikipedia.ro.model.table.WikiTableCell;
import org.wikipedia.ro.model.table.WikiTableRow;

public class TestWikiTableParser {
    @Test
    public void testParseEmptyTableWithCaption() {

        String wikiText = "{|" + "\n|+ my caption" + "\n|}";

        WikiTableParser parser = new WikiTableParser();
        ParseResult<WikiTable> parseResult = parser.parse(wikiText);
        WikiTable parsedTable = parseResult.getIdentifiedPart();
        Assert.assertNotNull(parsedTable.getSubParts());
        Assert.assertEquals(1, parsedTable.getSubParts().size());
        Assert.assertTrue(parsedTable.getSubParts().get(0) instanceof WikiTableCell);
        WikiTableCell captionPart = (WikiTableCell) parsedTable.getSubParts().get(0);
        Assert.assertEquals(1, captionPart.getSubParts().size());
        Assert.assertTrue(captionPart.getSubParts().get(0) instanceof PlainText);
        PlainText captionTxt = (PlainText) captionPart.getSubParts().get(0);
        Assert.assertEquals("my caption", captionTxt.getText().trim());
    }

    @Test
    public void testParseTableWithTwoCellsOnDifferentLines() {
        String wikiText = "{|" + "\n|-" + "\n| first cell" + "\n| second cell" + "\n|}";

        WikiTableParser parser = new WikiTableParser();
        ParseResult<WikiTable> parseResult = parser.parse(wikiText);
        WikiTable parsedTable = parseResult.getIdentifiedPart();
        Assert.assertNotNull(parsedTable.getSubParts());

        Assert.assertEquals(1, parsedTable.getSubParts().size());
        Assert.assertTrue(parsedTable.getSubParts().get(0) instanceof WikiTableRow);
        WikiTableRow row = (WikiTableRow) parsedTable.getSubParts().get(0);

        Assert.assertEquals(2, row.getSubParts().size());
        Assert.assertTrue(row.getSubParts().get(0) instanceof WikiTableCell);

        WikiTableCell wikiCell1 = (WikiTableCell) row.getSubParts().get(0);
        Assert.assertEquals(1, wikiCell1.getSubParts().size());
        Assert.assertTrue(wikiCell1.getSubParts().get(0) instanceof PlainText);
        PlainText wikiCell1Text = (PlainText) wikiCell1.getSubParts().get(0);
        Assert.assertEquals("first cell", wikiCell1Text.getText().trim());

        WikiTableCell wikiCell2 = (WikiTableCell) row.getSubParts().get(1);
        Assert.assertEquals(1, wikiCell2.getSubParts().size());
        Assert.assertTrue(wikiCell2.getSubParts().get(0) instanceof PlainText);
        PlainText wikiCelleText = (PlainText) wikiCell2.getSubParts().get(0);
        Assert.assertEquals("second cell", wikiCelleText.getText().trim());
    }
    
    @Test
    public void testParseTableWithTwoCellsOnSameLine() {
        String wikiText = "{|"
            + "\n|-"
            + "\n| first cell || second cell"
            + "\n|}";

        WikiTableParser parser = new WikiTableParser();
        ParseResult<WikiTable> parseResult = parser.parse(wikiText);
        WikiTable parsedTable = parseResult.getIdentifiedPart();
        Assert.assertNotNull(parsedTable.getSubParts());

        Assert.assertEquals(1, parsedTable.getSubParts().size());
        Assert.assertTrue(parsedTable.getSubParts().get(0) instanceof WikiTableRow);
        WikiTableRow row = (WikiTableRow) parsedTable.getSubParts().get(0);

        Assert.assertEquals(2, row.getSubParts().size());
        Assert.assertTrue(row.getSubParts().get(0) instanceof WikiTableCell);

        WikiTableCell wikiCell1 = (WikiTableCell) row.getSubParts().get(0);
        Assert.assertEquals(1, wikiCell1.getSubParts().size());
        Assert.assertTrue(wikiCell1.getSubParts().get(0) instanceof PlainText);
        PlainText wikiCell1Text = (PlainText) wikiCell1.getSubParts().get(0);
        Assert.assertEquals("first cell", wikiCell1Text.getText().trim());

        WikiTableCell wikiCell2 = (WikiTableCell) row.getSubParts().get(1);
        Assert.assertEquals(1, wikiCell2.getSubParts().size());
        Assert.assertTrue(wikiCell2.getSubParts().get(0) instanceof PlainText);
        PlainText wikiCell2Text = (PlainText) wikiCell2.getSubParts().get(0);
        Assert.assertEquals("second cell", wikiCell2Text.getText().trim());
    }
    @Test
    public void testParseTableWithTwoRowsAndTwoCellsOnDifferentLines() {
        String wikiText = "{|" 
                + "\n|-"
                + "\n| first cell"
                + "\n| second cell"
                + "\n|-"
                + "\n| third cell"
                + "\n| fourth cell"
                + "\n|}";

        WikiTableParser parser = new WikiTableParser();
        ParseResult<WikiTable> parseResult = parser.parse(wikiText);
        WikiTable parsedTable = parseResult.getIdentifiedPart();
        Assert.assertNotNull(parsedTable.getSubParts());

        Assert.assertEquals(2, parsedTable.getSubParts().size());
        Assert.assertTrue(parsedTable.getSubParts().get(0) instanceof WikiTableRow);
        WikiTableRow row = (WikiTableRow) parsedTable.getSubParts().get(0);

        Assert.assertEquals(2, row.getSubParts().size());
        Assert.assertTrue(row.getSubParts().get(0) instanceof WikiTableCell);
        Assert.assertTrue(row.getSubParts().get(1) instanceof WikiTableCell);

        WikiTableCell wikiCell1 = (WikiTableCell) row.getSubParts().get(0);
        Assert.assertEquals(1, wikiCell1.getSubParts().size());
        Assert.assertTrue(wikiCell1.getSubParts().get(0) instanceof PlainText);
        PlainText wikiCell1Text = (PlainText) wikiCell1.getSubParts().get(0);
        Assert.assertEquals("first cell", wikiCell1Text.getText().trim());

        WikiTableCell wikiCell2 = (WikiTableCell) row.getSubParts().get(1);
        Assert.assertEquals(1, wikiCell2.getSubParts().size());
        Assert.assertTrue(wikiCell2.getSubParts().get(0) instanceof PlainText);
        PlainText wikiCelleText = (PlainText) wikiCell2.getSubParts().get(0);
        Assert.assertEquals("second cell", wikiCelleText.getText().trim());

        WikiTableRow row2 = (WikiTableRow) parsedTable.getSubParts().get(1);

        Assert.assertTrue(row.getSubParts().get(0) instanceof WikiTableCell);
        Assert.assertTrue(row.getSubParts().get(1) instanceof WikiTableCell);

        WikiTableCell wikiCell3 = (WikiTableCell) row2.getSubParts().get(0);
        Assert.assertEquals(1, wikiCell3.getSubParts().size());
        Assert.assertTrue(wikiCell3.getSubParts().get(0) instanceof PlainText);
        PlainText wikiCell3Text = (PlainText) wikiCell3.getSubParts().get(0);
        Assert.assertEquals("third cell", wikiCell3Text.getText().trim());

        WikiTableCell wikiCell4 = (WikiTableCell) row2.getSubParts().get(1);
        Assert.assertEquals(1, wikiCell4.getSubParts().size());
        Assert.assertTrue(wikiCell4.getSubParts().get(0) instanceof PlainText);
        PlainText wikiCell4Text = (PlainText) wikiCell4.getSubParts().get(0);
        Assert.assertEquals("fourth cell", wikiCell4Text.getText().trim());
}
    
    @Test
    public void testParseTableWithTwoRowsAndTwoCellsOnSameLine() {
        String wikiText = "{|"
            + "\n|-"
            + "\n| first cell || second cell"
            + "\n|-"
            + "\n| third cell || fourth cell"
            + "\n|}";

        WikiTableParser parser = new WikiTableParser();
        ParseResult<WikiTable> parseResult = parser.parse(wikiText);
        WikiTable parsedTable = parseResult.getIdentifiedPart();
        Assert.assertNotNull(parsedTable.getSubParts());

        Assert.assertEquals(2, parsedTable.getSubParts().size());
        Assert.assertTrue(parsedTable.getSubParts().get(0) instanceof WikiTableRow);
        WikiTableRow row1 = (WikiTableRow) parsedTable.getSubParts().get(0);

        Assert.assertEquals(2, row1.getSubParts().size());
        Assert.assertTrue(row1.getSubParts().get(0) instanceof WikiTableCell);
        Assert.assertTrue(row1.getSubParts().get(1) instanceof WikiTableCell);

        WikiTableCell wikiCell1 = (WikiTableCell) row1.getSubParts().get(0);
        Assert.assertEquals(1, wikiCell1.getSubParts().size());
        Assert.assertTrue(wikiCell1.getSubParts().get(0) instanceof PlainText);
        PlainText wikiCell1Text = (PlainText) wikiCell1.getSubParts().get(0);
        Assert.assertEquals("first cell", wikiCell1Text.getText().trim());

        WikiTableCell wikiCell2 = (WikiTableCell) row1.getSubParts().get(1);
        Assert.assertEquals(1, wikiCell2.getSubParts().size());
        Assert.assertTrue(wikiCell2.getSubParts().get(0) instanceof PlainText);
        PlainText wikiCell2Text = (PlainText) wikiCell2.getSubParts().get(0);
        Assert.assertEquals("second cell", wikiCell2Text.getText().trim());

        WikiTableRow row2 = (WikiTableRow) parsedTable.getSubParts().get(1);
        Assert.assertEquals(2, row2.getSubParts().size());
        Assert.assertTrue(row2.getSubParts().get(0) instanceof WikiTableCell);
        Assert.assertTrue(row2.getSubParts().get(1) instanceof WikiTableCell);

        WikiTableCell wikiCell3 = (WikiTableCell) row2.getSubParts().get(0);
        Assert.assertEquals(1, wikiCell3.getSubParts().size());
        Assert.assertTrue(wikiCell3.getSubParts().get(0) instanceof PlainText);
        PlainText wikiCell3Text = (PlainText) wikiCell3.getSubParts().get(0);
        Assert.assertEquals("third cell", wikiCell3Text.getText().trim());

        WikiTableCell wikiCell4 = (WikiTableCell) row2.getSubParts().get(1);
        Assert.assertEquals(1, wikiCell4.getSubParts().size());
        Assert.assertTrue(wikiCell4.getSubParts().get(0) instanceof PlainText);
        PlainText wikiCell4Text = (PlainText) wikiCell4.getSubParts().get(0);
        Assert.assertEquals("fourth cell", wikiCell4Text.getText().trim());
}
}
