package org.wikipedia.ro.java.citation;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikipedia.ro.java.citation.handlers.DefaultCitationHandler;
import org.wikipedia.ro.java.citation.handlers.GoogleBooksHandler;
import org.wikipedia.ro.java.citation.handlers.Handler;
import org.wikipedia.ro.java.citation.handlers.IMDbCitationHandler;

public class HandlerFactory
{
    private static final Logger LOG = LoggerFactory.getLogger(HandlerFactory.class);

    private HandlerFactory()
    {
    }

    public List<Handler> getHandlers(String url)
    {
        LOG.info("Searching loggers for URL {}", url);
        List<Handler> handlerList = new ArrayList<>();
        Matcher imdbMatcher = IMDbCitationHandler.IMDB_PATTERN.matcher(url);
        if (imdbMatcher.find())
        {
            handlerList.add(new IMDbCitationHandler());
        }
        
        if (GoogleBooksHandler.GOOGLE_BOOKS_PATTERN.matcher(URI.create(url).getHost()).matches()) {
            handlerList.add(new GoogleBooksHandler());
        }

        handlerList.add(new DefaultCitationHandler());
        LOG.info("Returning {} loggers, first is {}", handlerList.size(), handlerList.stream().findFirst().map(Object::getClass).map(Class::getName).orElse("none"));
        return handlerList;
    }

    public static HandlerFactory createHandlerFactory()
    {
        return new HandlerFactory();
    }
}
