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
        Assert.assertTrue(
            text.contains("În comuna Cucu se află monumentul istoric de arhitectură de interes național X datând din Y"));

    }

    @Test
    public void testOneLocMonumentOnly() {
        MonumentInCommuneGenerator generator = new MonumentInCommuneGenerator("BR", "cCucu");

        List<Document> docs = new ArrayList<Document>();
        Document doc = new Document();
        doc.put("Denumire", "X");
        doc.put("Datare", "Y");
        doc.put("Cod", "BR-II-m-B-20000");
        doc.put("Localitate", "sat [[Braca, Brăila|Braca]]; comuna [[Comuna Cucu, Brăila|Cucu]]");
        docs.add(doc);
        generator.setMongoClient(prepareMock(emptyDocArrayList, docs));

        String text = generator.generate();
        Assert.assertTrue(text.length() > 0);
        String expectedString =
            "Un singur obiectiv din comună este inclus în [[lista monumentelor istorice din județul Brăila]]: monumentul istoric de arhitectură de interes local X datând din Y";
        Assert.assertTrue("Text should contain string: \"" + expectedString + "\" but is \"" + text + "\"",
            text.contains(expectedString));

    }

}
