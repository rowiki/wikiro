package org.wikipedia.ro.villagesfixer;

public enum DemographicGroup {
    ROMANI("români", "Q485150", "#8080ff"),
    MAGHIARI("maghiari", "Q518365", "#80ff80"),
    ROMI("romi", "Q923163", "#80ffff"),
    GERMANI("germani", "Q700469", "#ff80ff"),
    TURCI("turci", "Q2509562", "#ff8080"),
    TATARI("tătari", "Q4170775", "#9f3f3f"),
    UCRAINENI("ucraineni", "Q369739", "#ffff80"),
    SARBI("sârbi", "Q1064264", "#c03f3f"),
    CEHI("cehi", "Q853303", "#ff5555"),
    SLOVACI("slovaci", "Q2989388", "#3f3fc0"),
    LIPOVENI("ruși lipoveni", "Q748548", "#ffafaf"),
    CROATI("croați", "Q5187245", "#c0c03f"),
    POLONEZI("polonezi", "Q3395424", "#5555ff"),
    BULGARI("bulgari", "Q3656818", "#3fc03f"),
    GRECI("greci", "Q3315585", "#c03fc0"),
    ITALIENI("italieni", "Q1990195", "#666666"),
    EVREI("evrei", "Q696662", "#666666"),
    RUTENI("ruteni", "Q28194448", "#eeee70"),
    ARMENI("armeni", "Q4793348", "#666666"),
    ALBANEZI("albanezi", "Q719847", "#666666"),
    MACEDONENI("macedoneni", "Q3409357", "#666666"),
    
    OTHERS("altele", null, "#555555"),
    UNKNOWN("necunoscută", null, "#9f9f9f"),

    ORTODOCSI("ortodocși", "Q181901", "#8080ff"),
    ROMANO_CATOLICI("romano-catolici", "Q144306", "#ffff80"),
    PENTICOSTALI("penticostali", "Q3291465", "#80ffff"),
    GRECO_CATOLICI("greco-catolici", "Q856349", "#ff80ff"),
    BAPTISTI("baptiști", "Q93191", "#3fc03f"),
    MUSULMANI("musulmani", "Q1414351", "#ff8080"),
    REFORMATI("reformați", "Q252250", "#80ff80"),
    RIT_VECHI_RUSI("creștini de rit vechi", "Q1623306", "#c0c03f"),
    RIT_VECHI_ROMANI("creștini de rit vechi", "Q1997209", "#c0c03f"),
    ATEI("atei", "Q7066", "#c0c0c0"),
    AGNOSTICI("agnostici", "Q288928", "#c0adad"),
    FARA_RELIGIE("făra religie", "Q58721", "#adbac0"),
    MARTORI_IEHOVA("martori ai lui Iehova", "Q35269", "#3f3fc0"),
    UNITARIENI("unitarieni", "Q106687", "#3fc0c0"),
    CRESTINI_EVANGHELIE("creștini după evanghelie", "Q685812", "#ffff55"),
    EVANGHELICI_LUTERANI("evanghelici luterani", "Q490692", "#ffa600"),
    ORTODOCSI_SR("ortodocși sârbi", "Q188814", "#c03f3f"),
    ADVENTISTI("adventiști", "Q215168", "#ffafaf");

    private String id;
    private String qId;
    private String color;
    
    private DemographicGroup(String id, String qId, String color) {
        this.id = id;
        this.qId = qId;
        this.color = color;
    }

    public String getId() {
        return id;
    }

    public String getqId() {
        return qId;
    }

    public String getColor() {
        return color;
    }
    
    public static DemographicGroup fromId(String id, boolean ro) {
        if ("creștini de rit vechi".equalsIgnoreCase(id)) {
            return ro ? RIT_VECHI_ROMANI : RIT_VECHI_RUSI;
        }
        for (DemographicGroup grp: values()) {
            if (id.equalsIgnoreCase(grp.getId())) {
                return grp;
            }
        }
        return null;
    }
    
    public static DemographicGroup fromId(String id) {
        return fromId(id, false);
    }
}
