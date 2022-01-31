package org.wikipedia.ro.java.citation.handlers;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

public class TestGoogleBooksHandler
{

    @Test
    public void testGetGoogleBooksTitle()
    {
        GoogleBooksHandler sut = new GoogleBooksHandler();
        Optional<String> citation = sut.processCitationParams("https://books.google.ro/books?id=OfywzQEACAAJ&hl=ro&sa=X&redir_esc=y");
        
        Assert.assertTrue("Citation should be present", citation.isPresent());
    }
}
