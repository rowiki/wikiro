package org.wikipedia.ro.java.wikiprojects.utils;

public enum QualityClass implements ArticleClass {
    FA, A, GA, B, C, START, STUB, L, FL, LIST, TEMPLATE, PORTAL, FUTURE, ALL_QUALITY, UNKNOWN_QUALITY;
    
    public static QualityClass fromString(String s) {
        if (null == s) {
            return null;
        }
        switch (s.toUpperCase()) {
        case "AC":
        case "FA":
            return FA;
        case "A":
            return A;
        case "GA":
        case "AB":
            return GA;
        case "B":
            return B;
        case "C":
            return C;
        case "START":
        case "ÎNCEPUT":
            return START;
        case "STUB":
        case "CIOT":
            return STUB;
        case "LIST":
        case "LISTĂ":
            return LIST;
        case "LC":
        case "FL":
            return FL;
        case "TEMPLATE":
        case "FORMAT":
            return TEMPLATE;
        case "PORTAL":
            return PORTAL;
        case "FUTURE":
        case "VIITOR":
            return FUTURE;
        default:
            return null;
        }
    }

}
