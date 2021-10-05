package org.wikipedia.ro.java;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.security.auth.login.LoginException;

import org.wikibase.WikibaseException;
import org.wikipedia.Wiki;
import org.wikipedia.Wiki.RequestHelper;
import org.wikipedia.Wiki.Revision;
import org.wikipedia.ro.legacyoperations.CleanupIll;
import org.wikipedia.ro.legacyoperations.ReplaceCrossLinkWithIll;
import org.wikipedia.ro.model.WikiTemplate;
import org.wikipedia.ro.utility.AbstractExecutable;

public class TranslationManager extends AbstractExecutable
{
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
                try
                {
                    String notReplacedText = wiki.getPageText(List.of(newPage)).stream().findFirst().orElse("");
                    ReplaceCrossLinkWithIll rcl = new ReplaceCrossLinkWithIll(wiki, Wiki.newSession(lang + ".wikipedia.org"), dwiki, newPage);
                    String replacedText = rcl.execute();
                    if (!notReplacedText.equals(replacedText))
                    {
                        wiki.edit(newPage, replacedText, "Robot: înlocuit legături roșii sau spre alte wikiuri cu Ill");
                    }
                }
                catch (Throwable e)
                {
                    System.err.printf("Failed to replace cross-links and red links with Ill-wd in page %s%n", newPage);
                    e.printStackTrace();
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

        helper = wiki.new RequestHelper();
        helper.inNamespaces(Wiki.MAIN_NAMESPACE);
        helper.withinDateRange(lastVisit.atStartOfDay().atOffset(ZoneId.of("Europe/Bucharest").getRules().getOffset(LocalDateTime.now())), OffsetDateTime.now());

        List<Revision> recentNewPages = wiki.newPages(helper);
        for (Revision eachNewPage : recentNewPages)
        {
            String eachNewPageTitle = wiki.getRevision(eachNewPage.getID()).getTitle();
            String[] newPageLinks = wiki.whatLinksHere(eachNewPageTitle, Wiki.MAIN_NAMESPACE);
            for (String eachNewPageLink : newPageLinks)
            {
                try
                {
                    String notReplacedText = wiki.getPageText(List.of(eachNewPageLink)).stream().findFirst().orElse("");
                    CleanupIll illCleanup = new CleanupIll(wiki, wiki, dwiki, eachNewPageLink);
                    String replacedText = illCleanup.execute();
                    if (!notReplacedText.equals(replacedText)) {
                        wiki.edit(eachNewPageLink, replacedText, "Robot: înlocuit formate Ill redundante");
                    }
                }
                catch (Throwable e)
                {
                    System.err.printf("Failed to cleanup redundant Ill templates in page %s that links to %s%n", eachNewPageLink, eachNewPageTitle);
                    e.printStackTrace();
                }
            }
        }
        
        wiki.edit("Utilizator:Andrebot/dată-vizitare-pagini-noi", now.toString(), "Robot: actualizare dată vizitare pagini noi");

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
