package org.wikipedia.ro.populationdb.ua.model;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "raion")
public class Raion {

    private long id;
    private String name;
    private String transliteratedName;
    private String romanianName;

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

    private Region county;
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
    public Region getCounty() {
        return county;
    }

    public void setCounty(final Region county) {
        this.county = county;
    }
}
