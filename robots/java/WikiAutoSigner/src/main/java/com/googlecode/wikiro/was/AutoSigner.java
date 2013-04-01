package com.googlecode.wikiro.was;

import static org.wikipedia.Wiki.CATEGORY_TALK_NAMESPACE;
import static org.wikipedia.Wiki.HIDE_BOT;
import static org.wikipedia.Wiki.HIDE_SELF;
import static org.wikipedia.Wiki.PROJECT_NAMESPACE;
import static org.wikipedia.Wiki.PROJECT_TALK_NAMESPACE;
import static org.wikipedia.Wiki.TALK_NAMESPACE;
import static org.wikipedia.Wiki.USER_TALK_NAMESPACE;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.Wiki;
import org.wikipedia.Wiki.Revision;

/**
 * In order to build and compile this robot, you need to download the Wiki API project from
 * http://wiki-java.googlecode.com/svn This project depends on that one. All other dependencies are managed by Maven.
 * 
 * It also requires a file named credentials.properties located in the classpath that contains two variables: Username and
 * Password in order to authenticate to the wiki.
 * 
 * @author andrei.stroe
 */
public class AutoSigner {

    private static final String defaultSummary = "Robot: semnãturã automatã";

    private static Wiki wiki;
    private static Map<String, Long> lastReviewedVersion = new HashMap<String, Long>();

    private static Set<String> projectPages = new HashSet<String>() {
        private static final long serialVersionUID = -2245300967753508562L;

        {
            add("Wikipedia:Cafenea");
            add("Wikipedia:Reclama\u021Bii");
            add("Wikipedia:Pagini de \u2018ters");
        }
    };

    /**
     * @param args
     */
    public static void main(final String[] args) {

        wiki = new Wiki("ro.wikipedia.org");
        final Properties credentials = new Properties();

        try {
            credentials.load(AutoSigner.class.getClassLoader().getResourceAsStream("credentials.properties"));

            final String username = credentials.getProperty("Username");
            final String password = credentials.getProperty("Password");
            wiki.login(username, password.toCharArray());
            wiki.setMarkBot(true);
            while (true) {
                final Revision[] revisions = wiki.recentChanges(20, HIDE_BOT | HIDE_SELF, new int[] { TALK_NAMESPACE,
                    USER_TALK_NAMESPACE, PROJECT_TALK_NAMESPACE, PROJECT_NAMESPACE, CATEGORY_TALK_NAMESPACE });
                revisions: for (final Revision rev : revisions) {
                    System.out.println(rev.getPage() + "\t" + rev.getRevid());
                    if (rev.getRevid() == 0) {
                        continue revisions;
                    }
                    if (rev.getPage().startsWith("Wikipedia:") && !projectPages.contains(rev.getPage())) {
                        continue revisions;
                    }
                    final Long lastRevVer = lastReviewedVersion.get(rev.getPage());
                    if (null != lastRevVer && lastRevVer < rev.getRevid()) {
                        continue revisions;
                    } else {
                        lastReviewedVersion.put(rev.getPage(), rev.getRevid());
                    }
                    final String crtAuthor = rev.getUser();

                    // get a chunk of history large enough to compare
                    final Calendar now = Calendar.getInstance();
                    final Calendar yesterday = Calendar.getInstance();
                    yesterday.add(Calendar.DATE, -1);
                    Revision[] pageHistory = wiki.getPageHistory(rev.getPage(), now, yesterday);
                    while (pageHistory.length < 2 && countEditors(pageHistory) < 2
                        && !isNew(pageHistory[pageHistory.length - 1])) {
                        yesterday.add(Calendar.MONTH, -1);
                        pageHistory = wiki.getPageHistory(rev.getPage(), now, yesterday);
                    }

                    if (isNew(pageHistory[0])) {
                        analyzeText(pageHistory[0]);
                        continue revisions;
                    }
                    // locate the last message entered by another user
                    int i;
                    Revision lastMessageByOther = pageHistory[0];
                    for (i = 1; i < pageHistory.length; i++) {
                        if (isNew(pageHistory[i])) {
                            analyzeText(pageHistory[0]);
                            continue revisions;
                        }
                        // for edits spaced larger than 1 hour, we consider them
                        // different messages
                        final long timediff = pageHistory[0].getTimestamp().getTimeInMillis()
                            - pageHistory[i].getTimestamp().getTimeInMillis();
                        if (!pageHistory[i].getUser().equals(crtAuthor) || timediff > 1000 * 60 * 60) {
                            lastMessageByOther = pageHistory[i];
                            break;
                        }
                    }
                    if (lastMessageByOther.getRevid() == rev.getRevid()) {
                        analyzeText(rev);
                        continue revisions;
                    }
                    analyzeDiff(rev, lastMessageByOther);
                }
                Thread.sleep(60000);
            }
        } catch (final FailedLoginException e) {
            e.printStackTrace();
        } catch (final IOException e) {
            e.printStackTrace();
        } catch (final LoginException e) {
            e.printStackTrace();
        } catch (final InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (null != wiki) {
                wiki.logout();
            }
        }
    }

    private static void analyzeDiff(final Revision crtRev, final Revision prevRev) throws IOException {
        // TODO Auto-generated method stub
        final DiffParser dp = new DiffParser(prevRev, crtRev);
        dp.analyze(crtRev.getUser());

        if (!dp.isSigned()) {
            final StringBuilder signature = composeAutoSignature(crtRev);
            System.out.println("I should add: " + signature);
            System.out.println("    ... at line " + dp.getLine());

            final List<String> crtContents = Arrays.asList(crtRev.getText().split("\\r?\\n"));
            final String modifiedLine = crtContents.get(dp.getLine() - 1) + composeAutoSignature(crtRev);
            System.out.println("Line should become: " + modifiedLine);
            crtContents.set(dp.getLine() - 1, modifiedLine);
            final String newContents = StringUtils.join(crtContents, "\r\n");
            try {
                //if (crtRev.getPage().contains("Andrei Stroe")) {
                wiki.edit(crtRev.getPage(), newContents, "Robot:semnãturã automatã pentru mesajul lui " + crtRev.getUser(), crtRev.getTimestamp());
                //}
            } catch (final LoginException e) {
                e.printStackTrace();
            }
        }
    }

    private static void analyzeText(final Revision revision) throws IOException, LoginException {
        final String text = revision.getText();
        if (isTemplatesOnly(text)) {
            System.out.println("Page made up only of templates");
            return;
        }
        // System.out.println(text);
        final Pattern userPageDetector = Pattern.compile("\\[\\[\\:?((Utilizator\\:)|(User\\:))" + revision.getUser());
        final Pattern userTalkPageDetector = Pattern.compile("\\[\\[\\:?((Discu\u021Bie Utilizator\\:)|(User talk\\:))"
            + revision.getUser());
        Matcher matcher = userPageDetector.matcher(text);
        if (matcher.find()) {
            System.out.println("----Mesaj de " + revision.getUser() + " semnat cu link spre pagina de utilizator.");
            return;
        }
        matcher = userTalkPageDetector.matcher(text);
        if (matcher.find()) {
            System.out
            .println("Mesaj de " + revision.getUser() + " semnat cu link spre pagina de discu\u021Bii utilizator.");
            return;
        }

        final StringBuilder textToAdd = composeAutoSignature(revision);

        //if (revision.getPage().contains("Andrei Stroe")) {
        wiki.edit(revision.getPage(), revision.getText() + textToAdd, defaultSummary, revision.getTimestamp());
        //}
    }

    protected static StringBuilder composeAutoSignature(final Revision revision) {
        final StringBuilder textToAdd = new StringBuilder("{{subst:nesemnat|");
        textToAdd.append(revision.getUser());
        textToAdd.append("|data=");
        final Calendar timestamp = revision.getTimestamp();
        final TimeZone zone = TimeZone.getTimeZone("EET");
        final SimpleDateFormat dateFormatter = new SimpleDateFormat("d MMMM yyyy HH:mm", new Locale("ro"));
        textToAdd.append(dateFormatter.format(timestamp.getTime()));
        textToAdd.append(" (");
        textToAdd.append(zone.getDisplayName(true, TimeZone.SHORT));
        textToAdd.append(")}}");
        return textToAdd;
    }

    private static boolean isTemplatesOnly(final String text) {
        final Pattern templatesDetectorPattern = Pattern.compile("\\s*(\\{\\{.*\\}\\}\\s*)+");
        final Matcher matcher = templatesDetectorPattern.matcher(text);
        return matcher.matches();
    }

    private static int countEditors(final Revision[] pageHistory) {
        final Set<String> editors = new HashSet<String>();
        for (final Revision rev : pageHistory) {
            editors.add(rev.getUser());
        }
        return editors.size();
    }

    private static boolean isNew(final Revision rev) throws IOException {
        if (rev.isNew()) {
            return true;
        }

        final String page = rev.getPage();
        final Revision firstPageRev = wiki.getFirstRevision(page);
        return firstPageRev.getRevid() == rev.getRevid();
    }
}
