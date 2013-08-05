package info.astroe.populationdb;

public enum Nationality {
    RO(1), HU(2), RR(3), UK(4), DE(5), TR(6), RU(7), TT(8), SR(9), SK(10), BG(11), HR(12), GR(13), IT(14), HE(15), CZ(16), PL(
        17), CN(18), AR(19), CEANG(20), MK(21), OTH(22), NONE(23);

    public static Nationality getByIndex(final int idx) {
        return Nationality.values()[idx - 1];
    }

    private int index;

    Nationality(final int idx) {
        index = idx;
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        switch (this) {
        case RO:
            return "Români";
        case HU:
            return "Maghiari";
        case RR:
            return "Romi";
        case UK:
            return "Ucraineni";
        case DE:
            return "Germani";
        case TR:
            return "Turci";
        case TT:
            return "Tătari";
        case RU:
            return "Ruși lipoveni";
        case SR:
            return "Sârbi";
        case SK:
            return "Slovaci";
        case BG:
            return "Bulgari";
        case HR:
            return "Croați";
        case GR:
            return "Greci";
        case IT:
            return "Italieni";
        case CZ:
            return "Cehi";
        case HE:
            return "Evrei";
        case PL:
            return "Polonezi";
        case CN:
            return "Chinezi";
        case AR:
            return "Armeni";
        case CEANG:
            return "Ceangăi";
        case MK:
            return "Macedoneni";
        case OTH:
            return "Altă etnie";
        default:
            return "Necunoscut";
        }
    }
}
