package org.wikipedia.ro.populationdb.ua.model;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "oblast")
public class Region {
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

    @OneToMany(mappedBy = "region")
    public Set<Raion> getRaioane() {
        return raioane;
    }

    public void setRaioane(final Set<Raion> raioane) {
        this.raioane = raioane;
    }

    private Set<Raion> raioane;

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

    @OneToMany(mappedBy = "region")
    public Set<Raion> getDistricts() {
        return raioane;
    }

    public void setDistricts(final Set<Raion> districts) {
        this.raioane = districts;
    }
}
