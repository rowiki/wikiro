package org.wikipedia.ro.populationdb.hu;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.Wiki;
import org.wikipedia.ro.populationdb.util.WikiTemplate;

public class BilingualIdentifier {
    private Wiki rowiki;

    public static void main(final String[] args) {
        final BilingualIdentifier bi = new BilingualIdentifier();

        try {
            bi.init();
            bi.identify();
        } catch (final FailedLoginException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final LoginException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if (null != bi.rowiki) {
                bi.rowiki.logout();
            }
        }

    }

    private void identify() throws IOException, LoginException {
        final String[] categoryMembers = rowiki.getCategoryMembers("Categorie:Orașe în Ungaria", false,
            Wiki.CATEGORY_NAMESPACE);
        final StringBuilder sbuild = new StringBuilder();
        final List<String> townCategories = new ArrayList<String>();
        townCategories.addAll(Arrays.asList(categoryMembers));
        townCategories.add("Categorie:Orașe în Ungaria");
        for (final String eachCat : categoryMembers) {
            if (!StringUtils.startsWith(eachCat, "Categorie:Orașe în județul")) {
                continue;
            }
            final String[] villages = rowiki.getCategoryMembers(eachCat, false, Wiki.MAIN_NAMESPACE);
            for (final String eachVillage : villages) {
                final String articleText = rowiki.getPageText(eachVillage);
                if (!StringUtils.contains(articleText, "<div style=\"float:left\">{{Pie chart")) {
                    continue;
                }
                final WikiTemplate pr = new WikiTemplate(StringUtils.substring(
                    articleText,
                    StringUtils.indexOf(articleText, "<div style=\"float:left\">{{Pie chart")
                        + StringUtils.length("<div style=\"float:left\">")));
                final Map<String, String> params = pr.getParams();
                int i = 1;
                boolean paramExists = true;
                boolean hasMinorities = false;
                do {
                    final String crtLabel = params.get("label" + i);
                    if (null == crtLabel) {
                        paramExists = false;
                    } else {
                        final double crtValue = Double.valueOf(params.get("value" + i));
                        if (crtValue > 15 && !Arrays.asList("Alții", "Necunoscut", "Maghiari", "Romi").contains(crtLabel)) {
                            hasMinorities = true;
                        }
                    }
                    i++;
                } while (paramExists);
                if (hasMinorities) {
                    sbuild.append("\n* [[" + eachVillage + "]]");
                }
            }
        }

        final String currentList = rowiki.getPageText("Utilizator:Andrebot/Comune Ungaria/Sate bilingve");
        rowiki.edit("Utilizator:Andrebot/Comune Ungaria/Sate bilingve", currentList + "\n==Orașe==\n" + sbuild.toString(),
            "Robot: generare listă orașe bilingve în Ungaria");
    }

    private void init() throws IOException, FailedLoginException {
        rowiki = new Wiki("ro.wikipedia.org");
        final Properties credentials = new Properties();
        credentials.load(BilingualIdentifier.class.getClassLoader().getResourceAsStream("credentials.properties"));
        final String user = credentials.getProperty("Username");
        final String pass = credentials.getProperty("Password");
        rowiki.login(user, pass.toCharArray());
        rowiki.setMarkBot(true);
    }
}
