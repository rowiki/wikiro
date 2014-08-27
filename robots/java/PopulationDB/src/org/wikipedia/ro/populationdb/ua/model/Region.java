package org.wikipedia.ro.populationdb.ua.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyJoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "oblast")
public class Region implements LanguageStructurable {
    private long id;
    private String name;
    private String transliteratedName;
    private String romanianName;
    private Commune capital;

    @ManyToOne
    @JoinColumn(name = "capital")
    public Commune getCapital() {
        return capital;
    }

    public void setCapital(final Commune capital) {
        this.capital = capital;
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

    @OneToMany(mappedBy = "region")
    public Set<Raion> getRaioane() {
        return raioane;
    }

    public void setRaioane(final Set<Raion> raioane) {
        this.raioane = raioane;
    }

    private Set<Raion> raioane = new HashSet<Raion>();
    private Set<Commune> cities = new HashSet<Commune>();
    private Map<Language, Double> languageStructure = new HashMap<Language, Double>();

    @OneToMany(mappedBy = "region")
    public Set<Commune> getCities() {
        return cities;
    }

    public void setCities(final Set<Commune> cities) {
        this.cities = cities;
    }

    @Id
    @GeneratedValue(generator = "increment")
    @GenericGenerator(strategy = "increment", name = "increment")
    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    @Column(name = "nume")
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @CollectionTable(name = "oblast_nationalitate", joinColumns = @JoinColumn(name = "oblast"))
    @MapKeyJoinColumn(name = "nationalitate")
    @Column(name = "procent")
    @ElementCollection
    public Map<Language, Double> getLanguageStructure() {
        return languageStructure;
    }

    public void setLanguageStructure(final Map<Language, Double> languageStructure) {
        this.languageStructure = languageStructure;
    }

    @Override
    public String toString() {
        return "Region [name=" + name + ", transliteratedName=" + transliteratedName + ", romanianName=" + romanianName
            + "]";
    }

    @Transient
    public String getGenitive() {
        return "regiunii";
    }

    @Transient
    public Region computeRegion() {
        return this;
    }
}
