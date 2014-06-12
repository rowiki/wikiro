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
    private Region county;
    private Set<Settlement> settlements = new HashSet<Settlement>();

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
    @JoinColumn(name = "county")
    public Region getCounty() {
        return county;
    }

    public void setCounty(final Region county) {
        this.county = county;
    }

    @OneToMany(mappedBy = "district")
    public Set<Settlement> getSettlements() {
        return settlements;
    }

    public void setSettlements(final Set<Settlement> settlements) {
        this.settlements = settlements;
    }
}
