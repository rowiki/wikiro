package org.wikipedia.ro.populationdb.ua;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

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
                while (null != (line = reader.readNext())) {
                    final String nume = line[0];
                    final Transliterator t = new UkrainianTransliterator(nume);
                    final String numeTransliterat = t.transliterate();
                    System.out.println(nume + " - " + numeTransliterat);
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
