package org.wikipedia.ro.java.wikiprojects.model;

import java.util.ArrayList;
import java.util.List;

public class Wikiproject {
    private String name;
    
    private List<Wikiproject> parents = new ArrayList<>();

    public Wikiproject(String name) {
        super();
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Wikiproject setName(String name) {
        this.name = name;
        return this;
    }

    public List<Wikiproject> getParents() {
        return parents;
    }

    public Wikiproject setParents(List<Wikiproject> parents) {
        this.parents = parents;
        return this;
    }
}
