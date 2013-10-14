package org.wikipedia.ro.populationdb.hr;

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.length;
import static org.apache.commons.lang3.StringUtils.replace;
import static org.apache.commons.lang3.StringUtils.split;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.trim;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.hibernate.Session;
import org.wikipedia.ro.populationdb.hr.dao.Hibernator;
import org.wikipedia.ro.populationdb.hr.model.Commune;
import org.wikipedia.ro.populationdb.hr.model.County;
import org.wikipedia.ro.populationdb.hr.model.Nationality;
import org.wikipedia.ro.populationdb.hr.model.Religion;

public class HRPopulationParser {

    private final File ethnicFile, religiousFile;
    private final List<String> nationalitiesList = Arrays.asList("Croați", "Albanezi", "Austrieci", "Bosniaci", "Bulgari",
        "Muntenegreni", "Cehi", "Maghiari", "Macedoneni", "Germani", "Polonezi", "Romi", "Români", "Ruși", "Ruteni",
        "Slovaci", "Sloveni", "Sârbi", "Italieni", "Turci", "Ucraineni", "Vlahi", "Evrei", "Afiliați regional",
        "Afiliați religios", "Neclasificat", "Nedeclarat", "Necunoscut");
    private final List<String> religionsList = Arrays.asList("Catolici", "Ortodocși", "Protestanți", "Alți creștini",
        "Musulmani", "Iudaici", "Religii orientale", "Alte religii", "Agnostici și sceptici", "Fără religie și atei",
        "Nu au declarat religia", "Necunoscută");

    private Hibernator hib;

    public HRPopulationParser(final File ethnicFile, final File religiousFile) {
        this.ethnicFile = ethnicFile;
        this.religiousFile = religiousFile;
    }

    public static void main(final String[] args) {
        if (args.length < 2) {
            System.out.println("Missing two arguments representing files with ethnic and religious composition");
            System.exit(1);
        }
        final File ethnicFile = new File(args[0]);
        final File religiousFile = new File(args[1]);
        if (!ethnicFile.exists() || !ethnicFile.isFile() || !religiousFile.exists() || !religiousFile.isFile()) {
            System.out.println("Missing two arguments representing files with ethnic and religious composition");
            System.exit(1);
        }

        final HRPopulationParser parser = new HRPopulationParser(ethnicFile, religiousFile);
        parser.parse();
    }

    private void parse() {
        hib = new Hibernator();

        parseEthnicData();
        parseReligiousData();
    }

    private void parseReligiousData() {
        FileInputStream xlsIS = null;
        try {
            final HSSFWorkbook wb = new HSSFWorkbook(xlsIS = new FileInputStream(religiousFile));
            final HSSFSheet sheet = wb.getSheetAt(0);

            final Iterator<Row> rowIterator = sheet.iterator();

            County crtCounty = null;
            final Session ses = hib.getSession();
            ses.beginTransaction();
            for (Row crtRow = rowIterator.next(); rowIterator.hasNext(); crtRow = rowIterator.next()) {
                final Iterator<Cell> cellIterator = crtRow.cellIterator();
                final Cell firstCell = cellIterator.next();
                if (firstCell.getCellType() != Cell.CELL_TYPE_STRING) {
                    continue;
                }
                final String firstCellString = trim(firstCell.getStringCellValue());
                if (startsWith(firstCellString, "County of")) {
                    crtCounty = hib.getCountyByName(translateCountyName(firstCellString));
                } else {
                    continue;
                }

                if (!cellIterator.hasNext()) {
                    continue;
                }
                final Cell secondCell = cellIterator.next();
                if (secondCell.getCellType() != Cell.CELL_TYPE_STRING || length(secondCell.getStringCellValue()) == 0) {
                    continue;
                }
                final boolean isTown = StringUtils.equals(secondCell.getStringCellValue(), "Town");
                final Cell thirdCell = cellIterator.next();
                final String communeName = thirdCell.getStringCellValue();
                final Commune commune = hib.getCommuneByName(communeName, crtCounty, isTown ? 1 : 0);
                commune.setName(communeName);
                commune.setTown(isTown ? 1 : 0);
                commune.setCounty(crtCounty);

                final Cell fourthCell = cellIterator.next();
                commune.setPopulation((int) fourthCell.getNumericCellValue());
                int religionIndex = 0;
                while (cellIterator.hasNext() && religionIndex < religionsList.size()) {
                    final Cell percentCell = cellIterator.next();
                    if (!cellIterator.hasNext()) {
                        break;
                    }
                    final Cell popCell = cellIterator.next();
                    int pop = 0;
                    if (popCell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                        pop = (int) popCell.getNumericCellValue();
                    }

                    final Religion nat = hib.getReligionByName(religionsList.get(religionIndex));
                    commune.getReligiousStructure().put(nat, pop);

                    religionIndex++;
                }
                hib.saveCommune(commune);
            }
            ses.getTransaction().commit();
        } catch (final FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(xlsIS);
        }
    }

    private void parseEthnicData() {
        FileInputStream xlsIS = null;
        try {
            final HSSFWorkbook wb = new HSSFWorkbook(xlsIS = new FileInputStream(ethnicFile));
            final HSSFSheet sheet = wb.getSheetAt(0);

            final Iterator<Row> rowIterator = sheet.iterator();

            County crtCounty = null;
            final Session ses = hib.getSession();
            ses.beginTransaction();
            for (Row crtRow = rowIterator.next(); rowIterator.hasNext(); crtRow = rowIterator.next()) {
                final Iterator<Cell> cellIterator = crtRow.cellIterator();
                final Cell firstCell = cellIterator.next();
                if (firstCell.getCellType() != Cell.CELL_TYPE_STRING) {
                    continue;
                }
                final String firstCellString = trim(firstCell.getStringCellValue());
                if (startsWith(firstCellString, "County of")) {
                    crtCounty = hib.getCountyByName(translateCountyName(firstCellString));
                } else {
                    continue;
                }

                if (!cellIterator.hasNext()) {
                    continue;
                }
                final Cell secondCell = cellIterator.next();
                if (secondCell.getCellType() != Cell.CELL_TYPE_STRING || length(secondCell.getStringCellValue()) == 0) {
                    continue;
                }
                final boolean isTown = StringUtils.equals(secondCell.getStringCellValue(), "Town");
                final Cell thirdCell = cellIterator.next();
                final String communeName = thirdCell.getStringCellValue();
                final Commune commune = hib.getCommuneByName(communeName, crtCounty, isTown ? 1 : 0);
                commune.setName(communeName);
                commune.setTown(isTown ? 1 : 0);
                commune.setCounty(crtCounty);

                final Cell fourthCell = cellIterator.next();
                commune.setPopulation((int) fourthCell.getNumericCellValue());
                int nationalityIndex = 0;
                while (cellIterator.hasNext() && nationalityIndex < nationalitiesList.size()) {
                    final Cell percentCell = cellIterator.next();
                    if (!cellIterator.hasNext()) {
                        break;
                    }
                    final Cell popCell = cellIterator.next();
                    int pop = 0;
                    if (popCell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                        pop = (int) popCell.getNumericCellValue();
                    }

                    final Nationality nat = hib.getNationalityByName(nationalitiesList.get(nationalityIndex));
                    commune.getEthnicStructure().put(nat, pop);

                    nationalityIndex++;
                }
                hib.saveCommune(commune);
            }
            ses.getTransaction().commit();
        } catch (final FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(xlsIS);
        }
    }

    private String translateCountyName(final String firstCellString) {
        String ret = replace(firstCellString, "County of ", "");
        ret = trim(ret);
        final String[] nameParts = split(ret, '-');
        for (int i = 0; i < nameParts.length; i++) {
            if (equalsIgnoreCase(nameParts[i], "Sirmium")) {
                nameParts[i] = "Srijem";
            } else if (equalsIgnoreCase(nameParts[i], "Dalmatia")) {
                nameParts[i] = "Dalmația";
            }
        }
        return join(nameParts, '-');
    }

}
