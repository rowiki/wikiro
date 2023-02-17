package org.wikipedia.ro.java;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Assert;
import org.junit.Test;
import org.wikipedia.ro.java.citation.handlers.DefaultCitationHandler;

public class TestJsonLdCitation
{
    @Test
    public void testRealRfiCitationPage() throws IOException, URISyntaxException {
        URL rfiHtmlFileURL = getClass().getClassLoader().getResource("rfi1.html");
        Document document = Jsoup.parse(new File(rfiHtmlFileURL.toURI()), StandardCharsets.UTF_8.toString());
        
        Map<String, String> params = new HashMap<>();
        new DefaultCitationHandler().populateMapFromJsonLd(document, params);
        
        Assert.assertEquals("Europa Plus: Cum i-a prins guvernul cu m\u00e2\u021ba \u00een sac pe produc\u0103torii albanezi de lactate BIO", params.get("title"));
        Assert.assertEquals("Ana Maria Florea-Harrison", params.get("author1"));
        Assert.assertEquals("2021-11-15", params.get("date"));
    }

    @Test
    public void testRealGspCitationPage() throws IOException, URISyntaxException {
        URL rfiHtmlFileURL = getClass().getClassLoader().getResource("gsp1.html");
        Document document = Jsoup.parse(new File(rfiHtmlFileURL.toURI()), StandardCharsets.UTF_8.toString());
        
        Map<String, String> params = new HashMap<>();
        new DefaultCitationHandler().populateMapFromJsonLd(document, params);
        
        Assert.assertEquals("Singurul român din F1: \"Sezonul viitor vom fi decisivi\"", params.get("title"));
        Assert.assertEquals("2010-12-20", params.get("date"));
    }
    
    @Test
    public void testRealHotnewsCitationPage() throws IOException, URISyntaxException {
        URL htnHtmlFileURL = getClass().getClassLoader().getResource("hotnews.html");
        Document document = Jsoup.parse(new File(htnHtmlFileURL.toURI()), StandardCharsets.UTF_8.toString());
        
        Map<String, String> params = new HashMap<>();
        new DefaultCitationHandler().populateMapFromJsonLd(document, params);
        
        Assert.assertEquals("HARTĂ INTERACTIVĂ Autostrada spre sudul Litoralului. Pe unde va trece „Alternativa Techirghiol”, noul drum care va ocoli aglomerația din Eforie / Nod spectaculos cu A4 și A2", params.get("title"));
        Assert.assertEquals("2022-09-03", params.get("date"));
        Assert.assertEquals("Victor Cozmei", params.get("author1"));
        Assert.assertEquals("HotNews.ro", params.get("publisher"));
    }
    
    @Test
    public void testRealAdevarulCitationPage() throws IOException, URISyntaxException {
        URL advHtmlFileURL = getClass().getClassLoader().getResource("adevarul.html");
        Document document = Jsoup.parse(new File(advHtmlFileURL.toURI()), StandardCharsets.UTF_8.toString());
        
        Map<String, String> params = new HashMap<>();
        new DefaultCitationHandler().populateMapFromJsonLd(document, params);
        
        Assert.assertEquals("Cum a ajuns geniul comediei românești să se numească Birlic. Ultima dorință înainte de a se stinge ...", params.get("title"));
        Assert.assertEquals("2022-08-21", params.get("date"));
        Assert.assertEquals("Ionela Stănilă", params.get("author1"));
        Assert.assertEquals("Adevărul", params.get("publisher"));
    }
   
}
