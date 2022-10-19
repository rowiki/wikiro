package org.wikipedia.ro.java.citation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonSyntaxException;
import com.google.schemaorg.JsonLdSerializer;
import com.google.schemaorg.JsonLdSyntaxException;
import com.google.schemaorg.SchemaOrgException;
import com.google.schemaorg.SchemaOrgType;
import com.google.schemaorg.core.Article;
import com.google.schemaorg.core.NewsArticle;
import com.google.schemaorg.core.Organization;
import com.google.schemaorg.core.Person;
import com.google.schemaorg.core.Thing;
import com.google.schemaorg.core.datatype.Text;

public class SchemaorgUtils
{
    private static final Logger LOG = LoggerFactory.getLogger(SchemaorgUtils.class);
    
    private static final JsonLdSerializer SERIALIZER = new JsonLdSerializer(true);
    
    public static void extractFromJsonSchema(String ldJson, Map<String, String> retParams)
    {
        try
        {
            //ldJson = pruneLdJson(ldJson);
            List<Thing> thing = SERIALIZER.deserialize(ldJson);
            Optional<Thing> firstThing = thing.stream().findFirst();
            if (!firstThing.isPresent())
            {
                return;
            }
            if ("http://schema.org/NewsArticle".equals(firstThing.get().getFullTypeName())) {
                NewsArticle article = (NewsArticle) thing.get(0);
                extractFromArticle(retParams, article, null);
            }
            else if ("http://schema.org/Thing".equals(firstThing.get().getFullTypeName()))
            {
                ImmutableList<SchemaOrgType> graph = firstThing.get().getProperty("@graph");
                for (SchemaOrgType sot: graph)
                {
                    if (Arrays.asList("http://schema.org/NewsArticle", "http://schema.org/Article").contains(sot.getFullTypeName()))
                    {
                        extractFromArticle(retParams, (Article) sot, graph);
                    }
                }
            }
            
        }
        catch (JsonSyntaxException | JsonLdSyntaxException e)
        {
            LOG.error("Error deserializing json", e);
        }
    }

    private static void extractFromArticle(Map<String, String> retParams, Article article, List<SchemaOrgType> graph)
    {
        ImmutableList<SchemaOrgType> datePubList = article.getDatePublishedList();
        if (null != datePubList)
        {
            datePubList.stream().filter(Text.class::isInstance).findFirst().map(Text.class::cast).map(Text::getValue)
                .map(org.wikipedia.ro.java.citation.Utils::extractDate).ifPresent(d -> retParams.put("date", d));
        }
        ImmutableList<SchemaOrgType> dateCreatedList = article.getDateCreatedList();
        if (null != dateCreatedList)
        {
            dateCreatedList.stream().filter(Text.class::isInstance).findFirst().map(Text.class::cast).map(Text::getValue)
                .map(org.wikipedia.ro.java.citation.Utils::extractDate).ifPresent(d -> retParams.put("date", d));
        }
        
        ImmutableList<SchemaOrgType> authorList = article.getAuthorList();
        if (null != authorList)
        {
            List<SchemaOrgType> authorNames = authorList.stream()
                .filter(Person.class::isInstance)
                .map(Person.class::cast)
                .map(Person::getNameList)
                .reduce((List<SchemaOrgType>) new ArrayList<SchemaOrgType>(), (partial, next) -> {
                    partial.addAll(next);
                    return partial;
                }, (l1, l2) -> {
                    List<SchemaOrgType> x = new ArrayList<>();
                    x.addAll(l1);
                    x.addAll(l2);
                    return x;
                });
            
            for (int i = 0; i < authorNames.size(); i++)
            {
                retParams.put(String.format("author%d", 1 + i), ((Text) authorNames.get(i)).getValue()); 
            }
        }
        
        ImmutableList<SchemaOrgType> publisherList = article.getPublisherList();
        if (null != publisherList)
        {
            List<SchemaOrgType> publisherNames = publisherList.stream()
                .filter(Organization.class::isInstance)
                .map(Organization.class::cast)
                .map(Organization::getNameList)
                .reduce((List<SchemaOrgType>) new ArrayList<SchemaOrgType>(), (partial, next) -> {
                    partial.addAll(next);
                    return partial;
                }, (l1, l2) -> {
                    List<SchemaOrgType> x = new ArrayList<>();
                    x.addAll(l1);
                    x.addAll(l2);
                    return x;
                });
            if (publisherNames.isEmpty() && !graph.isEmpty())
            {
                for (SchemaOrgType maybePublisher: publisherList)
                {
                    if (!Thing.class.isInstance(maybePublisher))
                    {
                        continue;
                    }
                    String jsonLdId;
                    try
                    {
                        jsonLdId = Thing.class.cast(maybePublisher).getJsonLdId();
                        Organization o = findObjectByIdInGraph(jsonLdId, Organization.class, graph);
                        publisherNames = o.getNameList();
                    }
                    catch (SchemaOrgException e)
                    {
                        LOG.warn("Could not get LD JSON ID for object", e);
                    }
                }
            }
            retParams.put("publisher", 
                publisherNames.stream().map(Text.class::cast).map(Text::getValue).collect(Collectors.joining(", ")));
        }
        
        ImmutableList<SchemaOrgType> headlineList = article.getHeadlineList();
        if (null != headlineList)
        {
            headlineList.stream()
                .filter(Text.class::isInstance)
                .findFirst()
                .map(Text.class::cast)
                .map(Text::getValue)
                .ifPresent(t -> retParams.put("title", t));
        }
    }

    private static <T> T findObjectByIdInGraph(String id, Class<T> clazz, List<SchemaOrgType> graph)
    {
        for (SchemaOrgType indexedObj : graph)
        {
            String crtId = indexedObj.getProperty("@id").stream().filter(Text.class::isInstance).findFirst().map(Text.class::cast).map(Text::getValue).orElse(null);
            if (StringUtils.equals(crtId, id) && clazz.isInstance(indexedObj))
            {
                return clazz.cast(indexedObj);
            }
        }
        return null;
    }
}
