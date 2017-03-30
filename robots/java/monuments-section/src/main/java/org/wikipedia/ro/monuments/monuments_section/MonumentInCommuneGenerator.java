package org.wikipedia.ro.monuments.monuments_section;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.regex;
import static org.wikipedia.ro.monuments.monuments_section.Utils.capitalize;
import static org.wikipedia.ro.monuments.monuments_section.Utils.joinWithConjunction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.bson.Document;
import org.wikipedia.ro.monuments.monuments_section.data.Monument;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class MonumentInCommuneGenerator {
    private String county;
    private String communeName;
    private MongoClient mongoClient;

    public MonumentInCommuneGenerator(String county, String communeName) {
        super();
        this.county = county;
        this.communeName = communeName;
    }

    public void setMongoClient(MongoClient client) {
        mongoClient = client;
    }

    private static String[][] MONUMENT_TYPE_DESCRIPTIONS = new String[][] {
        new String[] { "monument istoric", "monumentul istoric", "monumente istorice", "monumentele istorice" },
        new String[] { "sit arheologic", "situl arheologic", "situri arheologice", "siturile arheologice" },
        new String[] { "monument istoric de arhitectură", "monumentul istoric de arhitectură",
            "monumente istorice de arhitectură", "monumentele istorice de arhitectură" },
        new String[] { "monument de for public", "monumentul de for public", "monumente de for public",
            "monumentele de for public" },
        new String[] { "monument memorial sau funerar", "monumentul memorial sau funerar",
            "monumente memoriale sau funerare", "monumentele memoriale sau funerare" } };

    private static final Map<Character, String[]> UNIT_TYPE_DESCRIPTIONS = new HashMap<Character, String[]>() {
        {
            put('m', new String[] { "municipiu", "municipiul" });
            put('o', new String[] { "oraș", "orașul" });
            put('c', new String[] { "comună", "comuna" });
        }
    };

    private static final Map<String, String> COUNTY_NAMES = new HashMap<String, String>() {
        {
            put("AB", "Alba");
            put("AR", "Arad");
            put("AG", "Argeș");
            put("BC", "Bacău");
            put("BH", "Bihor");
            put("BN", "Bistrița-Năsăud");
            put("BT", "Botoșani");
            put("BV", "Brașov");
            put("BR", "Brăila");
            put("BZ", "Buzău");
            put("CS", "Caraș-Severin");
            put("CJ", "Cluj");
            put("CT", "Constanța");
            put("CV", "Covasna");
            put("DB", "Dâmbovița");
            put("DJ", "Dolj");
            put("GL", "Galați");
            put("GR", "Giurgiu");
            put("GJ", "Gorj");
            put("HR", "Harghita");
            put("HD", "Hunedoara");
            put("IL", "Ialomița");
            put("IS", "Iași");
            put("IF", "Ilfov");
            put("MM", "Maramureș");
            put("MH", "Mehedinți");
            put("MS", "Mureș");
            put("NT", "Neamț");
            put("OT", "Olt");
            put("PH", "Prahova");
            put("SM", "Satu Mare");
            put("SJ", "Sălaj");
            put("SB", "Sibiu");
            put("SV", "Suceava");
            put("TR", "Teleorman");
            put("TM", "Timiș");
            put("TL", "Tulcea");
            put("VS", "Vaslui");
            put("VL", "Vâlcea");
            put("VN", "Vrancea");

        }
    };

    private Pattern computeRegex(String communeName) {
        StringBuilder communePatternBuilder = new StringBuilder();

        switch (communeName.charAt(0)) {
        case 'm':
        case 'c':
            communePatternBuilder.append(UNIT_TYPE_DESCRIPTIONS.get(communeName.charAt(0))[1]);
            break;
        case 'o':
            communePatternBuilder.append(UNIT_TYPE_DESCRIPTIONS.get(communeName.charAt(0))[0]);
            break;
        }

        communePatternBuilder.append("\\s+\\[\\[");
        if (communeName.charAt(0) == 'c') {
            communePatternBuilder.append("Comuna\\s+");
        }

        communePatternBuilder.append(communeName.substring(1)).append(".*");

        return Pattern.compile(communePatternBuilder.toString());
    }

    public String generate() {
        try {

            MongoDatabase db = mongoClient.getDatabase("monumente");

            MongoCollection<Document> collection = db.getCollection("Monument");

            Pattern nationalScopedPattern = Pattern.compile("^" + county + "-[IV]+-[sma]-A-\\d+$");
            /*
             * Pattern localScopedArcheoPattern = Pattern.compile(county + "-I-[sma]-B-\\d+"); Pattern
             * localScopedArchiPattern = Pattern.compile(county + "-II-[sma]-B-\\d+"); Pattern localScopedForumPattern =
             * Pattern.compile(county + "-III-[sma]-B-\\d+"); Pattern localScopedMemorialPattern = Pattern.compile(county +
             * "-IV-[sma]-B-\\d+");
             */
            Pattern localScopedPattern = Pattern.compile("^" + county + "-[IV]+-[sma]-B-\\d+$");

            FindIterable<Document> nationalScoped =
                collection.find(and(regex("Cod", nationalScopedPattern), regex("Localitate", computeRegex(communeName))));
            FindIterable<Document> localScoped =
                collection.find(and(regex("Cod", localScopedPattern), regex("Localitate", computeRegex(communeName))));

            final List<Monument> nationalScopedMonuments = new ArrayList<Monument>();
            nationalScoped.forEach(new MonumentCollectorBlock(nationalScopedMonuments));
            processSubmons(collection, nationalScopedMonuments);

            final List<Monument> localScopedMonuments = new ArrayList<Monument>();
            localScoped.forEach(new MonumentCollectorBlock(localScopedMonuments));
            processSubmons(collection, localScopedMonuments);

            // printMons(nationalScopedMonuments);
            // System.out.println();
            // printMons(localScopedArcheoMonuments);
            // System.out.println();
            // printMons(localScopedArchiMonuments);
            // System.out.println();
            List<String> paragraphs = new ArrayList<String>();
            if (nationalScopedMonuments.size() > 0) {
                StringBuilder paraBuilder = new StringBuilder();
                paraBuilder.append("În ");
                paraBuilder.append(retrieveQualifiedCommuneName(communeName));
                paraBuilder.append(" se află ");
                paraBuilder.append(retrieveTextForMultipleTypeMons(nationalScopedMonuments, "de interes național"));
                paragraphs.add(paraBuilder.toString());
            }
            if (localScopedMonuments.size() > 0) {
                StringBuilder paraBuilder = new StringBuilder();
                boolean capitalize = true;
                if (0 < paragraphs.size()) {
                    paraBuilder.append("În rest, ");
                    capitalize = false;
                }
                String monumentsListSizeInWords = new NumberToWordsConvertor(localScopedMonuments.size()).convert();
                if (capitalize) {
                    monumentsListSizeInWords = capitalize(monumentsListSizeInWords);
                }
                if (1 == localScopedMonuments.size()) {
                    monumentsListSizeInWords = monumentsListSizeInWords + " singur";
                }

                paraBuilder.append(monumentsListSizeInWords).append(" obiectiv");
                if (1 != localScopedMonuments.size()) {
                    paraBuilder.append('e');
                }
                paraBuilder.append(" din ").append(UNIT_TYPE_DESCRIPTIONS.get(communeName.charAt(0))[0]).append(' ')
                    .append(1 == localScopedMonuments.size() ? "este": "sunt").append(" inclus").append(1 == localScopedMonuments.size()? "" : "e")
                    .append(" în [[lista monumentelor istorice din județul ").append(COUNTY_NAMES.get(county))
                    .append("]]: ");
                paraBuilder.append(retrieveTextForMultipleTypeMons(localScopedMonuments, "de interes local"));
                paragraphs.add(paraBuilder.toString());
            }

            return joinWithConjunction(paragraphs, ".\n\n", ".\n\n");
        } finally {
            if (null != mongoClient) {
                mongoClient.close();
            }
        }
    }

    // big big function that implements converting the lists
    private String retrieveTextForMultipleTypeMons(List<Monument> monList, String scopeDescription) {
        List<List<Monument>> splitMonuments = splitMonumentsByType(monList);
        if (splitMonuments.size() == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();

        if (splitMonuments.size() == 1) { // only one type of monuments
            if (splitMonuments.get(0).size() == 1) { // only one monument total
                Monument theMonument = splitMonuments.get(0).get(0);
                sb.append(MONUMENT_TYPE_DESCRIPTIONS[theMonument.type][1]).append(' ').append(scopeDescription).append(' ');
                sb.append(theMonument.name);
                sb.append(" datând din ").append(theMonument.dating);
                if (0 < theMonument.submonuments.size()) {
                    sb.append(", ansamblu alcătuit din ");
                    sb.append(retrieveSubmonumentsText(theMonument));
                }
            } else { // more monuments of only one type
                sb.append(", toate clasificate ca ").append(MONUMENT_TYPE_DESCRIPTIONS[splitMonuments.get(0).get(0).type][2])
                    .append(' ').append(scopeDescription).append(": ");
                List<String> monumentDescriptions = generateMonumentsListDescription(splitMonuments.get(0));
                sb.append(joinWithConjunction(monumentDescriptions, ", ", " și "));
            }
        } else { // more types of monuments
            String introWordSingle = "Unul";
            String introWordMultiple = "";
            sb.append(new NumberToWordsConvertor(monList.size()).convert()).append(' ')
                .append(MONUMENT_TYPE_DESCRIPTIONS[0][2]).append(' ').append(scopeDescription).append(". ");
            for (List<Monument> eachMonumentTypeList : splitMonuments) {
                if (1 == eachMonumentTypeList.size()) {
                    sb.append(introWordSingle).append(" este ")
                        .append(MONUMENT_TYPE_DESCRIPTIONS[eachMonumentTypeList.get(0).type][1]).append(' ');

                } else {
                    sb.append(introWordMultiple).append(' ')
                        .append(new NumberToWordsConvertor(eachMonumentTypeList.size()).convert()).append(" sunt ")
                        .append(MONUMENT_TYPE_DESCRIPTIONS[eachMonumentTypeList.get(0).type][3]).append(": ");
                }
                introWordSingle = "Altul";
                introWordMultiple = "Alte";
                List<String> monumentDescriptions = generateMonumentsListDescription(eachMonumentTypeList);
                sb.append(joinWithConjunction(monumentDescriptions, "; ", "; și ")).append(". ");
            }
        }

        return sb.toString();
    }

    private List<String> generateMonumentsListDescription(List<Monument> monumentsList) {
        int monIdx = 0;
        List<String> monumentDescriptions = new ArrayList<String>();
        for (Monument eachMonument : monumentsList) {
            StringBuilder eachMonumentDescription = new StringBuilder(retrieveMonumentMention(eachMonument));
            if (0 < eachMonument.submonuments.size()) {
                eachMonumentDescription.append("\u00A0\u2014 ansamblu alcătuit din ")
                    .append(retrieveSubmonumentsText(eachMonument));
                if (monIdx < monumentsList.size() - 1) {
                    eachMonumentDescription.append("\u00A0\u2014");
                }
            }

            monumentDescriptions.add(eachMonumentDescription.toString());
            monIdx++;
        }
        return monumentDescriptions;
    }

    private String retrieveMonumentMention(Monument theMonument) {
        StringBuilder monumentMentionBuilder = new StringBuilder(theMonument.name);
        if (null != theMonument.dating && 0 < theMonument.dating.trim().length()) {
            monumentMentionBuilder.append(" (").append(theMonument.dating).append(")");
        }
        return monumentMentionBuilder.toString();
    }

    private String retrieveSubmonumentsText(Monument theMonument) {
        if (theMonument.submonuments.size() == 0) {
            return "";
        }

        List<String> submonumentDescriptions = new ArrayList<String>();
        List<Monument> sortedSubmonuments = new ArrayList<Monument>(theMonument.submonuments);
        Collections.sort(sortedSubmonuments,
            (Monument m1, Monument m2) -> m1.supplementalCodeNumber.compareTo(m2.supplementalCodeNumber));
        for (Monument eachSubmon : sortedSubmonuments) {
            submonumentDescriptions.add(retrieveMonumentMention(eachSubmon));
        }
        return joinWithConjunction(submonumentDescriptions, ", ", " și ");
    }

    private String retrieveQualifiedCommuneName(String communeName2) {
        StringBuilder communeQualifiedNameBuilder = new StringBuilder();

        communeQualifiedNameBuilder.append(UNIT_TYPE_DESCRIPTIONS.get(communeName2.charAt(0))[1]).append(' ')
            .append(communeName.substring(1));
        return communeQualifiedNameBuilder.toString();
    }

    private List<List<Monument>> splitMonumentsByType(List<Monument> monList) {
        Map<Integer, List<Monument>> listsMap = new HashMap<Integer, List<Monument>>();
        for (Monument eachMon : monList) {
            List<Monument> specificMonumentList = listsMap.get(eachMon.type);
            if (null == specificMonumentList) {
                specificMonumentList = new ArrayList<Monument>();
            }
            specificMonumentList.add(eachMon);
            listsMap.put(eachMon.type, specificMonumentList);
        }
        return new ArrayList<List<Monument>>(listsMap.values());
    }

    private void processSubmons(MongoCollection<Document> collection, final List<Monument> nationalScopedMonuments) {
        for (int monIdx = 0; monIdx < nationalScopedMonuments.size(); monIdx++) {
            Monument eachMon = nationalScopedMonuments.get(monIdx);

            if (Arrays.asList('a', 's').contains(eachMon.structure)) {
                String romanNum = new String[] { "I", "II", "III", "IV" }[eachMon.type - 1];
                FindIterable<Document> submons = collection.find(regex("Cod",
                    Pattern.compile(eachMon.county + "-" + romanNum + "-[sm]-[AB]-" + eachMon.codeNumber + "\\.\\d+")));
                submons.forEach(new MonumentCollectorBlock(eachMon.submonuments));
            }
        }
    }
}
