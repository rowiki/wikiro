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
    Pattern translPagePattern = Pattern.compile("\\{\\{\\s*[Pp]agină tradusă");

    @Override
    protected void execute() throws IOException, WikibaseException, LoginException
    {
        RequestHelper helper = wiki.new RequestHelper();
        helper.inNamespaces(Wiki.MAIN_NAMESPACE);
        helper.taggedWith("contenttranslation");

        LocalDate now = LocalDate.now();
        LocalDate threeDaysAgo = now.minusDays(3);
        helper.withinDateRange(threeDaysAgo.atStartOfDay().atOffset(ZoneId.of("Europe/Bucharest").getRules().getOffset(LocalDateTime.now())), OffsetDateTime.now());

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
                String newPage = Optional.ofNullable(wiki.resolveRedirect(rev.getTitle())).orElse(rev.getTitle());
                String talkPage = wiki.getTalkPage(newPage);
                if (wiki.exists(new String[] { talkPage })[0])
                {
                    talkText = wiki.getPageText(talkPage);
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
                try
                {
                    ReplaceCrossLinkWithIll rcl = new ReplaceCrossLinkWithIll(wiki, Wiki.newSession(lang + ".wikipedia.org"), dwiki, newPage);
                    rcl.execute();
                }
                catch (Throwable e)
                {
                    System.err.printf("Failed to replace cross-links and red links with Ill-wd in page %s%n", newPage);
                    e.printStackTrace();
                }
            }

        }

        helper = wiki.new RequestHelper();
        helper.inNamespaces(Wiki.MAIN_NAMESPACE);
        helper.withinDateRange(threeDaysAgo.atStartOfDay().atOffset(ZoneId.of("Europe/Bucharest").getRules().getOffset(LocalDateTime.now())), OffsetDateTime.now());

        List<Revision> recentNewPages = wiki.newPages(helper);
        for (Revision eachNewPage : recentNewPages)
        {
            String eachNewPageTitle = Optional.ofNullable(wiki.resolveRedirect(eachNewPage.getTitle())).orElse(eachNewPage.getTitle());
            String[] newPageLinks = wiki.whatLinksHere(eachNewPageTitle, Wiki.MAIN_NAMESPACE);
            for (String eachNewPageLink : newPageLinks)
            {
                try
                {
                    CleanupIll illCleanup = new CleanupIll(wiki, wiki, dwiki, eachNewPageLink);
                    illCleanup.execute();
                }
                catch (Throwable e)
                {
                    System.err.printf("Failed to cleanup redundant Ill templates in page %s that links to %s%n", eachNewPageLink, eachNewPageTitle);
                    e.printStackTrace();
                }
            }
        }

    }
}
