package org.wikipedia.ro.java;

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
import org.wikipedia.ro.model.WikiTemplate;
import org.wikipedia.ro.utility.AbstractExecutable;

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
        List<Revision> recentNewPages = wiki.newPages(helper);
        for (Revision eachNewPage : recentNewPages)
        {
            LOG.info("Page created: {}", eachNewPage.getTitle());
            String eachNewPageTitle = wiki.getRevision(eachNewPage.getID()).getTitle();
            String[] newPageLinks = wiki.whatLinksHere(eachNewPageTitle, Wiki.MAIN_NAMESPACE);
            for (String eachNewPageLink : newPageLinks)
            {
                if (null == eachNewPageLink)
                {
                    continue;
                }
                try
                {
                    String notReplacedText = wiki.getPageText(List.of(eachNewPageLink)).stream().findFirst().orElse("");
                    CleanupIll illCleanup = new CleanupIll(wiki, wiki, dwiki, eachNewPageLink);
                    String replacedText = illCleanup.execute();
                    if (!notReplacedText.equals(replacedText)) {
                        wiki.edit(eachNewPageLink, replacedText, "Robot: înlocuit formate Ill redundante");
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
                msg.append("* [[").append(processingFailure.getKey()).append(" – ").append(processingFailure.getValue()).append("]]\n");
            }
        }
        msg.append("\n").append(editedTranslationsStatus.getSuccessfulProcessings().size()).append(" pagini modificate prin traducere procesate fără probleme.\n");
        if (!newTranslationsStatus.getProcessingFailures().isEmpty())
        {
            msg.append("\nNu am reușit să procesez următoarele pagini traduse noi:\n");
            for (Entry<String, String> processingFailure : newTranslationsStatus.getProcessingFailures().entrySet())
            {
                msg.append("* [[").append(processingFailure.getKey()).append("]] – ").append(processingFailure.getValue()).append("\n");
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
                msg.append("* [[").append(processingFailure.getKey()).append("]] – ").append(processingFailure.getValue()).append("\n");
            }
        }
        else
        {
            msg.append(".\n");
        }
        wiki.edit("Utilizator:Andrebot/Statut procesare pagini traduse", msg.toString(), "Robot: raport statut procesare pagini traduse");
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
                    String replacedText = rcl.execute();
                    if (!replacedText.equals(notReplacedText))
                    {
                        opsDone.add("înlocuit legături roșii sau spre alte wikiuri cu Ill");
                    }
                    
                    ReindexFootnotes rfn = new ReindexFootnotes(wiki, Wiki.newSession(lang + ".wikipedia.org"), dwiki, newPage);
                    String reindexedFnText = rfn.processText(replacedText);
                    if (!reindexedFnText.equals(replacedText))
                    {
                        opsDone.add("reindexat note de subsol");
                    }
                    
                    if (!opsDone.isEmpty())
                    {
                        wiki.edit(newPage, reindexedFnText, "Robot: " + opsDone.stream().collect(Collectors.joining("; ")));
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
