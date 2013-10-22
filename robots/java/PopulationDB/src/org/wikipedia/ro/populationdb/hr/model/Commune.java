package org.wikipedia.ro.populationdb.hr.model;

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

import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "localitate")
public class Commune implements EthnicallyStructurable, ReligionStructurable {
    private long id;
    private String name;
    private int population;
    private County county;
    private int town;
    private Map<Nationality, Integer> ethnicStructure = new HashMap<Nationality, Integer>();
    private Map<Religion, Integer> religiousStructure = new HashMap<Religion, Integer>();

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


    @ManyToOne
    @JoinColumn(name = "county")
    public County getCounty() {
        return county;
    }

    public void setCounty(final County county) {
        this.county = county;
    }

    @Column(name = "town")
    public int getTown() {
        return town;
    }

    public void setTown(final int town) {
        this.town = town;
    }

}
