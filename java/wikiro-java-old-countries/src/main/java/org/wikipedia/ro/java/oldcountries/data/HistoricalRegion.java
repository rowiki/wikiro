package org.wikipedia.ro.java.oldcountries.data;

import java.util.List;

import org.wikibase.WikibasePropertyFactory;
import org.wikibase.data.Claim;
import org.wikibase.data.Entity;
import org.wikibase.data.Item;

import static org.wikipedia.ro.java.oldcountries.data.CountryPeriod.*;

public enum HistoricalRegion
{
    MUNTENIA_REG_IST(List.of(RO, RSR, RPR, RO_REGAT, PRINC_UNITE, MUNTENIA), "Q207388"),
    OLTENIA(List.of(RO, RSR, RPR, RO_REGAT, PRINC_UNITE, POST_AUSTRIAN_OLTENIA, AUSTRIAN_OLTENIA, PRE_AUSTRIAN_OLTENIA), "Q208629"),
    BRAILA(List.of(RO, RSR, RPR, RO_REGAT, PRINC_UNITE, POST_OTTOMAN_BRAILA_GIURGIU, OTTOMAN_BRAILA, PRE_OTTOMAN_BRAILA), "Q207388"),
    DOBROGEA(List.of(RO, RSR, RPR, RO_REGAT, POST_OTTOMAN_DOBROGEA, OTTOMAN_DOBROGEA), "Q2673270"),
    GIURGIU_TURNU(List.of(RO, RSR, RPR, RO_REGAT, PRINC_UNITE, POST_OTTOMAN_BRAILA_GIURGIU, OTTOMAN_GIURGIU, PRE_OTTOMAN_GIURGIU), "Q207388"),
    MOLDOVA(List.of(RO, RSR, RPR, RO_REGAT, PRINC_UNITE, MOLDOVA_VEST), "Q209754"),
    BUCOVINA(List.of(RO, RSR, RPR, POST_AUT_HU_BUCOVINA, AUT_HU_BUCOVINA, AUSTRIA_IMP_TRANSILVANIA_ET_AL, HABSBURG_BUCOVINA, MOLDOVA_BUCOVINA), "Q105206"),
    BANAT_VEST(List.of(RO, RSR, RPR, BANAT_RO, BANAT_YU, BANAT_REP, HU_REP_TRANSILVANIA_ET_AL, AUT_HU_TRANSILVANIA_ET_AL, AUSTRIA_IMP_TRANSILVANIA_ET_AL, HABSBURG_BANAT, OTTOMAN_WEST_BANAT, PRE_OTTOMAN_WEST_BANAT, EAST_HUNGARIAN_KINGDOM, HUNGARY_KINGDOM_TRANSILVANIA_ET_AL), "Q170143"),
    BANAT_EST(List.of(RO, RSR, RPR, BANAT_RO, BANAT_YU, BANAT_REP, HU_REP_TRANSILVANIA_ET_AL, AUT_HU_TRANSILVANIA_ET_AL, AUSTRIA_IMP_TRANSILVANIA_ET_AL, HABSBURG_BANAT, OTTOMAN_EAST_BANAT, PRE_OTTOMAN_EAST_BANAT, EAST_HUNGARIAN_KINGDOM, HUNGARY_KINGDOM_TRANSILVANIA_ET_AL), "Q170143"),
    TRANSILVANIA_SUD(List.of(RO, RSR, RPR, SOUTHERN_TRANSYLVANIA, HU_REP_TRANSILVANIA_ET_AL, AUT_HU_TRANSILVANIA_ET_AL, AUSTRIA_IMP_TRANSILVANIA_ET_AL, HABSBURG_TRANSYLVANIA, TRANSYLVANIA_PRINCIPAT, EAST_HUNGARIAN_KINGDOM, HUNGARY_KINGDOM_TRANSILVANIA_ET_AL), "Q39473"),
    TRANSILVANIA_NORD(List.of(RO, RSR, RPR, POST_WWII_NORTHERN_TRANSYLVANIA, WWII_NORTHERN_TRANSYLVANIA, PRE_WWII_NORTHERN_TRANSYLVANIA, HU_REP_TRANSILVANIA_ET_AL, AUT_HU_TRANSILVANIA_ET_AL, AUSTRIA_IMP_TRANSILVANIA_ET_AL, HABSBURG_TRANSYLVANIA, TRANSYLVANIA_PRINCIPAT, EAST_HUNGARIAN_KINGDOM, HUNGARY_KINGDOM_TRANSILVANIA_ET_AL), "Q39473"),
    CRISANA_SUD(List.of(RO, RSR, RPR, SOUTHERN_TRANSYLVANIA, HU_REP_TRANSILVANIA_ET_AL, AUT_HU_TRANSILVANIA_ET_AL, AUSTRIA_IMP_TRANSILVANIA_ET_AL, HABSBURG_CRISANA, OTTOMAN_SOUTH_CRISANA, PRE_OTTOMAN_SOUTH_CRISANA, HUNGARY_KINGDOM_TRANSILVANIA_ET_AL), "Q268034"),
    CRISANA_NORD(List.of(RO, RSR, RPR, SOUTHERN_TRANSYLVANIA, HU_REP_TRANSILVANIA_ET_AL, AUT_HU_TRANSILVANIA_ET_AL, AUSTRIA_IMP_TRANSILVANIA_ET_AL, HABSBURG_CRISANA, OTTOMAN_NORTH_CRISANA, TRANSYLVANIAN_NORTH_CRISANA, EAST_HUNGARIAN_KINGDOM, HUNGARY_KINGDOM_TRANSILVANIA_ET_AL), "Q268034");

    List<CountryPeriod> countries;
    String qId;

    private HistoricalRegion(List<CountryPeriod> countries, String qId)
    {
        this.countries = countries;
        this.qId = qId;
    }
    
    public List<CountryPeriod> getCountries() {
        return countries;
    }
    
    public Claim getHistoricalRegionClaim() {
        Claim claim = new Claim();
        claim.setProperty(WikibasePropertyFactory.getWikibaseProperty("P6885"));
        claim.setType("wikibase-item");
        claim.setValue(new Item(new Entity(qId)));
        return claim;
    }
}
