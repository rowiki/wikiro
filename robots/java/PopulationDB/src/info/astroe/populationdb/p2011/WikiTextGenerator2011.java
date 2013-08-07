package info.astroe.populationdb.p2011;

import info.astroe.populationdb.Nationality;
import info.astroe.populationdb.PopulationDb2002Entry;
import info.astroe.populationdb.Religion;
import info.astroe.populationdb.UTAType;

import java.awt.Color;
import java.awt.Paint;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot3D;
import org.jfree.data.general.DefaultPieDataset;
import org.wikipedia.Wiki;
import org.wikipedia.Wiki.Revision;

public class WikiTextGenerator2011 {

    private static Connection conn;

    private static Connection conn2002;

    private final static Map<String, Paint> paintMap = new HashMap<String, Paint>();

    private final static Map<String, Paint> religionMap = new HashMap<String, Paint>();

    private static Pattern regexCCR = Pattern
        .compile("\\{\\{((C|c)final utie Comune România|(C|c)aset(a|ă) comune Rom(a|â)nia)\\s*(\\|(?:\\{\\{[^{}]*+\\}\\}|[^{}])*+)?\\}\\}\\s*");
    private static Pattern regexInfocAsezare = Pattern
        .compile("\\{\\{(?:(?:C|c)asetă așezare|(?:I|i)nfocaseta Așezare|(?:C|c)utie așezare)\\s*(\\|(?:\\{\\{[^{}]*+\\}\\}|[^{}])*+)?\\}\\}\\s*");
    private static Pattern footnotesRegex = Pattern
        .compile("\\{\\{(?:(?:L|l)istănote|(?:R|r)eflist)|(?:\\<\\s*references\\s*\\/\\>)");

    private static int pop2002 = -1;
    private static Wiki wiki;

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

    private static Connection getConnection2002() {
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

    public static void main(final String[] args) {
        generateCounty(/* 10, 11, 12, 14 , 26 , */28 /* ,41 */);

        /*
         * for (int i = 1; i < 41; i++) { generateCounty(i); }
         */
        closeConnection(conn);
        closeConnection(conn2002);
    }

    private static void generateCounty(final int... ids) {
        for (final int id : ids) {
            generateCounty(id);
        }
    }

    private static void generateCounty(final int countyId) {
        final Connection cn = getConnection();
        try {
            final PreparedStatement st = cn
                .prepareStatement("select uta.siruta id,uta.tip tip,uta.populatie pop,judet.nume judet,uta.name nume from uta left join judet on judet.id=uta.judet where uta.judet=?");
            st.setInt(1, countyId);
            final ResultSet rs = st.executeQuery();

            while (rs.next()) {
                final int utaId = rs.getInt("id");
                final UTAType utaType = UTAType.fromId(rs.getInt("tip"));
                final String judet = rs.getString("judet");
                final PopulationDb2002Entry uta = new PopulationDb2002Entry();
                uta.setSiruta(utaId);
                uta.setName(rs.getString("nume"));
                uta.setPopulation(rs.getInt("pop"));
                uta.setType(utaType);
                uta.setVillage(false);

                final PreparedStatement utaSt = cn
                    .prepareStatement("select "
                        + "uta.name Nume,judet.nume Judet,uta.populatie Pop_total,nationalitate.name Etnie,uta_nationalitate.populatie Pop_etnie,nationalitate.id natid "
                        + "from uta " + "left join judet on uta.judet=judet.id "
                        + "left join uta_nationalitate on uta_nationalitate.uta=uta.siruta "
                        + "left join nationalitate on uta_nationalitate.nationalitate=nationalitate.id "
                        + "where uta.siruta=? order by Pop_etnie desc");
                utaSt.setInt(1, utaId);

                final ResultSet utaRs = utaSt.executeQuery();
                while (utaRs.next()) {
                    final Nationality nat = Nationality.getByIndex(utaRs.getInt("natid"));
                    final int pop = utaRs.getInt("Pop_etnie");
                    uta.getNationalStructure().put(nat, pop);
                }

                final PreparedStatement utaRel = cn
                    .prepareStatement("select "
                        + "uta.name Nume,judet.nume Judet,religie.name Religie,uta_religie.populatie Pop_religie,religie.id relid "
                        + "from uta " + "left join judet on uta.judet=judet.id "
                        + "left join uta_religie on uta_religie.uta=uta.siruta "
                        + "left join religie on uta_religie.religie=religie.id "
                        + "where uta.siruta=? order by Pop_religie desc");
                utaRel.setInt(1, uta.getSiruta());
                final ResultSet utaRelRs = utaRel.executeQuery();
                while (utaRelRs.next()) {
                    final Religion rel = Religion.getByIndex(utaRelRs.getInt("relid"));
                    final int pop = utaRelRs.getInt("Pop_religie");
                    uta.getReligiousStructure().put(rel, pop);
                }

                final String wikiText = "<div style=\"float:left\">" + generateCountyNationalData(judet, uta)
                    + generateCountyReligiousData(judet, uta) + "</div>\n" + generateCountyNationalText(uta)
                    + generateCountyReligiousText(uta) + "<br clear=\"left\"/>";
                System.out.println(wikiText);
                System.out.println();

                final StringBuilder summaryBuilder = new StringBuilder("Robot:");
                wiki = new Wiki("ro.wikipedia.org");
                final Properties credentials = new Properties();

                credentials.load(WikiTextGenerator2011.class.getClassLoader().getResourceAsStream("credentials.properties"));

                final String username = credentials.getProperty("Username");
                final String password = credentials.getProperty("Password");
                wiki.login(username, password.toCharArray());
                wiki.setMarkBot(true);

                final String articleTitle = getArticleTitle(uta, judet);

                final Long lastrevid = (Long) wiki.getPageInfo(articleTitle).get("lastrevid");
                final Revision lastRev = wiki.getRevision(lastrevid);
                final String pageText = lastRev.getText();

                final Map sectionMap = wiki.getSectionMap(articleTitle);
                final boolean generateDemographySection = !sectionMap.containsValue("Demografie")
                    && !sectionMap.containsValue("Populație") && !sectionMap.containsValue("Populația")
                    && !sectionMap.containsValue("Demografia");

                String infoboxText = null;
                String infoboxName = null;
                final Matcher infoboxMatcher = regexInfocAsezare.matcher(pageText);
                if (infoboxMatcher.find()) {
                    infoboxText = infoboxMatcher.group();
                    infoboxName = "Infocaseta Așezare";
                    // System.out.println("---Found infobox text: " + infoboxText);
                } else {
                    System.out.println("---Infobox Așezare not found");
                    // search for CutieComune
                    final Matcher ccrMatcher = regexCCR.matcher(pageText);
                    if (ccrMatcher.find()) {
                        infoboxText = ccrMatcher.group();
                        infoboxName = "Casetă comune România";
                    }
                }
                String newInfoboxText = infoboxText;
                if (null != infoboxName) {
                    final ParameterReader reader = new ParameterReader(infoboxText);
                    reader.run();
                    final Map<String, String> params = reader.getParams();
                    // System.out.println(params);

                    if (StringUtils.equals(infoboxName, "Infocaseta Așezare")) {
                        switch (uta.getType()) {
                        case MUNICIPIU:
                            params.put("tip_asezare", "[[Municipiile României|Municipiu]]");
                            break;
                        case ORAS:
                            params.put("tip_asezare", "[[Orașele României|Oraș]]");
                            break;
                        case COMUNA:
                            params.put("tip_asezare", "[[Comunele României|Comună]]");
                        }
                        params.put("tip_cod_clasificare", "[[SIRUTA]]");
                        params.put("cod_clasificare", String.valueOf(uta.getSiruta()));
                        params.put("recensământ", "[[Recensământul populației din 2011 (România)|2011]]");
                        params.put("populație",
                            getTendencyTemplate(uta.getPopulation()) + String.valueOf(uta.getPopulation()));
                        if (params.get("population_blank1_title") == null) {
                            params.put("population_blank1_title",
                                "[[Recensământul populației din 2002 (România)|Recensământul anterior, 2002]]");
                            params.put("population_blank1", String.valueOf(pop2002));
                        }

                    } else if (StringUtils.equals(infoboxName, "Casetă comune România")) {
                        params.put("recensământ", "[[Recensământul populației din 2011 (România)|2011]]");
                        params.put("populație",
                            getTendencyTemplate(uta.getPopulation()) + String.valueOf(uta.getPopulation()));
                        params.put("siruta", String.valueOf(uta.getSiruta()));
                    }

                    final StringBuilder poprefBuilder = new StringBuilder("<ref name=\"kia.hu\"");
                    if (!generateDemographySection) {
                        poprefBuilder
                        .append(">{{cite web|url=http://www.kia.hu/konyvtar/erdely/erd2002/etnii2002.zip|title=Recensământul Populației și al Locuințelor 2002 - populația unităților administrative pe etnii|publisher=K");
                        poprefBuilder.append(StringUtils.lowerCase("ULTURÁLIS "));
                        poprefBuilder.append('I');
                        poprefBuilder.append(StringUtils.lowerCase("NNOVÁCIÓS "));
                        poprefBuilder.append('A');
                        poprefBuilder.append(StringUtils.lowerCase("LAPÍTVÁNY"));
                        poprefBuilder
                        .append(" (KIA.hu - Fundația Culturală pentru Inovație)|accessdate=2013-08-06}}</ref> ");
                    } else {
                        poprefBuilder.append("/>");
                    }
                    poprefBuilder.append("<ref name=\"insse_2011_nat\"");
                    if (!generateDemographySection) {
                        poprefBuilder
                        .append(">Rezultatele finale ale Recensământului din 2011: {{Citat web|url=http://www.recensamantromania.ro/wp-content/uploads/2013/07/sR_Tab_8.xls|title=Tab8. Populaţia stabilă după etnie – judeţe, municipii, oraşe, comune|publisher=[[Institutul Național de Statistică]] din România|accessdate=2013-08-05|date=iulie 2013}}</ref>");
                    } else {
                        poprefBuilder.append("/>");
                    }
                    params.put("populație_note_subsol", poprefBuilder.toString());

                    newInfoboxText = generateNewInfobox(params, infoboxName);
                    summaryBuilder.append(" actualizare populație, tip, cod siruta în infocasetă;");
                }
                String newPageText = pageText.replace(infoboxText, newInfoboxText);

                final boolean hasReferences = footnotesRegex.matcher(pageText).find();

                if (generateDemographySection) {
                    final List<String> sectionsBefore = Arrays.asList("Geografie", "Geografia", "Așezare", "Așezarea",
                        "Amplasare", "Amplasarea", "Date geografice", "Poziție", "Poziția");
                    final List<String> sectionsAfter = Arrays.asList("Monumente istorice", "Atracții turistice",
                        "Personalități", "Note", "Vezi și", "Legături externe", "Bibliografie");

                    String preSection = null;
                    String postSection = null;
                    int preSectionIndex = 0;
                    int postSectionIndex = 0;
                    int sectionIndex = 1;
                    for (final Object sectionKey : sectionMap.keySet()) {
                        final String sectionTitle = sectionMap.get(sectionKey).toString();
                        if (sectionsBefore.contains(sectionTitle)) {
                            preSection = sectionTitle;
                            preSectionIndex = sectionIndex;
                        }
                        // the first postsection found remains
                        if (sectionsAfter.contains(sectionTitle) && postSection == null) {
                            postSection = sectionTitle;
                            postSectionIndex = sectionIndex;
                        }
                        sectionIndex++;
                    }

                    if (preSection != null) {
                        final String sectionText = wiki.getSectionText(articleTitle, preSectionIndex);
                        final StringBuilder sectionTextBuilder = new StringBuilder(sectionText);
                        sectionTextBuilder.append("\n== Demografie ==\n");
                        sectionTextBuilder.append(StringUtils.chomp(StringUtils.trim(wikiText)));
                        sectionTextBuilder.append("\n");

                        newPageText = newPageText.replace(sectionText, sectionTextBuilder.toString());

                        System.out.println(sectionTextBuilder);
                    } else if (postSection != null) {
                        final String sectionText = wiki.getSectionText(articleTitle, postSectionIndex);
                        final StringBuilder sectionTextBuilder = new StringBuilder(sectionText);
                        sectionTextBuilder.insert(0, "\n");
                        sectionTextBuilder.insert(0, StringUtils.chomp(StringUtils.trim(wikiText)));
                        sectionTextBuilder.insert(0, "\n== Demografie ==\n");

                        newPageText = newPageText.replace(sectionText, sectionTextBuilder.toString());

                        System.out.println(sectionTextBuilder);
                    } else {
                        final List<Integer> endIndices = new ArrayList<Integer>();
                        endIndices.add(StringUtils.indexOf(newPageText, "{{ciot"));
                        endIndices.add(StringUtils.indexOf(newPageText, "{{Comune"));
                        endIndices.add(StringUtils.indexOf(newPageText, "{{Județ"));
                        endIndices.add(StringUtils.indexOf(newPageText, "{{Orașe"));
                        endIndices.add(StringUtils.indexOf(newPageText, "{{DN"));
                        endIndices.add(StringUtils.indexOf(newPageText, "[[Categori"));
                        while (endIndices.contains(-1)) {
                            endIndices.remove(new Integer(-1));
                        }
                        final int endIndex = Collections.min(endIndices);
                        final StringBuilder articleTextBuilder = new StringBuilder(newPageText);
                        articleTextBuilder.insert(endIndex - 1, "\n");
                        articleTextBuilder.insert(endIndex - 1, StringUtils.chomp(StringUtils.trim(wikiText)));
                        articleTextBuilder.insert(endIndex - 1, "\n== Demografie ==\n");

                        System.out.println(articleTextBuilder);

                        newPageText = articleTextBuilder.toString();
                    }
                    summaryBuilder.append(" adăugare secțiune demografie");
                }

                // wiki.edit(articleTitle, newPageText, summaryBuilder.toString());
            }
        } catch (final SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            closeConnection(conn);
            closeConnection(conn2002);
            System.exit(1);
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            closeConnection(conn);
            closeConnection(conn2002);
            System.exit(1);
        } catch (final Exception ex) {
            ex.printStackTrace();
            closeConnection(conn);
            closeConnection(conn2002);
            System.exit(1);
        } finally {
            if (null != wiki) {
                wiki.logout();
            }
        }

    }

    private static String generateNewInfobox(final Map<String, String> params, final String infoboxName) {
        final StringBuilder ibBuilder = new StringBuilder("{{");
        ibBuilder.append(infoboxName);
        ibBuilder.append('\n');
        for (final String paramname : params.keySet()) {
            ibBuilder.append("|");
            ibBuilder.append(paramname);
            ibBuilder.append(" = ");
            ibBuilder.append(params.get(paramname));
            ibBuilder.append('\n');
        }
        ibBuilder.append("}}");
        return ibBuilder.toString();
    }

    private static String getTendencyTemplate(final int population) {
        if (pop2002 == population) {
            return "{{stabil}} ";
        }
        return (pop2002 > population ? "{{de" : "{{in") + "crease}} ";
    }

    private static String getArticleTitle(final PopulationDb2002Entry uta, final String judet) throws IOException {
        wiki.setResolveRedirects(true);
        final StringBuilder sb = new StringBuilder();
        if (uta.getType() == UTAType.COMUNA) {
            sb.append("Comuna ");
            sb.append(capitalizeName(uta.getName()));
            sb.append(", ");
            sb.append(capitalizeName(judet));
        } else {
            sb.append(capitalizeName(uta.getName()));
            sb.append(", ");
            sb.append(capitalizeName(judet));
        }

        final Map pageInfo = wiki.getPageInfo(sb.toString());
        return pageInfo.get("displaytitle").toString();
    }

    private static String generateCountyReligiousData(final String judet, final PopulationDb2002Entry uta)
        throws IOException {
        final int populatie = uta.getPopulation();

        final DefaultPieDataset dataset = new DefaultPieDataset();
        int totalPerNat = 0;
        final Map<String, Integer> smallGroups = new HashMap<String, Integer>();
        for (final Religion rel : Religion.values()) {
            final int relpop = ObjectUtils.defaultIfNull(uta.getReligiousStructure().get(rel), 0);
            if (relpop * 100.0 / populatie > 1.0 && Religion.OTH != rel) {
                dataset.setValue(rel.getName(), relpop);
                totalPerNat += relpop;
            } else if (relpop * 100.0 / populatie <= 1.0 && Religion.OTH != rel) {
                smallGroups.put(rel.getName(), relpop);
            }
        }
        if (2 < smallGroups.size()) {
            dataset.setValue(Religion.OTH.getName(), populatie - totalPerNat);
        } else {
            for (final String natname : smallGroups.keySet()) {
                dataset.setValue(natname, smallGroups.get(natname));
            }
        }

        final JFreeChart chart = ChartFactory.createPieChart3D("", dataset, false, false, false);
        final PiePlot3D plot = (PiePlot3D) chart.getPlot();
        plot.setForegroundAlpha(0.5f);
        plot.setBackgroundAlpha(0.0f);
        plot.setIgnoreZeroValues(true);
        if (0 == religionMap.size()) {
            initRelPaintMap(plot);
        }
        for (final Religion rel : Religion.values()) {
            plot.setSectionPaint(rel.getName(), religionMap.get(rel.getName()));
        }

        final BufferedImage bufferedImage = chart.createBufferedImage(640, 480);
        final File imgdir = new File("E:\\var\\wki\\doc\\rec2011finale\\img", getCountyName(judet));
        if (!imgdir.exists()) {
            imgdir.mkdirs();
        }
        final File outFile = new File(imgdir, "Romania 2011 population religion chart " + getCountyName(judet) + "_"
            + getShortedName(uta) + ".png");
        ImageIO.write(bufferedImage, "PNG", outFile);

        final StringBuilder pieChart = new StringBuilder(
            "{{Pie chart\n|thumb=left\n|style=clear:none;\n|caption=Componența confesională a ");
        pieChart.append(getGenitiveFullName(uta));

        int i = 1;
        for (final Object k : dataset.getKeys()) {
            pieChart.append("\n|label");
            pieChart.append(i);
            pieChart.append('=');
            pieChart.append(k.toString());
            pieChart.append("|value");
            pieChart.append(i);
            pieChart.append('=');
            pieChart
            .append((int) (100 * (dataset.getValue(k.toString()).doubleValue() * 100.0 / uta.getPopulation())) / 100.0);
            pieChart.append("|color");
            pieChart.append(i);
            pieChart.append('=');
            final Color color = (Color) plot.getSectionPaint(k.toString());
            pieChart.append(colorToHtml(color));
            i++;
        }
        pieChart.append("}}\n");
        return pieChart.toString();
    }

    private static String generateCountyReligiousText(final PopulationDb2002Entry uta) {
        final StringBuilder textBuilder = new StringBuilder();
        textBuilder.append("\nDin punct de vedere confesional, ");
        final Religion majority = findMajorityReligion(uta);
        final Map<Religion, Double> minorities = findSignificantReligiousMinorities(uta);

        if (majority != null) {
            textBuilder.append("majoritatea locuitorilor sunt ");
            textBuilder.append(getReligionLink(majority));
            textBuilder.append(" (");
            textBuilder.append(formatPercentage(100.0 * uta.getReligiousStructure().get(majority) / uta.getPopulation()));
            textBuilder.append("%)");
            if (minorities.size() == 1) {
                textBuilder.append(", cu o minoritate de ");
                final Religion min = minorities.keySet().iterator().next();
                textBuilder.append(getReligionLink(min));
                textBuilder.append(" (");
                textBuilder.append(formatPercentage(100.0 * minorities.get(min)));
                textBuilder.append("%).");
            } else if (minorities.size() > 1) {
                textBuilder.append(", dar există și minorități de ");
                final List<String> religii = new ArrayList<String>();
                for (final Religion min : minorities.keySet()) {
                    religii.add(getReligionLink(min) + " (" + formatPercentage(100.0 * minorities.get(min)) + "%)");
                }
                textBuilder.append(StringUtils.join(religii.toArray(), ", ", 0, religii.size() - 1));
                textBuilder.append(" și ");
                textBuilder.append(religii.get(religii.size() - 1));
                textBuilder.append(".");
            } else {
                textBuilder.append(".");
            }
        } else {
            textBuilder.append("nu există o religie majoritară, locuitorii fiind ");
            final List<String> religii = new ArrayList<String>();
            for (final Religion min : minorities.keySet()) {
                religii.add(getReligionLink(min) + " (" + formatPercentage(100.0 * minorities.get(min)) + "%)");
            }
            textBuilder.append(StringUtils.join(religii.toArray(), ", ", 0, religii.size() - 1));
            textBuilder.append(" și ");
            textBuilder.append(religii.get(religii.size() - 1));
            textBuilder.append(".");
        }
        final int unknownRel = ObjectUtils.defaultIfNull(uta.getReligiousStructure().get(Religion.UNKNOWN), 0);
        if (0 < unknownRel) {
            textBuilder.append(" Pentru ");
            textBuilder.append(formatPercentage(100.0 * unknownRel / uta.getPopulation()));
            textBuilder.append("% din populație, nu este cunoscută apartenența confesională.");
        }
        textBuilder
        .append("<ref name=\"insse_2011_rel\">Rezultatele finale ale Recensământului din 2011: {{Citat web|url=http://www.recensamantromania.ro/wp-content/uploads/2013/07/sR_TAB_13.xls|title=Tab13. Populaţia stabilă după religie – judeţe, municipii, oraşe, comune|publisher=[[Institutul Național de Statistică]] din România|accessdate=2013-08-05|date=iulie 2013}}</ref>");

        return textBuilder.toString();
    }

    private static String getReligionLink(final Religion religion) {
        switch (religion) {
        case ORTHO:
            return "[[Biserica Ortodoxă Română|ortodocși]]";
        case ROM_CATH:
            return "[[Biserica Romano-Catolică din România|romano-catolici]]";
        case EVANGH:
            return "[[Biserica Evanghelică Română|evanghelici]]";
        case AUGUST:
            return "[[Biserica Evanghelică de Confesiune Augustană din România|luterani de confesiune augustană]]";
        case LUTH:
            return "[[Biserica Evanghelică-Luterană din România|evanghelici-luterani]]";
        case MUSL:
            return "[[Islamul în România|musulmani]]";
        case CALVINIST:
            return "[[Biserica Reformată din România|reformați]]";
        case ADV7:
            return "[[Biserica Adventistă de Ziua a Șaptea|adventiști de ziua a șaptea]]";
        case JEHOVA:
            return "[[Organizația Religioasă Martorii lui Iehova|martori ai lui Iehova]]";
        case CR_EVANGH:
            return "[[Biserica Creștină după Evanghelie|creștini după evanghelie]]";
        case OLD_ORTHO:
            return "[[Biserica Ortodoxă Rusă de Rit Vechi din România|ortodocși de rit vechi]]";
        case SR_ORTHO:
            return "[[Biserica Ortodoxă Sârbă|ortodocși sârbi]]";
        case JEWISH:
            return "[[Iudaism|mozaici]]";
        case ARM:
            return "[[Biserica Armeano-Catolică|armeni]]";
        case NONE:
            return "[[Umanism secular|fără religie]]";
        case ATHEISM:
            return "[[Ateismul în România|atei]]";
        default:
            return "[[" + StringUtils.lowerCase(religion.getName()) + "]]";
        }
    }

    private static Map<Religion, Double> findSignificantReligiousMinorities(final PopulationDb2002Entry uta) {
        final Map<Religion, Double> ret = new LinkedHashMap<Religion, Double>();
        final Map<Religion, Integer> religiousStructure = uta.getReligiousStructure();
        for (final Religion rel : religiousStructure.keySet()) {
            if (rel == Religion.UNKNOWN || rel == Religion.OTH) {
                continue;
            }
            final int pop = religiousStructure.get(rel);
            final double ratio = (double) pop / uta.getPopulation();
            if (0.5 < ratio || 0.01 > ratio) {
                continue;
            }
            ret.put(rel, ratio);
        }
        return ret;
    }

    private static Religion findMajorityReligion(final PopulationDb2002Entry uta) {
        final Map<Religion, Integer> religiousStructure = uta.getReligiousStructure();
        for (final Religion rel : religiousStructure.keySet()) {
            final int pop = religiousStructure.get(rel);
            if ((double) pop / uta.getPopulation() > 0.5) {
                return rel;
            }
        }
        return null;
    }

    private static String generateCountyNationalData(final String judet, final PopulationDb2002Entry uta) throws IOException {
        final int populatie = uta.getPopulation();

        final DefaultPieDataset dataset = new DefaultPieDataset();
        int totalPerNat = 0;
        final Map<String, Integer> smallGroups = new HashMap<String, Integer>();
        for (final Nationality nat : Nationality.values()) {
            final int natpop = ObjectUtils.defaultIfNull(uta.getNationalStructure().get(nat), 0);
            if (natpop * 100.0 / populatie > 1.0 && Nationality.OTH != nat) {
                dataset.setValue(nat.getName(), natpop);
                totalPerNat += natpop;
            } else if (natpop * 100.0 / populatie <= 1.0 && Nationality.OTH != nat) {
                smallGroups.put(nat.getName(), natpop);
            }
        }
        if (2 < smallGroups.size()) {
            dataset.setValue(Nationality.OTH.getName(), populatie - totalPerNat);
        } else {
            for (final String natname : smallGroups.keySet()) {
                dataset.setValue(natname, smallGroups.get(natname));
            }
        }

        final JFreeChart chart = ChartFactory.createPieChart3D("", dataset, false, false, false);
        final PiePlot3D plot = (PiePlot3D) chart.getPlot();
        plot.setForegroundAlpha(0.5f);
        plot.setBackgroundAlpha(0.0f);
        plot.setIgnoreZeroValues(true);
        if (0 == paintMap.size()) {
            initNatPaintMap(plot);
        }
        for (final Nationality nat : Nationality.values()) {
            plot.setSectionPaint(nat.getName(), paintMap.get(nat.getName()));
        }

        final BufferedImage bufferedImage = chart.createBufferedImage(640, 480);
        final File imgdir = new File("E:\\var\\wki\\doc\\rec2011finale\\img", getCountyName(judet));
        if (!imgdir.exists()) {
            imgdir.mkdirs();
        }
        final File outFile = new File(imgdir, "Romania 2011 population nationality chart " + getCountyName(judet) + "_"
            + getShortedName(uta) + ".png");
        ImageIO.write(bufferedImage, "PNG", outFile);

        final StringBuilder pieChart = new StringBuilder("{{Pie chart\n|thumb=left\n|caption=Componența etnică a ");
        pieChart.append(getGenitiveFullName(uta));
        int i = 1;
        for (final Object k : dataset.getKeys()) {
            pieChart.append("\n|label");
            pieChart.append(i);
            pieChart.append('=');
            pieChart.append(k.toString());
            pieChart.append("|value");
            pieChart.append(i);
            pieChart.append('=');
            pieChart
            .append((int) (100 * (dataset.getValue(k.toString()).doubleValue() * 100.0 / uta.getPopulation())) / 100.0);
            pieChart.append("|color");
            pieChart.append(i);
            pieChart.append('=');
            final Color color = (Color) plot.getSectionPaint(k.toString());
            pieChart.append(colorToHtml(color));
            i++;
        }
        pieChart.append("}}\n");
        return pieChart.toString();
    }

    private static String colorToHtml(final Color color) {
        final StringBuilder sb = new StringBuilder("#");
        sb.append(StringUtils.substring(StringUtils.leftPad(Integer.toHexString(color.getRed()), 2, '0'), 0, 2));
        sb.append(StringUtils.substring(StringUtils.leftPad(Integer.toHexString(color.getGreen()), 2, '0'), 0, 2));
        sb.append(StringUtils.substring(StringUtils.leftPad(Integer.toHexString(color.getBlue()), 2, '0'), 0, 2));
        return sb.toString();
    }

    private static String generateCountyNationalText(final PopulationDb2002Entry uta) {
        final int populatie = uta.getPopulation();
        final StringBuilder textBuilder = new StringBuilder();
        generateCountyIntroductionText(uta, textBuilder);

        final Nationality majority = findMajorityNationality(uta);
        final Map<Nationality, Double> minorities = findSignificantNationalMinorities(uta);
        if (majority != null) {
            textBuilder.append("Majoritatea locuitorilor sunt ");
            textBuilder.append(getNationalityLink(majority));
            textBuilder.append(" (");
            textBuilder.append(formatPercentage(100.0 * uta.getNationalStructure().get(majority) / uta.getPopulation()));
            textBuilder.append("%)");
            if (minorities.size() == 1) {
                textBuilder.append(", cu o minoritate de ");
                final Nationality min = minorities.keySet().iterator().next();
                textBuilder.append(getNationalityLink(min));
                textBuilder.append(" (");
                textBuilder.append(formatPercentage(100.0 * minorities.get(min)));
                textBuilder.append("%).");
            } else if (minorities.size() > 1) {
                textBuilder.append(". Principalele minorități sunt cele de ");
                final List<String> etnii = new ArrayList<String>();
                for (final Nationality min : minorities.keySet()) {
                    etnii.add(getNationalityLink(min) + " (" + formatPercentage(100.0 * minorities.get(min)) + "%)");
                }
                textBuilder.append(StringUtils.join(etnii.toArray(), ", ", 0, etnii.size() - 1));
                textBuilder.append(" și ");
                textBuilder.append(etnii.get(etnii.size() - 1));
                textBuilder.append(".");
            } else {
                textBuilder.append(".");
            }
        } else {
            textBuilder.append("Nu există o etnie majoritară, locuitorii fiind ");
            final List<String> etnii = new ArrayList<String>();
            for (final Nationality min : minorities.keySet()) {
                etnii.add(getNationalityLink(min) + " (" + formatPercentage(100.0 * minorities.get(min)) + "%)");
            }
            textBuilder.append(StringUtils.join(etnii.toArray(), ", ", 0, etnii.size() - 1));
            textBuilder.append(" și ");
            textBuilder.append(etnii.get(etnii.size() - 1));
            textBuilder.append(".");
        }
        final int unknownNat = ObjectUtils.defaultIfNull(uta.getNationalStructure().get(Nationality.NONE), 0);
        if (0 < unknownNat) {
            textBuilder.append(" Pentru ");
            textBuilder.append(formatPercentage(100.0 * unknownNat / populatie));
            textBuilder.append("% din populație, apartenența etnică nu este cunoscută.");
        }
        textBuilder
        .append("<ref name=\"insse_2011_nat\">Rezultatele finale ale Recensământului din 2011: {{Citat web|url=http://www.recensamantromania.ro/wp-content/uploads/2013/07/sR_Tab_8.xls|title=Tab8. Populaţia stabilă după etnie – judeţe, municipii, oraşe, comune|publisher=[[Institutul Național de Statistică]] din România|accessdate=2013-08-05|date=iulie 2013}}</ref>");
        return textBuilder.toString();
    }

    private static void generateCountyIntroductionText(final PopulationDb2002Entry uta, final StringBuilder textBuilder) {
        textBuilder
        .append("Conform [[Recensământul populației din 2011 (România)|recensământului efectuat în 2011]], populația ");
        textBuilder.append(getGenitiveFullName(uta));
        textBuilder.append(" se ridică la {{formatnum:");
        textBuilder.append(uta.getPopulation());
        textBuilder.append("}} ");
        textBuilder.append(de(uta.getPopulation(), "locuitor", "locuitori"));

        final Connection c2011 = getConnection();
        PreparedStatement comp2011St, comp2002St;
        try {
            comp2011St = c2011.prepareStatement("select localitate.populatie pop from localitate where localitate.uta=?");
            comp2011St.setInt(1, uta.getSiruta());
            final ResultSet comp2011rs = comp2011St.executeQuery();
            int currentComponentCount = 0;
            while (comp2011rs.next()) {
                currentComponentCount++;
            }

            final Connection c2002 = getConnection2002();
            comp2002St = c2002.prepareStatement("select localitate.populatie pop from localitate where localitate.uta=?");
            comp2002St.setInt(1, uta.getSiruta());
            final ResultSet comp2002rs = comp2002St.executeQuery();
            int oldComponentCount = 0;
            int oldPopulationSum = 0;
            while (comp2002rs.next()) {
                oldComponentCount++;
                oldPopulationSum += comp2002rs.getInt("pop");
            }
            final PreparedStatement pop2002St = c2002
                .prepareStatement("select uta.populatie pop from uta where uta.siruta=?");
            pop2002St.setInt(1, uta.getSiruta());
            final ResultSet pop2002rs = pop2002St.executeQuery();
            int oldPopulationSelect = -1;
            if (pop2002rs.next()) {
                oldPopulationSelect = pop2002rs.getInt("pop");
            }
            pop2002 = oldPopulationSum;

            if (oldPopulationSum == uta.getPopulation()) {
                textBuilder
                .append(", aceeași ca și la [[Recensământul populației din 2002 (România)|recensământul anterior din 2002]].");
            } else if (oldPopulationSum > uta.getPopulation()) {
                textBuilder
                .append(", în scădere față de [[Recensământul populației din 2002 (România)|recensământul anterior din 2002]], când se înregistraseră {{formatnum:");
                textBuilder.append(oldPopulationSum);
                textBuilder.append("}}&nbsp;");
                textBuilder.append(de(oldPopulationSum, "locuitor", "locuitori"));
                textBuilder.append('.');
            } else {
                textBuilder
                .append(", în creștere față de [[Recensământul populației din 2002 (România)|recensământul anterior din 2002]], când se înregistraseră {{formatnum:");
                textBuilder.append(oldPopulationSum);
                textBuilder.append("}}&nbsp;");
                textBuilder.append(de(oldPopulationSum, "locuitor", "locuitori"));
                textBuilder.append('.');
            }
            textBuilder.append("<ref name=\"kia.hu\">");
            if (oldPopulationSelect < 0) {
                textBuilder
                .append("Populația satelor componente la recensământul din 2002. Pe atunci, comuna încă nu exista, ea fiind înființată la o dată ulterioară. ");
            } else if (currentComponentCount != oldComponentCount) {
                textBuilder
                .append("Populația satelor aflate actualmente în componența comunei, ea având la acea vreme (2002) altă componență.");
            }
            textBuilder
            .append("{{cite web|url=http://www.kia.hu/konyvtar/erdely/erd2002/etnii2002.zip|title=Recensământul Populației și al Locuințelor 2002 - populația unităților administrative pe etnii|publisher=K");
            textBuilder.append(StringUtils.lowerCase("ULTURÁLIS "));
            textBuilder.append('I');
            textBuilder.append(StringUtils.lowerCase("NNOVÁCIÓS "));
            textBuilder.append('A');
            textBuilder.append(StringUtils.lowerCase("LAPÍTVÁNY"));
            textBuilder.append(" (KIA.hu - Fundația Culturală pentru Inovație)|accessdate=2013-08-06}}</ref> ");
        } catch (final SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            closeConnection(conn2002);
            closeConnection(conn);
            System.exit(1);
        }
    }

    private static String getNationalityLink(final Nationality min) {
        if (min == Nationality.RO) {
            return "[[Români|" + StringUtils.lowerCase(min.getName()) + "]]";
        } else {
            return "[[" + min.getName() + "i din România|" + StringUtils.lowerCase(min.getName()) + "]]";
        }
    }

    private static String getCountyName(final String judet) {
        final String[] ctyParts = StringUtils.splitByCharacterType(judet);
        for (int i = 0; i < ctyParts.length; i++) {
            ctyParts[i] = StringUtils.capitalize(StringUtils.lowerCase(ctyParts[i]));
        }
        return StringUtils.join(ctyParts);
    }

    private static void initNatPaintMap(final PiePlot3D plot) {
        for (final Nationality nat : Nationality.values()) {
            final Paint pm = plot.getDrawingSupplier().getNextPaint();
            paintMap.put(nat.getName(), pm);
        }
        paintMap.put(Nationality.RO.getName(), new Color(85, 85, 255));
        paintMap.put(Nationality.TR.getName(), new Color(255, 85, 85));
        paintMap.put(Nationality.RR.getName(), new Color(85, 255, 255));
        paintMap.put(Nationality.HU.getName(), new Color(85, 255, 85));
        paintMap.put(Nationality.TT.getName(), new Color(128, 0, 0));
        paintMap.put(Nationality.NONE.getName(), new Color(128, 128, 128));
        paintMap.put(Nationality.HE.getName(), new Color(192, 192, 192));
        paintMap.put(Nationality.OTH.getName(), new Color(64, 64, 64));

        blandifyColors(paintMap);
    }

    private static void blandifyColors(final Map<String, Paint> map) {
        for (final String key : map.keySet()) {
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

    private static void initRelPaintMap(final PiePlot3D plot) {
        for (final Religion rel : Religion.values()) {
            final Paint pm = plot.getDrawingSupplier().getNextPaint();
            religionMap.put(rel.getName(), pm);
        }
        religionMap.put(Religion.ORTHO.getName(), new Color(85, 85, 255));
        religionMap.put(Religion.MUSL.getName(), new Color(255, 85, 85));
        religionMap.put(Religion.ROM_CATH.getName(), new Color(255, 255, 85));
        religionMap.put(Religion.SR_ORTHO.getName(), new Color(192, 0, 0));
        religionMap.put(Religion.OTH.getName(), new Color(128, 128, 128));
        religionMap.put(Religion.PENTICOST.getName(), new Color(255, 255, 64));
        religionMap.put(Religion.UNIT.getName(), new Color(0, 192, 192));
        religionMap.put(Religion.MUSL.getName(), new Color(255, 85, 85));
        blandifyColors(religionMap);
    }

    private static final String formatPercentage(final double d) {
        final NumberFormat nf = NumberFormat.getNumberInstance(new Locale("ro"));
        nf.setMaximumFractionDigits(2);
        return nf.format(d);
    }

    private static Nationality findMajorityNationality(final PopulationDb2002Entry uta) {
        for (final Nationality nat : uta.getNationalStructure().keySet()) {
            final int pop = uta.getNationalStructure().get(nat);
            if ((double) pop / uta.getPopulation() > 0.5) {
                return nat;
            }
        }
        return null;
    }

    private static Map<Nationality, Double> findSignificantNationalMinorities(final PopulationDb2002Entry uta) {
        final Map<Nationality, Double> ret = new LinkedHashMap<Nationality, Double>();
        for (final Nationality nat : uta.getNationalStructure().keySet()) {
            if (nat == Nationality.NONE || nat == Nationality.OTH) {
                continue;
            }
            final int pop = uta.getNationalStructure().get(nat);
            final double ratio = (double) pop / uta.getPopulation();
            if (0.5 < ratio || 0.01 > ratio) {
                continue;
            }
            ret.put(nat, ratio);
        }
        return ret;
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

    private static String getShortedName(final PopulationDb2002Entry uta) {
        final StringBuilder sb = new StringBuilder();
        switch (uta.getType()) {
        case MUNICIPIU:
            sb.append("mun");
            break;
        case ORAS:
            sb.append("ors");
            break;
        case COMUNA:
            sb.append("com");
            break;
        default:
            break;
        }
        sb.append("_");
        sb.append(StringUtils.replace(capitalizeName(uta.getName()), " ", ""));
        return sb.toString();
    }

    private static String de(final int number, final String singular, final String plural) {
        if (number == 1) {
            return singular;
        }
        if (number == 0) {
            return plural;
        }

        final int mod100 = number % 100;
        if (mod100 == 0 || mod100 > 19) {
            return "de&nbsp;" + plural;
        } else {
            return plural;
        }
    }

    private static String getGenitiveFullName(final PopulationDb2002Entry uta) {
        final StringBuilder sb = new StringBuilder();
        switch (uta.getType()) {
        case MUNICIPIU:
            sb.append("municipiului");
            break;
        case ORAS:
            sb.append("orașului");
            break;
        case COMUNA:
            sb.append("comunei");
            break;
        default:
            break;
        }
        sb.append(" ");
        sb.append(capitalizeName(uta.getName()));
        return sb.toString();
    }

    private static String capitalizeName(final String name) {
        final String onlyLower = StringUtils.lowerCase(name);
        final String[] lowerItems = StringUtils.splitByCharacterType(onlyLower);
        final StringBuilder sb = new StringBuilder();

        final List<String> notCapitalized = Arrays.asList("de", "din", "pe", "sub", "peste", "la");

        for (final String item : lowerItems) {
            sb.append(notCapitalized.contains(item) ? item : StringUtils.capitalize(item));
        }
        return sb.toString();
    }
}
