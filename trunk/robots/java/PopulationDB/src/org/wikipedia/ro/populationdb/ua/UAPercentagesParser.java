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

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.wikipedia.ro.populationdb.ua.model.Commune;
import org.wikipedia.ro.populationdb.util.Transliterator;
import org.wikipedia.ro.populationdb.util.UkrainianTransliterator;

import au.com.bytecode.opencsv.CSVReader;

public class UAPercentagesParser {

    private static File[] files;

    public UAPercentagesParser(final File[] files2) {
        this.files = files2;
    }

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

        final File[] files = inDir.listFiles();
        final UAPercentagesParser parser = new UAPercentagesParser(files);
        parser.parse();
    }

    public void parse() {
        for (final File eachFile : files) {
            CSVReader reader = null;
            try {
                reader = new CSVReader(new InputStreamReader(new FileInputStream(eachFile), "Cp1251"), '\t', '\"');
                String[] line = null;
                for (int i = 0; i < 5; i++) {
                    line = reader.readNext();
                }

                // comuna, oras, asezare urbana
                while (null != (line = reader.readNext())) {
                    final String nume = line[0];
                    final Transliterator t = new UkrainianTransliterator(nume);
                    final String numeTransliterat = t.transliterate();

                    final String[] splitName = split(numeTransliterat);
                    final String[] splitNameUa = split(nume);

                    if (splitName.length < 2) {
                        continue;
                    }

                    if (ArrayUtils.contains(splitName, "silrada") || StringUtils.equals(splitName[0], "smt")
                        || StringUtils.equals(splitName[0], "m.")) {
                        final Commune com = new Commune();
                        if (ArrayUtils.contains(splitName, "silrada")) {
                            final int indexOfSilrada = ArrayUtils.indexOf(splitName, "silrada");
                            final String[] nameParts = ArrayUtils.subarray(splitName, 0, indexOfSilrada);
                            final String[] namePartsUa = ArrayUtils.subarray(splitNameUa, 0, indexOfSilrada);

                            for (int i = 0; i < nameParts.length; i++) {
                                nameParts[i] = capitalize(lowerCase(nameParts[i]));
                                namePartsUa[i] = capitalize(lowerCase(namePartsUa[i]));
                            }

                            com.setTransliteratedName(join(nameParts, " "));
                            com.setName(join(namePartsUa, " "));
                            com.setTown(0);
                        }
                        if (StringUtils.equals(splitName[0], "smt")) {
                            int i = splitName.length;
                            for (i = 1; i < splitName.length && isAlpha(splitName[i]); i++) {
                            }
                            final String[] nameParts = ArrayUtils.subarray(splitName, 1, i);
                            final String[] namePartsUa = ArrayUtils.subarray(splitNameUa, 1, i);
                            com.setTown(1);
                            com.setTransliteratedName(capitalize(lowerCase(join(nameParts, " "))));
                            com.setName(capitalize(lowerCase(join(namePartsUa, " "))));
                        }
                        if (StringUtils.equals(splitName[0], "m.")) {
                            int i = splitName.length;
                            for (i = 1; i < splitName.length && isAlpha(splitName[i]); i++) {
                            }
                            final String[] nameParts = ArrayUtils.subarray(splitName, 1, i);
                            final String[] namePartsUa = ArrayUtils.subarray(splitNameUa, 1, i);
                            com.setTown(2);
                            com.setTransliteratedName(capitalize(lowerCase(join(nameParts, " "))));
                            com.setName(capitalize(lowerCase(join(namePartsUa, " "))));
                        }
                        final Transliterator t1 = new UkrainianTransliterator(com.getName());
                        final String numeTransliterat1 = t1.transliterate();

                        System.out.println(com.getTown() + " - " + com.getName() + " - " + numeTransliterat1);
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

    }
}
