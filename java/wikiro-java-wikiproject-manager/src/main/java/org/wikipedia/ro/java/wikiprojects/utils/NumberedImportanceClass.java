package org.wikipedia.ro.java.wikiprojects.utils;

public enum NumberedImportanceClass implements ImportanceClass  {
    L0("0"), L1("1"), L2("2"), L3("3"), L4("4"), L5("5"), UNSPECIFIED("");
    
    private String designation;
    private NumberedImportanceClass lower;
    private NumberedImportanceClass higher;
    
    static {
        UNSPECIFIED.lower = NumberedImportanceClass.UNSPECIFIED;
        UNSPECIFIED.higher = NumberedImportanceClass.UNSPECIFIED;
        L5.lower = L5;
        L5.higher = L4;
        L4.lower = L4;
        L4.higher = L3;
        L3.lower = L4;
        L3.higher = L2;
        L2.lower = L3;
        L2.higher = L1;
        L1.lower = L2;
        L1.higher = L0;
        L0.lower = L1;
        L0.higher = L0;
    }

    public NumberedImportanceClass getLower() {
        return lower;
    }

    public void setLower(NumberedImportanceClass lower) {
        this.lower = lower;
    }

    public NumberedImportanceClass getHigher() {
        return higher;
    }

    public void setHigher(NumberedImportanceClass higher) {
        this.higher = higher;
    }

    private NumberedImportanceClass(String s) {
        designation = s;
    }

    @Override
    public String toString() {
        return designation;
    }

    public static NumberedImportanceClass fromString(String s) {
        if (null == s) {
            return null;
        }
        switch (s.toLowerCase()) {
        case "0":
        case "L0":
            return L0;
        case "1":
        case "L1":
            return L1;
        case "2":
        case "L2":
            return L2;
        case "3":
        case "L3":
            return L3;
        case "4":
        case "L4":
            return L4;
        case "5":
        case "L5":
            return L5;
        default:
            return null;
        }
    }
}
