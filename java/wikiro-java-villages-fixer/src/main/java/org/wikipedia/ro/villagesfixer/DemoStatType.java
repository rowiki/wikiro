package org.wikipedia.ro.villagesfixer;

public enum DemoStatType {
    ETNIC("etnică", "etnii", "etnie", "2.2.2"),
    RELIGIOS("confesională", "religii", "religie", "2.4.2");

    private String adjective;
    private String plural;
    private String singular;
    private String tabel;

    private DemoStatType(String adjective, String plural, String singular, String tabel) {
        this.adjective = adjective;
        this.plural = plural;
        this.singular = singular;
        this.tabel = tabel;
    }

    public String getAdjective() {
        return adjective;
    }

    public String getPlural() {
        return plural;
    }

    public String getSingular() {
        return singular;
    }

    public String getTabel() {
        return tabel;
    }

}
