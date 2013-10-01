package info.astroe.populationdb.hu;

import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.leftPad;
import static org.apache.commons.lang3.StringUtils.replaceChars;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import info.astroe.populationdb.hu.model.County;
import info.astroe.populationdb.hu.model.District;
import info.astroe.populationdb.util.HibernateUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

public class HUPopulationParser {

    private final List<File> countyHistPopFiles = new ArrayList<File>();
    private final List<File> countyNatPopFiles = new ArrayList<File>();
    private final List<File> countyRelPopFiles = new ArrayList<File>();

    private final Map<Integer, String> counties = new HashMap<Integer, String>() {
        {
            put(2, "Baranya");
            put(3, "Bács-Kiskun");
            put(4, "Békés");
            put(5, "Borsod-Abaúj-Zemplén");
            put(6, "Csongrád");
            put(7, "Fejér");
            put(8, "Gyõr-Moson-Sopron");
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
    private SessionFactory sessionFactory;

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
                parser.countyHistPopFiles.add(historicalPopFile);
                parser.countyNatPopFiles.add(nationalityPopFile);
                parser.countyRelPopFiles.add(religionPopFile);
            }
            countyIndex++;
        } while (!stop);

        parser.parseAllCounties();
    }

    private void parseAllCounties() {
        sessionFactory = initHibernate();
        for (int i = 0; i < countyHistPopFiles.size(); i++) {
            this.parseCounty(i);
        }
    }

    private void parseCounty(final int countyIndex) {
        this.parseHistoricalData(countyIndex);
        this.parseNationalityData(countyIndex);
        this.parseReligionData(countyIndex);
    }

    private void parseReligionData(final int countyIndex) {
        // TODO Auto-generated method stub

    }

    private void parseNationalityData(final int countyIndex) {
        // TODO Auto-generated method stub

    }

    private void parseHistoricalData(final int countyIndex) {
        final File xlsFile = countyHistPopFiles.get(countyIndex);
        FileInputStream xlsIS = null;
        int currentUnitType = 4;
        final Map<String, District> districtCodes = new HashMap<String, District>();
        final County county = getCountyByName(counties.get(countyIndex));
        try {
            final HSSFWorkbook wb = new HSSFWorkbook(xlsIS = new FileInputStream(xlsFile));
            final HSSFSheet sheet = wb.getSheetAt(0);

            final Iterator<Row> rowIterator = sheet.iterator();
            boolean foundDistricts = false;
            boolean foundCities = false;
            for (final Row crtRow = rowIterator.next(); rowIterator.hasNext();) {
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
                } else if (!foundCities) {
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
                    final District district = getDistrictByName(districtName, counties.get(countyIndex));
                    districtCodes.put(districtCode, district);
                    continue;
                }
                if (foundDistricts && foundCities && startsWith(firstCellString, "J")
                    && firstCell.getCellStyle().getIndention() > 0) {
                    final String districtCode = substringBefore(firstCellString, " ");
                    final District district = districtCodes.get(districtCode);
                    final String settlementName = substringAfter(substringAfter(firstCellString, " "), " ");
                    continue;
                }

            }
        } catch (final FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private County getCountyByName(final String countyName) {
        County ret = null;
        final Session ses = sessionFactory.getCurrentSession();
        final Query findCounty = ses.createQuery("from County county where county.name=:countyName");
        findCounty.setParameter("countyName", countyName);
        final List<County> res = findCounty.list();
        if (0 == res.size()) {
            ret = new County();
            ret.setName(countyName);
            ses.beginTransaction();
            ses.save(ret);
            ses.getTransaction().commit();
        } else {
            ret = res.get(0);
        }
        return ret;
    }

    private District getDistrictByName(final String districtName, final String countyName) {
        final Session ses = sessionFactory.getCurrentSession();
        final Query findDistrict = ses
            .createQuery("from District district where district.name=:districtName and district.county.name=:countyName");
        findDistrict.setParameter("districtName", districtName);
        findDistrict.setParameter("countyName", countyName);
        final List<District> res = findDistrict.list();
        District ret = null;
        if (0 == res.size()) {
            ret = new District();
            ret.setName(districtName);
            ret.setCounty(getCountyByName(counties.get(countyName)));
            ses.beginTransaction();
            ses.save(ret);
            ses.getTransaction().commit();
        } else {
            ret = res.get(0);
        }
        return ret;
    }

    private SessionFactory initHibernate() {
        final String packageName = this.getClass().getPackage().getName();
        final URL url = HUPopulationParser.class.getResource(replaceChars(packageName, '.', '/') + "/hibernate.cfg.xml");
        File f;
        try {
            f = new File(url.toURI());
            return HibernateUtil.getSessionFactory(f);
        } catch (final URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
}
