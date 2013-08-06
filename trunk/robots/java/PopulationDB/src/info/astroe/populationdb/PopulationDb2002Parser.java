package info.astroe.populationdb;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;

public class PopulationDb2002Parser {
    private static int countyIndex = 0;
    private static int communeSiruta = 0;
    private static Connection conn;

    public static void main(final String[] args) throws SQLException {
        if (args.length < 1) {
            System.err.println("File argument not specified.");
            System.exit(1);
        }

        final File infile = new File(args[0]);
        if (!infile.exists() || !infile.isFile()) {
            System.err.println("Specified file not found or unopenable.");
            System.exit(2);
        }

        FileInputStream infileStream = null;

        try {
            final HSSFWorkbook wb = new HSSFWorkbook(infileStream = new FileInputStream(infile));

            final HSSFSheet sheet = wb.getSheetAt(0);

            final Iterator<Row> rowIterator = sheet.iterator();
            while (rowIterator.hasNext()) {
                PopulationDb2002Entry crtEntry = null;
                final Row row = rowIterator.next();
                final Iterator<Cell> cellIterator = row.cellIterator();
                if (!cellIterator.hasNext()) {
                    continue;
                }

                final Cell sirutaCell = cellIterator.next();
                if (sirutaCell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                    crtEntry = new PopulationDb2002Entry();
                    final int siruta = (int) sirutaCell.getNumericCellValue();
                    System.out.println("Siruta=" + siruta);
                    crtEntry.setSiruta(siruta);
                } else {
                    if (!cellIterator.hasNext()) {
                        continue;
                    }
                    final Cell ctyNameCell = cellIterator.next();
                    if (ctyNameCell.getCellType() == Cell.CELL_TYPE_STRING
                        && (sirutaCell.getCellType() != Cell.CELL_TYPE_STRING || sirutaCell.getStringCellValue().length() == 0)) {
                        final String ctyName = ctyNameCell.getStringCellValue();
                        if ("ROMANIA".equals(ctyName)) {
                            continue;
                        }
                        saveCounty(ctyName);
                    }
                    continue;
                }

                if (!cellIterator.hasNext()) {
                    continue;
                }
                final Cell nameCell = cellIterator.next();
                if (nameCell.getCellType() == Cell.CELL_TYPE_STRING) {
                    String fullName = nameCell.getStringCellValue();
                    final String[] fullNameParts = fullName.split(" ");
                    if (fullNameParts[0].equals("ORAS")) {
                        fullName = fullName.substring("ORAS".length() + 1);
                        crtEntry.setType(UTAType.ORAS);
                    } else if (fullNameParts[0].equals("MUNICIPIUL")) {
                        fullName = fullName.substring("MUNICIPIUL".length() + 1);
                        crtEntry.setType(UTAType.MUNICIPIU);
                    }
                    crtEntry.setName(fullName);
                    final CellStyle style = nameCell.getCellStyle();
                    final HSSFFont usedFont = wb.getFontAt(style.getFontIndex());
                    if (400 >= usedFont.getBoldweight()) {
                        crtEntry.setVillage(true);
                        crtEntry.setParentSiruta(communeSiruta);
                    } else if (crtEntry.getType() == null) {
                        crtEntry.setType(UTAType.COMUNA);
                        communeSiruta = crtEntry.getSiruta();
                    } else {
                        communeSiruta = crtEntry.getSiruta();
                    }
                    // System.out.println("BOLDWEIGHT: " + usedFont.getBoldweight());
                } else {
                    continue;
                }
                if (!cellIterator.hasNext()) {
                    continue;
                }
                final Cell totalPopCell = cellIterator.next();
                if (totalPopCell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                    crtEntry.setPopulation((int) totalPopCell.getNumericCellValue());
                } else {
                    continue;
                }

                int idx = 1;
                while (cellIterator.hasNext()) {
                    final Cell popCell = cellIterator.next();
                    if (popCell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                        final Nationality nat = Nationality.getByIndex(idx);
                        crtEntry.getNationalStructure().put(nat, (int) popCell.getNumericCellValue());
                    }
                    idx++;
                }

                saveToDb(crtEntry);
                // TODO save to postgresdb
            }

        } catch (final FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(infileStream);
            if (null != PopulationDb2002Parser.conn) {
                PopulationDb2002Parser.conn.close();
            }
        }
    }

    private static void saveCounty(final String ctyName) {
        final Connection conn = getConnection();
        countyIndex++;
        try {
            final PreparedStatement st = conn.prepareStatement("insert into judet (id, nume) values(?,?)");
            st.setInt(1, countyIndex);
            st.setString(2, ctyName);
            st.executeUpdate();
        } catch (final SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void saveToDb(final PopulationDb2002Entry crtEntry) throws SQLException {
        final Connection conn = getConnection();
        PreparedStatement st = null;
        try {
            if (crtEntry.isVillage()) {
                st = conn.prepareStatement("insert into localitate (siruta, name, uta, populatie) values(?,?,?,?)");
                st.setInt(1, crtEntry.getSiruta());
                st.setString(2, crtEntry.getName());
                st.setInt(3, crtEntry.getParentSiruta());
                st.setInt(4, crtEntry.getPopulation());
                st.executeUpdate();

                final Map<Nationality, Integer> nationalStructure = crtEntry.getNationalStructure();
                for (final Nationality nat : nationalStructure.keySet()) {
                    final PreparedStatement stNat = conn
                        .prepareStatement("insert into localitate_nationalitate (localitate,nationalitate,populatie) values(?,?,?)");
                    stNat.setInt(1, crtEntry.getSiruta());
                    stNat.setInt(2, nat.getIndex());
                    stNat.setInt(3, nationalStructure.get(nat));
                    stNat.executeUpdate();
                }
            } else {
                st = conn.prepareStatement("insert into uta (siruta, name, judet, populatie, tip) values(?,?,?,?,?)");
                st.setInt(1, crtEntry.getSiruta());
                st.setString(2, crtEntry.getName());
                st.setInt(3, countyIndex);
                st.setInt(4, crtEntry.getPopulation());
                st.setInt(5, crtEntry.getType().getId());
                st.executeUpdate();
                final Map<Nationality, Integer> nationalStructure = crtEntry.getNationalStructure();
                for (final Nationality nat : nationalStructure.keySet()) {
                    final PreparedStatement stNat = conn
                        .prepareStatement("insert into uta_nationalitate (uta,nationalitate,populatie) values(?,?,?)");
                    stNat.setInt(1, crtEntry.getSiruta());
                    stNat.setInt(2, nat.getIndex());
                    stNat.setInt(3, nationalStructure.get(nat));
                    stNat.executeUpdate();
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static Connection getConnection() {
        if (null != conn) {
            return conn;
        }
        try {
            Class.forName("org.postgresql.Driver");
            conn = DriverManager.getConnection("jdbc:postgresql://localhost/populatie_2002", "postgres", "postgres");
            return conn;
        } catch (final ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

}
