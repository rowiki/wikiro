package org.wikipedia.ro.romania;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.wikibase.data.Item;
import org.wikipedia.ro.wikidata.WikidataQueryProcessor;

import lombok.extern.slf4j.Slf4j;

/**
 * Lists every municipality of Romania (municipiu, oraș, comună; no villages) with its name,
 * county, SIRUTA code and area as stored on Wikidata, and writes them as a wikitable to
 * {@link #OUTPUT_FILE}.
 */
@Slf4j
public class RomaniaMunicipalitiesLister extends WikidataQueryProcessor {

    private static final Path OUTPUT_FILE = Path.of("romania-municipalities.txt");

    // Fill in with the actual path of the data.gov.ro .dbf file.
    private static final Path DBF_FILE = Path.of("/work/var/wki/Unitate_administrativa_UAT.dbf");
    private static final String DBF_CODE_COLUMN = "natCode";
    private static final String DBF_NAME_COLUMN = "name";
    private static final String DBF_AREA_COLUMN = "Shape_Area";
    private static final BigDecimal SQUARE_METERS_PER_SQUARE_KM = BigDecimal.valueOf(1_000_000);

    // Q640364 municipality of Romania, Q16858213 town of Romania, Q659103 commune of Romania;
    // Q1776764 county of Romania; P843 SIRUTA code; P5061 unit symbol.
    // Area (P2046) is read through psv: to keep the stored unit.
    private static final String ROMANIA_MUNICIPALITIES_SPARQL = """
            SELECT ?item ?roLabel ?enLabel ?countyRoLabel ?siruta ?area ?unitLabel (SAMPLE(STR(?unitSymbolRaw)) AS ?unitSymbol) WHERE {
              VALUES ?type { wd:Q640364 wd:Q16858213 wd:Q659103 }
              ?item wdt:P31 ?type .
              OPTIONAL { ?item rdfs:label ?roLabel . FILTER(LANG(?roLabel) = "ro") }
              OPTIONAL { ?item rdfs:label ?enLabel . FILTER(LANG(?enLabel) = "en") }
              OPTIONAL { ?item wdt:P843 ?siruta }
              OPTIONAL {
                ?item wdt:P131 ?county .
                ?county wdt:P31 wd:Q1776764 .
                OPTIONAL { ?county rdfs:label ?countyRoLabel . FILTER(LANG(?countyRoLabel) = "ro") }
              }
              OPTIONAL {
                ?item p:P2046/psv:P2046 ?areaNode .
                ?areaNode wikibase:quantityAmount ?area ;
                          wikibase:quantityUnit ?unit .
                OPTIONAL { ?unit wdt:P5061 ?unitSymbolRaw . FILTER(LANG(?unitSymbolRaw) IN ("mul", "en", "ro")) }
                OPTIONAL { ?unit rdfs:label ?unitLabel . FILTER(LANG(?unitLabel) = "en") }
              }
            }
            GROUP BY ?item ?roLabel ?enLabel ?countyRoLabel ?siruta ?area ?unitLabel
            ORDER BY ?countyRoLabel ?roLabel
            """;

    // Wikidata returns one row per (SIRUTA, area) combination, so rows are merged per item.
    private static class Municipality {
        String name;
        String county;
        final Set<String> siruta = new LinkedHashSet<>();
        final Set<String> areas = new LinkedHashSet<>();
    }

    private final Map<String, Municipality> municipalities = new LinkedHashMap<>();
    private final Map<String, Map<String, String>> dbfByCode = new HashMap<>();

    public static void main(String[] args) {
        new RomaniaMunicipalitiesLister().doExecution();
    }

    @Override
    protected void beforeQuery() throws IOException {
        for (Map<String, String> dbfRow : DbfReader.read(DBF_FILE, StandardCharsets.UTF_8)) {
            dbfByCode.putIfAbsent(normalizeCode(dbfRow.get(DBF_CODE_COLUMN)), dbfRow);
        }
        log.info("Loaded " + dbfByCode.size() + " records from " + DBF_FILE);
    }

    @Override
    protected String getQuery() {
        return ROMANIA_MUNICIPALITIES_SPARQL;
    }

    @Override
    protected void processResult(Map<String, Object> row) {
        if (!(row.get("item") instanceof Item item)) {
            return;
        }
        Municipality m = municipalities.computeIfAbsent(item.getEnt().getId(), id -> new Municipality());
        m.name = row.get("roLabel") != null ? (String) row.get("roLabel") : (String) row.get("enLabel");
        m.county = (String) row.get("countyRoLabel");
        if (row.get("siruta") != null) {
            m.siruta.add((String) row.get("siruta"));
        }
        if (row.get("area") != null) {
            String unit = row.get("unitSymbol") != null ? (String) row.get("unitSymbol") : (String) row.get("unitLabel");
            m.areas.add(unit == null ? (String) row.get("area") : row.get("area") + " " + unit);
        }
    }

    @Override
    protected void afterProcessing(List<Map<String, Object>> results) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{| class=\"wikitable sortable\"\n");
        sb.append("! Nume !! Județ !! Cod SIRUTA !! Suprafață !! Suprafață data.gov !! Nume data.gov\n");
        for (Municipality m : municipalities.values()) {
            Map<String, String> dbfRecord = m.siruta.stream()
                    .map(code -> dbfByCode.get(normalizeCode(code)))
                    .filter(r -> r != null)
                    .findFirst()
                    .orElse(null);
            sb.append("|-\n");
            sb.append("| ").append(nullToEmpty(m.name))
                    .append(" || ").append(nullToEmpty(m.county))
                    .append(" || ").append(String.join(", ", m.siruta))
                    .append(" || ").append(String.join(", ", m.areas))
                    .append(" || ").append(dbfRecord == null ? "" : toSquareKm(dbfRecord.get(DBF_AREA_COLUMN)))
                    .append(" || ").append(dbfRecord == null ? "" : nullToEmpty(dbfRecord.get(DBF_NAME_COLUMN)))
                    .append('\n');
        }
        sb.append("|}\n");
        Files.writeString(OUTPUT_FILE, sb.toString(), StandardCharsets.UTF_8);
        log.info("Wrote " + municipalities.size() + " municipalities to " + OUTPUT_FILE.toAbsolutePath());
    }

    // Leading zeros are ignored so that padded and unpadded codes match.
    private static String normalizeCode(String code) {
        return code == null ? "" : code.trim().replaceFirst("^0+(?=.)", "");
    }

    private static String toSquareKm(String squareMeters) {
        if (squareMeters == null || squareMeters.isEmpty()) {
            return "";
        }
        return new BigDecimal(squareMeters).divide(SQUARE_METERS_PER_SQUARE_KM).setScale(2, RoundingMode.DOWN).toPlainString();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
