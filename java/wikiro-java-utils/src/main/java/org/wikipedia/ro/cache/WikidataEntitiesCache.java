package org.wikipedia.ro.cache;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.wikibase.Wikibase;
import org.wikibase.WikibaseException;
import org.wikibase.data.Entity;

public class WikidataEntitiesCache {

    private Wikibase wikidata;
    private final Map<String, Entity> qIdIndex = new HashMap<>();
    private final Map<String, String> articleIndex = new HashMap<>();

    public WikidataEntitiesCache(Wikibase wikidata) {
        super();
        this.wikidata = wikidata;
    }

    public Entity get(String id) throws IOException, WikibaseException {
        Entity entity = qIdIndex.get(id);
        if (null == entity) {
            entity = wikidata.getWikibaseItemById(id);
            qIdIndex.put(id, entity);
        }
        return entity;
    }

    public Entity getByArticle(String wiki, String title) throws IOException, WikibaseException {
        String key = String.format("%s:%s", wiki, title);
        String qId = articleIndex.get(key);
        if (null == qId) {
            Entity ent = wikidata.getWikibaseItemBySiteAndTitle(wiki, title);
            if (null != ent) {
                articleIndex.put(key, ent.getId());
                qIdIndex.put(ent.getId(), ent);
                return ent;
            }
        }
        return null;
    }

    public Entity get(Entity ent) throws IOException, WikibaseException {
        return get(ent.getId());
    }

    public void invalidate(Entity ent) {
        invalidate(ent.getId());
    }

    public void invalidate(String id) {
        qIdIndex.remove(id);
    }
    
    public Wikibase getWiki() {
        return wikidata;
    }

    public Entity refresh(Entity ent) throws IOException, WikibaseException {
        invalidate(ent);
        return get(ent);
    }
}
