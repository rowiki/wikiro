package org.wikipedia.ro.java.wikiprojects.utils;

public enum DefaultImportanceClass implements ImportanceClass {
    UNSPECIFIED(""), LOW("mică"), MID("medie"), HIGH("mare"), TOP("top"), ALL("all");

    private String designation;
    private DefaultImportanceClass lower;
    private DefaultImportanceClass higher;
    
    static {
        UNSPECIFIED.lower = DefaultImportanceClass.UNSPECIFIED;
        UNSPECIFIED.higher = DefaultImportanceClass.UNSPECIFIED;
        LOW.lower = LOW;
        LOW.higher = MID;
        MID.lower = LOW;
        MID.higher = HIGH;
        HIGH.lower = MID;
        HIGH.higher = TOP;
        TOP.lower = HIGH;
        TOP.higher = TOP;
    }

    public DefaultImportanceClass getLower() {
        return lower;
    }

    public void setLower(DefaultImportanceClass lower) {
        this.lower = lower;
    }

    public DefaultImportanceClass getHigher() {
        return higher;
    }

    public void setHigher(DefaultImportanceClass higher) {
        this.higher = higher;
    }

    private DefaultImportanceClass(String s) {
        designation = s;
    }

    @Override
    public String toString() {
        return designation;
    }

    

    public static DefaultImportanceClass fromString(String s) {
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
