package org.wikipedia.ro.java.citation.clientapi;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImdbClientApi
{
    private static final Logger LOG = LoggerFactory.getLogger(ImdbClientApi.class);
    
    public static String rapidApiKey;
    {
        rapidApiKey = System.getenv("WIKI_IMDB_RAPID_API_KEY");
        if (StringUtils.isEmpty(rapidApiKey)) {
            rapidApiKey = System.getProperty("WIKI_IMDB_RAPID_API_KEY");
        }
        if (StringUtils.isEmpty(rapidApiKey)) {
            throw new IllegalStateException("Could not initialize IMDb API. Missing rapid key, please set env variable WIKI_IMDB_RAPID_API_KEY");
        }
    }
    
    public Optional<String> getTitle(String id)
    {
        LOG.debug("Requesting title from imdb API");
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://imdb8.p.rapidapi.com/title/get-details?tconst=" + id))
            .header("x-rapidapi-host", "imdb8.p.rapidapi.com")
            .header("x-rapidapi-key", rapidApiKey)
            .method("GET", BodyPublishers.noBody())
            .build();
        HttpResponse<String> response;
        String responseJson = null;
        try
        {
            response = HttpClient.newHttpClient().send(request, BodyHandlers.ofString());
            responseJson = response.body();
            LOG.debug("Received response: {}", responseJson);
            return Optional.ofNullable(responseJson);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
        catch (IOException e)
        {
            LOG.error("Error getting title from IMDB", e);
        }
        return Optional.empty();
    }

    public Optional<String> getName(String id)
    {
        LOG.debug("Requesting name from imdb API for id={}", id);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://imdb8.p.rapidapi.com/actors/get-bio?nconst=" + id))
            .header("x-rapidapi-host", "imdb8.p.rapidapi.com")
            .header("x-rapidapi-key", rapidApiKey)
            .method("GET", BodyPublishers.noBody())
            .build();
        HttpResponse<String> response;
        try
        {
            response = HttpClient.newHttpClient().send(request, BodyHandlers.ofString());
            return Optional.ofNullable(response.body());
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
        catch (IOException e)
        {
            LOG.error("Error getting title from IMDB", e);
        }
        return Optional.empty();
    }

}
