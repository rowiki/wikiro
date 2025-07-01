package org.wikipedia.ro.java.oldcountries.data;

import java.time.LocalDate;
import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Settlement extends InceptedWbObject
{
    private HistoricalRegion region;
    private List<CountryPeriod> countryPeriods;
    private UAT uat;
}
