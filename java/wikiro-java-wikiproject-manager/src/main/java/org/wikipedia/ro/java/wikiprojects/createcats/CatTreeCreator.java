package org.wikipedia.ro.java.wikiprojects.createcats;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.prependIfMissing;
import static org.wikipedia.ro.java.wikiprojects.utils.QualityClass.A;
import static org.wikipedia.ro.java.wikiprojects.utils.QualityClass.B;
import static org.wikipedia.ro.java.wikiprojects.utils.QualityClass.C;
import static org.wikipedia.ro.java.wikiprojects.utils.QualityClass.FA;
import static org.wikipedia.ro.java.wikiprojects.utils.QualityClass.FL;
import static org.wikipedia.ro.java.wikiprojects.utils.QualityClass.FUTURE;
import static org.wikipedia.ro.java.wikiprojects.utils.QualityClass.GA;
import static org.wikipedia.ro.java.wikiprojects.utils.QualityClass.LIST;
import static org.wikipedia.ro.java.wikiprojects.utils.QualityClass.PORTAL;
import static org.wikipedia.ro.java.wikiprojects.utils.QualityClass.START;
import static org.wikipedia.ro.java.wikiprojects.utils.QualityClass.STUB;
import static org.wikipedia.ro.java.wikiprojects.utils.QualityClass.TEMPLATE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.LoginException;

import org.wikipedia.Wiki;
import org.wikipedia.ro.java.wikiprojects.utils.ArticleClass;
import org.wikipedia.ro.java.wikiprojects.utils.Credentials;
import org.wikipedia.ro.java.wikiprojects.utils.DefaultImportanceClass;
import org.wikipedia.ro.java.wikiprojects.utils.ImportanceClass;
import org.wikipedia.ro.java.wikiprojects.utils.NumberedImportanceClass;
import org.wikipedia.ro.java.wikiprojects.utils.QualityClass;
import org.wikipedia.ro.java.wikiprojects.utils.WikiprojectsUtils;

public class CatTreeCreator {

    private String wikiprojectName;

    private String wikiAddress;

    private static final Map<ArticleClass, String> classKeys = new HashMap<ArticleClass, String>() {
        {
            put(FA, "AC");
            put(GA, "AB");
            put(START, "început");
            put(STUB, "ciot");
            put(DefaultImportanceClass.HIGH, "mare");
            put(DefaultImportanceClass.MID, "medie");
            put(DefaultImportanceClass.LOW, "mică");
            put(TEMPLATE, "format");
            put(LIST, "listă");
            put(FUTURE, "viitor");
            put(FL, "listă de calitate");
        }
    };
    private static final Map<ArticleClass, String> catLinks = new HashMap<ArticleClass, String>() {
        {
            put(DefaultImportanceClass.HIGH, "pentru proiectul");
            put(DefaultImportanceClass.MID, "pentru proiectul");
            put(DefaultImportanceClass.LOW, "pentru proiectul");
        }
    };
    private static final Map<ArticleClass, String> categoryPrefixes = new HashMap<ArticleClass, String>() {
        {
            put(FA, "Articole de calitate");
            put(A, "Articole de clasa „A”");
            put(GA, "Articole bune");
            put(B, "Articole de clasa „B”");
            put(C, "Articole de clasa „C”");
            put(START, "Articole de clasa „început”");
            put(STUB, "Articole de clasa „ciot”");
            put(LIST, "Articole de clasa „listă”");
            put(FL, "Liste de calitate");
            put(PORTAL, "Articole de clasa „portal”");
            put(TEMPLATE, "Articole de clasa „format”");
            put(FUTURE, "Articole cu subiect de viitor");
            put(DefaultImportanceClass.TOP, "Cele mai importante articole");
            put(DefaultImportanceClass.HIGH, "Articole de importanță mare");
            put(DefaultImportanceClass.MID, "Articole de importanță medie");
            put(DefaultImportanceClass.LOW, "Articole de importanță mică");
        }
    };

    private static final Map<ArticleClass, String> parentCategories = new HashMap<ArticleClass, String>() {
        {
            put(FA, "Articole de calitate (clasament)");
            put(GA, "Articole bune (clasament)");
            put(FL, "Liste de calitate (clasament)");
            put(FUTURE, "Viitor (clasament)");
        }
    };

    public CatTreeCreator(String wikiprojectName, String wikiAddress) {
        this.wikiprojectName = wikiprojectName;
        this.wikiAddress = wikiAddress;
    }

    public void createCats() throws IOException, LoginException {

        Wiki wiki =  Wiki.newSession(wikiAddress);

        Credentials credentials = WikiprojectsUtils.identifyCredentials();

        try {
            wiki.login(credentials.username, credentials.password);

            wiki.setMarkBot(true);

            String mainCat = "Categorie:Articole din domeniul proiectului " + wikiprojectName;
            if (!wiki.exists(new String[] { mainCat })[0]) {
                StringBuilder sbuild = new StringBuilder("[[Categorie:Articole din domeniul proiectelor|");
                sbuild.append(wikiprojectName);
                sbuild.append("]]");
                wiki.edit(mainCat, sbuild.toString(), "Creare categorie principală pentru proiectul " + wikiprojectName);
            }

            List<ArticleClass> classesToCreate = new ArrayList<>();
            classesToCreate.addAll(Arrays.asList(QualityClass.values()));
            ImportanceClass[] impClasses = equalsIgnoreCase("România în Primul Război Mondial", wikiprojectName) ? NumberedImportanceClass.values() :DefaultImportanceClass.values();
            classesToCreate.addAll(Arrays.asList(impClasses));
            for (ArticleClass eachQualClass : classesToCreate) {
                String catToCreate = categoryPrefixes.get(eachQualClass);
                if (null == catToCreate && (eachQualClass instanceof NumberedImportanceClass)) {
                    catToCreate = "Articole de nivel " + eachQualClass;
                }
                String parentQualCat = defaultIfNull(parentCategories.get(eachQualClass), catToCreate);
                String catLink = defaultString(catLinks.get(eachQualClass), "ale proiectului");
                if (null != catToCreate) {
                    catToCreate = "Categorie:" + catToCreate + " " + catLink + " " + wikiprojectName;
                    parentQualCat = prependIfMissing(parentQualCat, "Categorie:");
                }

                if (null != catToCreate && !wiki.exists(new String[] { catToCreate })[0]) {
                    String catKey = defaultIfNull(classKeys.get(eachQualClass), lowerCase(eachQualClass.name()));
                    StringBuilder sbuild = new StringBuilder();
                    sbuild.append("[[").append(mainCat).append('|').append(catKey).append("]]").append("\n[[")
                        .append(parentQualCat).append('|').append(wikiprojectName).append("]]");
                    wiki.edit(catToCreate, sbuild.toString(),
                        "Creare categorie de evaluare pentru proiectul " + wikiprojectName);
                }
            }

            
            String unclassifiedPrefix = "Categorie:Articole neclasificate ale proiectului ";
            String[] unclassifiedTypes = new String[] { "importanță", "calitate" };
            String[] unclassifiedCategories = new String[unclassifiedTypes.length];
            for (int unclassifiedIdx = 0; unclassifiedIdx < unclassifiedTypes.length; unclassifiedIdx++) {
                unclassifiedCategories[unclassifiedIdx] =
                    unclassifiedPrefix + wikiprojectName + " (" + unclassifiedTypes[unclassifiedIdx] + ")";
                String unclassifiedCat = unclassifiedCategories[unclassifiedIdx];
                if (null != unclassifiedCat && !wiki.exists(new String[] { unclassifiedCat })[0]) {
                    StringBuilder sbuild = new StringBuilder();
                    sbuild.append("[[").append(mainCat).append('|').append("neclasificate, ")
                        .append(unclassifiedTypes[unclassifiedIdx]).append("]]").append("\n[[")
                        .append("Categorie:Articole neclasificate").append('|').append(wikiprojectName).append(", ")
                        .append(unclassifiedTypes[unclassifiedIdx]).append("]]");
                    wiki.edit(unclassifiedCat, sbuild.toString(),
                        "Creare categorie de evaluare pentru proiectul " + wikiprojectName);
                }
            }

        } finally {
            wiki.logout();
        }
    }

}
