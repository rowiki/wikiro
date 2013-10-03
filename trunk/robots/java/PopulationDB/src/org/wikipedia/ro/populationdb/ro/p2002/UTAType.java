package org.wikipedia.ro.populationdb.ro.p2002;

public enum UTAType {
    MUNICIPIU(1), ORAS(2), COMUNA(3);

    private int id;

    private UTAType(final int i) {
        id = i;
    }

    public int getId() {
        return id;
    }

    public static UTAType fromId(final int i) {
        switch (i) {
        case 1:
            return MUNICIPIU;
        case 2:return ORAS;
        case 3:return COMUNA;
        default:
            return null;
        }
    }
}
