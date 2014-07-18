package org.wikipedia.ro.populationdb.ua;

import java.io.File;

public class UAPercentagesParser {

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

    }

}
