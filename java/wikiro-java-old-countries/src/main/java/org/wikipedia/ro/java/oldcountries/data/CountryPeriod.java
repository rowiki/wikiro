package org.wikipedia.ro.java.oldcountries.data;

import java.time.LocalDate;

import org.wikibase.data.Time;
import static org.wikipedia.ro.java.oldcountries.data.HistoricCountry.*;

public enum CountryPeriod
{
    RO(ROMANIA_CRT, new Time(LocalDate.of(1989, 12, 22)), null),
    RSR(ROMANIA_RS, new Time(LocalDate.of(1965, 8, 21)), new Time(LocalDate.of(1989, 12, 22))),
    RPR(ROMANIA_RP, new Time(LocalDate.of(1947, 12, 30)), new Time(LocalDate.of(1965, 8, 21))),
    RO_REGAT(ROMANIA_REGAT, new Time(LocalDate.of(1866, 5, 10)).setCalendarModelToJulian(), new Time(LocalDate.of(1947, 12, 30)).setCalendarModelToJulian()),
    PRINC_UNITE(ROMANIA_PRINCIPAT, new Time(LocalDate.of(1859, 1, 24)).setCalendarModelToJulian(), new Time(LocalDate.of(1866, 5, 10)).setCalendarModelToJulian()),
    MUNTENIA(TARA_ROMANEASCA, null, new Time(LocalDate.of(1859, 1, 24)).setCalendarModelToJulian()),
    MOLDOVA_VEST(MOLDOVA, null, new Time(LocalDate.of(1859, 1, 24)).setCalendarModelToJulian()),

    OTTOMAN_BRAILA(IMP_OTOMAN, new Time(1540).setCalendarModelToJulian(), new Time(LocalDate.of(1829, 9, 2)).setCalendarModelToJulian()),
    OTTOMAN_GIURGIU(IMP_OTOMAN, new Time(1417).setCalendarModelToJulian(), new Time(LocalDate.of(1829, 9, 2)).setCalendarModelToJulian()),
    PRE_OTTOMAN_BRAILA(TARA_ROMANEASCA, null, new Time(1540).setCalendarModelToJulian()),
    PRE_OTTOMAN_GIURGIU(TARA_ROMANEASCA, null, new Time(1417).setCalendarModelToJulian()),
    POST_OTTOMAN_BRAILA_GIURGIU(TARA_ROMANEASCA, new Time(LocalDate.of(1829, 9, 2)).setCalendarModelToJulian(), new Time(LocalDate.of(1859, 1, 24)).setCalendarModelToJulian()),

    POST_AUSTRIAN_OLTENIA(TARA_ROMANEASCA, new Time(LocalDate.of(1739, 8, 21)), new Time(LocalDate.of(1859, 1, 24)).setCalendarModelToJulian()),
    AUSTRIAN_OLTENIA(IMP_HABSBURG, new Time(LocalDate.of(1718, 7, 21)), new Time(LocalDate.of(1739, 8, 21))),
    PRE_AUSTRIAN_OLTENIA(TARA_ROMANEASCA, null, new Time(LocalDate.of(1718, 7, 21))),

    POST_OTTOMAN_DOBROGEA(ROMANIA_PRINCIPAT, new Time(LocalDate.of(1878, 7, 1)).setCalendarModelToJulian(), new Time(LocalDate.of(1866, 5, 10)).setCalendarModelToJulian()),
    OTTOMAN_DOBROGEA(IMP_OTOMAN, null, new Time(LocalDate.of(1878, 7, 1)).setCalendarModelToJulian()),
    
    POST_AUT_HU_BUCOVINA(ROMANIA_REGAT, new Time(LocalDate.of(1918, 11, 28)), new Time(LocalDate.of(1947, 12, 30))),
    AUT_HU_BUCOVINA(AUSTRO_UNGARIA, new Time(LocalDate.of(1867, 3, 30)), new Time(LocalDate.of(1918, 11, 28))),
    
    POST_WWII_NORTHERN_TRANSYLVANIA(ROMANIA_REGAT, new Time(LocalDate.of(1944, 10, 25)), new Time(LocalDate.of(1947, 12, 30))),
    WWII_NORTHERN_TRANSYLVANIA(UNGARIA_HORTHY, new Time(LocalDate.of(1940, 8, 30)), new Time(LocalDate.of(1944, 10, 25))),
    PRE_WWII_NORTHERN_TRANSYLVANIA(ROMANIA_REGAT, new Time(LocalDate.of(1918, 12, 1)), new Time(LocalDate.of(1940, 8, 30))),
    
    POST_RO_YU_EXCHANGE(ROMANIA_REGAT, new Time(LocalDate.of(1924, 4, 10)), new Time(LocalDate.of(1947, 12, 30))),
    PRE_RO_YU_EXCHANGE(STAT_SR_HR_SL, new Time(LocalDate.of(1918, 12, 1)), new Time(LocalDate.of(1924, 4, 10))),
    
    BANAT_RO(ROMANIA_REGAT, new Time(LocalDate.of(1919, 8, 4)), new Time(LocalDate.of(1947, 12, 30))),
    BANAT_YU(STAT_SR_HR_SL, new Time(LocalDate.of(1918, 12, 1)), new Time(LocalDate.of(1919, 8, 4))),
    BANAT_REP(BANAT_REPUBLIC, new Time(LocalDate.of(1918, 10, 31)), new Time(LocalDate.of(1918, 12, 1))),
    
    SOUTHERN_TRANSYLVANIA(ROMANIA_REGAT, new Time(LocalDate.of(1918, 12, 1)), new Time(LocalDate.of(1947, 12, 30))),
    HU_REP_TRANSILVANIA_ET_AL(UNGARIA_1REP, new Time(LocalDate.of(1918, 11, 16)), new Time(LocalDate.of(1918, 12, 1))),
    AUT_HU_TRANSILVANIA_ET_AL(AUSTRO_UNGARIA, new Time(LocalDate.of(1867, 3, 30)), new Time(LocalDate.of(1918, 11, 16))),
    AUSTRIA_IMP_TRANSILVANIA_ET_AL(IMP_AUSTRIA, new Time(LocalDate.of(1804, 8, 11)), new Time(LocalDate.of(1867, 3, 30))),
    
    HABSBURG_TRANSYLVANIA(IMP_HABSBURG, new Time(LocalDate.of(1711, 4, 29)), new Time(LocalDate.of(1804, 8, 11))),
    HABSBURG_BUCOVINA(IMP_HABSBURG, new Time(LocalDate.of(1774, 7, 21)), new Time(LocalDate.of(1804, 8, 11))),
    HABSBURG_BANAT(IMP_HABSBURG, new Time(LocalDate.of(1718, 7, 21)), new Time(LocalDate.of(1804, 8, 11))),
    MOLDOVA_BUCOVINA(MOLDOVA, null, new Time(LocalDate.of(1774, 7, 21))),
    
    OTTOMAN_WEST_BANAT(IMP_OTOMAN, new Time(LocalDate.of(1552, 7, 26)).setCalendarModelToJulian(), new Time(LocalDate.of(1718, 7, 21))),
    OTTOMAN_EAST_BANAT(IMP_OTOMAN,new Time(1658l), new Time(LocalDate.of(1718, 7, 21))),
    PRE_OTTOMAN_WEST_BANAT(UNGARIA_EST_REGAT, new Time(LocalDate.of(1526, 11, 11)).setCalendarModelToJulian(), new Time(LocalDate.of(1552, 7, 26)).setCalendarModelToJulian()),
    PRE_OTTOMAN_EAST_BANAT(TRANSILVANIA, new Time(LocalDate.of(1570, 8, 16)).setCalendarModelToJulian(), new Time(1658l)),
    
    HABSBURG_CRISANA(IMP_HABSBURG, new Time(LocalDate.of(1699, 1, 26)), new Time(LocalDate.of(1804, 8, 11))),
    OTTOMAN_SOUTH_CRISANA(IMP_OTOMAN, new Time(1551), new Time(LocalDate.of(1699, 1, 26))),
    PRE_OTTOMAN_SOUTH_CRISANA(UNGARIA_EST_REGAT, new Time(LocalDate.of(1526, 11, 11)).setCalendarModelToJulian(),new Time(1551)),
    
    OTTOMAN_NORTH_CRISANA(IMP_OTOMAN, new Time(1660), new Time(LocalDate.of(1699, 1, 26))),
    
    TRANSYLVANIA_PRINCIPAT(TRANSILVANIA, new Time(LocalDate.of(1570, 8, 16)).setCalendarModelToJulian(), new Time(LocalDate.of(1711, 4, 29))),
    TRANSYLVANIAN_NORTH_CRISANA(TRANSILVANIA, new Time(LocalDate.of(1570, 8, 16)).setCalendarModelToJulian(), new Time(1660)),
    EAST_HUNGARIAN_KINGDOM(UNGARIA_EST_REGAT, new Time(LocalDate.of(1526, 11, 11)).setCalendarModelToJulian(), new Time(LocalDate.of(1570, 8, 16)).setCalendarModelToJulian()),
    
    HUNGARY_KINGDOM_TRANSILVANIA_ET_AL(UNGARIA_REGAT, null, new Time(LocalDate.of(1526, 11, 11)).setCalendarModelToJulian());
    
    HistoricCountry country;
    Time startTime;
    Time endTime;

    private CountryPeriod(HistoricCountry country, Time startTime, Time endTime)
    {
        this.country = country;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
