package org.wikipedia.ro.populationdb.hr.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "zupanja")
public class County {
    private long id;
    private String name;
    private String nameHr;

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

    @Column(name = "nume_hr")
    public String getNameHr() {
        return nameHr;
    }

    public void setNameHr(String nameHr) {
        this.nameHr = nameHr;
    }

}
