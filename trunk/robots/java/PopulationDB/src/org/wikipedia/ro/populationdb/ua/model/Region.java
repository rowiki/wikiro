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
    private String nameUa;
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

    @Column(name = "nume_ua")
    public String getNameUa() {
        return nameUa;
    }

    public void setNameUa(final String name) {
        this.nameUa = name;
    }

    @OneToMany(mappedBy = "county")
    public Set<Raion> getDistricts() {
        return raioane;
    }

    public void setDistricts(final Set<Raion> districts) {
        this.raioane = districts;
    }
}
