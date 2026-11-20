package org.wikipedia.ro.java;

/**
 * Command line entry point: creates the {@link CopyCategories} executable, hands it the command line arguments
 * and runs it.
 * <p>
 * Usage: {@code java org.wikipedia.ro.java.Starter <non-existing ro category> <Wikidata item ID> [source wiki code]}
 * <p>
 * The source wiki code is optional and defaults to {@code en}.
 */
public class Starter {
    private Starter() {
    }

    public static void main(String[] args) {
        if (args.length < 2 || args.length > 3) {
            System.err.println("Usage: Starter <name of non-existing ro category> <Wikidata item ID, e.g. Q12345> "
                + "[source wiki code, default: " + CopyCategories.DEFAULT_SOURCE_WIKI_CODE + "]");
            System.exit(1);
        }
        String sourceWikiCode = args.length == 3 ? args[2] : CopyCategories.DEFAULT_SOURCE_WIKI_CODE;
        CopyCategories executable = new CopyCategories(args[0], args[1], sourceWikiCode);
        executable.doExecution();
    }
}
