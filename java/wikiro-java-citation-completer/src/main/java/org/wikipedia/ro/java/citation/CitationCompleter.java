package org.wikipedia.ro.java.citation;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.security.auth.login.LoginException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.wikibase.WikibaseException;
import org.wikipedia.Wiki;
import org.wikipedia.Wiki.RequestHelper;
import org.wikipedia.Wiki.Revision;
import org.wikipedia.ro.java.citation.data.Creator;
import org.wikipedia.ro.java.citation.data.Zotero;
import org.wikipedia.ro.utility.AbstractExecutable;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class CitationCompleter extends AbstractExecutable
{
    private static final Pattern REF_URL_PATTERN = Pattern
        .compile("\\<ref\\>\\s*(https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*))\\s*\\</ref\\>");
    private static final Pattern LOCALE_PATTERN = Pattern.compile("([^_\\-]+)(?:[_\\-](\\w+))?");

    @Override
    protected void execute() throws IOException, WikibaseException, LoginException
    {
        LocalDate lastVisit = findLastVisit();
        RequestHelper helper = wiki.new RequestHelper();
        helper.inNamespaces(Wiki.MAIN_NAMESPACE);

        LocalDate now = LocalDate.now();
        helper.withinDateRange(lastVisit.atStartOfDay().atOffset(ZoneId.of("Europe/Bucharest").getRules().getOffset(LocalDateTime.now())), OffsetDateTime.now());

        List<Revision> recentChanges = wiki.recentChanges(helper);
        long[] revIds = recentChanges.stream().mapToLong(Revision::getID).toArray();

        final Set<String> pageTitles = new LinkedHashSet<>();

        wiki.getRevisions(revIds).stream().filter(Objects::nonNull).map(Revision::getTitle).forEach(pageTitles::add);
        ArrayList<String> pageTitlesList = new ArrayList<>(pageTitles);
        List<String> pageTexts = wiki.getPageText(pageTitlesList);
        Map<String, String> pagesTitlesAndTexts = new LinkedHashMap<>();

        for (int i = 0; i < pageTexts.size(); i++)
        {
            if (pageTexts.get(i).startsWith("<revisions><rev"))
            {
                pageTexts.set(i, pageTexts.get(i).substring(1 + pageTexts.get(i).indexOf('>', "<revisions><rev".length())));
            }
            pagesTitlesAndTexts.put(pageTitlesList.get(i), pageTexts.get(i));
        }

        pagesTitlesAndTexts = pagesTitlesAndTexts.entrySet().stream().filter(e -> REF_URL_PATTERN.matcher(e.getValue()).find())
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

        System.out.printf("%d pages found%n", pagesTitlesAndTexts.size());
        for (Entry<String, String> pgdata : pagesTitlesAndTexts.entrySet())
        {
            System.out.printf("Page %s%n", pgdata.getKey());

            int citationsChanged = 0;
            StringBuilder sb = new StringBuilder();
            Matcher refUrlMatcher = REF_URL_PATTERN.matcher(pgdata.getValue());
            while (refUrlMatcher.find())
            {
                String url = refUrlMatcher.group(1);
                System.out.println("Found bare URL ref: " + url);

                Map<String, String> citationFromPage = completeCitationFromUrl(url);

                Map<String, String> citationFromCitoid = completeCitationFromCitoid(url);

                Map<String, String> citationParams = new HashMap<>(citationFromPage);
                citationFromCitoid.forEach((k, v) -> citationParams.merge(k, v, (v1, v2) -> v2));

                if (!citationParams.isEmpty())
                {
                    citationParams.put("url", url);
                    String assembledCitation = assembleCitation(citationParams);
                    refUrlMatcher.appendReplacement(sb, String.format("<ref>%s</ref>", assembledCitation.replaceAll("\\\\", "\\\\").replaceAll("\\$", "\\$")));
                    citationsChanged++;
                }
            }
            refUrlMatcher.appendTail(sb);

            if (0 < citationsChanged)
            {
                wiki.edit(pgdata.getKey(), sb.toString(), "Robot: completat automat " + (1 == citationsChanged ? "o citare" : (citationsChanged + " citări")));
            }
        }
        wiki.edit("Utilizator:Andrebot/dată-vizitare-pagini-editate", now.toString(), "Robot: actualizare dată vizitare pagini editate");
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
            System.out.println("Zotero body: " + body);
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
        catch (URISyntaxException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (InterruptedException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return Collections.emptyMap();
    }

    private String encodeURIComponent(String s)
    {
        try
        {
            return URLEncoder.encode(s, "UTF-8").replaceAll("\\+", "%20").replaceAll("\\%21", "!").replaceAll("\\%27", "'").replaceAll("\\%28", "(").replaceAll("\\%29", ")")
                .replaceAll("\\%7E", "~");
        }
        catch (UnsupportedEncodingException e)
        {
            return s;
        }
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
            Elements articleAuthorElems = doc.select("meta[property=article:author]");
            if (null != articleAuthorElems && !articleAuthorElems.isEmpty())
            {
                articleAuthorElems.forEach(el -> {
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
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return retParams;
    }

    private String extractDate(String publicationDate)
    {
        try
        {
            OffsetDateTime publicationDateTime = OffsetDateTime.parse(publicationDate);
            publicationDate = DateTimeFormatter.ofPattern("uuuu-MM-dd").format(publicationDateTime);
        }
        catch (DateTimeParseException dpe)
        {
            dpe.printStackTrace();
        }
        return publicationDate;
    }

    public void populateMapFromJsonLd(Document doc, Map<String, String> retParams)
    {
        Elements ldJsonElements = doc.select("script[type=application/ld+json]");
        if (!ldJsonElements.isEmpty())
        {
            for (Element ldJsonEl : ldJsonElements)
            {
                String ldJson = ldJsonEl.html();
                System.out.println("Found json:" + ldJson);
                Optional<JsonObject> ldJsonData = extractJsonObject(ldJson);

                if (ldJsonData.isEmpty())
                {
                    continue;
                }
                JsonObject ldJsonObject = ldJsonData.get();
                JsonElement dateElement = ldJsonObject.get("dateCreated");
                if (null != dateElement && dateElement.isJsonPrimitive())
                {
                    retParams.put("date", extractDate(dateElement.getAsString()));
                }
                JsonElement authorElement = ldJsonObject.get("author");
                if (null != authorElement && authorElement.isJsonObject())
                {
                    Optional<String> authorName = Optional.ofNullable(authorElement.getAsJsonObject().get("name")).map(JsonElement::getAsString);
                    if (authorName.isPresent())
                    {
                        retParams.put("author1", authorName.get());
                    }
                }
                JsonElement publisherElement = ldJsonObject.get("publisher");
                if (null != publisherElement && publisherElement.isJsonObject())
                {
                    retParams.put("publisher", publisherElement.getAsJsonObject().get("name").getAsString());
                }
                JsonElement titleElement = ldJsonObject.get("headline");
                if (null != titleElement && titleElement.isJsonPrimitive())
                {
                    retParams.put("title", titleElement.getAsString());
                }
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
            e.printStackTrace();
        }
        return ldJsonData;
    }

    private LocalDate findLastVisit() throws IOException
    {
        RequestHelper rh = wiki.new RequestHelper();
        rh.byUser("Andrebot");
        rh.limitedTo(1);
        List<Revision> revs = wiki.getPageHistory("Utilizator:Andrebot/dată-vizitare-pagini-editate", rh);

        rh.byUser("Andrei Stroe");
        revs.addAll(wiki.getPageHistory("Utilizator:Andrebot/dată-vizitare-pagini-editate", rh));

        revs.sort((r1, r2) -> r2.getTimestamp().compareTo(r1.getTimestamp()));

        Optional<Revision> latestRevOpt = revs.stream().findFirst();
        if (!latestRevOpt.isPresent())
        {
            return LocalDate.now().minusDays(1);
        }

        String strText = latestRevOpt.get().getText();
        return LocalDate.parse(strText);
    }
}
