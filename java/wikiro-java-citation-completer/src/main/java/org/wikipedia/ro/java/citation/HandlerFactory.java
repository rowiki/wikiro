package org.wikipedia.ro.java.citation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import org.wikipedia.ro.java.citation.handlers.DefaultCitationHandler;
import org.wikipedia.ro.java.citation.handlers.Handler;
import org.wikipedia.ro.java.citation.handlers.IMDbCitationHandler;

public class HandlerFactory
{
    private HandlerFactory()
    {
    }

    public List<Handler> getHandlers(String url)
    {
        List<Handler> handlerList = new ArrayList<>();
        Matcher imdbMatcher = IMDbCitationHandler.IMDB_PATTERN.matcher(url);
        if (imdbMatcher.find())
        {
            handlerList.add(new IMDbCitationHandler());
        }

        handlerList.add(new DefaultCitationHandler());
        return handlerList;
    }

    public static HandlerFactory createHandlerFactory()
    {
        return new HandlerFactory();
    }
}
