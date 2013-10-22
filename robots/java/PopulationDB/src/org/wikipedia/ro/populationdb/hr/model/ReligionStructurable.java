package org.wikipedia.ro.populationdb.hr.model;

import java.util.Map;

public interface ReligionStructurable extends Populated {
    Map<Religion, Integer> getReligiousStructure();
}
