package org.wikipedia.ro.java.citation.handlers;

import static org.wikipedia.ro.java.citation.Utils.encodeURIComponent;
import static org.wikipedia.ro.java.citation.Utils.extractDate;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikipedia.ro.java.citation.SchemaorgUtils;
import org.wikipedia.ro.java.citation.data.Creator;
import org.wikipedia.ro.java.citation.data.Zotero;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DefaultCitationHandler implements Handler
{
    private static final Logger LOG = LoggerFactory.getLogger(DefaultCitationHandler.class);
    
    private static final Pattern LOCALE_PATTERN = Pattern.compile("([^_\\-]+)(?:[_\\-](\\w+))?");
    
    private Set<ProcessingStep> additionalProcessingSteps = new LinkedHashSet<>(); 

    public DefaultCitationHandler addProcessingStep(ProcessingStep step) {
        additionalProcessingSteps.add(step);
        return this;
    }
    
    @Override
    public Optional<String> processCitationParams(String url)
    {
        Map<String, String> citationFromPage = completeCitationFromUrl(url);

        Map<String, String> citationFromCitoid = completeCitationFromCitoid(url);

        Map<String, String> citationParams = new HashMap<>(citationFromPage);
        citationFromCitoid.forEach((k, v) -> citationParams.merge(k, v, (v1, v2) -> v2));

        if (!citationParams.isEmpty())
        {
            additionalProcessingSteps.stream().forEach(step -> step.processParams(citationParams));
            citationParams.put("url", url);
            return Optional.of(assembleCitation(citationParams));
        }
        return Optional.empty();
    }

    private String assembleCitation(Map<String, String> citationParams)
    {
        final StringBuilder sb = new StringBuilder("{{Citation");
        citationParams.entrySet().forEach(e -> sb.append(" |").append(e.getKey()).append('=').append(e.getValue()));
        sb.append("}}");
        return sb.toString();
    }

    private Map<String, String> completeCitationFromCitoid(String url)
    {
        HttpClient client = HttpClient.newBuilder().build();

        try
        {
            HttpRequest req = HttpRequest.newBuilder(new URI("https://ro.wikipedia.org/api/rest_v1/data/citation/zotero/" + encodeURIComponent(url)))
                .setHeader("User-Agent", "Andrebot CitationCompleter; Java " + System.getProperty("java.version")).build();
            HttpResponse<String> response = client.send(req, BodyHandlers.ofString());
            Gson gson = new Gson();
            String body = response.body();
            LOG.debug("Zotero body: {}", body);
            if (200 != response.statusCode())
            {
                return Collections.emptyMap();
            }
            Zotero[] zoteroObjs = gson.fromJson(body, Zotero[].class);
            Optional<Zotero> optZoteroObj = Arrays.stream(zoteroObjs).findFirst();
            if (optZoteroObj.isEmpty())
            {
                return Collections.emptyMap();
            }
            Zotero zoteroObj = optZoteroObj.get();

            Map<String, String> ret = new HashMap<String, String>();
            if (null != zoteroObj.getCreators())
            {
                int i = 0;
                for (Creator c : zoteroObj.getCreators())
                {
                    i++;
                    if (null != c.getLastName() && null != c.getFirstName())
                    {
                        ret.put(Optional.ofNullable(c.getCreatorType()).orElse("author") + i, c.getLastName() + ", " + c.getFirstName());
                    }
                    else
                    {
                        ret.put(Optional.ofNullable(c.getCreatorType()).orElse("author") + i, c.getName());
                    }
                }
            }
            if (null != zoteroObj.getDate())
            {
                ret.put("date", zoteroObj.getDate());
            }
            if (null != zoteroObj.getPublicationTitle())
            {
                ret.put("publisher", zoteroObj.getPublicationTitle());
            }
            else if (null != zoteroObj.getWebsiteTitle())
            {
                ret.put("publisher", zoteroObj.getWebsiteTitle());
            }
            if (null != zoteroObj.getTitle())
            {
                ret.put("title", zoteroObj.getTitle().replaceAll("\\|", "{{!}}"));
            }
            if (null != zoteroObj.getISBN())
            {
                ret.put("ISBN", zoteroObj.getISBN());
            }
            if (null != zoteroObj.getISSN())
            {
                ret.put("ISSN", zoteroObj.getISSN());
            }
            if (null != zoteroObj.getDOI())
            {
                ret.put("DOI", zoteroObj.getDOI());
            }
            if (null != zoteroObj.getIssue())
            {
                ret.put("issue", zoteroObj.getIssue());
            }
            if (null != zoteroObj.getVolume())
            {
                ret.put("volum", zoteroObj.getVolume());
            }

            if (null != zoteroObj.getLanguage())
            {
                Matcher localeMatcher = LOCALE_PATTERN.matcher(zoteroObj.getLanguage());
                if (localeMatcher.matches())
                {
                    String lang = localeMatcher.group(1);
                    if (!"ro".equalsIgnoreCase(lang))
                    {
                        ret.put("language", lang);
                    }
                }
            }
            return ret;
        }
        catch (URISyntaxException |IOException e)
        {
            LOG.warn("Failed to create citation for URL {}. Skipping...", e);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
        return Collections.emptyMap();
    }

    private Map<String, String> completeCitationFromUrl(String url)
    {
        Map<String, String> retParams = new HashMap<>();
        String title = null;
        String publisher = null;
        List<String> authors = new ArrayList<>();
        String publicationDate = null;
        String language = null;
        try
        {
            Document doc = Jsoup.connect(url).get();
            Elements ogTitleElems = doc.select("meta[property=og:title]");
            if (null != ogTitleElems && !ogTitleElems.isEmpty())
            {
                title = ogTitleElems.first().attr("content");
            }
            Elements ogSiteNameElems = doc.select("meta[property=og:site_name]");
            if (null != ogSiteNameElems && !ogSiteNameElems.isEmpty())
            {
                publisher = ogSiteNameElems.first().attr("content");
            }
            Elements articlePubTimeElems = doc.select("meta[property=article:published_time]");
            if (null != articlePubTimeElems && !articlePubTimeElems.isEmpty())
            {
                publicationDate = articlePubTimeElems.first().attr("content");
                publicationDate = extractDate(publicationDate);
            }
            Optional<Elements> articleAuthorElems = Stream.of("meta[property=article:author]", "meta[property=og:article:author]")
                .map(s -> doc.select(s))
                .filter(Objects::nonNull)
                .filter(Predicate.not(Elements::isEmpty)))
                .findFirst();
            
            if (articleAuthorElems.isPresent())
            {
                articleAuthorElems.get().forEach(el -> {
                    authors.add(el.attr("content"));
                });
            }
            Elements localeElems = doc.select("meta[property=og:locale]");
            if (null != localeElems && !localeElems.isEmpty())
            {
                Matcher localeMatcher = LOCALE_PATTERN.matcher(localeElems.first().attr("content"));
                if (localeMatcher.matches())
                {
                    language = localeMatcher.group(1);
                }
            }

            if (null != title && 0 < title.trim().length())
            {
                retParams.put("title", title.replaceAll("\\|", "{{!}}"));
            }
            if (null != publisher && 0 < publisher.trim().length())
            {
                retParams.put("publisher", publisher);
            }
            if (null != publicationDate)
            {
                retParams.put("date", publicationDate);
            }
            if (!authors.isEmpty())
            {
                for (int i = 0; i < authors.size(); i++)
                {
                    retParams.put("author" + (1 + i), authors.get(i));
                }
            }
            if (null != language && 0 < language.length() && !"ro".equalsIgnoreCase(language))
            {
                retParams.put("language", language);
            }
            populateMapFromJsonLd(doc, retParams);

        }
        catch (IOException e)
        {
            LOG.warn("Could not fill in citation from plain URL {}", url, e);
        }
        return retParams;
    }

    public void populateMapFromJsonLd(Document doc, Map<String, String> retParams)
    {
        Elements ldJsonElements = doc.select("script[type=application/ld+json]");
        if (!ldJsonElements.isEmpty())
        {
            for (Element ldJsonEl : ldJsonElements)
            {
                String ldJson = ldJsonEl.html();
                LOG.info("Found json: {}", ldJson);
                
                SchemaorgUtils.extractFromJsonSchema(ldJson, retParams);
            }
        }
    }
    private Optional<JsonObject> extractJsonObject(String ldJson)
    {
        Optional<JsonObject> ldJsonData = Optional.empty();
        try
        {
            JsonElement parsedLdJson = JsonParser.parseString(ldJson);
            if (parsedLdJson.isJsonArray())
            {
                JsonArray metadataJsonArray = parsedLdJson.getAsJsonArray();
                JsonElement jsonElement = metadataJsonArray.get(0);
                ldJsonData = Optional.ofNullable(jsonElement.getAsJsonObject());
            }
            else if (parsedLdJson.isJsonObject())
            {
                ldJsonData = Optional.ofNullable(parsedLdJson.getAsJsonObject());
            }
        }
        catch (Exception e)
        {
            LOG.warn("Error extracting json object from json {}", ldJson, e);
        }
        return ldJsonData;
    }

}
