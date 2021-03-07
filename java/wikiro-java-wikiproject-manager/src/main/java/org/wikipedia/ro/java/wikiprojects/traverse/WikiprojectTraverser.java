package org.wikipedia.ro.java.wikiprojects.traverse;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.prependIfMissing;
import static org.apache.commons.lang3.StringUtils.removeStart;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.security.auth.login.LoginException;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.lang3.StringUtils;
import org.wikipedia.Wiki;
import org.wikipedia.ro.java.wikiprojects.model.WikiprojectsModel;
import org.wikipedia.ro.java.wikiprojects.utils.ArticleClass;
import org.wikipedia.ro.java.wikiprojects.utils.DefaultImportanceClass;
import org.wikipedia.ro.java.wikiprojects.utils.ImportanceClass;
import org.wikipedia.ro.java.wikiprojects.utils.NumberedImportanceClass;
import org.wikipedia.ro.java.wikiprojects.utils.QualityClass;
import org.wikipedia.ro.utils.CredentialUtils;
import org.wikipedia.ro.utils.Credentials;

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

        Wiki wiki = Wiki.newSession(wikiAddress);

        Credentials credentials = CredentialUtils.identifyCredentials();

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

                QualityClass qualClass = null;
                ImportanceClass impClass = null;

                WikiprojectsModel projModel = WikiprojectsModel.fromTalkPage(classificationText);

                qualClass = QualityClass.fromString(projModel.getQualClass());
                for (Map.Entry<String, String> eachProjectEntries : projModel.getImportanceMap().entrySet()) {
                    if (StringUtils.equals(
                        defaultString(ALIASES.get(eachProjectEntries.getKey()), eachProjectEntries.getKey()),
                        defaultString(ALIASES.get(wikiprojectName), wikiprojectName))) {
                        impClass = defaultIfNull(NumberedImportanceClass.fromString(eachProjectEntries.getValue()),
                            DefaultImportanceClass.fromString(eachProjectEntries.getValue()));
                    }
                }

                addArticleToClass(defaultIfNull(qualClass, QualityClass.UNKNOWN_QUALITY),
                    defaultIfNull(impClass, DefaultImportanceClass.UNSPECIFIED));
            }

            String projectBriefingPage = "Proiect:" + wikiprojectName + "/rezumat";

            wiki.edit(projectBriefingPage, createBriefingPage(),
                "Actualizat statut articole pentru proiectul " + wikiprojectName);

        } finally {
            wiki.logout();
        }

    }

    private void addArticleToClass(QualityClass qualClass, ImportanceClass impClass) {

        Integer prevValue = defaultIfNull(articles.get(qualClass, impClass), Integer.valueOf(0));
        articles.put(qualClass, impClass, Integer.valueOf(1 + prevValue));

        prevValue = defaultIfNull(articles.get(qualClass, DefaultImportanceClass.ALL), Integer.valueOf(0));
        articles.put(qualClass, DefaultImportanceClass.ALL, Integer.valueOf(1 + prevValue));

        prevValue = defaultIfNull(articles.get(QualityClass.ALL_QUALITY, impClass), Integer.valueOf(0));
        articles.put(QualityClass.ALL_QUALITY, impClass, Integer.valueOf(1 + prevValue));

        prevValue = defaultIfNull(articles.get(QualityClass.ALL_QUALITY, DefaultImportanceClass.ALL), Integer.valueOf(0));
        articles.put(QualityClass.ALL_QUALITY, DefaultImportanceClass.ALL, Integer.valueOf(1 + prevValue));

    }

    private String createBriefingPage() {
        StringBuilder sbuilder = new StringBuilder("{| class=\"wikitable\" style=\"text-align: center;\"");
        if (equalsIgnoreCase("România în Primul Război Mondial", wikiprojectName)) {
            sbuilder.append("\n|-")
                .append("\n! colspan=\"2\" rowspan=\"2\" | Clasamentul<br/>articolelor !! colspan=\"9\" | Importanță")
                .append("\n|-").append("\n!");
            String colHeaders =
                IntStream.range(0, 6).mapToObj(i -> "{{Clasament-Nivel|" + i + "|categorie=Categorie:Articole de nivel " + i
                    + " ale proiectului " + wikiprojectName + "|" + i + "}}").collect(Collectors.joining(" !! "));

            sbuilder.append(colHeaders);

            sbuilder.append(" !! [[:Categorie:Articole neclasificate ale proiectului ").append(wikiprojectName)
                .append(" (importanță)|Neclasificate]] || Total ").append("\n|-").append("\n! rowspan=\"15\" | Calitate");

        } else {
            sbuilder.append("\n|-")
                .append("\n! colspan=\"2\" rowspan=\"2\" | Clasamentul<br/>articolelor !! colspan=\"7\" | Importanță")
                .append("\n|-")
                .append("\n!{{clasament-top|categorie=Categorie:Cele mai importante articole ale proiectului ")
                .append(wikiprojectName)
                .append("|Top}} !! {{clasament-mare|categorie=Categorie:Articole de importanță mare pentru proiectul ")
                .append(wikiprojectName)
                .append("|Mare}} !! {{clasament-mediu|categorie=Categorie:Articole de importanță medie pentru proiectul ")
                .append(wikiprojectName)
                .append("|Medie}} !! {{clasament-mic|categorie=Categorie:Articole de importanță mică pentru proiectul ")
                .append(wikiprojectName).append("|Mică}} !! [[:Categorie:Articole neclasificate ale proiectului ")
                .append(wikiprojectName).append(" (importanță)|Neclasificate]] || Total ").append("\n|-")
                .append("\n! rowspan=\"15\" | Calitate");
        }
        printTableLine(sbuilder, QualityClass.FA, "{{clasament-AC|categorie=Categorie:Articole de calitate ale proiectului ",
            "|AC}}");
        printTableLine(sbuilder, QualityClass.A, "{{clasament-A|categorie=Categorie:Articole de clasa „A” ale proiectului ",
            "|A}}");
        printTableLine(sbuilder, QualityClass.GA, "{{clasament-AB|categorie=Categorie:Articole bune ale proiectului ",
            "|AB}}");
        printTableLine(sbuilder, QualityClass.B, "{{clasament-B|categorie=Categorie:Articole de clasa „B” ale proiectului ",
            "|B}}");
        printTableLine(sbuilder, QualityClass.C, "{{clasament-C|categorie=Categorie:Articole de clasa „C” ale proiectului ",
            "|C}}");
        printTableLine(sbuilder, QualityClass.START,
            "{{clasament-început|categorie=Categorie:Articole de clasa „început” ale proiectului ", "|început}}");
        printTableLine(sbuilder, QualityClass.STUB,
            "{{clasament-ciot|categorie=Categorie:Articole de clasa „ciot” ale proiectului ", "|ciot}}");
        printTableLine(sbuilder, QualityClass.LIST,
            "{{clasament-listă|categorie=Categorie:Articole de clasa „listă” ale proiectului ", "|listă}}");
        printTableLine(sbuilder, QualityClass.FL, "{{clasament-LC|categorie=Categorie:Liste de calitate ale proiectului ",
            "|listă}}");
        printTableLine(sbuilder, QualityClass.PORTAL,
            "{{clasament-portal|categorie=Categorie:Articole de clasa „portal” ale proiectului ", "|portal}}");
        printTableLine(sbuilder, QualityClass.TEMPLATE,
            "{{clasament-format|categorie=Categorie:Articole de clasa „format” ale proiectului ", "|format}}");
        printTableLine(sbuilder, QualityClass.FUTURE,
            "{{clasament-viitor|categorie=Categorie:Articole cu subiect de viitor ale proiectului ", "|viitor}}");
        printTableLine(sbuilder, QualityClass.UNKNOWN_QUALITY,
            "[[:Categorie:Articole neclasificate ale proiectului " + wikiprojectName + " (calitate)|Neclasificate]]", null);
        printTableLine(sbuilder, QualityClass.ALL_QUALITY, "Total", null);

        sbuilder.append("\n|}");
        return sbuilder.toString();
    }

    private void printTableLine(StringBuilder sb, QualityClass aclass, String catPrefix, String catSuffix) {
        sb.append("\n|-").append("\n! ").append(catPrefix);
        if (null != catSuffix) {
            sb.append(wikiprojectName).append(catSuffix);
        }
        if (equalsIgnoreCase("România în Primul Război Mondial", wikiprojectName)) {
            sb.append("\n| ").append(stringifyValue(articles.get(aclass, NumberedImportanceClass.L0))).append(" || ")
                .append(stringifyValue(articles.get(aclass, NumberedImportanceClass.L1))).append(" || ")
                .append(stringifyValue(articles.get(aclass, NumberedImportanceClass.L2))).append(" || ")
                .append(stringifyValue(articles.get(aclass, NumberedImportanceClass.L3))).append(" || ")
                .append(stringifyValue(articles.get(aclass, NumberedImportanceClass.L4))).append(" || ")
                .append(stringifyValue(articles.get(aclass, NumberedImportanceClass.L5))).append(" || ")
                .append(stringifyValue(articles.get(aclass, DefaultImportanceClass.UNSPECIFIED))).append(" || ")
                .append(stringifyValue(articles.get(aclass, DefaultImportanceClass.ALL)));

        } else {
            sb.append("\n| ").append(stringifyValue(articles.get(aclass, DefaultImportanceClass.TOP))).append(" || ")
                .append(stringifyValue(articles.get(aclass, DefaultImportanceClass.HIGH))).append(" || ")
                .append(stringifyValue(articles.get(aclass, DefaultImportanceClass.MID))).append(" || ")
                .append(stringifyValue(articles.get(aclass, DefaultImportanceClass.LOW))).append(" || ")
                .append(stringifyValue(articles.get(aclass, DefaultImportanceClass.UNSPECIFIED))).append(" || ")
                .append(stringifyValue(articles.get(aclass, DefaultImportanceClass.ALL)));
        }
    }

    private String stringifyValue(Integer value) {
        if (null == value || 0 == value.intValue()) {
            return "";
        }
        return "{{formatnum|" + String.valueOf(value) + "}}";
    }
}
