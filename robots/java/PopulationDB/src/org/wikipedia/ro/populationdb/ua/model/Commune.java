package org.wikipedia.ro.populationdb.ua.model;

import java.util.HashMap;
import java.util.Map;

public class Commune {
    private long id;
    private String name;
    private int population;
    private int town;
    private Raion raion;
    private double area;
    private final Map<Nationality, Integer> ethnicStructure = new HashMap<Nationality, Integer>();

}
