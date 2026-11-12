package org.wikipedia.ro.romania;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.wikibase.WikibaseException;
import org.wikibase.data.Entity;
import org.wikibase.data.Item;
import org.wikipedia.ro.cache.WikidataEntitiesCache;
import org.wikipedia.ro.model.WikiLink;
import org.wikipedia.ro.model.WikiTemplate;
import org.wikipedia.ro.wikidata.WikidataQueryProcessor;

import lombok.extern.slf4j.Slf4j;

/**
 * Enumerates the counties of Romania and (re)writes the demography section of their ro.wikipedia
 * article from the 2021 census tables (ethnicity and religion) found in two xlsx files.
 *
 * <p>The files are given by {@code -DethnicityXlsx=} and {@code -DreligionXlsx=}. Runs in dry-run mode
 * unless started with {@code -DdryRun=false}; see {@link WikidataQueryProcessor}.
 *
 * <p>If the article already contains the delimiting comments, the block between them is replaced; otherwise
 * the block is inserted right below the "Populația"/"Demografie" heading. Articles with neither are skipped.
 */
@Slf4j
public class RomaniaCountyDemographyUpdater extends WikidataQueryProcessor {

    private static final String EDIT_SUMMARY = "Robot: secțiune demografie generată din datele recensământului din 2021";
    private static final String ETHNICITY_SHEET = "Tab.2.2.1";
    private static final String RELIGION_SHEET = "Tab 2.4.1";
    private static final String START_MARKER = "<!-- secțiune demografie -->";
    private static final String END_MARKER = "<!--sfârșit secțiune demografie-->";
    private static final Pattern MARKED_PATTERN = Pattern
            .compile("<!--\\s*secțiune demografie\\s*-->.*?<!--\\s*sfârșit secțiune demografie\\s*-->", Pattern.DOTALL);
    private static final Pattern HEADING_PATTERN = Pattern.compile("^==\\s*(Populați(?:e|a)|Demografie)[^=\\r\\n]*==[ \\t]*\\r?\\n",
            Pattern.MULTILINE);

    private static final String OTHERS = "altele";
    private static final String UNKNOWN = "necunoscută";
    private static final String COUNTY_PREFIX = "Județul ";

    private static final NumberFormat RO_NUMBER_FORMAT = NumberFormat.getInstance(Locale.of("ro"));

    // Q1776764 county of Romania.
    private static final String COUNTIES_SPARQL = """
            SELECT DISTINCT ?item ?roArticleName WHERE {
              ?item wdt:P31 wd:Q1776764 .
              ?roArticle schema:about ?item ;
                         schema:isPartOf <https://ro.wikipedia.org/> ;
                         schema:name ?roArticleName .
            }
            ORDER BY ?roArticleName
            """;

    private enum StatType {
        ETHNIC("etnică", "etnii", "etn", "2.2.2"),
        RELIGIOUS("confesională", "religii", "rel", "2.4.2");

        final String adjective;
        final String plural;
        final String refKey;
        final String table;

        StatType(String adjective, String plural, String refKey, String table) {
            this.adjective = adjective;
            this.plural = plural;
            this.refKey = refKey;
            this.table = table;
        }
    }

    /** Display label, Wikidata item (may be null when no suitable item is known) and chart color of a group. */
    private record Group(String label, String qid, String color) {
    }

    private static final Map<String, Group> ETHNIC_GROUPS = new LinkedHashMap<>();
    private static final Map<String, Group> RELIGIOUS_GROUPS = new LinkedHashMap<>();

    static {
        // Keys are the lower-cased, diacritics-free start of the column header in the xlsx files.
        ethnic("romani", "români", "Q485150", "#8080ff");
        ethnic("maghiari", "maghiari", "Q518365", "#80ff80");
        ethnic("romi", "romi", "Q923163", "#80ffff");
        ethnic("ucraineni", "ucraineni", "Q369739", "#ffff80");
        ethnic("germani", "germani", "Q700469", "#ff80ff");
        ethnic("turci", "turci", "Q2509562", "#ff8080");
        ethnic("rusi-lipoveni", "ruși lipoveni", "Q748548", "#ffafaf");
        ethnic("tatari", "tătari", "Q4170775", "#9f3f3f");
        ethnic("sarbi", "sârbi", "Q1064264", "#c03f3f");
        ethnic("slovaci", "slovaci", "Q2989388", "#3f3fc0");
        ethnic("bulgari", "bulgari", "Q3656818", "#3fc03f");
        ethnic("croati", "croați", "Q5187245", "#c0c03f");
        ethnic("greci", "greci", "Q3315585", "#c03fc0");
        ethnic("italieni", "italieni", "Q1990195", "#666666");
        ethnic("evrei", "evrei", "Q696662", "#666666");
        ethnic("cehi", "cehi", "Q853303", "#ff5555");
        ethnic("polonezi", "polonezi", "Q3395424", "#5555ff");
        ethnic("ruteni", "ruteni", "Q28194448", "#eeee70");
        ethnic("armeni", "armeni", "Q4793348", "#666666");
        ethnic("albanezi", "albanezi", "Q719847", "#666666");
        ethnic("macedoneni", "macedoneni", "Q3409357", "#666666");
        ethnic("alta etnie", OTHERS, null, "#555555");
        ethnic("informatie nedisponibila", UNKNOWN, null, "#9f9f9f");

        religious("ortodoxa sarba", "ortodocși sârbi", "Q188814", "#c03f3f");
        religious("ortodoxa", "ortodocși", "Q181901", "#8080ff");
        religious("romano-catolica", "romano-catolici", "Q144306", "#ffff80");
        religious("reformata", "reformați", "Q252250", "#80ff80");
        religious("penticostala", "penticostali", "Q3291465", "#80ffff");
        religious("greco-catolica", "greco-catolici", "Q856349", "#ff80ff");
        religious("baptista", "baptiști", "Q93191", "#3fc03f");
        religious("adventista", "adventiști", "Q215168", "#ffafaf");
        religious("musulmana", "musulmani", "Q1414351", "#ff8080");
        religious("unitariana", "unitarieni", "Q106687", "#3fc0c0");
        religious("martorii lui iehova", "martori ai lui Iehova", "Q35269", "#3f3fc0");
        religious("crestina dupa evanghelie", "creștini după evanghelie", "Q137900642", "#ffff55");
        religious("crestina de rit vechi", "creștini de rit vechi", "Q1623306", "#c0c03f");
        religious("evanghelica lutherana", "evanghelici luterani", "Q490692", "#ffa600");
        religious("evanghelica de confesiune augustana", "evanghelici augustani", "Q329344", "#ff5555");
        religious("evanghelica", "evanghelici", "Q5415664", "#d5fff3");
        religious("mozaica", "mozaici", null, "#666666");
        religious("armeana", "armeni apostolici", null, "#666666");
        religious("alta religie", OTHERS, null, "#555555");
        religious("fara religie", "fără religie", "Q58721", "#adbac0");
        religious("ateu", "atei", "Q7066", "#c0c0c0");
        religious("agnostic", "agnostici", "Q288928", "#c0adad");
        religious("informatie nedisponibila", UNKNOWN, null, "#9f9f9f");
    }

    private static void ethnic(String key, String label, String qid, String color) {
        ETHNIC_GROUPS.put(key, new Group(label, qid, color));
    }

    private static void religious(String key, String label, String qid, String color) {
        RELIGIOUS_GROUPS.put(key, new Group(label, qid, color));
    }

    private static final String OLD_RITE_ROMANIAN_QID = "Q1997209";

    /** Population by group label, in the order of the columns of the source table. */
    private static class CountyStats {
        long total;
        final Map<Group, Long> values = new LinkedHashMap<>();
    }

    private final File ethnicityFile = new File(System.getProperty("ethnicityXlsx",
            "/work/var/wki/db/ro/2021/Tabel-2.02.1-si-Tabel-2.02.2.xlsx"));
    private final File religionFile = new File(System.getProperty("religionXlsx",
            "/work/var/wki/db/ro/2021/Tabel-2.04.1-si-Tabel-2.04.2.xlsx"));

    private WikidataEntitiesCache wdcache;
    private int changedCount = 0;
    private int skippedCount = 0;

    public static void main(String[] args) {
        new RomaniaCountyDemographyUpdater().doExecution();
    }

    // Only reads from Wikidata, of data that rarely changes, so replication lag is of no concern.
    @Override
    protected int getMaxLag() {
        return 20;
    }

    @Override
    protected void beforeQuery() {
        wdcache = new WikidataEntitiesCache(dwiki);
    }

    @Override
    protected String getQuery() {
        return COUNTIES_SPARQL;
    }

    @Override
    protected void processResult(Map<String, Object> row) throws IOException, WikibaseException {
        if (!(row.get("item") instanceof Item)) {
            return;
        }
        String title = (String) row.get("roArticleName");
        if (title == null || !title.startsWith(COUNTY_PREFIX) || title.contains("(")) {
            log.warn("Skipping unexpected county article name: " + title);
            skippedCount++;
            return;
        }
        String countyName = title.substring(COUNTY_PREFIX.length());

        CountyStats ethnic = readStats(ethnicityFile, ETHNICITY_SHEET, countyName, ETHNIC_GROUPS);
        CountyStats religious = readStats(religionFile, RELIGION_SHEET, countyName, RELIGIOUS_GROUPS);
        if (ethnic == null || religious == null) {
            log.warn("No census data found for " + title);
            skippedCount++;
            return;
        }

        String articleText = wiki.getPageText(List.of(title)).stream().findFirst().orElse("");
        String section = START_MARKER + "<div style=\"float:left\">" + buildPieChart(countyName, ethnic, StatType.ETHNIC, null)
                + buildPieChart(countyName, religious, StatType.RELIGIOUS, "clear: none;") + "\n</div>\n"
                + buildText(countyName, ethnic, religious) + END_MARKER;

        String newText;
        Matcher marked = MARKED_PATTERN.matcher(articleText);
        Matcher heading = HEADING_PATTERN.matcher(articleText);
        if (marked.find()) {
            newText = marked.replaceFirst(Matcher.quoteReplacement(section));
        } else if (heading.find()) {
            newText = articleText.substring(0, heading.end()) + section + "\n" + articleText.substring(heading.end());
        } else {
            log.warn(title + ": no demography section or markers found");
            skippedCount++;
            return;
        }
        if (savePage(title, articleText, newText, EDIT_SUMMARY) || !newText.equals(articleText)) {
            changedCount++;
        }
    }

    @Override
    protected void afterProcessing(List<Map<String, Object>> results) {
        log.info("Counties: " + results.size() + ", changed: " + changedCount + ", skipped: " + skippedCount);
    }

    // Locates the county's row, i.e. the one starting with (at least) two spaces followed by the upper-case name.
    private static CountyStats readStats(File file, String sheetName, String countyName, Map<String, Group> groups)
            throws IOException {
        String wanted = stripDiacritics(countyName).toUpperCase(Locale.ROOT);
        try (Workbook workbook = WorkbookFactory.create(file, null, true)) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new IOException("Sheet " + sheetName + " not found in " + file);
            }
            DataFormatter fmt = new DataFormatter();
            Row header = findHeaderRow(sheet, fmt);
            for (Row row : sheet) {
                String label = fmt.formatCellValue(row.getCell(0));
                if (label.startsWith("  ") && label.trim().equals(wanted)) {
                    return toStats(row, header, groups, fmt);
                }
            }
        }
        return null;
    }

    // The header with the group names is the first row whose second cell is empty and third one is not numeric.
    private static Row findHeaderRow(Sheet sheet, DataFormatter fmt) throws IOException {
        for (Row row : sheet) {
            Cell first = row.getCell(0);
            Cell second = row.getCell(1);
            Cell third = row.getCell(2);
            if ((first == null || fmt.formatCellValue(first).isBlank())
                    && (second == null || fmt.formatCellValue(second).isBlank())
                    && third != null && !fmt.formatCellValue(third).isBlank()) {
                return row;
            }
        }
        throw new IOException("Header row not found in sheet " + sheet.getSheetName());
    }

    private static CountyStats toStats(Row row, Row header, Map<String, Group> groups, DataFormatter fmt) {
        CountyStats stats = new CountyStats();
        stats.total = parseCount(fmt.formatCellValue(row.getCell(1)));
        for (int col = 2; col < header.getLastCellNum(); col++) {
            String headerText = fmt.formatCellValue(header.getCell(col));
            if (headerText.isBlank()) {
                continue;
            }
            Group group = findGroup(headerText, groups);
            if (group == null) {
                log.warn("Unknown column: " + headerText);
                continue;
            }
            stats.values.merge(group, parseCount(fmt.formatCellValue(row.getCell(col))), Long::sum);
        }
        return stats;
    }

    // Longest key first, so that "Ortodoxa Sarba" is not taken for "Ortodoxa".
    private static Group findGroup(String headerText, Map<String, Group> groups) {
        String normalized = stripDiacritics(headerText).toLowerCase(Locale.ROOT).trim();
        return groups.entrySet().stream().filter(e -> normalized.startsWith(e.getKey()))
                .max((a, b) -> Integer.compare(a.getKey().length(), b.getKey().length()))
                .map(Map.Entry::getValue).orElse(null);
    }

    // Cells holding "*" or "-" (suppressed or none) count as zero.
    private static long parseCount(String text) {
        String digits = text.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? 0L : Long.parseLong(digits);
    }

    private static String stripDiacritics(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    private String buildText(String countyName, CountyStats ethnic, CountyStats religious) {
        String intro = String.format(
                "Conform [[Recensământul Populației și Locuințelor 2021 (România)|recensământului efectuat în 2021]], populația județului %s se ridică la %s.",
                countyName, substPluralRo(ethnic.total));
        boolean lipovenii = ethnic.values.entrySet().stream()
                .anyMatch(e -> "ruși lipoveni".equals(e.getKey().label()) && e.getValue() > 0);
        String ethnicPhrase = StringUtils.capitalize(buildPhrase(ethnic, StatType.ETHNIC, true));
        String religiousPhrase = "Din punct de vedere confesional, " + buildPhrase(religious, StatType.RELIGIOUS, !lipovenii);
        return String.join(" ", intro, ethnicPhrase, religiousPhrase);
    }

    private static String substPluralRo(long number) {
        return "{{plural|" + number + "|locuitor|locuitori|de locuitori}}";
    }

    // Groups ordered by decreasing population; "altele" and "necunoscută" always come last.
    private static List<Map.Entry<Group, Long>> sorted(CountyStats stats) {
        List<Map.Entry<Group, Long>> list = new ArrayList<>(stats.values.entrySet());
        list.sort((a, b) -> {
            boolean aLast = OTHERS.equals(a.getKey().label()) || UNKNOWN.equals(a.getKey().label());
            boolean bLast = OTHERS.equals(b.getKey().label()) || UNKNOWN.equals(b.getKey().label());
            if (aLast != bLast) {
                return aLast ? 1 : -1;
            }
            if (aLast) {
                return OTHERS.equals(a.getKey().label()) ? -1 : 1;
            }
            return Long.compare(b.getValue(), a.getValue());
        });
        return list;
    }

    private String buildPhrase(CountyStats stats, StatType type, boolean oldRiteRomanian) {
        double total = stats.total;
        List<Map.Entry<Group, Long>> named = sorted(stats).stream()
                .filter(e -> !OTHERS.equals(e.getKey().label()) && !UNKNOWN.equals(e.getKey().label())).toList();
        if (named.isEmpty() || stats.total == 0) {
            return "nu există date despre " + type.plural + ".";
        }
        StringBuilder sb = new StringBuilder();
        Map.Entry<Group, Long> top = named.get(0);
        sb.append(top.getValue() > stats.total / 2 ? "majoritatea locuitorilor" : "cei mai mulți locuitori").append(" sunt ")
                .append(link(top.getKey(), oldRiteRomanian)).append(" (").append(percent(top.getValue(), total)).append("%)");

        List<String> minorities = named.stream().skip(1).filter(e -> e.getValue() * 100 > stats.total)
                .map(e -> String.format("%s (%s%%)", link(e.getKey(), oldRiteRomanian), percent(e.getValue(), total))).toList();
        if (minorities.size() == 1) {
            sb.append(", cu o minoritate de ").append(minorities.get(0));
        } else if (minorities.size() > 1) {
            sb.append(", cu minorități de ")
                    .append(minorities.stream().limit(minorities.size() - 1).collect(Collectors.joining(", ")))
                    .append(" și ").append(minorities.get(minorities.size() - 1));
        }
        stats.values.entrySet().stream().filter(e -> UNKNOWN.equals(e.getKey().label()) && e.getValue() > 0).findFirst()
                .ifPresent(e -> sb.append(", iar pentru ").append(percent(e.getValue(), total))
                        .append("% nu se cunoaște apartenența ").append(type.adjective));
        sb.append(".<ref name=\"insse_2021_").append(type.refKey).append("\">{{Citat recensământ România 2021|tabel=")
                .append(type.table).append("}}</ref>");
        return sb.toString();
    }

    private static String percent(long value, double total) {
        return RO_NUMBER_FORMAT.format(value / total * 100.);
    }

    private String link(Group group, boolean oldRiteRomanian) {
        String qid = group.qid();
        if (qid != null && "creștini de rit vechi".equals(group.label()) && oldRiteRomanian) {
            qid = OLD_RITE_ROMANIAN_QID;
        }
        if (qid == null) {
            return group.label();
        }
        try {
            Entity ent = wdcache.get(qid);
            boolean hasRoArticle = Optional.ofNullable(ent).map(Entity::getSitelinks).map(m -> m.containsKey("rowiki"))
                    .orElse(false);
            if (hasRoArticle) {
                return new WikiLink(ent.getSitelinks().get("rowiki").getPageName(), group.label()).toString();
            }
            return new WikiTemplate().setTemplateTitle("Ill-wd").setParam("1", qid).setParam("3", group.label()).toString();
        } catch (IOException | WikibaseException e) {
            log.error("Could not load entity " + qid + " (" + group.label() + ")", e);
            return group.label();
        }
    }

    private String buildPieChart(String countyName, CountyStats stats, StatType type, String style) {
        NumberFormat nf = NumberFormat.getInstance(Locale.ENGLISH);
        nf.setMaximumFractionDigits(2);
        StringBuilder sb = new StringBuilder("{{Pie chart\n|thumb=left\n|caption=Componența ").append(type.adjective)
                .append(" a județului ").append(countyName);
        if (StringUtils.isNotBlank(style)) {
            sb.append("\n|style=").append(style);
        }

        // Groups under 1% are folded into "altele", except for the unknowns, which are always shown.
        Map<Group, Double> printable = new LinkedHashMap<>();
        double printablePercentage = 0.;
        for (Map.Entry<Group, Long> e : sorted(stats)) {
            double pct = 100. * e.getValue() / stats.total;
            boolean unknown = UNKNOWN.equals(e.getKey().label());
            if (unknown && e.getValue() > 0 || pct >= 1.0 && !OTHERS.equals(e.getKey().label()) && !unknown) {
                printable.put(e.getKey(), pct);
                printablePercentage += pct;
            }
        }
        Group others = stats.values.keySet().stream().filter(g -> OTHERS.equals(g.label())).findFirst().orElse(null);
        if (others != null) {
            printable.put(others, 100. - printablePercentage);
        }

        int idx = 1;
        for (Map.Entry<Group, Double> e : printable.entrySet()) {
            String label = OTHERS.equals(e.getKey().label()) ? "Alte " + type.plural : StringUtils.capitalize(e.getKey().label());
            sb.append("\n|label").append(idx).append('=').append(label).append("|value").append(idx).append('=')
                    .append(nf.format(e.getValue())).append("|color").append(idx).append('=').append(e.getKey().color());
            idx++;
        }
        return sb.append("}}").toString();
    }
}
