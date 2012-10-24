package com.googlecode.wikiro.indevelopment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.Wiki;
import org.wikipedia.Wiki.Revision;

import com.googlecode.wikiro.was.AutoSigner;

/**
 * Removes the {{development}} tag placed in pages and left there for more than 48 hours
 * 
 * @author andrei.stroe
 * 
 */
public class InDevelopmentRemover {

    private final static String defaultSummary = "Robot: elimin formatul {{Dezvoltare}} din pagini needitate în ultimele douã zile.";

    private static Wiki wiki;

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

            final String[] pagesIncluded = wiki.whatTranscludesHere("Format:Dezvoltare", Wiki.MAIN_NAMESPACE);
            final String[] pagesThatRedirect = wiki.whatLinksHere("Format:Dezvoltare", true, Wiki.TEMPLATE_NAMESPACE);
            final List<String> textToRemove = new ArrayList<String>(Arrays.asList("{{Dezvoltare}}", "{{dezvoltare}}",
                "{{Format:Dezvoltare}}"));
            for (final String redirect : pagesThatRedirect) {
                final String templateName = StringUtils.substringAfter(redirect, "Format:");
                textToRemove.add("{{" + templateName + "}}");
                textToRemove.add("{{" + redirect + "}}");
                textToRemove.add("{{" + StringUtils.lowerCase(templateName) + "}}");
            }
            System.out.println(pagesIncluded.length + " pages include {{Dezvoltare}}");
            final Calendar now = Calendar.getInstance();
            final Calendar yesterday = Calendar.getInstance();
            yesterday.add(Calendar.HOUR, -48);
            for (final String page : pagesIncluded) {
                final Revision[] history = wiki.getPageHistory(page, yesterday, now);
                boolean isEdited = false;
                revisions: for (final Revision rev : history) {
                    if (!rev.isBot()) {
                        isEdited = true;
                        break revisions;
                    }
                }

                if (!isEdited) {
                    String pageText = wiki.getPageText(page);
                    for (final String alias : textToRemove) {
                        pageText = StringUtils.replace(pageText, alias, "");
                    }
                    wiki.edit(page, pageText, defaultSummary, now);
                }
            }
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final FailedLoginException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final LoginException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if (null != wiki) {
                wiki.logout();
            }
        }
    }
}
