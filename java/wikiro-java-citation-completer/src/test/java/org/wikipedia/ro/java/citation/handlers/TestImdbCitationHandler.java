package org.wikipedia.ro.java.citation.handlers;

import org.junit.Assert;
import org.junit.Test;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.wikipedia.ro.java.citation.clientapi.ImdbClientApi;

public class TestImdbCitationHandler
{
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
}
