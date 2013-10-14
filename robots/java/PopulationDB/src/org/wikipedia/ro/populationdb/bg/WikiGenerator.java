package org.wikipedia.ro.populationdb.bg;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.StringUtils.defaultString;

import java.awt.Color;
import java.awt.Paint;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.security.auth.login.FailedLoginException;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.jfree.data.general.DefaultPieDataset;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;
import org.wikipedia.Wiki;
import org.wikipedia.ro.populationdb.bg.model.Nationality;
import org.wikipedia.ro.populationdb.bg.model.Obshtina;
import org.wikipedia.ro.populationdb.util.Utilities;

public class WikiGenerator {

    private static final String findNationalityByNameQueryString = "from Nationality nat where nat.nume=:name";
    private final Wiki rowiki = new Wiki("ro.wikipedia.org");
    private final Wiki bgwiki = new Wiki("bg.wikipedia.org");
    private final Properties credentials = new Properties();
    private Session ses = null;
    private final Map<Nationality, Paint> nationColorMap = new HashMap<Nationality, Paint>();
    private final Map<String, Nationality> nationNameMap = new HashMap<String, Nationality>();
    private final Map<String, String> nationLinkMap = new HashMap<String, String>() {
        {
            put("Bulgari", "[[bulgari]]");
            put("Turci", "[[Turcii din Bulgaria|turci]]");
            put("Romi", "[[Romii din Bulgaria|romi]]");
            put("Altele", "alte naționalități");
            put("Nicio identificare", "persoane neidentificate etnic");
        }
    };

    public static void main(final String[] args) {
        final WikiGenerator generator = new WikiGenerator();
        try {
            generator.login();
            generator.generateObshtinas();
        } catch (final FailedLoginException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            generator.cleanup();
        }
    }

    private void generateObshtinas() {
        final Criteria allObshtinasCriteria = ses.createCriteria(Obshtina.class);
        final List<Obshtina> obshtinas = allObshtinasCriteria.list();

        final STGroup templateGroup = new STGroupFile("templates/bg/section_obshtina.stg");
        for (final Obshtina obshtina : obshtinas) {
            final StringBuilder demographics = new StringBuilder("");
            final ST piechart = templateGroup.getInstanceOf("piechart");
            piechart.add("nume", obshtina.getNumeRo());

            final Map<Nationality, Integer> ethnicStructure = obshtina.getEthnicStructure();
            final StringBuilder pieChartProps = new StringBuilder();
            final DefaultPieDataset dataset = new DefaultPieDataset();
            int totalPerNat = 0;
            final Map<String, Integer> smallGroups = new HashMap<String, Integer>();
            Nationality otherNat = null;
            for (final Nationality nat : nationColorMap.keySet()) {
                final int natpop = defaultIfNull(ethnicStructure.get(nat), 0);
                if (natpop * 100.0 / obshtina.getPopulation() > 1.0 && !StringUtils.equals(nat.getNume(), "Altele")) {
                    dataset.setValue(nat.getNume(), natpop);
                    totalPerNat += natpop;
                } else if (natpop * 100.0 / obshtina.getPopulation() <= 1.0 && !StringUtils.equals(nat.getNume(), "Altele")) {
                    smallGroups.put(nat.getNume(), natpop);
                } else if (StringUtils.equals(nat.getNume(), "Altele")) {
                    otherNat = nat;
                }
            }
            if (1 < smallGroups.size()) {
                dataset.setValue("Altele", obshtina.getPopulation() - totalPerNat);
            } else {
                for (final String natname : smallGroups.keySet()) {
                    dataset.setValue(natname, smallGroups.get(natname));
                }
                dataset.setValue("Altele", ethnicStructure.get(otherNat));
            }

            int i = 1;
            for (final Object k : dataset.getKeys()) {
                pieChartProps.append("\n|label");
                pieChartProps.append(i);
                pieChartProps.append('=');
                pieChartProps.append(k.toString());
                pieChartProps.append("|value");
                pieChartProps.append(i);
                pieChartProps.append('=');
                pieChartProps.append((int) (100 * (dataset.getValue(k.toString()).doubleValue() * 100.0 / obshtina
                    .getPopulation())) / 100.0);
                pieChartProps.append("|color");
                pieChartProps.append(i);
                pieChartProps.append('=');
                final Color color = (Color) nationColorMap.get(nationNameMap.get(k.toString()));
                pieChartProps.append(Utilities.colorToHtml(color));
                i++;
            }
            piechart.add("props", pieChartProps.toString());
            demographics.append(piechart.render());

            final ST intro = templateGroup.getInstanceOf("introTmpl");
            intro.add("nume", obshtina.getNumeRo());
            intro.add("populatie", obshtina.getPopulation());
            demographics.append(intro.render());

            System.out.println(demographics.toString());
        }
    }

    private void login() throws IOException, FailedLoginException {
        credentials.load(WikiGenerator.class.getClassLoader().getResourceAsStream("credentials.properties"));
        final String user = credentials.getProperty("Username");
        final String pass = credentials.getProperty("Password");
        rowiki.login(user, pass.toCharArray());
        rowiki.setMarkBot(true);

        BGPopulationParser.initHibernate();
        ses = BGPopulationParser.sessionFactory.getCurrentSession();
        ses.beginTransaction();

        final Query findNationalityByName;
        final List<Nationality> nats;
        assignColorToNationality("Bulgari", new Color(85, 255, 85));
        assignColorToNationality("Turci", new Color(255, 85, 85));
        assignColorToNationality("Romi", new Color(85, 255, 255));
        assignColorToNationality("Altele", new Color(64, 64, 64));
        assignColorToNationality("Nicio identificare", new Color(192, 192, 192));
    }

    private void assignColorToNationality(final String nationalityName, final Paint color) throws HibernateException {
        final Query findNationalityByName = ses.createQuery(findNationalityByNameQueryString);
        findNationalityByName.setParameter("name", nationalityName);
        final List<Nationality> nats = findNationalityByName.list();
        if (nats.size() > 0) {
            nationColorMap.put(nats.get(0), color);
            nationNameMap.put(nats.get(0).getNume(), nats.get(0));
        }
    }

    private void cleanup() {
        if (null != rowiki) {
            rowiki.logout();
        }
        if (null != ses) {
            final Transaction tx = ses.getTransaction();
            if (null != tx) {
                tx.rollback();
            }
        }
    }

    private String translateNationalityToLink(final Nationality nat) {
        return defaultString(nationLinkMap.get(nat.getNume()), nat.getNume());
    }

}
