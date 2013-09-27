package info.astroe.populationdb.bg;

import static info.astroe.populationdb.util.Utilities.capitalizeName;
import static info.astroe.populationdb.util.Utilities.transliterateBg;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.trim;
import info.astroe.populationdb.bg.model.Obshtina;
import info.astroe.populationdb.bg.model.Region;
import info.astroe.populationdb.bg.model.Settlement;
import info.astroe.populationdb.util.HibernateUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

public class BGPopulationParser {
    private static SessionFactory sessionFactory = initHibernate();

    public static void main(final String[] args) {
        // TODO Auto-generated method stub
        if (2 > args.length) {
            System.out.println("Please specify two xlsx files (age, ethnos) to read populace from.");
            System.exit(1);
        }
        final File infileAge = new File(args[0]);
        if (!infileAge.exists() || !infileAge.isFile()) {
            System.out.println("Please specify a proper age xlsx file to read populace from.");
            System.exit(1);
        }
        final File infileEthnos = new File(args[1]);
        if (!infileAge.exists() || !infileAge.isFile()) {
            System.out.println("Please specify a proper ethnos xlsx file to read populace from.");
            System.exit(1);
        }

        parseAgeFile(infileAge);
    }

    private static void parseAgeFile(final File infileAge) {
        // final SessionFactory hibernateSf = initHibernate();
        FileInputStream infileStream = null;
        try {
            final HSSFWorkbook wb = new HSSFWorkbook(infileStream = new FileInputStream(infileAge));

            final HSSFSheet sheet = wb.getSheetAt(0);

            final Iterator<Row> rowIterator = sheet.iterator();

            Region currentRegion = null;
            Obshtina currentObshtina = null;

            while (rowIterator.hasNext()) {
                final Row row = rowIterator.next();
                final Iterator<Cell> cellIterator = row.cellIterator();
                if (!cellIterator.hasNext()) {
                    continue;
                }

                final Cell nameCell = cellIterator.next();
                if (nameCell.getCellType() != Cell.CELL_TYPE_STRING) {
                    continue;
                }
                final String name = nameCell.getStringCellValue();
                if (name.length() < 2) {
                    continue;
                }

                // read population; if this isn't a number, then skip
                if (!cellIterator.hasNext()) {
                    continue;
                }
                final Cell popCell = cellIterator.next();
                if (popCell.getCellType() != Cell.CELL_TYPE_NUMERIC) {
                    continue;
                }
                final int population = (int) popCell.getNumericCellValue();
                if (population > 7e6) {
                    continue;
                }
                final CellStyle cellStyle = nameCell.getCellStyle();
                final int indent = cellStyle.getIndention();
                final Session ses = sessionFactory.getCurrentSession();
                ses.beginTransaction();
                if (0 == indent) {
                    currentRegion = new Region();
                    currentRegion.setNumeBg(trim(capitalizeName(name)));
                    currentRegion.setNumeRo(capitalizeName(transliterateBg(trim(name))));
                    System.out.println("Region " + currentRegion.getNumeBg() + "/" + currentRegion.getNumeRo() + " pop "
                        + population);
                    ses.save(currentRegion);
                } else if (1 == indent) {
                    currentObshtina = new Obshtina();
                    currentObshtina.setNumeBg(capitalizeName(trim(name)));
                    currentObshtina.setNumeRo(capitalizeName(transliterateBg(trim(name))));
                    currentObshtina.setPopulation(population);
                    currentObshtina.setRegion(currentRegion);
                    currentRegion.getObshtinas().add(currentObshtina);
                    ses.save(currentObshtina);
                } else {
                    final Settlement village = new Settlement();
                    village.setTown(startsWith(name, "ГР"));
                    final String villageName = substringAfter(name, ".");
                    village.setNumeBg(capitalizeName(trim(villageName)));
                    village.setNumeRo(capitalizeName(transliterateBg(trim(villageName))));
                    village.setPopulation(population);
                    village.setObshtina(currentObshtina);
                    currentObshtina.getSettlements().add(village);
                    ses.save(village);
                }
                ses.getTransaction().commit();
            }
        } catch (final FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(infileStream);
            sessionFactory.close();
        }
    }

    private static SessionFactory initHibernate() {
        return HibernateUtil.getSessionFactory(null);
    }

}
