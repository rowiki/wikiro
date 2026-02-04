package org.wikipedia.ro.java;

import static org.apache.commons.lang3.Strings.CS;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.wikibase.WikibaseException;
import org.wikipedia.Wiki;
import org.wikipedia.Wiki.RequestHelper;
import org.wikipedia.Wiki.Revision;
import org.wikipedia.ro.legacyoperations.CleanupIll;
import org.wikipedia.ro.legacyoperations.ReindexFootnotes;
import org.wikipedia.ro.legacyoperations.ReplaceCrossLinkWithIll;
import org.wikipedia.ro.model.WikiLink;
import org.wikipedia.ro.model.WikiTemplate;
import org.wikipedia.ro.utility.AbstractExecutable;
import org.wikipedia.ro.utils.RetryHelper;
import org.wikipedia.ro.utils.WikipediaPageCache;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;

public class TranslationManager extends AbstractExecutable
{
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(TranslationManager.class);
    Pattern commentPattern = Pattern.compile("\\[\\[:(\\w+):Special:Redirect/revision/(\\d+)\\|([^\\]]+)\\]\\]");
    Pattern translPagePattern = Pattern.compile("\\{\\{\\s*[Pp]agină[_\\s]tradusă");
    
    @Override
    protected void execute() throws IOException, WikibaseException, LoginException
    {
        LocalDate lastVisit = findLastVisit();
        RequestHelper helper = wiki.new RequestHelper();
        helper.inNamespaces(Wiki.MAIN_NAMESPACE);
        helper.taggedWith("contenttranslation");

        LocalDate now = LocalDate.now();
        helper.withinDateRange(lastVisit.atStartOfDay().atOffset(ZoneId.of("Europe/Bucharest").getRules().getOffset(LocalDateTime.now())), OffsetDateTime.now());

        Map<String, Boolean> filter = new HashMap<>();
        filter.put("new", Boolean.TRUE);
        helper.filterBy(filter);

        List<Revision> recentTranslations = wiki.newPages(helper);
        LOG.info("Found {} new pages translated to process", recentTranslations.size());
        BatchTranslationProcessingStatus newTranslationsStatus = processTranslations(recentTranslations);
        
        helper = wiki.new RequestHelper();
        helper.inNamespaces(Wiki.MAIN_NAMESPACE);
        helper.taggedWith("contenttranslation");

        now = LocalDate.now();
        helper.withinDateRange(lastVisit.atStartOfDay().atOffset(ZoneId.of("Europe/Bucharest").getRules().getOffset(LocalDateTime.now())), OffsetDateTime.now());
        List<Revision> editedTranslations = wiki.recentChanges(helper, "edit");
        LOG.info("Found {} edited translations to process", editedTranslations.size());
        BatchTranslationProcessingStatus editedTranslationsStatus = processTranslations(editedTranslations);

        helper = wiki.new RequestHelper();
        helper.inNamespaces(Wiki.MAIN_NAMESPACE);
        helper.withinDateRange(lastVisit.atStartOfDay().atOffset(ZoneId.of("Europe/Bucharest").getRules().getOffset(LocalDateTime.now())), OffsetDateTime.now());

        BatchTranslationProcessingStatus linkbackStatus = new BatchTranslationProcessingStatus();

        List<Revision> recentNewPages;
        try {
            final RequestHelper fHelper = helper;
            recentNewPages = RetryHelper.retry(() -> {
                try {
                    return wiki.newPages(fHelper);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, 10);
        } catch (TimeoutException e) {
            LOG.error("Failed to fetch recent new pages", e);
            return;
        }
        for (Revision eachNewPage : recentNewPages)
        {
            LOG.info("Page created: {}", eachNewPage.getTitle());
            Revision revision;
            try {
                final Revision fEachNewPage = eachNewPage;
                revision = RetryHelper.retry(() -> {
                    try {
                        return wiki.getRevision(fEachNewPage.getID());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, 10);
            } catch (TimeoutException e) {
                linkbackStatus.addProcessingFailure(eachNewPage.getTitle(), "Failed to fetch revision for page " + eachNewPage.getTitle() + ": "+ e.getMessage());
                LOG.error("Failed to fetch revision for page {}", eachNewPage.getTitle(), e);
                continue;
            }
            String eachNewPageTitle = revision.getTitle();
            String[] newPageLinks;
            try {
                newPageLinks = RetryHelper.retry(() -> {
                    try {
                        return wiki.whatLinksHere(eachNewPageTitle, Wiki.MAIN_NAMESPACE);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, 10);
            } catch (TimeoutException e) {
                linkbackStatus.addProcessingFailure(eachNewPageTitle, "Failed to fetch what links to page " + eachNewPageTitle + ": " + e.getMessage());
                LOG.error("Failed to fetch what links here for page {}", eachNewPageTitle, e);
                continue;
            }
            for (String eachNewPageLink : newPageLinks)
            {
                if (null == eachNewPageLink)
                {
                    continue;
                }
                try
                {
                    String notReplacedText = WikipediaPageCache.getInstance().getPageText(wiki, eachNewPageLink);
                    CleanupIll illCleanup = new CleanupIll(wiki, wiki, dwiki, eachNewPageLink);
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    Future<String> future = executor.submit(() -> illCleanup.executeWithInitialText(notReplacedText));
                    String replacedText;
                    try {
                        replacedText = future.get(10, TimeUnit.MINUTES);
                    } catch (TimeoutException e) {
                        future.cancel(true); // Interrupt the task
                        throw e;
                    } finally {
                        executor.shutdownNow();
                    }
                    if (!notReplacedText.equals(replacedText)) {
                        WikipediaPageCache.getInstance().savePage(wiki, eachNewPageLink, replacedText, "Robot: înlocuit formate Ill redundante");
                        linkbackStatus.addSuccessfulProcessing(eachNewPageLink);
                    }
                }
                catch (Throwable e)
                {
                    LOG.error("Failed to cleanup redundant Ill templates in page {} that links to {}", eachNewPageLink, eachNewPageTitle, e);
                    linkbackStatus.addProcessingFailure(eachNewPageLink, getExceptionDescriptor(e));
                }
            }
        }
        
        LOG.info("Finished visiting articles. Setting new reference date.");
        wiki.edit("Utilizator:Andrebot/dată-vizitare-pagini-noi", now.toString(), "Robot: actualizare dată vizitare pagini noi");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy").withLocale(Locale.of("ro"));

        StringBuilder msg = new StringBuilder("\n== Starea paginilor traduse în ")
            .append(now.format(formatter)).append(" ==\n");
        if (!editedTranslationsStatus.getProcessingFailures().isEmpty())
        {
            msg.append("\nNu am putut procesa următoarele pagini modificate prin traducere:\n");
            for (Entry<String, String> processingFailure: editedTranslationsStatus.getProcessingFailures().entrySet())
            {
                msg.append("** ").append(new WikiLink(processingFailure.getKey())).append(" – ").append(processingFailure.getValue()).append("\n");
            }
        }
        msg.append("\n").append(editedTranslationsStatus.getSuccessfulProcessings().size()).append(" pagini modificate prin traducere procesate fără probleme.\n");
        if (!newTranslationsStatus.getProcessingFailures().isEmpty())
        {
            msg.append("\nNu am reușit să procesez următoarele pagini traduse noi:\n");
            for (Entry<String, String> processingFailure : newTranslationsStatus.getProcessingFailures().entrySet())
            {
                msg.append("** ").append(new WikiLink(processingFailure.getKey())).append(" – ").append(processingFailure.getValue()).append("\n");
            }
        }
        msg.append("\n").append(newTranslationsStatus.getSuccessfulProcessings().size()).append(" pagini traduse noi procesate fără probleme.\n");
        
        msg.append("\nÎnlocuirea formatelor de tip Ill redundante în paginile care trimit spre articole nou-create a decurs astfel:\n");
        msg.append("\n* ").append(linkbackStatus.getSuccessfulProcessings().size()).append(" pagini procesate fără probleme.\n");
        msg.append("\n* ").append(linkbackStatus.getProcessingFailures().size()).append(" pagini cu erori");
        if (!linkbackStatus.getProcessingFailures().isEmpty())
        {
            msg.append(":\n");
            for (Entry<String, String> processingFailure : linkbackStatus.getProcessingFailures().entrySet())
            {
                msg.append("** ").append(new WikiLink(processingFailure.getKey())).append(" – ").append(processingFailure.getValue()).append("\n");
            }
        }
        else
        {
            msg.append(".\n");
        }
        
        BatchTranslationProcessingStatus processManualCallsStatus = processManualCalls();
        if (processManualCallsStatus.getProcessingFailures().size() + processManualCallsStatus.getSuccessfulProcessings().size() > 0)
        {
            msg.append("\n== Starea procesării manuale a paginilor traduse ==\n");
            msg.append("\nAm procesat manual ").append(processManualCallsStatus.getSuccessfulProcessings().size()).append(" pagini fără probleme.\n");
            if (!processManualCallsStatus.getProcessingFailures().isEmpty())
            {
                msg.append("\nNu am putut procesa următoarele pagini solicitate manual:\n");
                for (Entry<String, String> processingFailure : processManualCallsStatus.getProcessingFailures().entrySet())
                {
                    msg.append("* ").append(new WikiLink(processingFailure.getKey())).append(" – ").append(processingFailure.getValue()).append("\n");
                }
            }
        }        
        wiki.edit("Utilizator:Andrebot/Statut procesare pagini traduse", msg.toString(), "Robot: raport statut procesare pagini traduse");
    }

    public BatchTranslationProcessingStatus processManualCalls() throws IOException  {
        BatchTranslationProcessingStatus status = new BatchTranslationProcessingStatus();
        String manuallyProcessedTranslationsPage = WikipediaPageCache.getInstance().getPageText(wiki, "Utilizator:Andrebot/traduceri-de-prelucrat");
        // Read the list of page titles
        List<String> pageTitles = manuallyProcessedTranslationsPage.lines()
            .map(String::trim)
            .filter(line -> !line.isEmpty())
            .filter(line -> !line.startsWith("{"))
            .filter(line -> !line.startsWith("<"))
            .collect(Collectors.toList());
        
        List<String> linesKept = new ArrayList<>();

        for (String pageTitleLine : pageTitles) {
            try {
                String[] parts = pageTitleLine.split("\\|", 2);
                String pageTitle = parts[0].trim();
                String srcLang = parts.length > 1 ? parts[1].trim() : "en";
                Wiki srcWiki = Wiki.newSession(srcLang + ".wikipedia.org");

                // Read the page text
                String originalText = WikipediaPageCache.getInstance().getPageText(wiki, pageTitle);

                // ReplaceCrossLinkWithIll
                ReplaceCrossLinkWithIll rcl = new ReplaceCrossLinkWithIll(wiki, srcWiki, dwiki, pageTitle);
                ExecutorService rclExecutor = Executors.newSingleThreadExecutor();
                Future<String> rclFuture = rclExecutor.submit(() -> rcl.executeWithInitialText(originalText));
                String rclResult;
                try {
                    rclResult = rclFuture.get(15, TimeUnit.MINUTES);
                } catch (TimeoutException e) {
                    rclFuture.cancel(true);
                    throw new RuntimeException("ReplaceCrossLinkWithIll timed out for " + pageTitle, e);
                } finally {
                    rclExecutor.shutdownNow();
                }

                // CleanupIll
                CleanupIll illCleanup = new CleanupIll(wiki, srcWiki, dwiki, pageTitle);
                ExecutorService illExecutor = Executors.newSingleThreadExecutor();
                Future<String> illFuture = illExecutor.submit(() -> illCleanup.executeWithInitialText(rclResult));
                String illResult;
                try {
                    illResult = illFuture.get(15, TimeUnit.MINUTES);
                } catch (TimeoutException e) {
                    illFuture.cancel(true);
                    throw new RuntimeException("CleanupIll timed out for " + pageTitle, e);
                } finally {
                    illExecutor.shutdownNow();
                }

                // If the text changed, update the page
                if (!originalText.equals(illResult)) {
                    WikipediaPageCache.getInstance().savePage(wiki, pageTitle, illResult, "Robot: executat la cerere managementul formatelor Ill");
                }
                status.addSuccessfulProcessing(pageTitle);
                
            } catch (Throwable e) {
                LOG.error("Failed to process page {}", pageTitleLine, e);
                status.addProcessingFailure(pageTitleLine, getExceptionDescriptor(e));
                linesKept.add(pageTitleLine); // Keep the line for retry
            }
        }
        
        String replacement = "<pre>\n" + String.join("\n", linesKept) + "\n</pre>";
        String updatedPage = manuallyProcessedTranslationsPage.replaceAll(
            "(?s)<pre>.*?</pre>", 
            Matcher.quoteReplacement(replacement)
        );
        
        try
        {
            wiki.edit("Utilizator:Andrebot/traduceri-de-prelucrat", updatedPage, "Robot: actualizat lista de traduceri de prelucrat manual");
        }
        catch (LoginException | IOException e)
        {
            LOG.error("Failed to update the manual processing list page", e);
        }
        
        return status;
    }
    
    private BatchTranslationProcessingStatus processTranslations(List<Revision> recentTranslations) throws IOException, LoginException
    {
        int idx = 0;
        BatchTranslationProcessingStatus status = new BatchTranslationProcessingStatus();
        for (Revision rev : recentTranslations)
        {
            Matcher commentMatcher = commentPattern.matcher(rev.getComment());
            if (commentMatcher.find())
            {
                String lang = commentMatcher.group(1);
                String langRevision = commentMatcher.group(2);
                String langTitle = commentMatcher.group(3);

                String talkText = null;
                String newPage = wiki.getRevision(rev.getID()).getTitle();
                LOG.info("Working on page: \"{}\", {} of {}", newPage, ++idx, recentTranslations.size());
                
                List<String> opsDone = new ArrayList<String>();
                try
                {
                    String notReplacedText = wiki.getPageText(List.of(newPage)).stream().findFirst().orElse("");
                    ReplaceCrossLinkWithIll rcl = new ReplaceCrossLinkWithIll(wiki, Wiki.newSession(lang + ".wikipedia.org"), dwiki, newPage);
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    Future<String> future = executor.submit(() -> rcl.execute());
                    String replacedText;
                    try {
                        replacedText = future.get(5, TimeUnit.MINUTES); // 10 seconds timeout
                    } catch (TimeoutException e) {
                        future.cancel(true); // Interrupt the task
                        throw e;
                    } finally {
                        executor.shutdownNow();
                    }
                    if (!replacedText.equals(notReplacedText))
                    {
                        opsDone.add("înlocuit legături roșii sau spre alte wikiuri cu Ill");
                    }
                    
                    ReindexFootnotes rfn = new ReindexFootnotes(wiki, Wiki.newSession(lang + ".wikipedia.org"), dwiki, newPage);
                    executor = Executors.newSingleThreadExecutor();
                    Future<String> rfnFuture = executor.submit(() -> rfn.processText(replacedText));
                    String reindexedFnText;
                    try {
                        reindexedFnText = rfnFuture.get(5, TimeUnit.MINUTES);
                    } catch (TimeoutException e) {
                        rfnFuture.cancel(true);
                        throw e;
                    } finally {
                        executor.shutdownNow();
                    }
                    
                    if (!CS.equals(reindexedFnText, replacedText))
                    {
                        opsDone.add("reindexat note de subsol");
                    }
                    
                    if (!opsDone.isEmpty())
                    {
                        WikipediaPageCache.getInstance().savePage(wiki, newPage, reindexedFnText, "Robot: " + opsDone.stream().collect(Collectors.joining("; ")));
                    }
                    status.addSuccessfulProcessing(newPage);
                }
                catch (Throwable e)
                {
                    LOG.error("Failed to replace cross-links and red links with Ill-wd in page {}", newPage, e);
                    status.addProcessingFailure(newPage, getExceptionDescriptor(e));
                }
                String talkPage = wiki.getTalkPage(newPage);
                if (wiki.exists(List.of(talkPage))[0])
                {
                    talkText = wiki.getPageText(List.of(talkPage)).stream().findFirst().orElse("");
                    Matcher translPageMatcher = translPagePattern.matcher(talkText);
                    if (translPageMatcher.find())
                    {
                        continue;
                    }
                }

                WikiTemplate translTextTemplate = new WikiTemplate();
                translTextTemplate.setTemplateTitle("Pagină tradusă");
                translTextTemplate.setParam("small", "no");
                translTextTemplate.setParam("1", lang);
                translTextTemplate.setParam("2", langTitle);
                translTextTemplate.setParam("version", langRevision);

                talkText = Stream.of(translTextTemplate.toString(), talkText).filter(Objects::nonNull).collect(Collectors.joining("\n"));
                wiki.edit(talkPage, talkText, "Robot: adăugat format {{Pagină tradusă}}");
            }

        }
        return status;
    }

    private String getExceptionDescriptor(Throwable e)
    {
        Optional<Throwable> cause = Optional.ofNullable(e.getCause());
        return String.format("%s: %s cauzat de %s: %s", e.getClass(), e.getMessage(), cause.map(Throwable::getClass).orElse(null), cause.map(Throwable::getClass).map(Objects::toString).orElse("necunoscut"), cause.map(Throwable::getMessage).orElse("necunoscut"));
    }
    
    @Override
    protected void init() throws FailedLoginException, IOException
    {
        initLogging();
        super.init();
    }

    private void initLogging()
    {
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
        
        Logger wikiLog =  logbackContext.getLogger("wiki");
        wikiLog.setAdditive(false);
        wikiLog.setLevel(Level.WARN);
        wikiLog.addAppender(appender);

        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();        
    }



    private LocalDate findLastVisit() throws IOException
    {
        RequestHelper rh = wiki.new RequestHelper();
        rh.byUser("Andrebot");
        rh.limitedTo(1);
        List<Revision> revs = wiki.getPageHistory("Utilizator:Andrebot/dată-vizitare-pagini-noi", rh);
        
        rh.byUser("Andrei Stroe");
        revs.addAll(wiki.getPageHistory("Utilizator:Andrebot/dată-vizitare-pagini-noi", rh));
        
        revs.sort((r1, r2) -> r2.getTimestamp().compareTo(r1.getTimestamp()));
        
        Optional<Revision> latestRevOpt = revs.stream().findFirst();
        if (!latestRevOpt.isPresent()) {
            return LocalDate.now().minusDays(3);
        }
        
        String strText = latestRevOpt.get().getText();
        return LocalDate.parse(strText);
    }
}
