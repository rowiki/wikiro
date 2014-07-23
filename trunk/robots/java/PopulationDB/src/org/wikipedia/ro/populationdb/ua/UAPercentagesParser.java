package org.wikipedia.ro.populationdb.ua;

import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.isAlpha;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.split;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.security.auth.login.FailedLoginException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.wikipedia.Wiki;
import org.wikipedia.ro.populationdb.ua.dao.Hibernator;
import org.wikipedia.ro.populationdb.ua.model.Commune;
import org.wikipedia.ro.populationdb.ua.model.Language;
import org.wikipedia.ro.populationdb.ua.model.Raion;
import org.wikipedia.ro.populationdb.ua.model.Region;
import org.wikipedia.ro.populationdb.ua.model.Settlement;
import org.wikipedia.ro.populationdb.util.Transliterator;
import org.wikipedia.ro.populationdb.util.UkrainianTransliterator;

import au.com.bytecode.opencsv.CSVReader;

public class UAPercentagesParser {

    private static File[] files;

    public UAPercentagesParser(final File[] files2) {
        this.files = files2;
    }

    private static Wiki rowiki = null;

    public static void main(final String[] args) {
        if (args.length < 1) {
            System.out.println("Missing argument to point directory with data xls files");
            System.exit(1);
        }
        final File inDir = new File(args[0]);
        if (!inDir.isDirectory()) {
            System.out.println("Specified argument is not an existing directory");
            System.exit(1);
        }
        rowiki = new Wiki("ro.wikipedia.org");
        try {
            final Properties credentials = new Properties();
            credentials.load(UAPercentagesParser.class.getClassLoader().getResourceAsStream("credentials.properties"));
            final String user = credentials.getProperty("Username");
            final String pass = credentials.getProperty("Password");
            rowiki.login(user, pass.toCharArray());
            final File[] files = inDir.listFiles();
            final UAPercentagesParser parser = new UAPercentagesParser(files);
            parser.parse();
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final FailedLoginException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            rowiki.logout();
        }
    }

    public void parse() {
        final Hibernator hib = new Hibernator();
        final Session session = hib.getSession();
        session.beginTransaction();

        final List<Language> limbi = new ArrayList<Language>();
        limbi.add(new Language("Ucraineană"));
        limbi.add(new Language("Rusă"));
        limbi.add(new Language("Belarusă"));
        limbi.add(new Language("Bulgară"));
        limbi.add(new Language("Armeană"));
        limbi.add(new Language("Găgăuză"));
        limbi.add(new Language("Tătară crimeeană"));
        limbi.add(new Language("Română"));
        limbi.add(new Language("Germană"));
        limbi.add(new Language("Polonă"));
        limbi.add(new Language("Romani"));
        limbi.add(new Language("Slovacă"));
        limbi.add(new Language("Maghiară"));
        limbi.add(new Language("Karaim"));
        limbi.add(new Language("Ebraică"));
        limbi.add(new Language("Greacă"));
        for (final Language lang : limbi) {
            session.save(lang);
        }

        for (final File eachFile : files) {
            CSVReader reader = null;
            try {

                reader = new CSVReader(new InputStreamReader(new FileInputStream(eachFile), "Cp1251"), '\t', '\"');
                String[] line = null;
                for (int i = 0; i < 5; i++) {
                    line = reader.readNext();
                }
                Commune currentCommune = null;
                Raion currentRaion = null;
                Region currentRegion = null;

                while (null != (line = reader.readNext())) {
                    final String nume = line[0];
                    final Transliterator t = new UkrainianTransliterator(nume);
                    final String numeTransliterat = t.transliterate();

                    final String[] splitName = split(numeTransliterat);
                    final String[] splitNameUa = split(nume);

                    if (splitName.length < 2) {
                        continue;
                    }

                    // regiune
                    if (ArrayUtils.contains(splitName, "OBLAST") || ArrayUtils.contains(splitName, "KRÎM")) {
                        currentRegion = new Region();
                        currentRaion = null;
                        if (ArrayUtils.contains(splitName, "KRÎM")) {
                            currentRegion.setRomanianName("Crimeea");
                            currentRegion.setTransliteratedName("Krîm");
                            currentRegion.setName(capitalize(lowerCase("КРИМ")));
                            session.save(currentRegion);
                        }
                        continue;
                    }
                    // raion
                    if (ArrayUtils.contains(splitName, "RAION")
                        || (ArrayUtils.contains(splitName, "raion,") && StringUtils.equals("orașul Sevastopol",
                            currentRegion.getRomanianName()))) {
                        currentRaion = new Raion();
                        currentRegion.getRaioane().add(currentRaion);
                        currentRaion.setRegion(currentRegion);
                        continue;
                    }
                    // comuna, oras, asezare urbana
                    if (ArrayUtils.contains(splitName, "silrada") || StringUtils.equals(splitName[0], "smt")
                        || StringUtils.equals(splitName[0], "m.")) {

                        currentCommune = new Commune();
                        if (ArrayUtils.contains(splitName, "silrada")) {
                            final int indexOfSilrada = ArrayUtils.indexOf(splitName, "silrada");
                            final String[] nameParts = ArrayUtils.subarray(splitName, 0, indexOfSilrada);
                            final String[] namePartsUa = ArrayUtils.subarray(splitNameUa, 0, indexOfSilrada);

                            for (int i = 0; i < nameParts.length; i++) {
                                nameParts[i] = capitalize(lowerCase(nameParts[i]));
                                namePartsUa[i] = capitalize(lowerCase(namePartsUa[i]));
                            }

                            currentCommune.setTransliteratedName(join(nameParts, " "));
                            currentCommune.setRomanianName(getRomanianName(getPossibleNames(currentCommune)));
                            currentCommune.setName(join(namePartsUa, " "));
                            currentCommune.setTown(0);
                            currentCommune.setRaion(currentRaion);
                            session.save(currentCommune);
                        }
                        if (StringUtils.equals(splitName[0], "smt")) {
                            int i = splitName.length;
                            for (i = 1; i < splitName.length && isAlpha(splitName[i]); i++) {
                                splitName[i] = capitalize(lowerCase(splitName[i]));
                                splitNameUa[i] = capitalize(lowerCase(splitNameUa[i]));
                            }
                            final String[] nameParts = ArrayUtils.subarray(splitName, 1, i);
                            final String[] namePartsUa = ArrayUtils.subarray(splitNameUa, 1, i);
                            currentCommune.setTown(1);
                            currentCommune.setTransliteratedName(join(nameParts, " "));
                            currentCommune.setRomanianName(getRomanianName(getPossibleNames(currentCommune)));
                            currentCommune.setName(join(namePartsUa, " "));
                            if (null == currentRaion) {
                                currentCommune.setRegion(currentRegion);
                                currentRegion.getCities().add(currentCommune);
                                session.save(currentRegion);
                            }
                            session.save(currentCommune);

                            System.out.println(currentCommune.getTown() + " - " + currentCommune.getName() + " - "
                                + currentCommune.getTransliteratedName());
                        }
                        if (StringUtils.equals(splitName[0], "m.")) {
                            int i = splitName.length;
                            for (i = 1; i < splitName.length && isAlpha(splitName[i]); i++) {
                                splitName[i] = capitalize(lowerCase(splitName[i]));
                                splitNameUa[i] = capitalize(lowerCase(splitNameUa[i]));
                            }
                            final String[] nameParts = ArrayUtils.subarray(splitName, 1, i);
                            final String[] namePartsUa = ArrayUtils.subarray(splitNameUa, 1, i);
                            currentCommune.setTown(2);
                            currentCommune.setTransliteratedName(join(nameParts, " "));
                            currentCommune.setRomanianName(getRomanianName(getPossibleNames(currentCommune)));
                            currentCommune.setName(join(namePartsUa, " "));
                            if (null == currentRaion) {
                                if (null == currentRegion) {
                                    if (StringUtils.equals(currentCommune.getName(), capitalize(lowerCase("КИЇВ")))) {
                                        currentRegion = new Region();
                                        currentRegion.setName(capitalize(lowerCase("КИЇВ")));
                                        currentRegion.setTransliteratedName("Kîiiv-oraș");
                                        currentRegion.setRomanianName("orașul Kiev");
                                    }
                                    if (StringUtils.equals(currentCommune.getName(), capitalize(lowerCase("СЕВАСТОПОЛЬ")))) {
                                        currentRegion = new Region();
                                        currentRegion.setName(capitalize(lowerCase("СЕВАСТОПОЛЬ")));
                                        currentRegion.setTransliteratedName("Sevastopol-oraș");
                                        currentRegion.setRomanianName("orașul Sevastopol");
                                    }
                                }
                                if (currentRegion.getCities().size() == 0 && StringUtils.isEmpty(currentRegion.getName())) {
                                    currentRegion.setName(currentCommune.getName());
                                    currentRegion.setTransliteratedName(currentCommune.getTransliteratedName());
                                    currentRegion.setRomanianName(getRomanianName(getPossibleNames(currentRegion)));
                                    System.out.println("REGION - " + currentRegion.getName() + " - "
                                        + currentRegion.getTransliteratedName());
                                }
                                currentRegion.getCities().add(currentCommune);
                                currentCommune.setRegion(currentRegion);
                                session.save(currentRegion);
                            }
                        }
                        final Transliterator t1 = new UkrainianTransliterator(currentCommune.getName());
                        final String numeTransliterat1 = t1.transliterate();

                        if (null != currentRaion) {
                            if (currentRaion.getCommunes().size() == 0) {
                                currentRaion.setName(currentCommune.getName());
                                currentRaion.setTransliteratedName(numeTransliterat1);
                                currentRaion.setRomanianName(getRomanianName(getPossibleNames(currentRaion)));
                                currentCommune.setRaion(currentRaion);
                                System.out.println("Raion " + currentRaion.getName() + " - "
                                    + currentRaion.getTransliteratedName());
                            }
                            currentRaion.getCommunes().add(currentCommune);
                            session.save(currentRaion);
                        }
                        if (2 == currentCommune.getTown()) {
                            System.out.println(currentCommune.getTown() + " - " + currentCommune.getName() + " - "
                                + currentCommune.getTransliteratedName());
                        }

                        for (int langIndex = 0; langIndex < line.length - 1; langIndex++) {
                            final String langData = line[1 + langIndex];
                            int langListIndex = langIndex;
                            if (langIndex == 11) {
                                langListIndex = 7;
                            }
                            if (langIndex > 11) {
                                langListIndex = langIndex - 1;
                            }
                            Double langNumber = -1.0;
                            try {
                                langNumber = Double.parseDouble(langData);
                            } catch (final NumberFormatException nfe) {
                                continue;
                            }
                            if (langIndex == 11) {
                                final Double moldValue = currentCommune.getLanguageStructure().get(limbi.get(7));
                                if (null != moldValue && 0 < moldValue.doubleValue()) {
                                    langNumber += moldValue;
                                }
                            }
                            if (0 < langNumber) {
                                currentCommune.getLanguageStructure().put(limbi.get(langListIndex), langNumber);
                            }
                        }
                        session.save(currentCommune);
                    }
                    // sat
                    if (StringUtils.equals(splitName[0], "s.") || StringUtils.equals(splitName[0], "s-șce.")) {
                        int i = splitName.length;
                        final Settlement sat = new Settlement();
                        for (i = 1; i < splitName.length && isAlpha(StringUtils.replace(splitName[i], "-", "")); i++) {
                            final String[] lineSeparatedParts = split(splitName[i], '-');
                            for (int j = 0; j < lineSeparatedParts.length; j++) {
                                lineSeparatedParts[j] = capitalize(lowerCase(lineSeparatedParts[j]));
                            }
                            splitName[i] = join(lineSeparatedParts, '-');

                            final String[] lineSeparatedPartsUa = split(splitNameUa[i], '-');
                            for (int j = 0; j < lineSeparatedPartsUa.length; j++) {
                                lineSeparatedPartsUa[j] = capitalize(lowerCase(lineSeparatedPartsUa[j]));
                            }
                            splitNameUa[i] = join(lineSeparatedPartsUa, '-');
                        }

                        final String[] nameParts = ArrayUtils.subarray(splitName, 1, i);
                        final String[] namePartsUa = ArrayUtils.subarray(splitNameUa, 1, i);
                        sat.setTransliteratedName(join(nameParts, " "));
                        sat.setName(join(namePartsUa, " "));
                        if (0 == currentCommune.getSettlements().size() && 0 == currentCommune.getTown()) {
                            currentCommune.setName(sat.getName());
                            currentCommune.setTransliteratedName(sat.getTransliteratedName());
                            currentCommune.setRomanianName(getRomanianName(getPossibleNames(currentCommune)));
                            System.out.println(currentCommune.getTown() + " - " + currentCommune.getName() + " - "
                                + currentCommune.getTransliteratedName());
                        }
                        sat.setCommune(currentCommune);
                        sat.setRomanianName(getRomanianName(getPossibleNames(sat)));
                        currentCommune.getSettlements().add(sat);

                        for (int langIndex = 0; langIndex < line.length - 1; langIndex++) {
                            final String langData = line[1 + langIndex];
                            int langListIndex = langIndex;
                            if (langIndex == 11) {
                                langListIndex = 7;
                            }
                            if (langIndex > 11) {
                                langListIndex = langIndex - 1;
                            }
                            Double langNumber = -1.0;
                            try {
                                langNumber = Double.parseDouble(langData);
                            } catch (final NumberFormatException nfe) {
                                continue;
                            }
                            if (langIndex == 11) {
                                final Double moldValue = sat.getLanguageStructure().get(limbi.get(7));
                                if (null != moldValue && 0 < moldValue.doubleValue()) {
                                    langNumber += moldValue;
                                }
                            }
                            if (0 < langNumber) {
                                sat.getLanguageStructure().put(limbi.get(langListIndex), langNumber);
                            }
                        }

                        session.save(sat);
                        session.save(currentCommune);
                        System.out.println("\tsat " + sat.getName() + " - " + sat.getTransliteratedName());
                    }
                }
            } catch (final UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (final FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (final IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                if (null != reader) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }
        session.getTransaction().commit();
        session.close();
    }

    private String getRomanianName(final List<String> possibleNames) {
        try {

            final boolean[] existance = rowiki.exists(possibleNames.toArray(new String[possibleNames.size()]));
            for (int i = 0; i < possibleNames.size(); i++) {
                if (!existance[i]) {
                    continue;
                }
                String redirectResolution = rowiki.resolveRedirect(possibleNames.get(i));
                if (StringUtils.isEmpty(redirectResolution)) {
                    redirectResolution = possibleNames.get(i);
                }
                redirectResolution = StringUtils.removeStart(redirectResolution, "Raionul ");
                redirectResolution = StringUtils.removeStart(redirectResolution, "Regiunea ");
                redirectResolution = StringUtils.substringBefore(redirectResolution, ",");
                redirectResolution = StringUtils.substringBefore(redirectResolution, "(");
                redirectResolution = StringUtils.trim(redirectResolution);
                return redirectResolution;
            }
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return "";
    }

    private List<String> getPossibleNames(final Settlement sat) {
        final List<String> possibleNames = new ArrayList<String>();
        possibleNames.add(sat.getTransliteratedName() + ", Ucraina");
        possibleNames.add(sat.getTransliteratedName() + " (Ucraina)");
        if (null != sat.getCommune().getRaion()) {
            possibleNames.add(sat.getTransliteratedName() + ", " + sat.getCommune().getRaion().getTransliteratedName());
            possibleNames.add(sat.getTransliteratedName() + ", " + sat.getCommune().getRaion().getRomanianName());
        }
        possibleNames.add(sat.getTransliteratedName());
        return possibleNames;
    }

    private List<String> getPossibleNames(final Commune commune) {
        final List<String> possibleNames = new ArrayList<String>();
        possibleNames.add(commune.getTransliteratedName() + ", Ucraina");
        possibleNames.add(commune.getTransliteratedName() + " (Ucraina)");
        if (null != commune.getRaion()) {
            possibleNames.add(commune.getTransliteratedName() + ", " + commune.getRaion().getTransliteratedName());
            possibleNames.add(commune.getTransliteratedName() + ", " + commune.getRaion().getRomanianName());
        }
        possibleNames.add(commune.getTransliteratedName());
        return possibleNames;
    }

    private List<String> getPossibleNames(final Raion raion) {
        final List<String> possibleNames = new ArrayList<String>();
        possibleNames.add("Raionul " + raion.getTransliteratedName() + ", Ucraina");
        possibleNames.add("Raionul " + raion.getTransliteratedName() + " (Ucraina)");
        if (null != raion.getRegion()) {
            possibleNames.add("Raionul " + raion.getTransliteratedName() + ", " + raion.getRegion().getTransliteratedName());
            possibleNames.add("Raionul " + raion.getTransliteratedName() + ", " + raion.getRegion().getRomanianName());
        }
        possibleNames.add("Raionul " + raion.getTransliteratedName());
        return possibleNames;
    }

    private List<String> getPossibleNames(final Region raion) {
        final List<String> possibleNames = new ArrayList<String>();
        possibleNames.add("Regiunea " + raion.getTransliteratedName() + ", Ucraina");
        possibleNames.add("Regiunea " + raion.getTransliteratedName() + " (Ucraina)");
        possibleNames.add("Regiunea " + raion.getTransliteratedName());
        return possibleNames;
    }
}
