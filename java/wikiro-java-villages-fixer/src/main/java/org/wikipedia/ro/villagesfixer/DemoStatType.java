package org.wikipedia.ro.villagesfixer;

public enum DemoStatType {
    ETNIC("etnii", "etnică"),
    RELIGIOS("religii", "confesională");

    private String adjective;
    private String plural;

    private DemoStatType(String adjective, String plural) {
        this.adjective = adjective;
        this.plural = plural;
    }

    public String getAdjective() {
        return adjective;
    }

    public String getPlural() {
        return plural;
    }

}
