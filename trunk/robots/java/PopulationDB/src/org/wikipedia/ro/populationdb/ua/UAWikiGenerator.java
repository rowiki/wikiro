package org.wikipedia.ro.populationdb.ua;

import java.awt.Color;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.security.auth.login.FailedLoginException;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.wikibase.Wikibase;
import org.wikipedia.Wiki;
import org.wikipedia.ro.populationdb.ua.dao.Hibernator;
import org.wikipedia.ro.populationdb.ua.model.Language;
import org.wikipedia.ro.populationdb.util.WikiEditExecutor;

public class UAWikiGenerator {

    public static void main(final String[] args) throws Exception {
        final UAWikiGenerator generator = new UAWikiGenerator();
        try {
            generator.init();
            // generator.generateRegions();
        } finally {
            generator.close();
        }
    }

    private Wiki rowiki;
    private Wikibase dwiki;
    private WikiEditExecutor executor;
    private Hibernator hib;
    private Session ses;
    private final Map<Language, Color> nationColorMap = new HashMap<Language, Color>();
    private Map<Object, Object> nationNameMap;
    private Wiki ukwiki;

    private void init() throws FailedLoginException, IOException {
        rowiki = new Wiki("ro.wikipedia.org");
        ukwiki = new Wiki("hu.wikipedia.org");
        dwiki = new Wikibase();
        executor = new WikiEditExecutor(rowiki, dwiki);
        // executor = new SysoutExecutor();

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
        ses = hib.getSession();
        ses.beginTransaction();

        assignColorToNationality("Români", new Color(85, 85, 255));
        assignColorToNationality("Turci", new Color(255, 85, 85));
        assignColorToNationality("Romi", new Color(85, 255, 255));
        assignColorToNationality("Maghiari", new Color(85, 255, 85));
        assignColorToNationality("Bulgari", new Color(0, 192, 0));
        assignColorToNationality("Evrei", new Color(192, 192, 192));
        assignColorToNationality("Croați", new Color(32, 32, 192));
        assignColorToNationality("Sârbi", new Color(192, 32, 32));
        assignColorToNationality("Bosniaci", new Color(64, 64, 128));
        assignColorToNationality("Germani", new Color(255, 85, 255));
        assignColorToNationality("Vlahi", new Color(128, 128, 255));
        assignColorToNationality("Ucraineni", new Color(255, 255, 85));
        assignColorToNationality("Ruteni", new Color(255, 255, 128));
        assignColorToNationality("Ruși", new Color(192, 85, 85));
        assignColorToNationality("Italieni", new Color(64, 192, 64));
        assignColorToNationality("Sloveni", new Color(32, 32, 128));
        assignColorToNationality("Slovaci", new Color(48, 48, 160));
        assignColorToNationality("Cehi", new Color(128, 128, 32));
        assignColorToNationality("Afiliați religios", new Color(255, 255, 255));
        assignColorToNationality("Neclasificat", new Color(192, 192, 192));
        assignColorToNationality("Armeni", new Color(0x62, 0x46, 0x46));
        assignColorToNationality("Necunoscut", new Color(192, 192, 192));
        assignColorToNationality("Alții", new Color(192, 192, 192));
        blandifyColors(nationColorMap);
    }

    private void assignColorToNationality(final String languageName, final Color color) throws HibernateException {
        final Language nat = hib.getLanguageByName(languageName);
        if (null != nat) {
            nationColorMap.put(nat, color);
            nationNameMap.put(nat.getName(), nat);
        }
    }

    private void close() {
        if (null != ses) {
            final org.hibernate.Transaction tx = ses.getTransaction();
            if (null != tx) {
                tx.rollback();
            }
        }
        if (null != rowiki) {
            rowiki.logout();
        }
        if (null != ukwiki) {
            ukwiki.logout();
        }
        if (null != dwiki) {
            dwiki.logout();
        }

    }

    private static <T extends Object> void blandifyColors(final Map<T, Color> colorMap) {
        for (final T key : colorMap.keySet()) {
            final Color color = colorMap.get(key);
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
            colorMap.put(key, new Color(colorcomps[0], colorcomps[1], colorcomps[2]));
        }
    }
}
