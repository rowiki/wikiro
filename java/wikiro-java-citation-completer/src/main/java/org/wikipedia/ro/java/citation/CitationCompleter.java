package org.wikipedia.ro.java.citation;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.security.auth.login.LoginException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikibase.WikibaseException;
import org.wikipedia.Wiki;
import org.wikipedia.Wiki.RequestHelper;
import org.wikipedia.Wiki.Revision;
import org.wikipedia.ro.java.citation.handlers.Handler;
import org.wikipedia.ro.utility.AbstractExecutable;
import org.wikipedia.ro.utils.WikipediaPageCache;

public class CitationCompleter extends AbstractExecutable
{
    private static final Logger LOG = LoggerFactory.getLogger(CitationCompleter.class);

    public static final Pattern REF_URL_PATTERN = Pattern
        .compile("\\<ref\\s*(\\sname\\=(?:\\\"[^\\\"]+\\\"|\\w+))?\\>\\s*(((http|https)://)(www.)?[a-zA-Z0-9@:%._\\+~#?&//=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%._\\\\+~#?&//=]*))</ref>");

    @Override
    protected void execute() throws IOException, WikibaseException, LoginException
    {
        LocalDate lastVisit = findLastVisit();
        RequestHelper helper = wiki.new RequestHelper();
        helper.inNamespaces(Wiki.MAIN_NAMESPACE);

        LocalDate now = LocalDate.now();
        helper.withinDateRange(lastVisit.atStartOfDay().atOffset(ZoneId.of("Europe/Bucharest").getRules().getOffset(LocalDateTime.now())), OffsetDateTime.now());

        List<Revision> recentChanges = wiki.recentChanges(helper, "edit");
        long[] revIds = recentChanges.stream().mapToLong(Revision::getID).toArray();

        HandlerFactory handlerFactory = HandlerFactory.createHandlerFactory();

        final Set<String> pageTitles = new LinkedHashSet<>();

        wiki.getRevisions(revIds).stream().filter(Objects::nonNull).map(Revision::getTitle).forEach(pageTitles::add);
        ArrayList<String> pageTitlesList = new ArrayList<>(pageTitles);
        List<String> pageTexts = WikipediaPageCache.getInstance().getPageTexts(wiki, pageTitlesList.toArray(String[]::new));
        Map<String, String> pagesTitlesAndTexts = new LinkedHashMap<>();

        for (int i = 0; i < pageTexts.size(); i++)
        {
            if (pageTexts.get(i).startsWith("<revisions><rev"))
            {
                pageTexts.set(i, pageTexts.get(i).substring(1 + pageTexts.get(i).indexOf('>', "<revisions><rev".length())));
            }
            pagesTitlesAndTexts.put(pageTitlesList.get(i), pageTexts.get(i));
        }

        pagesTitlesAndTexts = pagesTitlesAndTexts.entrySet().stream().filter(e -> REF_URL_PATTERN.matcher(e.getValue()).find())
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

        LOG.info("{} pages found", pagesTitlesAndTexts.size());
        for (Entry<String, String> pgdata : pagesTitlesAndTexts.entrySet())
        {
            LOG.info("Page {}", pgdata.getKey());

            int citationsChanged = 0;
            StringBuilder sb = new StringBuilder();
            Matcher refUrlMatcher = REF_URL_PATTERN.matcher(pgdata.getValue());
            while (refUrlMatcher.find())
            {
                String refName = refUrlMatcher.group(1);
                String refNamePart = Optional.ofNullable(refName).map(s -> StringUtils.prependIfMissing(s, " ")).orElse("");
                String url = refUrlMatcher.group(2);
                LOG.info("Found bare URL ref: {}", url);

                List<Handler> handlersForUrl = handlerFactory.getHandlers(url);
                Optional<String> foundCitation = Optional.empty();
                for (Handler urlHandler : handlersForUrl)
                {
                    Optional<String> citationByHandler = urlHandler.processCitationParams(url);
                    LOG.info("{} found citation: {}", urlHandler.getClass().getName(), citationByHandler.isEmpty() ? "<empty>" : citationByHandler.get());
                    if (citationByHandler.isPresent())
                    {
                        foundCitation = citationByHandler;
                        break;
                    }
                }

                if (foundCitation.isPresent())
                {
                    refUrlMatcher.appendReplacement(sb, String.format("<ref%s>%s</ref>", Matcher.quoteReplacement(refNamePart), Matcher.quoteReplacement(foundCitation.get())));
                    citationsChanged++;
                }
            }
            refUrlMatcher.appendTail(sb);

            if (0 < citationsChanged)
            {
                WikipediaPageCache.getInstance().savePage(wiki, pgdata.getKey(), sb.toString(), "Robot: completat automat " + (1 == citationsChanged ? "o citare" : (citationsChanged + " citări")));
            }
        }
        wiki.edit("Utilizator:Andrebot/dată-vizitare-pagini-editate", now.toString(), "Robot: actualizare dată vizitare pagini editate");
    }

    private LocalDate findLastVisit() throws IOException
    {
        RequestHelper rh = wiki.new RequestHelper();
        rh.byUser("Andrebot");
        rh.limitedTo(1);
        List<Revision> revs = wiki.getPageHistory("Utilizator:Andrebot/dată-vizitare-pagini-editate", rh);

        rh.byUser("Andrei Stroe");
        revs.addAll(wiki.getPageHistory("Utilizator:Andrebot/dată-vizitare-pagini-editate", rh));

        revs.sort((r1, r2) -> r2.getTimestamp().compareTo(r1.getTimestamp()));

        Optional<Revision> latestRevOpt = revs.stream().findFirst();
        if (!latestRevOpt.isPresent())
        {
            return LocalDate.now().minusDays(1);
        }

        String strText = latestRevOpt.get().getText();
        return LocalDate.parse(strText);
    }
}
