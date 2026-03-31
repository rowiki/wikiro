package org.wikipedia.ro.textgen;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class App {
    public static void main(String[] args) {
        Options options = new Options();
        options.addOption(null, "bauer-name", true, "Name in Bauer's memoirs");
        options.addOption(null, "bauer-description", true, "Description from Bauer's memoirs");
        options.addOption(null, "bauer-page", true, "Page in Bauer's memoirs");
        options.addOption(null, "specht-name", true, "Name on the Specht map");
        options.addOption(null, "cata-name", true, "Name in the 1831 catography");
        options.addOption(null, "cata-plasa", true, "Plasa in the 1831 catography");
        options.addOption(null, "cata-county", true, "County in the 1831 catography");
        options.addOption(null, "cata-mosie", true, "Moșie in the 1831 catography");
        options.addOption(null, "cata-owner", true, "Owner in the 1831 catography");
        options.addOption(null, "cata-pop", true, "Families in the 1831 catography");
        options.addOption(null, "cata-feci", true, "Feciori de muncă in the 1831 catography");
        options.addOption(null, "cata-page", true, "Page in the 1831 catography source");
        options.addOption(null, "idx1954-name", true, "Name in the 1954 locality index");
        options.addOption(null, "idx1954-descr", true, "Description in the 1954 locality index");
        options.addOption(null, "idx1954-page", true, "Page in the 1954 locality index");
        options.addOption(null, "idx1956-name", true, "Name in the 1956 locality index");
        options.addOption(null, "idx1956-descr", true, "Description in the 1956 locality index");
        options.addOption(null, "idx1956-page", true, "Page in the 1956 locality index");
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);

            VillageHistoryParams params = mapCommandLineToParams(cmd);
            VillageHistoryService service = new VillageHistoryService();
            System.out.println(service.buildText(params));

        } catch (ParseException e) {
            System.err.println("Error parsing arguments: " + e.getMessage());
            new HelpFormatter().printHelp("textgen", options);
        }
    }

    private static VillageHistoryParams mapCommandLineToParams(CommandLine cmd) {
        VillageHistoryParams p = new VillageHistoryParams();
        p.setBauerName(cmd.getOptionValue("bauer-name"));
        p.setBauerDescription(cmd.getOptionValue("bauer-description"));
        p.setBauerPage(cmd.getOptionValue("bauer-page"));
        p.setSpechtName(cmd.getOptionValue("specht-name"));
        p.setCataName(cmd.getOptionValue("cata-name"));
        p.setCataPlasa(cmd.getOptionValue("cata-plasa"));
        p.setCataCounty(cmd.getOptionValue("cata-county"));
        p.setCataMosie(cmd.getOptionValue("cata-mosie"));
        p.setCataOwner(cmd.getOptionValue("cata-owner"));
        p.setCataPop(cmd.getOptionValue("cata-pop"));
        p.setCataFeci(cmd.getOptionValue("cata-feci"));
        p.setCataPage(cmd.getOptionValue("cata-page"));
        p.setIdx1954Name(cmd.getOptionValue("idx1954-name"));
        p.setIdx1954Descr(cmd.getOptionValue("idx1954-descr"));
        p.setIdx1954Page(cmd.getOptionValue("idx1954-page"));
        p.setIdx1956Name(cmd.getOptionValue("idx1956-name"));
        p.setIdx1956Descr(cmd.getOptionValue("idx1956-descr"));
        p.setIdx1956Page(cmd.getOptionValue("idx1956-page"));
        return p;
    }
}