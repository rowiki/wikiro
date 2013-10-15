package org.wikipedia.ro.populationdb.bg.model;

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
public class Settlement implements EthnicallyStructurable {
    private long id;
    private String numeBg;
    private String numeRo;
    private int population;
    private Obshtina obshtina;
    private boolean town;
    private Map<Nationality, Integer> ethnicStructure = new HashMap<Nationality, Integer>();

    @Id
    @GeneratedValue(generator = "increment")
    @GenericGenerator(name = "increment", strategy = "increment")
    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    @Column(name = "nume_bg")
    public String getNumeBg() {
        return numeBg;
    }

    public void setNumeBg(final String numeBg) {
        this.numeBg = numeBg;
    }

    @Column(name = "nume_ro")
    public String getNumeRo() {
        return numeRo;
    }

    public void setNumeRo(final String numeRo) {
        this.numeRo = numeRo;
    }

    @Column(name = "populatie")
    public int getPopulation() {
        return population;
    }

    public void setPopulation(final int population) {
        this.population = population;
    }

    @ManyToOne
    @JoinColumn(name = "obstina")
    public Obshtina getObshtina() {
        return obshtina;
    }

    @ManyToOne()
    @JoinColumn(name = "obstina")
    public void setObshtina(final Obshtina obshtina) {
        this.obshtina = obshtina;
    }

    @Column(name = "oras")
    public boolean isTown() {
        return town;
    }

    public void setTown(final boolean town) {
        this.town = town;
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

}
