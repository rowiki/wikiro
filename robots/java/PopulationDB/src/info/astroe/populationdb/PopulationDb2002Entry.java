package info.astroe.populationdb;

import java.util.LinkedHashMap;
import java.util.Map;

public class PopulationDb2002Entry {
    private int siruta;
    private int parentSiruta;
    private String name;
    private int population;
    private UTAType type;
    private Map<Nationality, Integer> nationalStructure = new LinkedHashMap<Nationality, Integer>();
    private Map<Religion, Integer> religiousStructure = new LinkedHashMap<Religion, Integer>();
    private boolean village;

    public int getSiruta() {
        return siruta;
    }

    public void setSiruta(final int siruta) {
        this.siruta = siruta;
    }

    public int getParentSiruta() {
        return parentSiruta;
    }

    public void setParentSiruta(final int parentSiruta) {
        this.parentSiruta = parentSiruta;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public int getPopulation() {
        return population;
    }

    public void setPopulation(final int population) {
        this.population = population;
    }

    public Map<Nationality, Integer> getNationalStructure() {
        return nationalStructure;
    }

    public void setNationalStructure(final Map<Nationality, Integer> nationalStructure) {
        this.nationalStructure = nationalStructure;
    }

    public Map<Religion, Integer> getReligiousStructure() {
        return religiousStructure;
    }

    public void setReligiousStructure(final Map<Religion, Integer> religiousStructure) {
        this.religiousStructure = religiousStructure;
    }

    public boolean isVillage() {
        return village;
    }

    public void setVillage(final boolean village) {
        this.village = village;
    }

    public UTAType getType() {
        return type;
    }

    public void setType(final UTAType type) {
        this.type = type;
    }
}
