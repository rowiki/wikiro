package org.wikipedia.ro.java.citation;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.wikipedia.ro.java.citation.handlers.DefaultCitationHandler;
import org.wikipedia.ro.java.citation.handlers.Handler;
import org.wikipedia.ro.java.citation.handlers.IMDbCitationHandler;

public class TestHandlerFactory
{
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
}
