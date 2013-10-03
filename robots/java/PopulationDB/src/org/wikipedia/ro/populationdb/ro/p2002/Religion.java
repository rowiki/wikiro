package org.wikipedia.ro.populationdb.ro.p2002;

public enum Religion {
    ORTHO(1), ROM_CATH(2), CALVINIST(3), PENTICOST(4), GR_CATH(5), BAPT(6), ADV7(7), MUSL(8), UNIT(9), JEHOVA(10), CR_EVANGH(
        11), OLD_ORTHO(12), LUTH(13), SR_ORTHO(14), EVANGH(15), AUGUST(16), JEWISH(17), ARM(18), OTH(19), NONE(20), ATHEISM(
            21), UNKNOWN(22);

    public static Religion getByIndex(final int idx) {
        return Religion.values()[idx - 1];
    }

    private int index;

    Religion(final int idx) {
        index = idx;
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        switch (this) {
        case ORTHO:
            return "Ortodocși";
        case ROM_CATH:
            return "Romano-catolici";
        case CALVINIST:
            return "Reformați";
        case GR_CATH:
            return "Greco-catolici";
        case BAPT:
            return "Baptiști";
        case ADV7:
            return "Adventiști de ziua a șaptea";
        case MUSL:
            return "Musulmani";
        case UNIT:
            return "Unitarieni";
        case JEHOVA:
            return "Martori ai lui Iehova";
        case CR_EVANGH:
            return "Creștini după evanghelie";
        case PENTICOST:
            return "Penticostali";
        case OLD_ORTHO:
            return "Ortodocși de rit vechi";
        case LUTH:
            return "Luterani";
        case SR_ORTHO:
            return "Ortodocși sârbi";
        case EVANGH:
            return "Evanghelici";
        case AUGUST:
            return "Evanghelici de confesiune augustană";
        case JEWISH:
            return "Mozaici";
        case ARM:
            return "Armeni";
        case OTH:
            return "Altă religie";
        case NONE:
            return "Nicio religie";
        case ATHEISM:
            return "Atei";
        default:
            return "Necunoscută";
        }
    }
}
