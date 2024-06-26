package org.wikipedia.ro.populationdb.hu;

import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.leftPad;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.hibernate.Session;
import org.wikipedia.ro.populationdb.hu.dao.Hibernator;
import org.wikipedia.ro.populationdb.hu.model.County;
import org.wikipedia.ro.populationdb.hu.model.District;
import org.wikipedia.ro.populationdb.hu.model.Nationality;
import org.wikipedia.ro.populationdb.hu.model.Religion;
import org.wikipedia.ro.populationdb.hu.model.Settlement;

public class HUPopulationParser {

    private final Map<Integer, File> countyHistPopFiles = new HashMap<Integer, File>();
    private final Map<Integer, File> countyNatPopFiles = new HashMap<Integer, File>();
    private final Map<Integer, File> countyRelPopFiles = new HashMap<Integer, File>();
    private Hibernator hib;

    private final Map<Integer, String> counties = new HashMap<Integer, String>() {
        {
            put(2, "Baranya");
            put(3, "Bács-Kiskun");
            put(4, "Békés");
            put(5, "Borsod-Abaúj-Zemplén");
            put(6, "Csongrád");
            put(7, "Fejér");
            put(8, "Győr-Moson-Sopron");
            put(9, "Hajdú-Bihar");
            put(10, "Heves");
            put(11, "Komárom-Esztergom");
            put(12, "Nógrád");
            put(13, "Pesta");
            put(14, "Somogy");
            put(15, "Szabolcs-Szatmár-Bereg");
            put(16, "Jász-Nagykun-Szolnok");
            put(17, "Tolna");
            put(18, "Vas");
            put(19, "Veszprém");
            put(20, "Zala");
        }
    };
    private final List<Integer> censi = Arrays.asList(1870, 1880, 1890, 1900, 1910, 1920, 1930, 1941, 1949, 1960, 1969,
        1970, 1980, 1990, 2001);

    public static void main(final String[] args) {
        if (args.length < 1) {
            System.out.println("Missing argument to point directory with data xls files");
            System.exit(1);
        }
        final File inDir = new File(args[0]);
        if (!inDir.exists() || !inDir.isDirectory()) {
            System.out.println("Specified argument is not a directory");
            System.exit(1);
        }
        int countyIndex = 2;

        final HUPopulationParser parser = new HUPopulationParser();

        boolean stop = false;
        do {
            final List<String> fileNameParts = new ArrayList<String>();
            fileNameParts.add(leftPad(String.valueOf(countyIndex), 2, '0'));
            fileNameParts.add("4");
            for (int i = 0; i < 3; i++) {
                fileNameParts.add("1");
            }
            final String historicalPopFileName = join(fileNameParts, '_') + ".xls";
            final File historicalPopFile = new File(inDir, historicalPopFileName);

            fileNameParts.set(3, "6");
            final String nationalityPopFileName = join(fileNameParts, '_') + ".xls";
            final File nationalityPopFile = new File(inDir, nationalityPopFileName);

            fileNameParts.set(3, "7");
            final String religionPopFileName = join(fileNameParts, '_') + ".xls";
            final File religionPopFile = new File(inDir, religionPopFileName);
            for (final File f : Arrays.asList(historicalPopFile, religionPopFile, nationalityPopFile)) {
                if (!f.exists() || !f.isFile()) {
                    stop = true;
                }
            }
            if (!stop) {
                parser.countyHistPopFiles.put(countyIndex, historicalPopFile);
                parser.countyNatPopFiles.put(countyIndex, nationalityPopFile);
                parser.countyRelPopFiles.put(countyIndex, religionPopFile);
            }
            countyIndex++;
        } while (!stop);

        parser.parseAllCounties();
    }

    private void parseAllCounties() {
        try {
            hib = new Hibernator();

            final List<Integer> countyIds = new ArrayList<Integer>();
            countyIds.addAll(countyHistPopFiles.keySet());
            Collections.sort(countyIds);
            for (final Integer eachId : countyIds) {
                this.parseCounty(eachId);
            }
        } finally {
            IOUtils.closeQuietly(hib);
        }
    }

    private void parseCounty(final int countyIndex) {
        System.out.println("Generating historical data for county " + countyIndex);
        this.parseHistoricalData(countyIndex);
        System.out.println("Generating nationality data for county " + countyIndex);
        this.parseNationalityData(countyIndex);
        System.out.println("Generating religion data for county " + countyIndex);
        this.parseReligionData(countyIndex);
    }

    private void parseReligionData(final int countyIndex) {
        final File xlsFile = countyRelPopFiles.get(countyIndex);
        int currentUnitType = 4;
        final Map<String, District> districtCodes = new HashMap<String, District>();
        final Session ses = hib.getSession();
        ses.beginTransaction();
        hib.getCountyByName(counties.get(countyIndex));
        try {
            final HSSFWorkbook wb = new HSSFWorkbook(new FileInputStream(xlsFile));
            final HSSFSheet sheet = wb.getSheetAt(0);

            final Iterator<Row> rowIterator = sheet.iterator();
            boolean foundDistricts = false;
            boolean foundCities = false;
            for (Row crtRow = rowIterator.next(); rowIterator.hasNext(); crtRow = rowIterator.next()) {
                final Iterator<Cell> cellIterator = crtRow.cellIterator();
                final Cell firstCell = cellIterator.next();
                if (firstCell.getCellType() != Cell.CELL_TYPE_STRING) {
                    continue;
                }
                final String firstCellString = firstCell.getStringCellValue();
                if (startsWith(firstCellString, "Járások")) {
                    foundDistricts = true;
                    continue;
                } else if (!foundDistricts) {
                    continue;
                }

                if (startsWith(firstCellString, "Települések adatai")) {
                    foundCities = true;
                    continue;
                }
                if (startsWith(firstCellString, "Megyeszékhely") && foundCities) {
                    currentUnitType = 4;
                    continue;
                }
                if (startsWith(firstCellString, "Megyei jogú város") && foundCities) {
                    currentUnitType = 3;
                    continue;
                }
                if (startsWith(firstCellString, "Többi város") && foundCities) {
                    currentUnitType = 2;
                    continue;
                }
                if (startsWith(firstCellString, "Községek, nagyközségek") && foundCities) {
                    currentUnitType = 1;
                    continue;
                }
                if (foundDistricts && !foundCities && startsWith(firstCellString, "J")
                    && firstCell.getCellStyle().getIndention() > 0) {
                    final String districtName = substringAfter(firstCellString, " ");
                    final String districtCode = substringBefore(firstCellString, " ");
                    final District district = hib.getDistrictByName(districtName, counties.get(countyIndex));
                    districtCodes.put(districtCode, district);
                    continue;
                }
                if (foundDistricts && foundCities && startsWith(firstCellString, "J")
                    && firstCell.getCellStyle().getIndention() > 0) {
                    final String districtCode = substringBefore(firstCellString, " ");
                    final District district = districtCodes.get(districtCode);
                    final String settlementName = substringAfter(substringAfter(firstCellString, " "), " ");
                    final Settlement settlement = hib.getSettlementByName(settlementName, district, currentUnitType);

                    final List<String> columnHeader1 = Arrays.asList("Romano-catolici", "Greco-catolici", "Ortodocși",
                        "Reformați", "Luterani", "Mozaici", "Alte religii", "Fără religie", "Atei", "Nu au răspuns");

                    Cell popCell = null;

                    if (cellIterator.hasNext()) {
                        popCell = cellIterator.next();
                    }

                    for (int i = 0; i < columnHeader1.size() && cellIterator.hasNext(); i++) {
                        popCell = cellIterator.next();
                        if (popCell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                            final Religion nat = hib.getReligionByName(columnHeader1.get(i));
                            settlement.getReligiousStructure().put(nat, (int) popCell.getNumericCellValue());
                        }
                    }
                    hib.saveSettlement(settlement);

                    continue;
                }

            }
            ses.getTransaction().commit();
        } catch (final FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void parseNationalityData(final int countyIndex) {
        final File xlsFile = countyNatPopFiles.get(countyIndex);
        int currentUnitType = 4;
        final Map<String, District> districtCodes = new HashMap<String, District>();
        final Session ses = hib.getSession();
        ses.beginTransaction();
        hib.getCountyByName(counties.get(countyIndex));
        try {
            final HSSFWorkbook wb = new HSSFWorkbook(new FileInputStream(xlsFile));
            final HSSFSheet sheet = wb.getSheetAt(0);

            final Iterator<Row> rowIterator = sheet.iterator();
            boolean foundDistricts = false;
            boolean foundCities = false;
            for (Row crtRow = rowIterator.next(); rowIterator.hasNext(); crtRow = rowIterator.next()) {
                final Iterator<Cell> cellIterator = crtRow.cellIterator();
                final Cell firstCell = cellIterator.next();
                if (firstCell.getCellType() != Cell.CELL_TYPE_STRING) {
                    continue;
                }
                final String firstCellString = firstCell.getStringCellValue();
                if (startsWith(firstCellString, "Járások")) {
                    foundDistricts = true;
                    continue;
                } else if (!foundDistricts) {
                    continue;
                }

                if (startsWith(firstCellString, "Települések adatai")) {
                    foundCities = true;
                    continue;
                }
                if (startsWith(firstCellString, "Megyeszékhely") && foundCities) {
                    currentUnitType = 4;
                    continue;
                }
                if (startsWith(firstCellString, "Megyei jogú város") && foundCities) {
                    currentUnitType = 3;
                    continue;
                }
                if (startsWith(firstCellString, "Többi város") && foundCities) {
                    currentUnitType = 2;
                    continue;
                }
                if (startsWith(firstCellString, "Községek, nagyközségek") && foundCities) {
                    currentUnitType = 1;
                    continue;
                }
                if (foundDistricts && !foundCities && startsWith(firstCellString, "J")
                    && firstCell.getCellStyle().getIndention() > 0) {
                    final String districtName = substringAfter(firstCellString, " ");
                    final String districtCode = substringBefore(firstCellString, " ");
                    final District district = hib.getDistrictByName(districtName, counties.get(countyIndex));
                    districtCodes.put(districtCode, district);
                    continue;
                }
                if (foundDistricts && foundCities && startsWith(firstCellString, "J")
                    && firstCell.getCellStyle().getIndention() > 0) {
                    final String districtCode = substringBefore(firstCellString, " ");
                    final District district = districtCodes.get(districtCode);
                    final String settlementName = substringAfter(substringAfter(firstCellString, " "), " ");
                    final Settlement settlement = hib.getSettlementByName(settlementName, district, currentUnitType);

                    final List<String> columnHeader1 = Arrays.asList("Maghiari", "Bulgari", "Romi", "Greci", "Croați",
                        "Polonezi", "Germani", "Armeni", "Români", "Ruteni", "Sârbi", "Slovaci", "Sloveni", "Ucraineni");
                    final List<String> columnHeader2 = Arrays.asList("Arabi", "Chinezi", "Ruși", "Vietnamezi", "Alții");

                    Cell popCell = null;
                    for (int i = 0; i < columnHeader1.size() && cellIterator.hasNext(); i++) {
                        popCell = cellIterator.next();
                        if (popCell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                            final Nationality nat = hib.getNationalityByName(columnHeader1.get(i));
                            settlement.getEthnicStructure().put(nat, (int) popCell.getNumericCellValue());
                        }
                    }
                    if (cellIterator.hasNext()) {
                        popCell = cellIterator.next();
                    }

                    for (int i = 0; i < columnHeader2.size() && cellIterator.hasNext(); i++) {
                        popCell = cellIterator.next();
                        if (popCell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                            final Nationality nat = hib.getNationalityByName(columnHeader2.get(i));
                            settlement.getEthnicStructure().put(nat, (int) popCell.getNumericCellValue());
                        }
                    }
                    hib.saveSettlement(settlement);

                    continue;
                }

            }
            ses.getTransaction().commit();
        } catch (final FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private void parseHistoricalData(final int countyIndex) {
        final File xlsFile = countyHistPopFiles.get(countyIndex);
        int currentUnitType = 4;
        final Map<String, District> districtCodes = new HashMap<String, District>();
        final Session ses = hib.getSession();
        ses.beginTransaction();
        final County county = hib.getCountyByName(counties.get(countyIndex));
        try {
            final HSSFWorkbook wb = new HSSFWorkbook(new FileInputStream(xlsFile));
            final HSSFSheet sheet = wb.getSheetAt(0);

            final Iterator<Row> rowIterator = sheet.iterator();
            boolean foundDistricts = false;
            boolean foundCities = false;
            for (Row crtRow = rowIterator.next(); rowIterator.hasNext(); crtRow = rowIterator.next()) {
                final Iterator<Cell> cellIterator = crtRow.cellIterator();
                final Cell firstCell = cellIterator.next();
                if (firstCell.getCellType() != Cell.CELL_TYPE_STRING) {
                    continue;
                }
                final String firstCellString = firstCell.getStringCellValue();
                if (startsWith(firstCellString, "Járások")) {
                    foundDistricts = true;
                    continue;
                } else if (!foundDistricts) {
                    continue;
                }

                if (startsWith(firstCellString, "Települések adatai")) {
                    foundCities = true;
                    continue;
                }
                if (startsWith(firstCellString, "Megyeszékhely") && foundCities) {
                    currentUnitType = 4;
                    continue;
                }
                if (startsWith(firstCellString, "Megyei jogú város") && foundCities) {
                    currentUnitType = 3;
                    continue;
                }
                if (startsWith(firstCellString, "Többi város") && foundCities) {
                    currentUnitType = 2;
                    continue;
                }
                if (startsWith(firstCellString, "Községek, nagyközségek") && foundCities) {
                    currentUnitType = 1;
                    continue;
                }
                if (foundDistricts && !foundCities && startsWith(firstCellString, "J")
                    && firstCell.getCellStyle().getIndention() > 0) {
                    final String districtName = substringAfter(firstCellString, " ");
                    final String districtCode = substringBefore(firstCellString, " ");
                    final District district = hib.getDistrictByName(districtName, counties.get(countyIndex));
                    districtCodes.put(districtCode, district);
                    hib.storeDistrict(district, county);
                    continue;
                }
                if (foundDistricts && foundCities && startsWith(firstCellString, "J")
                    && firstCell.getCellStyle().getIndention() > 0) {
                    final String districtCode = substringBefore(firstCellString, " ");
                    final District district = districtCodes.get(districtCode);
                    final String settlementName = substringAfter(substringAfter(firstCellString, " "), " ");
                    final Settlement settlement = hib.getSettlementByName(settlementName, district, currentUnitType);

                    final Cell secondCell = cellIterator.next();
                    if (secondCell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                        settlement.setArea(secondCell.getNumericCellValue());
                    }
                    Cell popCell = null;
                    for (int i = 0; i < censi.size() && cellIterator.hasNext(); i++) {
                        popCell = cellIterator.next();
                        if (popCell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                            settlement.getHistoricalPopulation().put(censi.get(i), (int) popCell.getNumericCellValue());
                        }
                    }
                    if (cellIterator.hasNext()) {
                        popCell = cellIterator.next();
                        if (popCell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                            settlement.setPopulation((int) popCell.getNumericCellValue());
                        }
                    }
                    settlement.setDistrict(district);
                    hib.saveSettlement(settlement);

                    continue;
                }

            }
            ses.getTransaction().commit();
        } catch (final FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
