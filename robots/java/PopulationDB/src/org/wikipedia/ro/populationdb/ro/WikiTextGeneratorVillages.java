package org.wikipedia.ro.populationdb.ro;

import java.awt.Color;
import java.awt.Paint;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.security.auth.login.FailedLoginException;

import org.hibernate.Session;
import org.wikibase.Wikibase;
import org.wikipedia.Wiki;
import org.wikipedia.ro.populationdb.ro.p2002.Nationality;
import org.wikipedia.ro.populationdb.ro.p2002.PopulationDb2002Entry;
import org.wikipedia.ro.populationdb.ro.p2002.Religion;
import org.wikipedia.ro.populationdb.ro.p2002.UTAType;
import org.wikipedia.ro.populationdb.ua.UAWikiGenerator;
import org.wikipedia.ro.populationdb.ua.dao.Hibernator;
import org.wikipedia.ro.populationdb.util.Executor;
import org.wikipedia.ro.populationdb.util.SysoutExecutor;

public class WikiTextGeneratorVillages {
    private Wiki rowiki;
    private Wikibase dwiki;
    private Executor executor;
    private Hibernator hib;
    private Connection conn;
    private Connection conn2002;
    private final Map<String, Paint> paintMap = new HashMap<String, Paint>();

    private final Map<String, Paint> religionMap = new HashMap<String, Paint>();

    public static void main(String[] args) {
        final WikiTextGeneratorVillages generator = new WikiTextGeneratorVillages();
        try {
            generator.init();
            generator.generateForAllCounties();
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            generator.close();
        }
    }

    private void init() throws IOException, FailedLoginException {
        rowiki = Wiki.newSession("ro.wikipedia.org");
        dwiki = new Wikibase();
        // executor = new WikiEditExecutor(rowiki, dwiki);
        executor = new SysoutExecutor();

        final Properties credentials = new Properties();
        credentials.load(UAWikiGenerator.class.getClassLoader().getResourceAsStream("credentials.properties"));
        final String datauser = credentials.getProperty("UsernameData");
        final String datapass = credentials.getProperty("PasswordData");
        final String user = credentials.getProperty("Username");
        final String pass = credentials.getProperty("Password");
        rowiki.login(user, pass.toCharArray());
        rowiki.setMarkBot(true);
        dwiki.login(datauser, datapass.toCharArray());

        hib = new Hibernator();
        final Session ses = hib.getSession();
        ses.beginTransaction();

        paintMap.put(Nationality.RO.getName(), new Color(85, 85, 255));
        paintMap.put(Nationality.TR.getName(), new Color(255, 85, 85));
        paintMap.put(Nationality.RR.getName(), new Color(85, 255, 255));
        paintMap.put(Nationality.HU.getName(), new Color(85, 255, 85));
        paintMap.put(Nationality.TT.getName(), new Color(128, 0, 0));
        paintMap.put(Nationality.NONE.getName(), new Color(128, 128, 128));
        paintMap.put(Nationality.HE.getName(), new Color(192, 192, 192));
        paintMap.put(Nationality.OTH.getName(), new Color(64, 64, 64));
        blandifyColors(paintMap);

        religionMap.put(Religion.ORTHO.getName(), new Color(85, 85, 255));
        religionMap.put(Religion.MUSL.getName(), new Color(255, 85, 85));
        religionMap.put(Religion.ROM_CATH.getName(), new Color(255, 255, 85));
        religionMap.put(Religion.SR_ORTHO.getName(), new Color(192, 0, 0));
        religionMap.put(Religion.OTH.getName(), new Color(128, 128, 128));
        religionMap.put(Religion.PENTICOST.getName(), new Color(0, 192, 0));
        religionMap.put(Religion.CR_EVANGH.getName(), new Color(255, 255, 64));
        religionMap.put(Religion.UNIT.getName(), new Color(0, 192, 192));
        religionMap.put(Religion.MUSL.getName(), new Color(255, 85, 85));
        blandifyColors(religionMap);

        ses.getTransaction().rollback();
    }

    private <T> void blandifyColors(final Map<T, Paint> map) {
        for (final T key : map.keySet()) {
            final Color color = (Color) map.get(key);
            final int[] colorcomps = new int[3];
            colorcomps[0] = color.getRed();
            colorcomps[1] = color.getGreen();
            colorcomps[2] = color.getBlue();

            for (int i = 0; i < colorcomps.length; i++) {
                if (colorcomps[i] == 0) {
                    colorcomps[i] = 0x3f;
                } else if (colorcomps[i] == 85) {
                    colorcomps[i] = 128;
                } else if (colorcomps[i] == 64) {
                    colorcomps[i] = 85;
                } else if (colorcomps[i] == 128) {
                    colorcomps[i] = 0x9f;
                }
            }
            map.put(key, new Color(colorcomps[0], colorcomps[1], colorcomps[2]));
        }
    }

    private void generateForCounties(final int... ids) {
        for (final int id : ids) {
            generateForCounty(id);
        }
    }

    private void generateForCounty(int countyId) {
        Connection conn = getConnection();
        Connection conn2 = getConnection2002();

        String judet = null;
        try {
            final PreparedStatement cnameStatement =
                conn.prepareStatement("select judet.nume judnume from judet where judet.id=?");
            cnameStatement.setInt(1, countyId);
            final ResultSet cnameRS = cnameStatement.executeQuery();
            if (cnameRS.next()) {
                judet = cnameRS.getString("judnume");
            }
            final PreparedStatement st = conn.prepareStatement(
                "select localitate.siruta id,uta.siruta parentSiruta, localitate.populatie pop,judet.nume judet,uta.name nume_uta, uta.tip tip, localitate.name nume from localitate left join uta on localitate.uta=uta.siruta left join judet on judet.id=uta.judet where uta.judet=? and (uta.tip = 3 or uta.name != localitate.name) order by uta.tip asc, uta.name asc, localitate.name asc");
            st.setInt(1, countyId);
            final ResultSet rs = st.executeQuery();
            while (rs.next()) {
                final int localitateId = rs.getInt("id");
                final UTAType utaType = UTAType.fromId(rs.getInt("tip"));
                final PopulationDb2002Entry uta = new PopulationDb2002Entry();
                uta.setSiruta(localitateId);
                uta.setParentSiruta(rs.getInt("parentSiruta"));

                PreparedStatement countLocalitiesStatement =
                    conn.prepareStatement("select count(*) cnt from localitate where localitate.uta=?");
                countLocalitiesStatement.setInt(1, uta.getParentSiruta());
                ResultSet localitiesCountRS = countLocalitiesStatement.executeQuery();
                if (rs.next() && rs.getInt("cnt") < 2) {
                    continue;
                }

                uta.setName(rs.getString("nume"));
                uta.setPopulation(rs.getInt("pop"));
                uta.setType(utaType);
                uta.setVillage(true);

                final PreparedStatement utaSt = conn2.prepareStatement("select "
                    + "localitate.name Nume,uta.name NumeComuna, judet.nume Judet,uta.populatie Pop_total,nationalitate.name Etnie,uta_nationalitate.populatie Pop_etnie,nationalitate.id natid "
                    + "from localitate left join uta on uta.siruta=localitate.uta" + "left join judet on uta.judet=judet.id "
                    + "left join localitate_nationalitate on localitate_nationalitate.localitate=localitate.siruta "
                    + "left join nationalitate on localitate_nationalitate.nationalitate=nationalitate.id "
                    + "where localitate.siruta=? order by Pop_etnie desc");
                utaSt.setInt(1, uta.getSiruta());

                final ResultSet utaRs = utaSt.executeQuery();
                while (utaRs.next()) {
                    final Nationality nat = Nationality.getByIndex(utaRs.getInt("natid"));
                    final int pop = utaRs.getInt("Pop_etnie");
                    uta.getNationalStructure().put(nat, pop);
                }

                final PreparedStatement utaRel = conn2.prepareStatement("select "
                    + "localitate.name Nume, uta.name NumeComuna,judet.nume Judet,religie.name Religie,uta_religie.populatie Pop_religie,religie.id relid "
                    + "from localitate left join uta on uta.siruta=localitate.uta" + "left join judet on uta.judet=judet.id "
                    + "left join localitate_religie on localitate_religie.localitate=localitate.siruta "
                    + "left join religie on localitate_religie.religie=religie.id "
                    + "where localitate.siruta=? order by Pop_religie desc");
                utaRel.setInt(1, uta.getSiruta());
                final ResultSet utaRelRs = utaRel.executeQuery();
                while (utaRelRs.next()) {
                    final Religion rel = Religion.getByIndex(utaRelRs.getInt("relid"));
                    final int pop = utaRelRs.getInt("Pop_religie");
                    uta.getReligiousStructure().put(rel, pop);
                }
            }

        } catch (Exception e) {
        }

    }

    private void generateForAllCounties() {
    }

    private void close() {
        Session ses;
        if (null != (ses = hib.getSession())) {
            final org.hibernate.Transaction tx = ses.getTransaction();
            if (null != tx) {
                tx.rollback();
            }
        }
        if (null != rowiki) {
            rowiki.logout();
        }
        if (null != dwiki) {
            dwiki.logout();
        }

    }

    private Connection getConnection() {
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

    private Connection getConnection2002() {
        if (null != conn2002) {
            return conn2002;
        }
        try {
            Class.forName("org.postgresql.Driver");
            conn2002 = DriverManager.getConnection("jdbc:postgresql://localhost/populatie_2002", "postgres", "postgres");
            return conn2002;
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
