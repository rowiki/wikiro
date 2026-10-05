package org.wikipedia.ro.wikidata;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import javax.security.auth.login.LoginException;

import org.wikibase.WikibaseException;
import org.wikipedia.ro.utility.AbstractExecutable;

import lombok.extern.slf4j.Slf4j;

/**
 * Generic engine: runs a SPARQL query against Wikidata, then hands each result row to
 * {@link #processResult(Map)}. Subclasses supply the query and the per-row action.
 *
 * <p>Subclasses must save pages through {@link #savePage(String, String, String, String)}. In
 * dry-run mode (the default) that only logs the change to a file instead of editing on-wiki;
 * pass {@code -DdryRun=false} or call {@link #setDryRun(boolean)} to perform real edits.
 */
@Slf4j
public abstract class WikidataQueryProcessor extends AbstractExecutable {

    private boolean dryRun = Boolean.parseBoolean(System.getProperty("dryRun", "true"));
    private BufferedWriter dryRunLog;

    /**
     * Tells whether page saves are only logged rather than performed on-wiki.
     *
     * @return {@code true} in dry-run mode, {@code false} if edits are really performed
     */
    public boolean isDryRun() {
        return dryRun;
    }

    /**
     * Switches between dry-run and real execution. Must be called before {@link #doExecution()}.
     *
     * @param dryRun {@code true} to only log changes to {@link #getDryRunLogFile()},
     *               {@code false} to edit pages on-wiki
     * @return this processor, for chaining
     */
    public WikidataQueryProcessor setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
        return this;
    }

    /**
     * Supplies the file that receives the logged changes in dry-run mode. Overridable.
     *
     * @return the path of the dry-run log; by default {@code dry-run-<ClassName>.txt} in the working directory
     */
    protected Path getDryRunLogFile() {
        return Path.of("dry-run-" + getClass().getSimpleName() + ".txt");
    }

    /**
     * Saves a page, or in dry-run mode only records the change in {@link #getDryRunLogFile()}.
     * The logged change lists the lines removed ({@code -}) and added ({@code +}) between the
     * two texts.
     *
     * @param title   the title of the page on ro.wikipedia.org
     * @param oldText the current page text; only used to describe the change in dry-run mode
     * @param newText the full page text to save
     * @param summary the edit summary
     * @return {@code true} if the page was really edited, {@code false} if it was only logged
     *         (dry-run) or if the text is unchanged
     * @throws IOException if the dry-run log cannot be written, or if the edit fails after retries
     */
    protected boolean savePage(String title, String oldText, String newText, String summary) throws IOException {
        if (oldText.equals(newText)) {
            return false;
        }
        if (dryRun) {
            dryRunLog.write("== " + title + " ==\nSummary: " + summary + "\n" + describeChange(oldText, newText) + "\n");
            dryRunLog.flush();
            return false;
        }
        try {
            wikiEditWithRetry(title, newText, summary);
            return true;
        } catch (TimeoutException e) {
            throw new IOException("Edit of " + title + " failed: " + e.getMessage(), e);
        }
    }

    // Strips the common leading/trailing lines, so only the changed region is listed.
    private static String describeChange(String oldText, String newText) {
        String[] oldLines = oldText.split("\n", -1);
        String[] newLines = newText.split("\n", -1);
        int prefix = 0;
        while (prefix < oldLines.length && prefix < newLines.length && oldLines[prefix].equals(newLines[prefix])) {
            prefix++;
        }
        int suffix = 0;
        while (suffix < oldLines.length - prefix && suffix < newLines.length - prefix
                && oldLines[oldLines.length - 1 - suffix].equals(newLines[newLines.length - 1 - suffix])) {
            suffix++;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = prefix; i < oldLines.length - suffix; i++) {
            sb.append("- ").append(oldLines[i]).append('\n');
        }
        for (int i = prefix; i < newLines.length - suffix; i++) {
            sb.append("+ ").append(newLines[i]).append('\n');
        }
        return sb.toString();
    }

    /**
     * Supplies the SPARQL query to run against the Wikidata Query Service. Called once per
     * execution, after {@link #beforeQuery()}.
     *
     * @return the complete SPARQL query text; never {@code null}
     */
    protected abstract String getQuery();

    /**
     * Performs the action for a single result row of the query. Called once for each row,
     * in the order returned by the query.
     *
     * @param row one query result, mapping each selected variable name (without the leading
     *            {@code ?}) to its value. Wikidata entity URIs are returned as
     *            {@link org.wikibase.data.Item}, other bindings as {@link String}; variables
     *            left unbound by an {@code OPTIONAL} clause are absent or {@code null}
     * @throws IOException       if a network or I/O error occurs while processing the row
     * @throws WikibaseException if a Wikibase/Wikidata API call fails while processing the row
     */
    protected abstract void processResult(Map<String, Object> row) throws IOException, WikibaseException;

    /**
     * Hook called once before the query is run, e.g. to load resources. The default
     * implementation does nothing.
     *
     * @throws IOException       if a network or I/O error occurs during setup
     * @throws WikibaseException if a Wikibase/Wikidata API call fails during setup
     */
    protected void beforeQuery() throws IOException, WikibaseException {
    }

    /**
     * Hook called once after all rows have been processed, e.g. for aggregate passes over
     * state collected by {@link #processResult(Map)}. The default implementation does nothing.
     *
     * @param results the complete, unmodified list of rows returned by the query, in the
     *                same form as passed to {@link #processResult(Map)}
     * @throws IOException       if a network or I/O error occurs during post-processing
     * @throws WikibaseException if a Wikibase/Wikidata API call fails during post-processing
     */
    protected void afterProcessing(List<Map<String, Object>> results) throws IOException, WikibaseException {
    }

    /**
     * Runs the engine: {@link #beforeQuery()}, then the query from {@link #getQuery()}, then
     * {@link #processResult(Map)} for every row, then {@link #afterProcessing(List)}. Final,
     * so subclasses customize behavior only through the abstract methods and hooks.
     * Invoked by {@link #doExecution()} after login.
     *
     * @throws IOException       if a network or I/O error occurs
     * @throws WikibaseException if a Wikibase/Wikidata API call fails
     * @throws LoginException    if a (re)login required during execution fails
     */
    @Override
    protected final void execute() throws IOException, WikibaseException, LoginException {
        if (dryRun) {
            log.info("Dry run: no on-wiki edits, changes are logged to " + getDryRunLogFile().toAbsolutePath());
            dryRunLog = Files.newBufferedWriter(getDryRunLogFile(), StandardCharsets.UTF_8);
        }
        try {
            beforeQuery();

            log.debug("Querying Wikidata...");
            List<Map<String, Object>> results = dwiki.query(getQuery());
            log.debug("Found " + results.size() + " results.");
            log.debug("");

            for (Map<String, Object> row : results) {
                processResult(row);
            }

            afterProcessing(results);
        } finally {
            if (dryRunLog != null) {
                dryRunLog.close();
                dryRunLog = null;
            }
        }
    }
}
