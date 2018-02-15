package org.wikipedia.ro.parser;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.wikipedia.ro.model.PartContext;
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
        String wikiText = "{|" + "\n|-" + "\n| first cell || second cell" + "\n|}";

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
        String wikiText =
            "{|" + "\n|-" + "\n| first cell" + "\n| second cell" + "\n|-" + "\n| third cell" + "\n| fourth cell" + "\n|}";

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
        String wikiText =
            "{|" + "\n|-" + "\n| first cell || second cell" + "\n|-" + "\n| third cell || fourth cell" + "\n|}";

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

    @Test
    public void testWikiTableFromActualArticleMaroon5() {
        String tableText = "{| class=\"wikitable\"\n" + "!align=\"center\"|An\n" + "!align=\"center\"|Single\n"
            + "!align=\"center\"|Album\n" + "|-\n" + "|align=\"center\" rowspan=\"1\"|2003\n"
            + "|align=\"center\"|„Harder to Breathe”\n" + "|align=\"center\" rowspan=\"5\"|''Songs About Jane''\n" + "|-\n"
            + "|align=\"center\" rowspan=\"3\"|2004\n" + "|align=\"center\"|„This Love” \n" + "|-\n"
            + "|align=\"center\"|„She Will Be Loved”\n" + "|-\n" + "|align=\"center\"|„Sunday Morning”\n" + "|-\n"
            + "|align=\"center\"|2005\n" + "|align=\"center\"|„Must Get Out”\n" + "|-\n"
            + "|align=\"center\" rowspan=\"4\"|2007\n" + "|align=\"center\"|„Makes Me Wonder”\n"
            + "|align=\"center\" rowspan=\"3\"|''It Won't Be Soon Before Long''\n" + "|-\n"
            + "|align=\"center\"|„Wake Up Call”\n" + "|-\n" + "|align=\"center\"|„Won't Go Home without You”\n" + "|-\n"
            + "|align=\"center\"|„Happy Xmas (War is Over)”\n"
            + "|align=\"center\" rowspan=\"1\"|''Happy Xmas (War is Over)''\n" + "|-\n"
            + "|align=\"center\" rowspan=\"1\"|2008\n" + "|align=\"center\"|„Goodnight Goodnight”\n"
            + "|align=\"center\" rowspan=\"1\"|''It Won't Be Soon Before Long''\n" + "|-\n" + "|}";

        WikiTableParser parser = new WikiTableParser();
        ParseResult<WikiTable> parseResult = parser.parse(tableText);
        WikiTable parsedTable = parseResult.getIdentifiedPart();
        Assert.assertNotNull(parsedTable.getSubParts());

        Assert.assertEquals(12, parsedTable.getSubParts().size());
        Assert.assertTrue(parsedTable.getSubParts().get(0) instanceof WikiTableRow);
        WikiTableRow row1 = (WikiTableRow) parsedTable.getSubParts().get(0);

    }

    @Test
    public void testWikiTableFromActualArticleEminescuBust() {
        String tableText = "{| class=\"wikitable\" border=\"1\"\n" + "| <center>ÎN AMINTIREA<br />\n"
            + "POETULUI MĂRII, <br />\n" + "<big>MIHAI EMINESCU</big><br />\n" + "15 ianuarie 1850 – 15 iunie 1889<br />\n"
            + "FĂURITORUL LIMBII ȘI<br />\n" + "CONȘTIINȚEI<br />\n" + "NAȚIONALE ROMÂNEȘTI, <br />\n"
            + "EVOCATOR AL PĂMÂNTULUI<br />\n" + "DOBROGEAN<br />\n" + "LA 160 DE ANI DE LA NAȘTERE<br />\n"
            + "15 IANUARIE 2010<br />\n" + "CONSILIUL JUDEȚEAN CONSTANȚA<br />\n" + "LIGA CULTURALĂ PENTRU UNITATEA<br />\n"
            + "ROMÂNILOR DE PRETUTINDENI</center>\n" + "|}";

        WikiTableParser parser = new WikiTableParser();
        ParseResult<WikiTable> parseResult = parser.parse(tableText);
        WikiTable parsedTable = parseResult.getIdentifiedPart();
        Assert.assertNotNull(parsedTable.getSubParts());

        Assert.assertEquals(1, parsedTable.getSubParts().size());
        Assert.assertTrue(parsedTable.getSubParts().get(0) instanceof WikiTableRow);
        WikiTableRow row1 = (WikiTableRow) parsedTable.getSubParts().get(0);

        Assert.assertEquals(1, row1.getSubParts().size());
        Assert.assertTrue(row1.getSubParts().get(0) instanceof WikiTableCell);
        WikiTableCell cell1 = (WikiTableCell) row1.getSubParts().get(0);
        List<PartContext> eminescuSearchRes =
            cell1.search(part -> part instanceof PlainText && ((PlainText) part).getText().contains("EMINESCU"));
        Assert.assertEquals(1, eminescuSearchRes.size());
    }
}
