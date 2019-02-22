package org.wikipedia.ro.java.lister.generators;

import org.wikibase.data.Entity;

public interface WikidataListGenerator
{
    String generateListContent(Entity wdEntity);
}
