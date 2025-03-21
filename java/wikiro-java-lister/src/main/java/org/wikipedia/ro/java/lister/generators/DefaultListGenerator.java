package org.wikipedia.ro.java.lister.generators;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.wikibase.Wikibase;
import org.wikibase.WikibaseException;
import org.wikibase.data.Entity;
import org.wikibase.data.Item;
import org.wikipedia.Wiki;
import org.wikipedia.ro.cache.WikidataEntitiesCache;
import org.wikipedia.ro.utils.LinkUtils;

public class DefaultListGenerator implements WikidataListGenerator
{

    private WikidataEntitiesCache cache = null;
    private Wiki localWiki = null;

    public DefaultListGenerator(Wiki wiki, WikidataEntitiesCache wikidataEntitiesCache)
    {
        localWiki = wiki;
        cache = wikidataEntitiesCache;
    }

    @Override
    public String generateListContent(Entity wdEntity, String configPage)
    {
        try
        {
            List<String> configuration = localWiki.getPageText(List.of(configPage + "/interogare", configPage + "/antet", configPage + "/linie", configPage + "/subsol"));
            String query = configuration.get(0);
            String header = configuration.get(1);
            String lineTemplate = configuration.get(2);
            String footer = configuration.get(3);

            StringBuilder listBuilder = new StringBuilder();
            listBuilder.append(header).append('\n');

            Wikibase wd = cache.getWiki();
            String personalizedQuery = null != wdEntity ? String.format(query, wdEntity.getId()) : query;
            List<Map<String, Object>> results = wd.query(personalizedQuery);

            for (Map<String, Object> result : results)
            {
                processResult(result);
                StringSubstitutor substitutor = new StringSubstitutor(result);
                listBuilder.append(substitutor.replace(lineTemplate)).append('\n');
            }

            listBuilder.append(footer);
            return listBuilder.toString();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (WikibaseException e)
        {
            e.printStackTrace();
            return "Eroare: " + e.getMessage();
        }

        return "";
    }

    private void processResult(Map<String, Object> result)
    {
        for (Map.Entry<String, Object> entry : result.entrySet())
        {
            if (entry.getValue() instanceof Item) {
                String label = (String) result.get(entry.getKey() + "Label");
                Item it = (Item) entry.getValue();
                entry.setValue(LinkUtils.createLinkViaWikidata(it.getEnt(), label));
            } else if (entry.getValue() instanceof String) {
                String value = (String) entry.getValue();
                if (StringUtils.startsWith(value, "http://commons.wikimedia.org/wiki/Special:FilePath/")) {
                    entry.setValue(URLDecoder.decode(StringUtils.substringAfter(value, "http://commons.wikimedia.org/wiki/Special:FilePath/"), StandardCharsets.UTF_8));
                }
            }
        }
    }

}
