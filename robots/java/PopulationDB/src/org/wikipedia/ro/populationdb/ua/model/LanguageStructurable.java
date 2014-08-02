package org.wikipedia.ro.populationdb.ua.model;

import java.util.Map;

public interface LanguageStructurable {

    public abstract Map<Language, Double> getLanguageStructure();

}
