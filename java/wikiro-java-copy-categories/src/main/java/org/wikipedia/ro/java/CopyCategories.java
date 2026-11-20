package org.wikipedia.ro.java;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.wikibase.Wikibase;
import org.wikibase.WikibaseException;
import org.wikibase.data.Entity;
import org.wikibase.data.Sitelink;
import org.wikipedia.Wiki;
import org.wikipedia.ro.cache.WikidataEntitiesCache;
import org.wikipedia.ro.utility.AbstractExecutable;
import org.wikipedia.ro.utils.WikidataCacheManager;
import org.wikipedia.ro.utils.WikipediaPageCache;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;

/**
 * Creates a category on the Romanian Wikipedia by mirroring the position of a category of another Wikipedia
 * (English by default, selectable with the wiki code).
 * <p>
 * The source category is identified through the sitelink to the source wiki (e.g. enwiki) of the given Wikidata item. Its parent categories
 * (hidden categories, which include tracking ones, are excluded) are walked breadth-first, level by level, looking on Wikidata for
 * Romanian counterparts. At the first level where at least one counterpart exists, the new Romanian category is
 * created inside all counterparts found at that level, and linked to the Wikidata item.
 * <p>
 * Edits are enabled; setting {@link #DRY_RUN} to true makes the class only log the intended edits, without logging in.
 */
public class CopyCategories extends AbstractExecutable {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(CopyCategories.class);

    /** Safety switch. When true, the edits to rowiki and Wikidata are logged but not performed (and no login is made). */
    static final boolean DRY_RUN = false;

    private static final String RO_WIKI_HOST = "ro.wikipedia.org";
    static final String DEFAULT_SOURCE_WIKI_CODE = "en";
    private static final String RO_SITE_ID = "rowiki";
    private static final String RO_CATEGORY_PREFIX = "Categorie:";
    private static final int MAX_DEPTH = 15;
    private static final Pattern QID_PATTERN = Pattern.compile("Q[1-9]\\d*");

    private static final Pattern WIKI_CODE_PATTERN = Pattern.compile("[a-z]{2,3}(-[a-z]+)*|simple");
    private static final String RO_LANGUAGE = "ro";
    private static final String SITELINK_SUMMARY = "Adăugat legătură către categoria din Wikipedia în română";

    private final String roCategoryArg;
    private final String wikidataIdArg;
    private final String sourceWikiCode;
    private final String srcSiteId;
    private final String createSummary;

    private Wiki srcWiki;

    /** Uses the English Wikipedia as source. */
    public CopyCategories(String roCategoryName, String wikidataItemId) {
        this(roCategoryName, wikidataItemId, DEFAULT_SOURCE_WIKI_CODE);
    }

    /**
     * @param sourceWikiCode language code of the Wikipedia whose category tree is explored, e.g. "en", "fr", "de";
     *            null or blank means "en"
     */
    public CopyCategories(String roCategoryName, String wikidataItemId, String sourceWikiCode) {
        this.roCategoryArg = roCategoryName;
        this.wikidataIdArg = wikidataItemId;
        this.sourceWikiCode = normalizeWikiCode(sourceWikiCode);
        this.srcSiteId = siteIdFor(this.sourceWikiCode);
        this.createSummary = "Robot: creat categorie pe baza categoriei echivalente din Wikipedia (" + this.sourceWikiCode + ")";
    }

    private static String normalizeWikiCode(String code) {
        String normalized = code == null || code.isBlank() ? DEFAULT_SOURCE_WIKI_CODE : code.strip().toLowerCase();
        if (!WIKI_CODE_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("'" + code + "' is not a valid Wikipedia language code (e.g. en, fr, de)");
        }
        if ("ro".equals(normalized)) {
            throw new IllegalArgumentException("The source wiki can't be the Romanian Wikipedia itself");
        }
        return normalized;
    }

    /** The Wikidata site ID of a Wikipedia: the code with hyphens turned into underscores, plus "wiki". */
    static String siteIdFor(String wikiCode) {
        if ("be-tarask".equals(wikiCode)) {
            return "be_x_oldwiki"; // historical exception
        }
        return wikiCode.replace('-', '_') + "wiki";
    }

    /**
     * Only creates the (anonymous) sessions. Unlike {@link AbstractExecutable#init()}, it does not identify credentials
     * and does not log in: that happens in {@link #loginForEditing()}, once we know that edits will actually be made.
     * All the reads done before that work fine without being logged in.
     */
    @Override
    protected void init() {
        initLogging();
        wiki = Wiki.newSession(RO_WIKI_HOST);
        dwiki = new Wikibase();
        srcWiki = Wiki.newSession(sourceWikiCode + ".wikipedia.org");
    }

    @Override
    protected void execute() throws IOException, WikibaseException, LoginException {
        if (DRY_RUN) {
            LOG.warn("DRY RUN: no login and no edits will be made, the edits will only be logged");
        }

        // 1. Validate the target category name (must not exist in rowiki)
        String roCategory = normalizeRoCategoryTitle(roCategoryArg);
        if (WikipediaPageCache.getInstance().pageExists(wiki, roCategory)) {
            throw fail("Category %s already exists on the Romanian Wikipedia", roCategory);
        }

        // 2. Load the Wikidata item and find the source category
        String qid = wikidataIdArg == null ? "" : wikidataIdArg.trim().toUpperCase();
        if (!QID_PATTERN.matcher(qid).matches()) {
            throw fail("'%s' is not a valid Wikidata item ID", wikidataIdArg);
        }
        WikidataEntitiesCache wdCache = WikidataCacheManager.getWikidataEntitiesCache(dwiki);
        Entity item = wdCache.get(qid);
        if (item == null) {
            throw fail("Wikidata item %s does not exist", qid);
        }
        Map<String, Sitelink> sitelinks = item.getSitelinks();
        Sitelink srcSitelink = sitelinks == null ? null : sitelinks.get(srcSiteId);
        if (srcSitelink == null) {
            throw fail("Wikidata item %s has no sitelink to the Wikipedia in \"%s\" (%s)", qid, sourceWikiCode, srcSiteId);
        }
        if (sitelinks.get(RO_SITE_ID) != null) {
            throw fail("Wikidata item %s already has a sitelink to the Romanian Wikipedia (%s)", qid,
                sitelinks.get(RO_SITE_ID).getPageName());
        }
        String srcCategory = srcSitelink.getPageName();
        if (srcWiki.namespace(srcCategory) != Wiki.CATEGORY_NAMESPACE) {
            throw fail("The %s sitelink of %s (%s) is not a category", srcSiteId, qid, srcCategory);
        }
        if (!WikipediaPageCache.getInstance().pageExists(srcWiki, srcCategory)) {
            throw fail("Category %s does not exist on the Wikipedia in \"%s\" (stale sitelink?)", srcCategory, sourceWikiCode);
        }
        String existingRoLabel = item.getLabels() == null ? null : item.getLabels().get(RO_LANGUAGE);
        boolean needsRoLabel = existingRoLabel == null || existingRoLabel.isBlank();
        LOG.info(needsRoLabel ? "Item {} has no Romanian label, it will be set" : "Item {} already has a Romanian label ({})",
            qid, existingRoLabel);
        LOG.info("Source: {}:{} ({}); new category: {}", sourceWikiCode, srcCategory, qid, roCategory);

        // 3. Breadth-first walk up the source wiki's category tree, looking for Romanian counterparts
        Set<String> roParents = findRomanianParents(srcCategory);
        if (roParents.isEmpty()) {
            throw fail("No Romanian counterpart found among the ancestors of %s within %d levels; nothing to do",
                srcCategory, MAX_DEPTH);
        }

        // 4. Create the category page and link it on Wikidata
        if (!DRY_RUN) {
            loginForEditing();
        }
        createRoCategory(roCategory, roParents);
        updateWikidataItem(qid, roCategory, needsRoLabel);
    }

    /**
     * Walks the parent categories of the given source category breadth-first (hidden categories, tracking ones
     * included, are filtered out in the API request). At each level, every category found is looked up for a Romanian counterpart. The walk stops at the first
     * level where at least one counterpart exists, and all counterparts found at that level are returned.
     */
    private Set<String> findRomanianParents(String srcStartCategory) throws IOException, WikibaseException {
        Set<String> visited = new HashSet<>();
        visited.add(srcStartCategory);
        List<String> currentLevel = List.of(srcStartCategory);

        for (int depth = 1; depth <= MAX_DEPTH && !currentLevel.isEmpty(); depth++) {
            // Not cacheable through the utility library (it only caches page text/existence), so this is a direct call.
            // filterBy(hidden=false) becomes clshow=!hidden: hidden categories (and thus tracking ones) are left out.
            Wiki.RequestHelper helper = srcWiki.new RequestHelper().filterBy(Map.of("hidden", Boolean.FALSE));
            List<List<String>> parentsPerCategory = srcWiki.getCategories(currentLevel, helper, false);

            Set<String> nextLevel = new LinkedHashSet<>();
            for (List<String> parents : parentsPerCategory) {
                if (parents == null) {
                    continue;
                }
                for (String parent : parents) {
                    if (visited.add(parent)) { // also protects against category cycles
                        nextLevel.add(parent);
                    }
                }
            }
            LOG.info("Level {}: {} categories: {}", depth, nextLevel.size(), nextLevel);

            Map<String, String> found = new LinkedHashMap<>();
            for (String srcParent : nextLevel) {
                Optional<String> roEquivalent = findRomanianEquivalent(srcParent);
                roEquivalent.ifPresent(ro -> found.put(srcParent, ro));
            }
            if (!found.isEmpty()) {
                final int level = depth;
                found.forEach((src, ro) -> LOG.info("Level {}: {} -> {}", level, src, ro));
                return new LinkedHashSet<>(found.values());
            }
            currentLevel = new ArrayList<>(nextLevel);
        }
        return Set.of();
    }

    /** Finds the existing (non-redirect) Romanian category that corresponds to a source wiki category, through Wikidata. */
    private Optional<String> findRomanianEquivalent(String srcCategory) throws IOException, WikibaseException {
        Entity entity = WikidataCacheManager.getWikidataEntitiesCache(dwiki).getByArticle(srcSiteId, srcCategory);
        if (entity == null || entity.getSitelinks() == null) {
            return Optional.empty();
        }
        Sitelink roSitelink = entity.getSitelinks().get(RO_SITE_ID);
        if (roSitelink == null) {
            return Optional.empty();
        }
        String roTitle = roSitelink.getPageName();
        WikipediaPageCache cache = WikipediaPageCache.getInstance();
        if (!cache.pageExists(wiki, roTitle)) {
            LOG.warn("Ignoring {}: the Wikidata sitelink of {} points to a missing page", roTitle, entity.getId());
            return Optional.empty();
        }
        if (cache.pageRedirects(wiki, roTitle)) {
            LOG.warn("Ignoring {}: it is a redirect", roTitle);
            return Optional.empty();
        }
        return Optional.of(roTitle);
    }

    /**
     * Identifies the credentials and logs in to both wikis, configuring them the way {@link AbstractExecutable#init()}
     * does (bot flag, relogin callback). The user assertion is
     * not set here but around each write, see {@link #withUserAssertion}. Called once, after all validations have passed and right
     * before the first edit; both logins are done up front so that a login problem can't leave us with the category
     * created but not linked on Wikidata.
     */
    private void loginForEditing() throws IOException, FailedLoginException {
        wikiCreds = identifyCredentials("rowiki");
        wiki.login(wikiCreds.username, wikiCreds.password);
        WikipediaPageCache.getInstance().registerReloginCallback(wiki, getReloginCallback());
        wiki.setMarkBot(true);

        dwikiCreds = identifyCredentials("dwiki");
        dwiki.loginWithCredentials(dwikiCreds.username, dwikiCreds.password);
        dwiki.setMarkBot(true);
        LOG.info("Logged in to rowiki as {} and to Wikidata as {}", wikiCreds.username, dwikiCreds.username);
    }

    private void createRoCategory(String roCategory, Set<String> roParents) throws IOException, WikibaseException {
        StringBuilder text = new StringBuilder();
        for (String parent : roParents) {
            text.append("[[").append(canonicalCategoryLink(parent)).append("]]\n");
        }
        String pageText = text.toString().stripTrailing();

        if (DRY_RUN) {
            LOG.info("DRY RUN: would create page [[{}]] with summary \"{}\" and text:\n{}", roCategory, createSummary,
                pageText);
            return;
        }
        withUserAssertion(wiki, () ->
            WikipediaPageCache.getInstance().savePage(wiki, roCategory, pageText, createSummary, getReloginCallback()));
    }

    /**
     * Adds the rowiki sitelink to the item and, if the item has no Romanian label, sets it to the title of the new
     * page (category items on Wikidata are labelled with the full title, namespace prefix included).
     */
    private void updateWikidataItem(String qid, String roCategory, boolean setRoLabel)
        throws IOException, WikibaseException {
        if (DRY_RUN) {
            LOG.info("DRY RUN: would add sitelink {}:{} to Wikidata item {} with summary \"{}\"", RO_SITE_ID, roCategory,
                qid, SITELINK_SUMMARY);
            if (setRoLabel) {
                LOG.info("DRY RUN: would set the Romanian label of Wikidata item {} to \"{}\"", qid, roCategory);
            }
            return;
        }
        withUserAssertion(dwiki, () -> dwiki.executeWithRelogin(() -> {
            dwiki.setSitelink(qid, RO_SITE_ID, roCategory);
            return null;
        }));
        LOG.info("Added sitelink {}:{} to {}", RO_SITE_ID, roCategory, qid);
        if (setRoLabel) {
            withUserAssertion(dwiki, () -> dwiki.executeWithRelogin(() -> {
                dwiki.setLabel(qid, RO_LANGUAGE, roCategory);
                return null;
            }));
            LOG.info("Set the Romanian label of {} to \"{}\"", qid, roCategory);
        }
        // the cached copy of the item is now stale
        WikidataCacheManager.getWikidataEntitiesCache(dwiki).invalidate(qid);
    }

    @FunctionalInterface
    private interface Write {
        void run() throws IOException, WikibaseException;
    }

    /**
     * The assertion mode ends up as a parameter of every API request, reads included. Reads don't need to be
     * authenticated, so the "assert=user" check is switched on only for the duration of a write, where it protects
     * against silently saving as an anonymous user if the session was lost.
     */
    private void withUserAssertion(Wiki target, Write write) throws IOException, WikibaseException {
        target.setAssertionMode(Wiki.ASSERT_USER);
        try {
            write.run();
        } finally {
            target.setAssertionMode(Wiki.ASSERT_NONE);
        }
    }

    /** Adds the "Categorie:" prefix if missing and trims/normalises the name. */
    private String normalizeRoCategoryTitle(String name) {
        if (name == null || name.isBlank()) {
            throw fail("The Romanian category name must not be empty");
        }
        String trimmed = name.strip().replace('_', ' ');
        if (trimmed.startsWith(":")) {
            trimmed = trimmed.substring(1);
        }
        if (wiki.namespace(trimmed) == Wiki.CATEGORY_NAMESPACE) {
            return trimmed;
        }
        return RO_CATEGORY_PREFIX + trimmed;
    }

    /** Category links in the page text always use the canonical Romanian prefix, whatever the sitelink spelling was. */
    private String canonicalCategoryLink(String roTitle) {
        int colon = roTitle.indexOf(':');
        return RO_CATEGORY_PREFIX + (colon >= 0 ? roTitle.substring(colon + 1) : roTitle);
    }

    private IllegalStateException fail(String format, Object... args) {
        String message = String.format(format, args);
        LOG.error(message);
        return new IllegalStateException(message);
    }

    private void initLogging() {
        LoggerContext logbackContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        PatternLayoutEncoder ple = new PatternLayoutEncoder();
        ple.setPattern("%date %level %logger{10} [%file:%line] %msg%n");
        ple.setContext(logbackContext);
        ple.start();

        ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
        appender.setContext(logbackContext);
        appender.setName("console");
        appender.setEncoder(ple);
        appender.start();

        Logger roWikiLog = logbackContext.getLogger("org.wikipedia.ro");
        roWikiLog.setAdditive(false);
        roWikiLog.setLevel(Level.INFO);
        roWikiLog.addAppender(appender);

        Logger wikiLog = logbackContext.getLogger("wiki");
        wikiLog.setAdditive(false);
        wikiLog.setLevel(Level.WARN);
        wikiLog.addAppender(appender);

        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }
}
