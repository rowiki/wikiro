package org.wikipedia.ro.java.citation;

import java.net.URI;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wikipedia.ro.java.citation.handlers.DefaultCitationHandler;
import org.wikipedia.ro.java.citation.handlers.GoogleBooksHandler;
import org.wikipedia.ro.java.citation.handlers.Handler;
import org.wikipedia.ro.java.citation.handlers.IMDbCitationHandler;

public class TestHandlerFactory
{
    @BeforeClass
    public static void beforeClass()
    {
        System.setProperty("WIKI_IMDB_RAPID_API_KEY", "bogusapikey");
    }

    @Test
    public void testRandomCitation()
    {
        HandlerFactory sut = HandlerFactory.createHandlerFactory();

        List<Handler> actualHandlers = sut.getHandlers("https://www.rfi.ro/special-paris-141689-emmanuel-macron-la-parlamentul-de-la-strasbourg-este-necesara-o-noua-ordine");

        Assert.assertEquals("There should be one handler", 1, actualHandlers.size());
        Assert.assertTrue("The handler should be default", actualHandlers.get(0) instanceof DefaultCitationHandler);
    }

    @Test
    public void testImdbNameCitation()
    {
        HandlerFactory sut = HandlerFactory.createHandlerFactory();

        List<Handler> actualHandlers = sut.getHandlers("https://www.imdb.com/name/nm0532235/?ref_=nv_sr_srsg_1");

        Assert.assertEquals("There should be two handlers", 2, actualHandlers.size());
        Assert.assertTrue("The Imdb handler should be first", actualHandlers.get(0) instanceof IMDbCitationHandler);
    }
    
    @Test
    public void testGoogleBooksCitation()
    {
        HandlerFactory sut = HandlerFactory.createHandlerFactory();
        
        String gbUrl = "https://books.google.ro/books?id=OfywzQEACAAJ&hl=ro&sa=X&redir_esc=y";
        List<Handler> actualHandlers = sut.getHandlers(gbUrl);

        Assert.assertEquals("There should be two handlers", 2, actualHandlers.size());
        Assert.assertTrue("The Google Books handler should be first", actualHandlers.get(0) instanceof GoogleBooksHandler);

        Assert.assertEquals("Expected to find id from URL", "OfywzQEACAAJ", new GoogleBooksHandler().findBookId(URI.create(gbUrl)).orElse(null));
    }
    
    @Test
    public void testGoogleBooksCoUkCitation()
    {
        HandlerFactory sut = HandlerFactory.createHandlerFactory();
        
        String gbUrl = "https://www.google.co.uk/books/edition/2006/mlugG4vfmw8C?hl=en&gbpv=1&dq=ionel+pantea+1941&pg=PA346&printsec=frontcover";
        List<Handler> actualHandlers = sut.getHandlers(gbUrl);

        Assert.assertEquals("There should be two handlers", 2, actualHandlers.size());
        Assert.assertTrue("The Google Books handler should be first", actualHandlers.get(0) instanceof GoogleBooksHandler);
        
        Assert.assertEquals("Expected to find id from URL", "mlugG4vfmw8C", new GoogleBooksHandler().findBookId(URI.create(gbUrl)).orElse(null));
    }
    
    @Test
    public void testUrlWithUnderscoreInHostname()
    {
        HandlerFactory sut = HandlerFactory.createHandlerFactory();
        
        // URL with underscore in hostname is technically invalid per URI spec,
        // but URI constructor succeeds and getHost() returns null.
        // The handler factory should handle this gracefully and return a default handler.
        String urlWithUnderscore = "https://forza_azzurri.homestead.com/clubs_prof_j.html";
        List<Handler> actualHandlers = sut.getHandlers(urlWithUnderscore);

        Assert.assertNotNull("Handler list should not be null", actualHandlers);
        Assert.assertEquals("Should return at least one handler (default)", 1, actualHandlers.size());
        Assert.assertTrue("The handler should be default", actualHandlers.get(0) instanceof DefaultCitationHandler);
    }
}
