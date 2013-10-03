package org.wikipedia.ro.populationdb.hu.model;

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
import javax.persistence.MapKey;
import javax.persistence.MapKeyJoinColumn;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "localitate")
public class Settlement {
    private long id;
    private String name;
    private int population;
    private int town;
    private District district;
    private double area;
    private Map<Nationality, Integer> ethnicStructure = new HashMap<Nationality, Integer>();
    private Map<Religion, Integer> religiousStructure = new HashMap<Religion, Integer>();
    private final Map<Integer, Integer> historicalPopulation = new HashMap<Integer, Integer>();

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
    @Column(name = "populatie")
    public Map<Nationality, Integer> getEthnicStructure() {
        return ethnicStructure;
    }

    public void setEthnicStructure(final Map<Nationality, Integer> ethnicStructure) {
        this.ethnicStructure = ethnicStructure;
    }

    @Column(name = "nume")
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @Column(name = "nivel_oras")
    public int getTown() {
        return town;
    }

    public void setTown(final int town) {
        this.town = town;
    }

    @ElementCollection
    @CollectionTable(name = "localitate_istoric", joinColumns = @JoinColumn(name = "localitate"))
    @Column(name = "populatie")
    @MapKey(name = "an")
    public Map<Integer, Integer> getHistoricalPopulation() {
        return historicalPopulation;
    }

    @ManyToOne
    @JoinColumn(name = "district")
    public District getDistrict() {
        return district;
    }

    public void setDistrict(final District district) {
        this.district = district;
    }

    public double getArea() {
        return area;
    }

    @Column(name = "suprafata")
    public void setArea(final double d) {
        this.area = d;
    }

    @ElementCollection
    @CollectionTable(name = "localitate_religie", joinColumns = @JoinColumn(name = "localitate"))
    @MapKeyJoinColumn(name = "religie")
    @Column(name = "populatie")
    public Map<Religion, Integer> getReligiousStructure() {
        return religiousStructure;
    }

    public void setReligiousStructure(final Map<Religion, Integer> religiousStructure) {
        this.religiousStructure = religiousStructure;
    }

}
