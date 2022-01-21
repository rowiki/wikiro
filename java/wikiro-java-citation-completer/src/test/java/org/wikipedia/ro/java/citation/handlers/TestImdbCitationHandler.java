package org.wikipedia.ro.java.citation.handlers;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.wikipedia.ro.java.citation.clientapi.ImdbClientApi;

public class TestImdbCitationHandler
{
    @BeforeClass
    public static void beforeClass()
    {
        System.setProperty("WIKI_IMDB_RAPID_API_KEY", "bogusapikey");
    }

    @Test
    public void testGetTitleFromImdb() throws IOException
    {
        InputStream gotJsonStream = this.getClass().getResourceAsStream("/gameofthrones-imdb-title.json");
        ImdbClientApi mockApi = mock(ImdbClientApi.class);
        doReturn(Optional.of(new String(gotJsonStream.readAllBytes(), StandardCharsets.UTF_8))).when(mockApi).getTitle(anyString());

        IMDbCitationHandler sut = new IMDbCitationHandler();
        sut.setImdbApi(mockApi);
        Optional<String> actualTitle = sut.getTitleFromImdb("aString");

        Assert.assertTrue("We should have found a title", actualTitle.isPresent());
        Assert.assertEquals("Game of Thrones title should have been found", "Game of Thrones", actualTitle.get());
    }

    @Test
    public void testGetNameFromImdb() throws IOException
    {
        InputStream gotJsonStream = this.getClass().getResourceAsStream("/rhys-meyers-actor.json");
        ImdbClientApi mockApi = mock(ImdbClientApi.class);
        doReturn(Optional.of(new String(gotJsonStream.readAllBytes(), StandardCharsets.UTF_8))).when(mockApi).getName(anyString());

        IMDbCitationHandler sut = new IMDbCitationHandler();
        sut.setImdbApi(mockApi);
        Optional<String> actualName = sut.getNameFromImdb("aString");

        Assert.assertTrue("We should have found a name", actualName.isPresent());
        Assert.assertEquals("Jonathan Rhys Meyers name should have been found", "Jonathan Rhys Meyers", actualName.get());
    }

    @Test
    public void testProcessImdbCitationParamsWithTitle()
    {
        IMDbCitationHandler sut = mock(IMDbCitationHandler.class);
        when(sut.processCitationParams(anyString())).thenCallRealMethod();
        when(sut.getTitleFromImdb(anyString())).thenReturn(Optional.of("Gigi Bobo"));

        Optional<String> actualResult = sut.processCitationParams("https://www.imdb.com/title/tt0121955");

        Assert.assertTrue("ImdbCitationHandler should have a result", actualResult.isPresent());
        Assert.assertEquals("Unexpected assembled citation", "{{Titlu IMDb|id=0121955|titlu=Gigi Bobo}}", actualResult.get());
    }

    @Test
    public void testProcessImdbCitationParamsWithName()
    {
        IMDbCitationHandler sut = mock(IMDbCitationHandler.class);
        when(sut.processCitationParams(anyString())).thenCallRealMethod();
        when(sut.getNameFromImdb(anyString())).thenReturn(Optional.of("Gigi Bobo"));

        Optional<String> actualResult = sut.processCitationParams("https://www.imdb.com/name/nm0532235");

        Assert.assertTrue("ImdbCitationHandler should have a result", actualResult.isPresent());
        Assert.assertEquals("Unexpected assembled citation", "{{Nume IMDb|id=0532235|name=Gigi Bobo}}", actualResult.get());
    }

}
