package org.wikipedia.ro.java.citation;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import static org.apache.commons.lang3.Strings.CS;
import static org.apache.commons.lang3.Strings.CI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikipedia.ro.java.citation.handlers.ConvertDateFromTimestampProcessingStep;
import org.wikipedia.ro.java.citation.handlers.DefaultCitationHandler;
import org.wikipedia.ro.java.citation.handlers.GoogleBooksHandler;
import org.wikipedia.ro.java.citation.handlers.Handler;
import org.wikipedia.ro.java.citation.handlers.IMDbCitationHandler;
import org.wikipedia.ro.java.citation.handlers.RemoveParamsProcessingStep;
import org.wikipedia.ro.java.citation.handlers.YoutubeHandler;

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

        URI srcURI = null;
        
        try {
            srcURI = new URI(url);
        }
        catch (URISyntaxException e)
        {
            LOG.info("Bad URI {}: {}", url, e.getMessage());
            return handlerList;
        }
        String host = srcURI.getHost();
        if (host != null) {
            // Only do host-based matching if we have a valid host
            if (GoogleBooksHandler.GOOGLE_BOOKS_PATTERN.matcher(host + srcURI.getPath()).matches()) {
                handlerList.add(new GoogleBooksHandler());
            }
            
            if (YoutubeHandler.YOUTUBE_PATTERN.matcher(host).find()) {
                handlerList.add(new YoutubeHandler());
            }
        }

        DefaultCitationHandler defaultCitationHandler = new DefaultCitationHandler();

        if (host != null) {
            if (CI.equals(host, "adevarul.ro")) {
                defaultCitationHandler.addProcessingStep(new ConvertDateFromTimestampProcessingStep());
                defaultCitationHandler.addProcessingStep(new RemoveParamsProcessingStep("language"));
            }
            if (CI.equals(host, "cinemagia.ro")) {
                defaultCitationHandler.addProcessingStep(new RemoveParamsProcessingStep("author1"));
            }
        }

        handlerList.add(defaultCitationHandler);
        LOG.info("Returning {} loggers, first is {}", handlerList.size(), handlerList.stream().findFirst().map(Object::getClass).map(Class::getName).orElse("none"));
        return handlerList;
    }

    public static HandlerFactory createHandlerFactory()
    {
        return new HandlerFactory();
    }
}
