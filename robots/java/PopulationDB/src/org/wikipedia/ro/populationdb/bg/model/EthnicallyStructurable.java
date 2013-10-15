package org.wikipedia.ro.populationdb.bg.model;

import java.util.Map;

public interface EthnicallyStructurable {
    Map<Nationality, Integer> getEthnicStructure();

    int getPopulation();
}
