package org.wikipedia.ro.villagesfixer;

public enum CommuneType {
    MUNICIPIU("municipiu", "municipiul", "municipiului", "MUNICIPIUL %s", false),
    ORAS("oraș", "orașul", "orașului", "ORAȘ %s", false),
    COMUNA("comună", "comuna", "comunei", "%s", true);

    private String typeName;
    private String typeNameAcc;
    private String typeNameGen;
    private String popDataKeyFormat;
    private boolean feminine;

    private CommuneType(String typeName, String typeNameAcc, String typeNameGen, String popDataKeyFormat, boolean feminine) {
        this.typeName = typeName;
        this.typeNameAcc = typeNameAcc;
        this.typeNameGen = typeNameGen;
        this.popDataKeyFormat = popDataKeyFormat;
        this.feminine = feminine;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getTypeNameAcc() {
        return typeNameAcc;
    }

    public String getTypeNameGen() {
        return typeNameGen;
    }

    public String getPopDataKeyFormat() {
        return popDataKeyFormat;
    }

    public boolean isFeminine() {
        return feminine;
    }

}
