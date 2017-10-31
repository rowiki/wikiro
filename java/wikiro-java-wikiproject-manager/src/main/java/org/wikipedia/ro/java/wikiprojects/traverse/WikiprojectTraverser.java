package org.wikipedia.ro.java.wikiprojects.traverse;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.StringUtils.prependIfMissing;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.apache.commons.lang3.StringUtils.trim;

import java.io.Console;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.login.LoginException;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.wikipedia.Wiki;
import org.wikipedia.ro.java.wikiprojects.utils.ArticleClass;
import org.wikipedia.ro.java.wikiprojects.utils.Credentials;
import org.wikipedia.ro.java.wikiprojects.utils.WikiprojectsUtils;

public class WikiprojectTraverser {
    private String wikiprojectName;

    private String wikiAddress;

    private MultiKeyMap<ArticleClass, Integer> articles = new MultiKeyMap<ArticleClass, Integer>();

    private static final Pattern classPattern =
        Pattern.compile("\\|\\s*clasament\\s*=\\s*([^\\|\\}\\s]+)", Pattern.MULTILINE);
    private static final Pattern importancePattern =
        Pattern.compile("\\|\\s*importanță\\s*=\\s*([^\\|\\}\\s]+)", Pattern.MULTILINE);
    private static final Pattern numberedProjectPattern =
        Pattern.compile("\\|\\s*proiect(\\d+)\\s*=\\s*([^\\|\\}]+)", Pattern.MULTILINE);

    public WikiprojectTraverser(String wikiprojectName, String wikiAddress) {
        this.wikiprojectName = wikiprojectName;
        this.wikiAddress = wikiAddress;
    }

    public void traverse() throws IOException, LoginException {

        Wiki wiki = new Wiki(wikiAddress);

        Credentials credentials = WikiprojectsUtils.identifyCredentials();

        try {
            wiki.login(credentials.username, credentials.password);

            wiki.setMarkBot(true);

            String pagesBigcat = "Articole din domeniul proiectului " + wikiprojectName;

            String[] pages = wiki.getCategoryMembers(pagesBigcat);

            for (String eachPage : pages) {
                int namespace = wiki.namespace(eachPage);
                if (Wiki.CATEGORY_NAMESPACE == namespace) {
                    continue;
                }
                int classificationNamespace = namespace;
                String classificationPage =
                    prependIfMissing(removeStart(eachPage, wiki.namespaceIdentifier(namespace) + ":"),
                        wiki.namespaceIdentifier(classificationNamespace) + ":");

                String classificationText = wiki.getPageText(classificationPage);

                ArticleClass qualClass = null;
                ArticleClass impClass = null;
                Matcher classMatcher = classPattern.matcher(classificationText);
                Matcher importanceMatcher = importancePattern.matcher(classificationText);
                if (classMatcher.find()) {
                    qualClass = ArticleClass.fromString(classMatcher.group(1));
                }
                if (importanceMatcher.find()) {
                    impClass = ArticleClass.fromString(importanceMatcher.group(1));
                } else {
                    Matcher numberedProjectMatcher = numberedProjectPattern.matcher(classificationText);
                    String projNumber = null;
                    while (numberedProjectMatcher.find()) {
                        if (wikiprojectName.equals(trim(numberedProjectMatcher.group(2)))) {
                            projNumber = numberedProjectMatcher.group(1);
                        }
                    }
                    if (null != projNumber) {
                        Pattern numberedImportancePattern = Pattern
                            .compile("\\|\\s*importanță" + projNumber + "\\s*=\\s*([^\\|\\}\\s]+)", Pattern.MULTILINE);
                        Matcher numberedImportanceMatcher = numberedImportancePattern.matcher(classificationText);
                        if (numberedImportanceMatcher.find()) {
                            impClass = ArticleClass.fromString(numberedImportanceMatcher.group(1));
                        }
                    }
                }

                addArticleToClass(defaultIfNull(qualClass, ArticleClass.UNKNOWN_QUALITY),
                    defaultIfNull(impClass, ArticleClass.UNKNOWN_IMPORTANCE));
            }

            String projectBriefingPage = "Proiect:" + wikiprojectName + "/rezumat";

            wiki.edit(projectBriefingPage, createBriefingPage(),
                "Actualizat statut articole pentru proiectul " + wikiprojectName);

        } finally {
            wiki.logout();
        }

    }

    private void addArticleToClass(ArticleClass qualClass, ArticleClass impClass) {

        Integer prevValue = defaultIfNull(articles.get(qualClass, impClass), Integer.valueOf(0));
        articles.put(qualClass, impClass, Integer.valueOf(1 + prevValue));

        prevValue = defaultIfNull(articles.get(qualClass, ArticleClass.ALL_IMPORTANCE), Integer.valueOf(0));
        articles.put(qualClass, ArticleClass.ALL_IMPORTANCE, Integer.valueOf(1 + prevValue));

        prevValue = defaultIfNull(articles.get(ArticleClass.ALL_QUALITY, impClass), Integer.valueOf(0));
        articles.put(ArticleClass.ALL_QUALITY, impClass, Integer.valueOf(1 + prevValue));

        prevValue = defaultIfNull(articles.get(ArticleClass.ALL_QUALITY, ArticleClass.ALL_IMPORTANCE), Integer.valueOf(0));
        articles.put(ArticleClass.ALL_QUALITY, ArticleClass.ALL_IMPORTANCE, Integer.valueOf(1 + prevValue));

    }

    private String createBriefingPage() {
        StringBuilder sbuilder = new StringBuilder("{| class=\"wikitable\" style=\"text-align: center;\"");
        sbuilder.append("\n|-")
            .append("\n! colspan=\"2\" rowspan=\"2\" | Clasamentul<br/>articolelor !! colspan=\"7\" | Importanță")
            .append("\n|-").append("\n!{{clasament-top|categorie=Categorie:Cele mai importante articole ale proiectului ")
            .append(wikiprojectName)
            .append("|Top}} !! {{clasament-mare|categorie=Categorie:Articole de importanță mare pentru proiectul ")
            .append(wikiprojectName)
            .append("|Mare}} !! {{clasament-mediu|categorie=Categorie:Articole de importanță medie pentru proiectul ")
            .append(wikiprojectName)
            .append("|Medie}} !! {{clasament-mic|categorie=Categorie:Articole de importanță mică pentru proiectul ")
            .append(wikiprojectName).append("|Mică}} !! [[:Categorie:Articole neclasificate ale proiectului ")
            .append(wikiprojectName).append("(importanță)|Neclasificate]] || Total ").append("\n|-")
            .append("\n! rowspan=\"14\" | Calitate");

        printTableLine(sbuilder, ArticleClass.FA, "{{clasament-AC|categorie=Categorie:Articole de calitate ale proiectului ",
            "|AC}}");
        printTableLine(sbuilder, ArticleClass.A, "{{clasament-A|categorie=Categorie:Articole de clasa „A” ale proiectului ",
            "|A}}");
        printTableLine(sbuilder, ArticleClass.GA, "{{clasament-AB|categorie=Categorie:Articole bune ale proiectului ",
            "|AB}}");
        printTableLine(sbuilder, ArticleClass.B, "{{clasament-B|categorie=Categorie:Articole de clasa „B” ale proiectului ",
            "|B}}");
        printTableLine(sbuilder, ArticleClass.C, "{{clasament-C|categorie=Categorie:Articole de clasa „C” ale proiectului ",
            "|C}}");
        printTableLine(sbuilder, ArticleClass.START,
            "{{clasament-început|categorie=Categorie:Articole de clasa „început” ale proiectului ", "|început}}");
        printTableLine(sbuilder, ArticleClass.STUB,
            "{{clasament-ciot|categorie=Categorie:Articole de clasa „ciot” ale proiectului ", "|ciot}}");
        printTableLine(sbuilder, ArticleClass.LIST,
            "{{clasament-listă|categorie=Categorie:Articole de clasa „listă” ale proiectului ", "|listă}}");
        printTableLine(sbuilder, ArticleClass.FL, "{{clasament-LC|categorie=Categorie:Liste de calitate ale proiectului ",
            "|listă}}");
        printTableLine(sbuilder, ArticleClass.PORTAL,
            "{{clasament-portal|categorie=Categorie:Articole de clasa „portal” ale proiectului ", "|portal}}");
        printTableLine(sbuilder, ArticleClass.TEMPLATE,
            "{{clasament-format|categorie=Categorie:Articole de clasa „format” ale proiectului ", "|format}}");
        printTableLine(sbuilder, ArticleClass.UNKNOWN_QUALITY, "[[:Categorie:Articole neclasificate ale proiectului " + wikiprojectName + " (calitate)|Neclasificate]]", null);
        printTableLine(sbuilder, ArticleClass.ALL_QUALITY, "Total", null);

        sbuilder.append("\n|}");
        return sbuilder.toString();
    }

    private void printTableLine(StringBuilder sb, ArticleClass aclass, String catPrefix, String catSuffix) {
        sb.append("\n|-").append("\n! ").append(catPrefix);
        if (null != catSuffix) {
            sb.append(wikiprojectName).append(catSuffix);
        }
        sb.append("\n| ").append(stringifyValue(articles.get(aclass, ArticleClass.TOP))).append(" || ")
            .append(stringifyValue(articles.get(aclass, ArticleClass.HIGH))).append(" || ")
            .append(stringifyValue(articles.get(aclass, ArticleClass.MEDIUM))).append(" || ")
            .append(stringifyValue(articles.get(aclass, ArticleClass.SMALL))).append(" || ")
            .append(stringifyValue(articles.get(aclass, ArticleClass.UNKNOWN_IMPORTANCE))).append(" || ")
            .append(stringifyValue(articles.get(aclass, ArticleClass.ALL_IMPORTANCE)));
    }

    private String stringifyValue(Integer value) {
        if (null == value || 0 == value.intValue()) {
            return "";
        }
        return "{{formatnum|" + String.valueOf(value) + "}}";
    }
}
