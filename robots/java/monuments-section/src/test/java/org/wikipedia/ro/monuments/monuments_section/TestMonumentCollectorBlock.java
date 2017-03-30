package org.wikipedia.ro.monuments.monuments_section;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.junit.Test;
import org.wikipedia.ro.monuments.monuments_section.data.Monument;

import junit.framework.Assert;

public class TestMonumentCollectorBlock {
    private static final String MONUMENT_NAME = "Nume monument";
    private static final String MONUMENT_DATING = "Dinioara a II-a";
    @Test
    public void testCollectDocumentWithArchaeologicalSite() {
        List<Monument> collectedMonuments = new ArrayList<Monument>();
        
        Document inDoc = new Document();
        inDoc.put("Cod", "AR-I-s-A-21245.09");
        inDoc.put("Denumire", MONUMENT_NAME);
        inDoc.put("Datare", MONUMENT_DATING);
        inDoc.put("Localitate", "sat [[Berindia, Arad|Berindia]]; comuna [[Comuna Buteni, Arad|Buteni]]");
        
        MonumentCollectorBlock mcb = new MonumentCollectorBlock(collectedMonuments);
        mcb.apply(inDoc);
        
        Assert.assertEquals("There should now be 1 monument in the list", 1, collectedMonuments.size());
        Monument collectedMonument = collectedMonuments.get(0);
        Assert.assertEquals("The monument name should have been imported", collectedMonument.name, MONUMENT_NAME);
        Assert.assertEquals("The monument dating should have been imported", collectedMonument.dating, MONUMENT_DATING);
        Assert.assertEquals("The monument code should have been imported", collectedMonument.code, "AR-I-s-A-21245.09");
        Assert.assertEquals("The monument type should have been identified", collectedMonument.type, 1);
        Assert.assertEquals("The monument code number should have been identified", collectedMonument.codeNumber, "21245");
        Assert.assertEquals("The monument supplemental code number should have been identified", collectedMonument.supplementalCodeNumber, "09");
        Assert.assertEquals("The monument village name should have been identified", collectedMonument.settlement, "Berindia");
    }
}
