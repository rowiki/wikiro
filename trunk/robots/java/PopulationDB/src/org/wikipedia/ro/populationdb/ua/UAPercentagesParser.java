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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
import org.wikipedia.ro.populationdb.ua.model.LanguageStructurable;
import org.wikipedia.ro.populationdb.ua.model.Raion;
import org.wikipedia.ro.populationdb.ua.model.Region;
import org.wikipedia.ro.populationdb.ua.model.Settlement;
import org.wikipedia.ro.populationdb.util.Transliterator;
import org.wikipedia.ro.populationdb.util.UkrainianTransliterator;

import au.com.bytecode.opencsv.CSVReader;

public class UAPercentagesParser {

    private static File[] files;

    public UAPercentagesParser(final File[] files2) {
        final List<File> fileList = new ArrayList<File>();
        fileList.addAll(Arrays.asList(files2));
        Collections.sort(fileList, new Comparator<File>() {

            public int compare(final File arg0, final File arg1) {
                return arg0.getName().compareTo(arg1.getName());
            }
        });
        this.files = fileList.toArray(new File[fileList.size()]);
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
            parser.performCorrection();
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

    public void performCorrection() {
        final Hibernator hib = new Hibernator();
        final Session ses = hib.getSession();

        final Region kievReg = hib.getRegionByTransliteratedName("Bila Țerkva");
        if (null != kievReg) {
            kievReg.setName(capitalize(lowerCase("КИЇВ")));
            kievReg.setTransliteratedName("Kîiiv");
            kievReg.setRomanianName("Kiev");

            final Commune kievCommune = hib.getCommuneByRomanianName("Kiev");
            kievReg.setCapital(kievCommune);
            ses.save(kievReg);
        }
        final Region transcarpatiaRegion = hib.getRegionByTransliteratedName("Ujhorod");
        if (null != transcarpatiaRegion) {
            transcarpatiaRegion.setName(capitalize(lowerCase("ЗАКАРПАТСЬКА")));
            transcarpatiaRegion.setRomanianName("Transcarpatia");
            transcarpatiaRegion.setTransliteratedName("Zakarpatska");
            ses.save(transcarpatiaRegion);
        }
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Krîm", "Bratska", "Krasnoperekopsk");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Vinnîțea", "Berezneanska", "Hmilnîk");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Dnipropetrovsk", "Bohdanivska", "Pavlohrad");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Donețk", "Oleksandro-Kalînovska", "Kosteantînivka");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Donețk", "V" + lowerCase("ELÎKOȘÎȘIVSKA"), "Șahtarsk");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Jîtomîr", "B" + lowerCase("ILKIVSKA"), "Korosten");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Zaporijjea", "V" + lowerCase("ELÎKOBILOZERSKA"),
            "Velîka Bilozerka");
        fixRaionNameAndCapitalByTransliteratedNames(hib, kievReg.getTransliteratedName(), "V"
            + lowerCase("ELÎKOOLEKSANDRIVSKA"), "Borîspil");
        fixRaionNameAndCapitalByTransliteratedNames(hib, kievReg.getTransliteratedName(),
            "V" + lowerCase("ELÎKOKARATULSKA"), "Pereiaslav-Hmelnîțkîi");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Kirovohrad", "B" + lowerCase("OHDANIVSKA"), "Znameanka");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Kirovohrad", "A" + lowerCase("DJAMSKA"), "Kirovohrad");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Kirovohrad", "V" + lowerCase("ELÎKOANDRUSIVSKA"), "Svitlovodsk");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Mîkolaiiv", "Dmîtrivska", "Oceakiv");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Odesa", "Adamivska", "Bilhorod-Dnistrovskîi");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Odesa", "Oleksiivka", "Kotovsk");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Poltava", "Bilețkivska", "Kremenciuk");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Poltava", "Berezivska", "Lubnî");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Poltava", "Abazivska", "Poltava");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Rivne", "Bilașivska", "Ostroh");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Sumî", "Bîșkinska", "Lebedîn");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Sumî", "Anastasivska", "Romnî");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Harkiv", "Oleksandrivska", "Izium");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Harkiv", "Vîșnivska", "Kupeansk");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Harkiv", "Oleksiivka", "Pervomaiskîi");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Herson", "Vasîlivka", "Kahovka");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Hmelnîțkîi", "Hannopilska", "Slavuta");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Hmelnîțkîi", "Bahlaiivska", "Starokosteantîniv");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Cerkasî", "Antîpivska", "Zolotonoșa");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Cerkasî", "Berzokivska", "Kaniv");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Cerkasî", "Balakliivska", "Smila");

        if (null != kievReg) {
            final Raion raion = hib.getRaionByTransliteratedNameAndRegion("VOLODARSKA", kievReg);
            if (null != raion) {
                final Commune com = hib.getCommuneByTransliteratedNameAndRaion("Kraseatîci", raion);
                if (null != com) {
                    raion.setTransliteratedName("Poliske");
                    raion.setName(capitalize(lowerCase("Поліський")));
                    raion.setRomanianName("");
                    raion.setCapital(com);
                    ses.save(raion);
                }
            }
        }
        ses.beginTransaction();
        ses.getTransaction().commit();
    }

    private void fixRaionNameAndCapitalByTransliteratedNames(final Hibernator hib, final String regionName,
                                                             final String raionWrongName, final String correctCapitalName) {
        final Region reg = hib.getRegionByTransliteratedName(regionName);
        if (null != reg) {
            final Raion raion = hib.getRaionByTransliteratedNameAndRegion(raionWrongName, reg);
            if (null != raion) {
                final Commune com = hib.getCommuneByTransliteratedNameAndRaion(correctCapitalName, reg);
                if (null != com) {
                    raion.setTransliteratedName(com.getTransliteratedName());
                    raion.setName(com.getName());
                    raion.setRomanianName(com.getName());
                    raion.setCapital(com);
                    final Session ses = hib.getSession();
                    ses.save(raion);
                }
            }
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
                int currentCommuneLevel = 2;
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
                    if (ArrayUtils.contains(splitName, "OBLAST")
                        || (ArrayUtils.contains(splitName, "KRÎM") && ArrayUtils.contains(splitName, "AVTONOMNA") && ArrayUtils
                            .contains(splitName, "RESPUBLIKA"))) {
                        currentRegion = new Region();
                        currentRaion = null;
                        if (ArrayUtils.contains(splitName, "KRÎM")) {
                            currentRegion.setRomanianName("Crimeea");
                            currentRegion.setTransliteratedName("Krîm");
                            currentRegion.setName(capitalize(lowerCase("КРИМ")));
                        }
                        extractLanguageData(limbi, line, currentRegion);
                        session.save(currentRegion);
                        currentCommuneLevel = 2;
                        continue;
                    }
                    // raion
                    if (ArrayUtils.contains(splitName, "RAION")) {
                        currentRaion = new Raion();
                        currentRegion.getRaioane().add(currentRaion);
                        currentRaion.setRegion(currentRegion);
                        if (ArrayUtils.contains(splitName, "RAION")) {
                            final int indexOfRaion = ArrayUtils.indexOf(splitName, "RAION");
                            final String[] nameParts = ArrayUtils.subarray(splitName, 0, indexOfRaion);
                            final String[] namePartsUa = ArrayUtils.subarray(splitNameUa, 0, indexOfRaion);
                            for (int i = 0; i < nameParts.length; i++) {
                                final String[] lineSeparatedParts = split(nameParts[i], '-');
                                for (int j = 0; j < lineSeparatedParts.length; j++) {
                                    lineSeparatedParts[j] = capitalize(lowerCase(lineSeparatedParts[j]));
                                }
                                nameParts[i] = join(lineSeparatedParts, '-');

                                final String[] lineSeparatedPartsUa = split(namePartsUa[i], '-');
                                for (int j = 0; j < lineSeparatedPartsUa.length; j++) {
                                    lineSeparatedPartsUa[j] = capitalize(lowerCase(lineSeparatedPartsUa[j]));
                                }
                                namePartsUa[i] = join(lineSeparatedPartsUa, '-');
                            }
                            currentRaion.setTransliteratedName(join(nameParts, " "));
                            currentRaion.setName(join(namePartsUa, " "));
                            extractLanguageData(limbi, line, currentRaion);
                        }
                        session.save(currentRaion);
                        currentCommuneLevel = 2;
                        continue;
                    }
                    if (ArrayUtils.contains(splitName, "(miskrada)")) {
                        currentRaion = new Raion();
                        final int indexOfMiskrada = ArrayUtils.indexOf(splitName, "(miskrada)");
                        final String[] nameParts = ArrayUtils.subarray(splitName, 0, indexOfMiskrada);
                        final String[] namePartsUa = ArrayUtils.subarray(splitNameUa, 0, indexOfMiskrada);

                        for (int i = 0; i < nameParts.length; i++) {
                            final String[] lineSeparatedParts = split(nameParts[i], '-');
                            for (int j = 0; j < lineSeparatedParts.length; j++) {
                                lineSeparatedParts[j] = capitalize(lowerCase(lineSeparatedParts[j]));
                            }
                            nameParts[i] = join(lineSeparatedParts, '-');

                            final String[] lineSeparatedPartsUa = split(namePartsUa[i], '-');
                            for (int j = 0; j < lineSeparatedPartsUa.length; j++) {
                                lineSeparatedPartsUa[j] = capitalize(lowerCase(lineSeparatedPartsUa[j]));
                            }
                            namePartsUa[i] = join(lineSeparatedPartsUa, '-');
                        }
                        currentRaion.setTransliteratedName(join(nameParts, " "));
                        currentRaion.setName(join(namePartsUa, " "));
                        currentRaion.setMiskrada(true);
                        currentRaion.setRegion(currentRegion);
                        currentCommuneLevel = 2;
                        System.out.println(" -- MISKRADA " + currentRaion.getName() + " - "
                            + currentRaion.getTransliteratedName());
                        extractLanguageData(limbi, line, currentRaion);
                        session.save(currentRaion);

                        if (null != currentCommune
                            && StringUtils.equals(currentCommune.getName(), capitalize(lowerCase("СЕВАСТОПОЛЬ")))) {
                            currentRegion = new Region();
                            currentRegion.setName(capitalize(lowerCase("СЕВАСТОПОЛЬ")));
                            currentRegion.setTransliteratedName("Sevastopol-oraș");
                            currentRegion.setRomanianName("orașul Sevastopol");
                            session.save(currentRegion);
                        }
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
                                final String[] lineSeparatedParts = split(nameParts[i], '-');
                                for (int j = 0; j < lineSeparatedParts.length; j++) {
                                    lineSeparatedParts[j] = capitalize(lowerCase(lineSeparatedParts[j]));
                                }
                                nameParts[i] = join(lineSeparatedParts, '-');

                                final String[] lineSeparatedPartsUa = split(namePartsUa[i], '-');
                                for (int j = 0; j < lineSeparatedPartsUa.length; j++) {
                                    lineSeparatedPartsUa[j] = capitalize(lowerCase(lineSeparatedPartsUa[j]));
                                }
                                namePartsUa[i] = join(lineSeparatedPartsUa, '-');
                            }

                            currentCommune.setTransliteratedName(join(nameParts, " "));
                            // currentCommune.setRomanianName(getRomanianName(getPossibleNames(currentCommune)));
                            currentCommune.setName(join(namePartsUa, " "));
                            currentCommune.setTown(0);
                            currentCommune.setRaion(currentRaion);
                            session.save(currentCommune);
                        }
                        if (StringUtils.equals(splitName[0], "smt")) {
                            int i = splitName.length;
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
                            currentCommune.setTown(2);
                            currentCommune.setTransliteratedName(join(nameParts, " "));
                            currentCommune.setRomanianName(getRomanianName(getPossibleNames(currentCommune)));
                            currentCommune.setName(join(namePartsUa, " "));

                            if (currentCommuneLevel < 2) {
                                currentRaion = null;
                            }
                            if (null == currentRaion && null == currentRegion) {
                                if (StringUtils.equals(currentCommune.getName(), capitalize(lowerCase("КИЇВ")))) {
                                    currentRegion = new Region();
                                    currentRegion.setName(capitalize(lowerCase("КИЇВ")));
                                    currentRegion.setTransliteratedName("Kîiiv-oraș");
                                    currentRegion.setRomanianName("orașul Kiev");
                                    session.save(currentRegion);
                                }
                            }
                            if ((null == currentRaion || currentRaion.isMiskrada()) && null != currentRegion) {
                                if (StringUtils.isEmpty(currentRegion.getName())) {
                                    currentRegion.setName(currentCommune.getName());
                                    currentRegion.setTransliteratedName(currentCommune.getTransliteratedName());
                                    currentRegion.setRomanianName(getRomanianName(getPossibleNames(currentRegion)));
                                    currentRegion.setCapital(currentCommune);
                                    System.out.println("REGION - " + currentRegion.getName() + " - "
                                        + currentRegion.getTransliteratedName());
                                }
                                if (null == currentRaion) {
                                    currentRegion.getCities().add(currentCommune);
                                    currentCommune.setRegion(currentRegion);
                                }
                                session.save(currentRegion);
                            }
                            session.save(currentCommune);
                        }
                        final Transliterator t1 = new UkrainianTransliterator(currentCommune.getName());
                        t1.transliterate();

                        if (null != currentRaion) {
                            if (currentRaion.getCommunes().size() == 0) {
                                currentRaion.setRomanianName(getRomanianName(getPossibleNames(
                                    currentRaion,
                                    StringUtils.defaultString(currentCommune.getRomanianName(),
                                        currentCommune.getTransliteratedName()))));
                                currentRaion.setName(currentCommune.getName());
                                currentRaion.setTransliteratedName(new UkrainianTransliterator(currentCommune.getName())
                                    .transliterate());
                                currentRaion.setCapital(currentCommune);
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

                        extractLanguageData(limbi, line, currentCommune);
                        session.save(currentCommune);
                        currentCommuneLevel = currentCommune.getTown();
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
                            currentCommune.setCapital(sat);
                            System.out.println(currentCommune.getTown() + " - " + currentCommune.getName() + " - "
                                + currentCommune.getTransliteratedName());
                        }
                        sat.setCommune(currentCommune);
                        sat.setRomanianName(getRomanianName(getPossibleNames(sat)));
                        currentCommune.getSettlements().add(sat);

                        extractLanguageData(limbi, line, sat);

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

    private void extractLanguageData(final List<Language> limbi, final String[] line,
                                     final LanguageStructurable currentCommune) {
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

    private List<String> getPossibleNames(final Raion raion, final String roName) {
        final List<String> possibleNames = new ArrayList<String>();
        possibleNames.add("Raionul " + roName + ", Ucraina");
        possibleNames.add("Raionul " + roName + " (Ucraina)");
        if (null != raion.getRegion()) {
            possibleNames.add("Raionul " + roName + ", " + raion.getRegion().getTransliteratedName());
            possibleNames.add("Raionul " + roName + ", " + raion.getRegion().getRomanianName());
        }
        possibleNames.add("Raionul " + roName);
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
