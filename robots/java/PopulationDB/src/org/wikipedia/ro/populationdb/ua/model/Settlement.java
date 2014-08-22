package org.wikipedia.ro.populationdb.ua.model;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyJoinColumn;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "localitate")
public class Settlement implements LanguageStructurable {
    private long id;
    private String name;
    private String transliteratedName;
    private String romanianName;
    private int population;
    private Commune commune;
    private double area;
    private Map<Language, Double> languageStructure = new HashMap<Language, Double>();

    @Id
    @GeneratedValue(generator = "increment")
    @GenericGenerator(name = "increment", strategy = "increment")
    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    @Column(name = "populatie")
    public int getPopulation() {
        return population;
    }

    public void setPopulation(final int population) {
        this.population = population;
    }

    @ElementCollection
    @CollectionTable(name = "localitate_nationalitate", joinColumns = @JoinColumn(name = "localitate"))
    @MapKeyJoinColumn(name = "nationalitate")
    @Column(name = "procent")
    public Map<Language, Double> getLanguageStructure() {
        return languageStructure;
    }

    public void setLanguageStructure(final Map<Language, Double> languageStructure) {
        this.languageStructure = languageStructure;
    }

    @Column(name = "nume")
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @ManyToOne
    @JoinColumn(name = "commune")
    public Commune getCommune() {
        return commune;
    }

    public void setCommune(final Commune commune) {
        this.commune = commune;
    }

    public double getArea() {
        return area;
    }

    @Column(name = "suprafata")
    public void setArea(final double d) {
        this.area = d;
    }

    @Column(name = "transliterare")
    public String getTransliteratedName() {
        return transliteratedName;
    }

    public void setTransliteratedName(final String transliteratedName) {
        this.transliteratedName = transliteratedName;
    }

    public String getRomanianName() {
        return romanianName;
    }

    public void setRomanianName(final String romanianName) {
        this.romanianName = romanianName;
    }

    @Override
    public String toString() {
        return "Settlement [name=" + name + ", transliteratedName=" + transliteratedName + ", romanianName=" + romanianName
            + "]";
    }

    @Transient
    public String getGenitive() {
        return "localității";
    }

}
