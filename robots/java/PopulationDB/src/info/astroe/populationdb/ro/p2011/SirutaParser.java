package info.astroe.populationdb.ro.p2011;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;

import au.com.bytecode.opencsv.CSVReader;

public class SirutaParser {
    private static Connection conn;
    private static int crtJudetId;

    public static void main(final String[] args) {
        if (1 > args.length) {
            System.out.println("Please specify a csv file to read siruta from.");
            System.exit(1);
        }
        final File infile = new File(args[0]);
        if (!infile.exists() || !infile.isFile()) {
            System.out.println("Please specify a csv file to read siruta from.");
            System.exit(1);
        }
        FileReader inreader = null;
        try {
            inreader = new FileReader(infile);
            final CSVReader reader = new CSVReader(inreader, ';');
            String[] eachLine = reader.readNext();
            while ((eachLine = reader.readNext()) != null) {
                storeSirutaToDb(eachLine);
            }

        } catch (final FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(inreader);
            if (conn != null) {
                try {
                    conn.close();
                } catch (final SQLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
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

    private static void storeSirutaToDb(final String[] eachLine) {
        final Connection conn = getConnection();
        try {
            if ("40".equals(eachLine[5])) {
                // judet
                final Connection conn2 = DriverManager.getConnection("jdbc:postgresql://localhost/populatie_2002",
                    "postgres", "postgres");
                String nume = eachLine[1].substring("JUDEÈšUL ".length()).trim();
                final PreparedStatement getJudId = conn2.prepareStatement("select id from judet where nume=?");
                getJudId.setString(1, removeDiacritics(nume));
                final ResultSet rs = getJudId.executeQuery();
                if (rs.next()) {
                    crtJudetId = rs.getInt("id");
                }
                conn2.close();
                if ("MUNICIPIUL BUCUREÈ?TI".equals(eachLine[1])) {
                    nume = "BUCUREÈ?TI";
                    crtJudetId++;

                }

                final PreparedStatement insertJud = conn.prepareStatement("insert into judet (id,nume) values (?,?)");
                insertJud.setInt(1, crtJudetId);
                insertJud.setString(2, nume);
                insertJud.executeUpdate();

            } else if (Arrays.asList("1", "2", "3", "4").contains(eachLine[5])
                || ("MUNICIPIUL BUCUREÈ?TI".equals(eachLine[1]) && "9".equals(eachLine[5]))) {
                int utaType = Integer.parseInt(eachLine[5]);
                utaType = (9 == utaType) ? 1 : utaType;
                utaType = 1 + ((utaType - 1) % 3);
                String name = null;
                switch (utaType) {
                case 1:
                    System.out.println(eachLine[1] + " identificat ca municipiu");
                    name = eachLine[1].substring("MUNICIPIUL ".length()).trim();
                    break;
                case 2:
                    System.out.println(eachLine[1] + " identificat ca oraÈ™");
                    name = eachLine[1].substring("ORAÈ? ".length()).trim();
                    break;
                default:
                    System.out.println(eachLine[1] + " identificatÄ? drept comunÄ?");
                    name = eachLine[1].trim();
                    break;
                }

                final PreparedStatement st = conn
                    .prepareStatement("insert into uta (siruta,name,judet,tip) values (?,?,?,?)");
                st.setInt(1, Integer.parseInt(eachLine[0]));
                st.setString(2, name);
                st.setInt(3, crtJudetId);
                st.setInt(4, utaType);
                st.executeUpdate();
            } else {
                final PreparedStatement st = conn
                    .prepareStatement("insert into localitate (siruta,name,uta) values (?,?,?)");
                st.setInt(1, Integer.parseInt(eachLine[0]));
                st.setString(2, eachLine[1]);
                st.setInt(3, Integer.parseInt(eachLine[4]));
                st.executeUpdate();
            }
        } catch (final SQLException e) {
            e.printStackTrace();
            if (null != SirutaParser.conn) {
                try {
                    SirutaParser.conn.close();
                } catch (final SQLException e1) {
                    e1.printStackTrace();
                }
            }
            System.exit(1);
        }
    }

    private static String removeDiacritics(final String s) {
        return s.replace("Ä‚", "A").replace("È?", "S").replace("Èš", "T").replace("ÃŽ", "I").replace("Ã‚", "A");
    }
}
