package org.wikipedia.ro.java.wikiprojects.utils;

public enum ImportanceClass {
    UNSPECIFIED(""), LOW("mică"), MID("medie"), HIGH("mare"), TOP("top");

    private String designation;
    private ImportanceClass lower;
    private ImportanceClass higher;
    
    static {
        UNSPECIFIED.lower = ImportanceClass.UNSPECIFIED;
        UNSPECIFIED.higher = ImportanceClass.UNSPECIFIED;
        LOW.lower = LOW;
        LOW.higher = MID;
        MID.lower = LOW;
        MID.higher = HIGH;
        HIGH.lower = MID;
        HIGH.higher = TOP;
        TOP.lower = HIGH;
        TOP.higher = TOP;
    }

    public ImportanceClass getLower() {
        return lower;
    }

    public void setLower(ImportanceClass lower) {
        this.lower = lower;
    }

    public ImportanceClass getHigher() {
        return higher;
    }

    public void setHigher(ImportanceClass higher) {
        this.higher = higher;
    }

    private ImportanceClass(String s) {
        designation = s;
    }

    @Override
    public String toString() {
        return designation;
    }

    

    public static ImportanceClass fromString(String s) {
        if (null == s) {
            return null;
        }
        switch (s.toLowerCase()) {
        case "mică":
        case "mica":
        case "low":
        case "small":
            return LOW;
        case "medie":
        case "mid":
            return MID;
        case "mare":
        case "high":
            return HIGH;
        case "top":
            return TOP;
        default:
            return null;
        }
    }
}
