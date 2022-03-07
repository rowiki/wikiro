package org.wikipedia.ro.java.citation.handlers;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.books.Books;
import com.google.api.services.books.BooksRequestInitializer;
import com.google.api.services.books.model.Volume;
import com.google.api.services.books.model.Volume.VolumeInfo;

public class GoogleBooksHandler implements Handler
{
    private static final Logger LOG = LoggerFactory.getLogger(GoogleBooksHandler.class);
    public static final Pattern GOOGLE_BOOKS_PATTERN = Pattern.compile("(?:www|books)\\.google\\.[\\w\\.]+/books.*");

    @Override
    public Optional<String> processCitationParams(String url)
    {
        try
        {
            URI gbUri = URI.create(url);
            if (!GOOGLE_BOOKS_PATTERN.matcher(gbUri.getHost() + gbUri.getPath()).matches())
            {
                return Optional.empty();
            }
            List<NameValuePair> urlParams = URLEncodedUtils.parse(gbUri, StandardCharsets.UTF_8);

            Optional<String> idParam = findBookId(gbUri);
            
            if (idParam.isEmpty())
            {
                return Optional.empty();
            }

            Books books = new Books.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), null)
                .setApplicationName("wikipedia-citations-completer").setGoogleClientRequestInitializer(new BooksRequestInitializer()).build();
            Volume foundVolume = books.volumes().get(idParam.get()).execute();
            if (null == foundVolume)
            {
                return Optional.empty();
            }

            VolumeInfo volumeInfo = foundVolume.getVolumeInfo();

            Map<String, String> citationParams = new HashMap<>();
            citationParams.put("title", Stream.of(volumeInfo.getTitle(), volumeInfo.getSubtitle()).filter(Objects::nonNull).collect(Collectors.joining(": ")));
            citationParams.put("language", volumeInfo.getLanguage());
            if (!Optional.ofNullable(volumeInfo.getAuthors()).map(Collection::isEmpty).orElse(true))
            {
                for (int idx = 0; idx < volumeInfo.getAuthors().size(); idx++)
                {
                    String crtAuthor = volumeInfo.getAuthors().get(idx);
                    citationParams.put(String.format("author%d", 1 + idx), crtAuthor);
                }
            }
            citationParams.put("data", volumeInfo.getPublishedDate());
            citationParams.put("publisher", volumeInfo.getPublisher());

            if (citationParams.entrySet().stream().anyMatch(e -> StringUtils.isNotBlank(e.getValue())))
            {
                Map<String, String> gbooksParams = new HashMap<>();
                gbooksParams.put("id", idParam.get());
                gbooksParams.put("plain-url", "yes");
                Optional<String> pgString = urlParams.stream().filter(u -> "pg".equalsIgnoreCase(u.getName())).findFirst().map(NameValuePair::getValue);
                if (pgString.isPresent())
                {
                    if (StringUtils.startsWith(pgString.get(), "PA"))
                    {
                        gbooksParams.put("page", StringUtils.removeStart(pgString.get(), "PA"));
                    }
                    else
                    {
                        gbooksParams.put("pg", pgString.get());
                    }
                }
                citationParams.put("url", String.format("{{Google books|%s}}",
                    gbooksParams.entrySet().stream().map(e -> String.join("=", e.getKey(), e.getValue())).collect(Collectors.joining("|"))));

                return Optional.of(String.format("{{Citation|%s}}",
                        citationParams.entrySet().stream().filter(e -> StringUtils.isNotBlank(e.getValue())).map(e -> String.join("=", e.getKey(), e.getValue())).collect(Collectors.joining("|"))
                    ));
            }
        }
        catch (GeneralSecurityException | IOException e)
        {
            LOG.error("Error filling in Google Books reference", e);
        }
        return Optional.empty();
    }

    public Optional<String> findBookId(URI gbUri)
    {
        List<NameValuePair> urlParams = URLEncodedUtils.parse(gbUri, StandardCharsets.UTF_8);
        Optional<String> idParam = urlParams.stream().filter(u -> "id".equalsIgnoreCase(u.getName())).findFirst().map(NameValuePair::getValue);

        if (idParam.isEmpty())
        {
            idParam = Optional.ofNullable(Paths.get(gbUri.getPath()).getFileName().toString());
        }
        return idParam;
    }

}
