package org.wikipedia.ro.java.oldcountries;

import java.io.IOException;
import java.util.List;

import javax.security.auth.login.LoginException;

import org.wikibase.WikibaseException;
import org.wikipedia.ro.java.oldcountries.data.UAT;
import org.wikipedia.ro.utility.AbstractExecutable;

public class OldCountriesFiller extends AbstractExecutable
{
    //https://query.wikidata.org/#SELECT%20DISTINCT%20%3Fitem%20%3FitemLabel%20%3FcountyLabel%20WHERE%20%7B%0A%20%20VALUES%20%3Frouats%20%7Bwd%3AQ659103%20wd%3AQ16858213%20wd%3AQ640364%7D%0A%20%20%3Fitem%20wdt%3AP31%20%3Frouats.%0A%20%20%3Fitem%20wdt%3AP17%20wd%3AQ218.%0A%20%20%3Fitem%20wdt%3AP131%20%3Fcounty.%0A%20%20%3Fcounty%20wdt%3AP31%20wd%3AQ1776764.%0A%20%20SERVICE%20wikibase%3Alabel%20%7B%0A%20%20%20%20bd%3AserviceParam%20wikibase%3Alanguage%20%22ro%2Cen%22.%0A%20%20%20%20%3Fitem%20rdfs%3Alabel%20%3FitemLabel.%0A%20%20%20%20%3Fcounty%20rdfs%3Alabel%20%3FcountyLabel.%0A%20%20%7D%0A%7D
    String allUATsQuery = "SELECT DISTINCT ?item ?itemLabel ?countyLabel WHERE {\n"
        + "  VALUES ?rouats {wd:Q659103 wd:Q16858213 wd:Q640364}\n"
        + "  ?item wdt:P31 ?rouats.\n"
        + "  ?item wdt:P17 wd:Q218.\n"
        + "  ?item wdt:P131 ?county.\n"
        + "  ?county wdt:P31 wd:Q1776764.\n"
        + "  SERVICE wikibase:label {\n"
        + "    bd:serviceParam wikibase:language \"ro,en\".\n"
        + "    ?item rdfs:label ?itemLabel.\n"
        + "    ?county rdfs:label ?countyLabel.\n"
        + "  }\n"
        + "}";

    @Override
    protected void execute() throws IOException, WikibaseException, LoginException
    {
        // get list of entities
        List<UAT> uats = getEntityList();
        
        // for each entity
        //    figure out historical region
        
        //    get historical region countries
        
        //    merge historical regions with wikidata
    }

    private List<UAT> getEntityList()
    {
        
        return null;
    }

}
