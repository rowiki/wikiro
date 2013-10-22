package org.wikipedia.ro.populationdb.hr.model;

import java.util.Map;

public interface EthnicallyStructurable extends Populated {
    Map<Nationality, Integer> getEthnicStructure();
}
