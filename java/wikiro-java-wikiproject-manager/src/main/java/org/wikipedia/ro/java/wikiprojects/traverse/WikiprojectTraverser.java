package org.wikipedia.ro.java.wikiprojects.traverse;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.prependIfMissing;
import static org.apache.commons.lang3.StringUtils.removeStart;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.LoginException;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.lang3.StringUtils;
import org.wikipedia.Wiki;
import org.wikipedia.ro.java.wikiprojects.model.WikiprojectsModel;
import org.wikipedia.ro.java.wikiprojects.utils.ArticleClass;
import org.wikipedia.ro.java.wikiprojects.utils.Credentials;
import org.wikipedia.ro.java.wikiprojects.utils.WikiprojectsUtils;

public class WikiprojectTraverser {
    private String wikiprojectName;

    private String wikiAddress;

    private MultiKeyMap<ArticleClass, Integer> articles = new MultiKeyMap<ArticleClass, Integer>();

    public WikiprojectTraverser(String wikiprojectName, String wikiAddress) {
        this.wikiprojectName = wikiprojectName;
        this.wikiAddress = wikiAddress;
    }

    public static final Map<String, String> ALIASES = new HashMap<String, String>() {
        {
            put("Limbi", "Lingvistică");
            put("Filosofie", "Filozofie");
            put("ȚL", "Țările lumii");
            put("Pn", "Perioada napoleoniană");
            put("Fb", "Fotbal");
            put("PLS", "Popoarele și limbile slave");
            put("Vexilologie", "Vexilologie și heraldică");
            put("Heraldică", "Vexilologie și heraldică");
        }
    };

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

                WikiprojectsModel projModel = WikiprojectsModel.fromTalkPage(classificationText);

                qualClass = ArticleClass.fromString(projModel.getQualClass());
                for (Map.Entry<String, String> eachProjectEntries : projModel.getImportanceMap().entrySet()) {
                    if (StringUtils.equals(
                        defaultString(ALIASES.get(eachProjectEntries.getKey()), eachProjectEntries.getKey()),
                        defaultString(ALIASES.get(wikiprojectName), wikiprojectName))) {
                        impClass = ArticleClass.fromString(eachProjectEntries.getValue());
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
            .append(wikiprojectName).append(" (importanță)|Neclasificate]] || Total ").append("\n|-")
            .append("\n! rowspan=\"15\" | Calitate");

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
        printTableLine(sbuilder, ArticleClass.FUTURE,
            "{{clasament-viitor|categorie=Categorie:Articole cu subiect de viitor ale proiectului ", "|viitor}}");
        printTableLine(sbuilder, ArticleClass.UNKNOWN_QUALITY,
            "[[:Categorie:Articole neclasificate ale proiectului " + wikiprojectName + " (calitate)|Neclasificate]]", null);
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
