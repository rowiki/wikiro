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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot3D;
import org.jfree.data.general.DefaultPieDataset;

public class WikiTextGenerator2011 {

    private static Connection conn;

    private final static Map<String, Paint> paintMap = new HashMap<String, Paint>();

    private final static Map<String, Paint> religionMap = new HashMap<String, Paint>();

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

    public static void main(final String[] args) {
        generateCounty(/* 10, 11, 12, */14/* , 26, 28 ,41 */);

        /*
         * for (int i = 1; i < 41; i++) { generateCounty(i); }
         */
        closeConnection(conn);
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

                final String wikiText = generateCountyNationalText(uta) + generateCountyReligiousText(uta);
                System.out.println(wikiText);
                System.out.println();

                generateCountyNationalData(judet, uta);
                generateCountyReligiousData(judet, uta);
            }
        } catch (final SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            closeConnection(conn);
            System.exit(1);
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            closeConnection(conn);
            System.exit(1);
        }

    }

    private static void generateCountyReligiousData(final String judet, final PopulationDb2002Entry uta) throws IOException {
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
            plot.setSectionPaint(rel.getName(), paintMap.get(rel.getName()));
        }

        final BufferedImage bufferedImage = chart.createBufferedImage(640, 480);
        final File imgdir = new File("E:\\var\\wki\\doc\\rec2011finale\\img", getCountyName(judet));
        if (!imgdir.exists()) {
            imgdir.mkdirs();
        }
        final File outFile = new File(imgdir, "Romania 2011 population religion chart " + getCountyName(judet) + "_"
            + getShortedName(uta) + ".png");
        ImageIO.write(bufferedImage, "PNG", outFile);
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

    private static void generateCountyNationalData(final String judet, final PopulationDb2002Entry uta) throws IOException {
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
    }

    private static String generateCountyNationalText(final PopulationDb2002Entry uta) {
        final int populatie = uta.getPopulation();
        final StringBuilder textBuilder = new StringBuilder();
        textBuilder
        .append("În urma [[Recensământul populației din 2011 (România)|recensământului efectuat în 2011]], populația ");
        textBuilder.append(getGenitiveFullName(uta));
        textBuilder.append(" se ridica la {{formatnum:");
        textBuilder.append(uta.getPopulation());
        textBuilder.append("}} ");
        textBuilder.append(de(uta.getPopulation(), "locuitor", "locuitori"));
        textBuilder.append(". ");
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

    private static String getNationalityLink(final Nationality min) {
        if (min == Nationality.RO) {
            return "[[Români|" + StringUtils.lowerCase(min.getName()) + "]]";
        } else {
            return "[[" + min.getName() + " din România|" + StringUtils.lowerCase(min.getName()) + "]]";
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
    }

    private static void initRelPaintMap(final PiePlot3D plot) {
        for (final Religion nat : Religion.values()) {
            final Paint pm = plot.getDrawingSupplier().getNextPaint();
            paintMap.put(nat.getName(), pm);
        }
        paintMap.put(Religion.ORTHO.getName(), new Color(85, 85, 255));
        paintMap.put(Religion.MUSL.getName(), new Color(255, 85, 85));

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

    private static Object de(final int number, final String singular, final String plural) {
        if (number == 1) {
            return singular;
        }
        final int mod100 = number % 100;
        if (mod100 == 0 || mod100 > 19) {
            return "de " + plural;
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