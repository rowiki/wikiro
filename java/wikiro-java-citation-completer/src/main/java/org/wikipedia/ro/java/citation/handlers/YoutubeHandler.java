package org.wikipedia.ro.java.citation.handlers;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeRequestInitializer;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;

public class YoutubeHandler implements Handler
{
    public static final Pattern YOUTUBE_PATTERN = Pattern.compile("youtu(\\.be|be.\\w+)");

    private static final Logger LOG = LoggerFactory.getLogger(YoutubeHandler.class);
    
    private static final String API_KEY = StringUtils.defaultString(System.getenv("YOUTUBE_API_KEY"), System.getProperty("YOUTUBE_API_KEY")); 

    @Override
    public Optional<String> processCitationParams(String url)
    {
        try
        {
            URI ytUri = URI.create(url);
            Matcher ytUriMatcher = YOUTUBE_PATTERN.matcher(ytUri.getHost());
            if (!ytUriMatcher.find())
            {
                return Optional.empty();
            }
            List<NameValuePair> ytUriParams = URLEncodedUtils.parse(ytUri, StandardCharsets.UTF_8);
            String uriPath = ytUri.getPath();
            if (StringUtils.startsWith(uriPath, "/watch"))
            {
                Optional<String> videoId = ytUriParams.stream().filter(p -> "v".equalsIgnoreCase(p.getName())).findFirst().map(NameValuePair::getValue);
                if (videoId.isEmpty())
                {
                    return Optional.empty();
                }
                Optional<String> timestamp = ytUriParams.stream().filter(p -> "t".equalsIgnoreCase(p.getName())).findFirst().map(NameValuePair::getValue);

                YouTube yt = new YouTube.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), null)
                    .setApplicationName("wikipedia-citations-completer").setGoogleClientRequestInitializer(new YouTubeRequestInitializer(API_KEY))
                    .build();

                VideoListResponse ytVideoList = yt.videos().list("snippet").setId(videoId.get()).setKey(API_KEY).execute();
                if (ytVideoList.isEmpty())
                {
                    return Optional.empty();
                }
                Video video = ytVideoList.getItems().stream().findFirst().get();

                String title = video.getSnippet().getTitle();

                Map<String, String> citationParams = new HashMap<String, String>();
                citationParams.put("title", title);
                citationParams.put("id", videoId.get());
                if (timestamp.isPresent())
                {
                    citationParams.put("t", timestamp.get());
                }
                return Optional.of(String.format("{{YouTube|%s}}", citationParams.entrySet().stream().filter(e -> StringUtils.isNotBlank(e.getValue()))
                    .map(e -> String.format("%s=%s", e.getKey(), e.getValue())).collect(Collectors.joining("|"))));
            }
        }
        catch (GeneralSecurityException e)
        {
            LOG.error("Could not get youtube information for URL {}", url, e);
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        finally
        {

        }
        return Optional.empty();
    }

}
