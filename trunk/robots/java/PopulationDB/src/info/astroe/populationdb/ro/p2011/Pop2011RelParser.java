package info.astroe.populationdb.ro.p2011;

import info.astroe.populationdb.ro.p2002.PopulationDb2002Entry;
import info.astroe.populationdb.ro.p2002.Religion;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

public class Pop2011RelParser {
    private static Connection conn;
    private static int countyId;

    public static void main(final String[] args) {
        if (1 > args.length) {
            System.out.println("Please specify an xlsx file to read populace from.");
            System.exit(1);
        }
        final File infile = new File(args[0]);
        if (!infile.exists() || !infile.isFile()) {
            System.out.println("Please specify an xlsx file to read populace from.");
            System.exit(1);
        }

        FileInputStream infileStream = null;

        try {
            final HSSFWorkbook wb = new HSSFWorkbook(infileStream = new FileInputStream(infile));

            final HSSFSheet sheet = wb.getSheetAt(0);

            final Iterator<Row> rowIterator = sheet.iterator();
            boolean countiesStarted = false;
            while (rowIterator.hasNext()) {
                // read first column; if type not string or length < 2 or written in bold then skip
                PopulationDb2002Entry crtEntry = null;
                final Row row = rowIterator.next();
                final Iterator<Cell> cellIterator = row.cellIterator();
                if (!cellIterator.hasNext()) {
                    continue;
                }

                final Cell nameCell = cellIterator.next();
                if (nameCell.getCellType() != Cell.CELL_TYPE_STRING) {
                    continue;
                }
                String name = nameCell.getStringCellValue();
                if (name.length() < 2) {
                    continue;
                }
                final CellStyle style = nameCell.getCellStyle();
                final HSSFFont usedFont = wb.getFontAt(style.getFontIndex());
                if (usedFont.getBoldweight() > 400) {
                    if (countiesStarted && name.startsWith(" ")) {
                        final String countyName = name.trim();
                        final int retid = getCountyId(countyName);
                        if (0 == retid) {
                            System.out.println("County ID not found for county " + countyName);
                            continue;
                        } else {
                            System.out.println("County is now: (" + countyId + ") " + countyName);
                            countyId = retid;
                        }
                    }
                    if ("ROMANIA".equals(name.trim())) {
                        countiesStarted = true;
                    }
                    continue;
                }
                name = name.trim();
                if (name.startsWith("MUNICIPIUL")) {
                    name = name.replace("MUNICIPIUL ", "");
                }
                if (name.startsWith("ORAS")) {
                    name = name.replace("ORAS ", "");
                }
                name = name.trim();

                // read population; if this isn't a number, then skip
                if (!cellIterator.hasNext()) {
                    continue;
                }
                final Cell popCell = cellIterator.next();
                if (popCell.getCellType() != Cell.CELL_TYPE_NUMERIC) {
                    continue;
                }
                final int population = (int) popCell.getNumericCellValue();
                //System.out.println("UTA " + name + " with " + population + " inhabitants");

                crtEntry = new PopulationDb2002Entry();
                crtEntry.setName(name);
                crtEntry.setPopulation(population);

                // read population for each religion
                int idx = 1;
                while (cellIterator.hasNext()) {
                    final Cell popNatCell = cellIterator.next();
                    if (popNatCell.getCellType() == Cell.CELL_TYPE_NUMERIC && idx < 23) {
                        final Religion nat = Religion.getByIndex(idx);
                        crtEntry.getReligiousStructure().put(nat, (int) popNatCell.getNumericCellValue());
                    }
                    idx++;
                }

                savePopulation(crtEntry);
            }

        } catch (final FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(infileStream);
            closeConnection(conn);
        }
    }

    private static int getCountyId(final String countyName) {
        final Connection conn = getConnection();
        try {
            final PreparedStatement st = conn.prepareStatement("select id from judet where ?=replace(replace(replace(replace(replace(judet.nume, 'Î', 'I'), 'Ț', 'T'), 'Â', 'A'), 'Ă', 'A'), '�?', 'S')");
            st.setString(1, countyName);
            final ResultSet rs = st.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }

        } catch (final SQLException e) {
            e.printStackTrace();
            closeConnection(conn);
            System.exit(1);
        }
        return 0;
    }

    private static void closeConnection(final Connection conn2) {
        if (null == conn2) {
            return;
        }
        try {
            conn2.close();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }

    private static void savePopulation(final PopulationDb2002Entry crtEntry) throws SQLException {
        final Connection conn = getConnection();

        final int entryId = getUtaId(crtEntry.getName());
        if (0 == entryId) {
            System.out.println("ID not found in DB for UTA " + crtEntry.getName() + ", county id " + countyId);
            return;
        }

        final Map<Religion, Integer> religiousStructure = crtEntry.getReligiousStructure();
        for (final Religion rel : religiousStructure.keySet()) {
            final PreparedStatement stRel = conn
                .prepareStatement("insert into uta_religie (uta,religie,populatie) values(?,?,?)");
            stRel.setInt(1, entryId);
            stRel.setInt(2, rel.getIndex());
            stRel.setInt(3, religiousStructure.get(rel));
            stRel.executeUpdate();
        }
    }

    private static int getUtaId(final String name) {
        final Connection conn = getConnection();
        try {
            final PreparedStatement st = conn.prepareStatement("select uta.siruta id from uta left join judet on uta.judet=judet.id where ?=replace(replace(replace(replace(replace(uta.name, 'Î', 'I'), 'Ț', 'T'), 'Â', 'A'), 'Ă', 'A'), '�?', 'S') and judet.id=?");
            st.setString(1, name);
            st.setInt(2, countyId);
            final ResultSet rs = st.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (final SQLException e) {
            e.printStackTrace();
            closeConnection(conn);
            System.exit(1);
        }
        return 0;
    }

    private static Connection getConnection() {
        if (null != conn) {
            return conn;
        }
        try {
            Class.forName("org.postgresql.Driver");
            conn = DriverManager.getConnection("jdbc:postgresql://localhost/populatie_2011", "postgres", "postgres");
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
