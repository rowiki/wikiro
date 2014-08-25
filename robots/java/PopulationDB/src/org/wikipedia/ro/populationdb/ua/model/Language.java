package org.wikipedia.ro.populationdb.ua.model;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "limba")
public class Language {
    private int id;
    private String name;

    @Id
    @GeneratedValue(generator = "increment")
    @GenericGenerator(name = "increment", strategy = "increment")
    @Column(name = "id")
    public int getId() {
        return id;
    }

    public Language(final String name) {
        super();
        this.name = name;
    }

    public Language() {
        super();
    }

    public void setId(final int id) {
        this.id = id;
    }

    @Column(name = "nume")
    public String getName() {
        return name;
    }

    public void setName(final String nume) {
        this.name = nume;
    }

    @Override
    public String toString() {
        return "Limba " + StringUtils.lowerCase(name);
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof Language)) {
            return false;
        }
        final Language otherLang = (Language) obj;
        return id == otherLang.getId() && StringUtils.equals(name, otherLang.getName());
    }

    @Override
    public int hashCode() {
        return id + Objects.hashCode(name);
    }

}
