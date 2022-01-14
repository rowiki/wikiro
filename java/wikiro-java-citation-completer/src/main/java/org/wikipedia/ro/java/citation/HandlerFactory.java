package org.wikipedia.ro.java.citation;

import org.wikipedia.ro.java.citation.handlers.DefaultCitationHandler;
import org.wikipedia.ro.java.citation.handlers.Handler;

public class HandlerFactory
{
    private HandlerFactory()
    {
    }

    public Handler getHandler(String url)
    {
        return new DefaultCitationHandler();
    }

    public static HandlerFactory createHandlerFactory()
    {
        return new HandlerFactory();
    }
}
