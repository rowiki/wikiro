package org.wikipedia.ro.java.citation.handlers;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikipedia.ro.java.citation.clientapi.ImdbClientApi;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

public class IMDbCitationHandler implements Handler
{
    public static final Logger LOG = LoggerFactory.getLogger(IMDbCitationHandler.class);

    public static final Pattern IMDB_PATTERN = Pattern
        .compile("^https?:\\/\\/(?:(?:www|m)\\.)?imdb\\.com\\/(?:(?:search\\/)?title(?:\\?companies=|\\/)|name\\/|event\\/|news\\/|company\\/)(\\w{2}\\d+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern TITLE_ID_PATTERN = Pattern.compile("tt(\\d{7,8})|(?:/characters\\/nm\\d{7,8})?");
    private static final Pattern NAME_ID_PATTERN = Pattern.compile("nm(\\d{7,8})");
    //private static final Pattern COMPANY_ID_PATTERN = Pattern.compile("co\\d{7}");

    public static String rapidApiKey = System.getenv("WIKI_IMDB_RAPID_API_KEY");
    
    private ImdbClientApi imdbApi = new ImdbClientApi();

    @Override
    public Optional<String> processCitationParams(String url)
    {
        URI citationURI = URI.create(url);
        String baseURI = String.format("%s://%s%s", citationURI.getScheme(), citationURI.getAuthority(), StringUtils.defaultString(citationURI.getPath()));
        Matcher baseURIMatch = IMDB_PATTERN.matcher(baseURI);
        if (!baseURIMatch.find())
        {
            return Optional.empty();
        }
        String imdbId = baseURIMatch.group(1);
        if (StringUtils.isEmpty(imdbId))
        {
            return Optional.empty();
        }

        Matcher titleMatcher = TITLE_ID_PATTERN.matcher(imdbId);
        if (titleMatcher.matches())
        {
            Optional<String> title = getTitleFromImdb(imdbId);
            if (title.isPresent())
            {
                return Optional.of(String.format("{{Titlu IMDb|id=%s|titlu=%s}}", titleMatcher.group(1), title.get()));
            }
        }
        else
        {
            Matcher nameMatcher = NAME_ID_PATTERN.matcher(imdbId);
            if (nameMatcher.matches())
            {
                Optional<String> name = getNameFromImdb(imdbId);
                if (name.isPresent())
                {
                    return Optional.of(String.format("{{Nume IMDb|id=%s|name=%s}}", nameMatcher.group(1), name.get()));
                }
            }
        }

        return Optional.empty();
    }

    Optional<String> getTitleFromImdb(String id)
    {
        Optional<String> responseJson = imdbApi.getTitle(id);
        if (responseJson.isEmpty())
        {
            return Optional.empty();
        }
        Gson gson = new Gson();
        JsonElement imdbTitleElement = gson.fromJson(responseJson.get(), JsonElement.class);
        return Optional.ofNullable(imdbTitleElement)
            .map(JsonElement::getAsJsonObject)
            .map(o -> o.get("title"))
            .filter(Objects::nonNull)
            .filter(JsonElement::isJsonPrimitive)
            .map(JsonElement::getAsString);
    }

    Optional<String> getNameFromImdb(String id)
    {
        Optional<String> responseJson = imdbApi.getName(id);
        if (responseJson.isEmpty())
        {
            return Optional.empty();
        }
        Gson gson = new Gson();
        JsonElement imdbTitleElement = gson.fromJson(responseJson.get(), JsonElement.class);
        return Optional.ofNullable(imdbTitleElement)
            .map(JsonElement::getAsJsonObject)
            .map(o -> o.get("name"))
            .filter(Objects::nonNull)
            .filter(Predicate.not(JsonElement::isJsonNull))
            .filter(JsonElement::isJsonPrimitive)
            .map(JsonElement::getAsString);
    }

    public void setImdbApi(ImdbClientApi imdbApi)
    {
        this.imdbApi = imdbApi;
    }

}
