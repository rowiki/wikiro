package org.wikipedia.ro.java.lister.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.wikibase.Wikibase;
import org.wikibase.WikibaseException;
import org.wikibase.data.Entity;

public class WikidataEntitiesCache {

    private Wikibase wikidata;
    private final Map<String, Entity> CACHE = new HashMap<String, Entity>();

    public WikidataEntitiesCache(Wikibase wikidata) {
        super();
        this.wikidata = wikidata;
    }

    public Entity get(String id) throws IOException, WikibaseException {
        Entity entity = CACHE.get(id);
        if (null == entity) {
            entity = wikidata.getWikibaseItemById(id);
            CACHE.put(id, entity);
        }
        return entity;
    }
    
    public Entity get(Entity ent) throws IOException, WikibaseException {
        return get(ent.getId());
    }

    public Wikibase getWiki() {
        return wikidata;
    }
}
