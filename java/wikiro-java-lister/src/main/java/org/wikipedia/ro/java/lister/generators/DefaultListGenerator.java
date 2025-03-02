package org.wikipedia.ro.java.lister.generators;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.text.StringSubstitutor;
import org.wikibase.Wikibase;
import org.wikibase.WikibaseException;
import org.wikibase.data.Entity;
import org.wikipedia.Wiki;
import org.wikipedia.ro.cache.WikidataEntitiesCache;

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
            listBuilder.append(header);
            
            Wikibase wd = cache.getWiki();
            String personalizedQuery = String.format(query, wdEntity.getId());
            List<Map<String, Object>> results = wd.query(personalizedQuery);
            
            for (Map<String, Object> result : results)
            {
                StringSubstitutor substitutor = new StringSubstitutor(result);
                listBuilder.append(substitutor.replace(lineTemplate));
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

}
