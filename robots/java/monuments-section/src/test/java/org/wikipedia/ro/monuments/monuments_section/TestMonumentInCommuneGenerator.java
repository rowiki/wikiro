package org.wikipedia.ro.monuments.monuments_section;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import junit.framework.Assert;

public class TestMonumentInCommuneGenerator {

    private List<Document> emptyDocArrayList = new ArrayList<Document>();

    private MongoClient prepareMock(final List<Document> nationalMonuments, final List<Document> localMonuments) {
        MongoClient mockClient = mock(MongoClient.class);
        MongoDatabase mockDb = mock(MongoDatabase.class);
        MongoCollection<Document> mockCollection = (MongoCollection<Document>) mock(MongoCollection.class);

        when(mockClient.getDatabase("monumente")).thenReturn(mockDb);
        when(mockDb.getCollection("Monument")).thenReturn(mockCollection);

        FindIterable<Document> nationalFind = mock(FindIterable.class);
        FindIterable<Document> localFind = mock(FindIterable.class);

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Block<Document> block = invocation.getArgument(0);
                for (Document eachNatMon : nationalMonuments) {
                    block.apply(eachNatMon);
                }
                return null;
            }

        }).when(nationalFind).forEach(any(Block.class));
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Block<Document> block = invocation.getArgument(0);
                for (Document eachLocMon : localMonuments) {
                    block.apply(eachLocMon);
                }
                return null;
            }

        }).when(localFind).forEach(any(Block.class));

        when(mockCollection.find(any(Bson.class))).thenReturn(nationalFind).thenReturn(localFind);

        return mockClient;
    }

    private void assertExpectedSubstring(String text, String expectedString) {
        Assert.assertTrue("Text should contain string: \"" + expectedString + "\" but is \"" + text + "\"",
            text.contains(expectedString));
    }

    @Test
    public void testNoMonuments() {
        MonumentInCommuneGenerator generator = new MonumentInCommuneGenerator("BR", "cComuna");

        generator.setMongoClient(prepareMock(emptyDocArrayList, emptyDocArrayList));

        String text = generator.generate();
        Assert.assertEquals(text.length(), 0);
    }

    @Test
    public void testOneNatMonumentOnly() {
        MonumentInCommuneGenerator generator = new MonumentInCommuneGenerator("BR", "cCucu");

        List<Document> docs = new ArrayList<Document>();
        Document doc = new Document();
        doc.put("Denumire", "X");
        doc.put("Datare", "Y");
        doc.put("Cod", "BR-II-m-A-20000");
        doc.put("Localitate", "sat [[Braca, Brăila|Braca]]; comuna [[Comuna Cucu, Brăila|Cucu]]");
        docs.add(doc);
        generator.setMongoClient(prepareMock(docs, emptyDocArrayList));

        String text = generator.generate();
        Assert.assertTrue(text.length() > 0);
        assertExpectedSubstring(text,
            "În comuna Cucu se află monumentul istoric de arhitectură de interes național X datând din Y");

    }

    @Test
    public void testOneLocMonumentOnly() {
        MonumentInCommuneGenerator generator = new MonumentInCommuneGenerator("BR", "cCucu");

        List<Document> docs = new ArrayList<Document>();
        Document doc = new Document();
        doc.put("Denumire", "X");
        doc.put("Datare", "Dinioara I");
        doc.put("Cod", "BR-II-m-B-20000");
        doc.put("Localitate", "sat [[Braca, Brăila|Braca]]; comuna [[Comuna Cucu, Brăila|Cucu]]");
        docs.add(doc);
        generator.setMongoClient(prepareMock(emptyDocArrayList, docs));

        String text = generator.generate();
        Assert.assertTrue(text.length() > 0);
        String expectedString =
            "Un singur obiectiv din comună este inclus în [[lista monumentelor istorice din județul Brăila]] ca monument de interes local: monumentul istoric de arhitectură X datând din Dinioara I";
        assertExpectedSubstring(text, expectedString);

    }

    @Test
    public void testMoreLocMonumentsOfOneTypeOnly() {
        MonumentInCommuneGenerator generator = new MonumentInCommuneGenerator("BR", "cCucu");

        List<Document> docs = new ArrayList<Document>();
        Document doc = new Document();
        doc.put("Denumire", "X");
        doc.put("Datare", "Dinioara I");
        doc.put("Cod", "BR-II-m-B-20000");
        doc.put("Localitate", "sat [[Braca, Brăila|Braca]]; comuna [[Comuna Cucu, Brăila|Cucu]]");
        docs.add(doc);

        doc = new Document();
        doc.put("Denumire", "Y");
        doc.put("Datare", "Dinioara a II-a");
        doc.put("Cod", "BR-II-m-B-20001");
        doc.put("Localitate", "sat [[Braca, Brăila|Braca]]; comuna [[Comuna Cucu, Brăila|Cucu]]");
        docs.add(doc);

        doc = new Document();
        doc.put("Denumire", "Z");
        doc.put("Datare", "Dinioara a II-a");
        doc.put("Cod", "BR-II-m-B-20002");
        doc.put("Localitate", "sat [[Braca, Brăila|Braca]]; comuna [[Comuna Cucu, Brăila|Cucu]]");
        docs.add(doc);
        generator.setMongoClient(prepareMock(emptyDocArrayList, docs));

        String text = generator.generate();
        Assert.assertTrue(text.length() > 0);
        String expectedString =
            "Trei obiective din comună sunt incluse în [[lista monumentelor istorice din județul Brăila]] ca monumente de interes local, toate clasificate ca monumente istorice de arhitectură: X (Dinioara I), Y (Dinioara a II-a) și Z (Dinioara a II-a)";
        assertExpectedSubstring(text, expectedString);

    }

    @Test
    public void testMoreNatMonumentsOfOneTypeOnly() {
        MonumentInCommuneGenerator generator = new MonumentInCommuneGenerator("BR", "cCucu");

        List<Document> docs = new ArrayList<Document>();
        Document doc = new Document();
        doc.put("Denumire", "X");
        doc.put("Datare", "Dinioara I");
        doc.put("Cod", "BR-II-m-A-20000");
        doc.put("Localitate", "sat [[Braca, Brăila|Braca]]; comuna [[Comuna Cucu, Brăila|Cucu]]");
        docs.add(doc);

        doc = new Document();
        doc.put("Denumire", "Y");
        doc.put("Datare", "Dinioara a II-a");
        doc.put("Cod", "BR-II-m-A-20001");
        doc.put("Localitate", "sat [[Braca, Brăila|Braca]]; comuna [[Comuna Cucu, Brăila|Cucu]]");
        docs.add(doc);
        generator.setMongoClient(prepareMock(docs, emptyDocArrayList));

        String text = generator.generate();
        Assert.assertTrue(text.length() > 0);
        String expectedString =
            "În comuna Cucu se află două obiective clasificate ca monumente istorice de arhitectură de interes național: X (Dinioara I) și Y (Dinioara a II-a)";
        assertExpectedSubstring(text, expectedString);

    }

    @Test
    public void testOneLocalAndOneNationalMonument() {
        MonumentInCommuneGenerator generator = new MonumentInCommuneGenerator("BR", "cCucu");

        List<Document> docsNat = new ArrayList<Document>();
        List<Document> docsLoc = new ArrayList<Document>();
        Document doc = new Document();
        doc.put("Denumire", "X");
        doc.put("Datare", "Dinioara I");
        doc.put("Cod", "BR-II-m-A-20000");
        doc.put("Localitate", "sat [[Braca, Brăila|Braca]]; comuna [[Comuna Cucu, Brăila|Cucu]]");
        docsNat.add(doc);

        doc = new Document();
        doc.put("Denumire", "Y");
        doc.put("Datare", "Dinioara a II-a");
        doc.put("Cod", "BR-II-m-A-20001");
        doc.put("Localitate", "sat [[Braca, Brăila|Braca]]; comuna [[Comuna Cucu, Brăila|Cucu]]");
        docsLoc.add(doc);
        generator.setMongoClient(prepareMock(docsNat, docsLoc));

        String text = generator.generate();
        Assert.assertTrue(text.length() > 0);
        assertExpectedSubstring(text,
            "În comuna Cucu se află monumentul istoric de arhitectură de interes național X datând din Dinioara I");
        assertExpectedSubstring(text,
            "În rest, un singur obiectiv din comună este inclus în [[lista monumentelor istorice din județul Brăila]] ca monument de interes local: monumentul istoric de arhitectură Y datând din Dinioara a II-a");

    }
}
