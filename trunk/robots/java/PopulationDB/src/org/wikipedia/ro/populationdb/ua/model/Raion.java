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

import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "raion")
public class Raion implements LanguageStructurable {

    private long id;
    private String name;
    private String transliteratedName;
    private String romanianName;
    private Commune capital;
    private boolean miskrada;
    private Map<Language, Double> languageStructure = new HashMap<Language, Double>();

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

    @OneToMany(mappedBy = "raion")
    public Set<Commune> getCommunes() {
        return communes;
    }

    public void setCommunes(final Set<Commune> communes) {
        this.communes = communes;
    }

    private Region region;
    private Set<Commune> communes = new HashSet<Commune>();

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

    @ManyToOne
    @JoinColumn(name = "region")
    public Region getRegion() {
        return region;
    }

    public void setRegion(final Region county) {
        this.region = county;
    }

    @Override
    public String toString() {
        return "Raion [name=" + name + ", transliteratedName=" + transliteratedName + ", romanianName=" + romanianName + "]";
    }

    @Column(name = "miskrada")
    public boolean isMiskrada() {
        return miskrada;
    }

    public void setMiskrada(final boolean miskrada) {
        this.miskrada = miskrada;
    }

    @CollectionTable(name = "raion_nationalitate", joinColumns = @JoinColumn(name = "raion"))
    @MapKeyJoinColumn(name = "nationalitate")
    @Column(name = "procent")
    @ElementCollection
    public Map<Language, Double> getLanguageStructure() {
        return languageStructure;
    }

    public void setLanguageStructure(final Map<Language, Double> languageStructure) {
        this.languageStructure = languageStructure;
    }
}
