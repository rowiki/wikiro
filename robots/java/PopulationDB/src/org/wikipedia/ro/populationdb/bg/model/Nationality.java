package org.wikipedia.ro.populationdb.bg.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "nationalitate")
public class Nationality {
    private int id;
    private String nume;

    @Id
    @GeneratedValue(generator = "increment")
    @GenericGenerator(name = "increment", strategy = "increment")
    @Column(name = "id")
    public int getId() {
        return id;
    }

    public void setId(final int id) {
        this.id = id;
    }

    @Column(name = "nume")
    public String getNume() {
        return nume;
    }

    public void setNume(final String nume) {
        this.nume = nume;
    }
}
