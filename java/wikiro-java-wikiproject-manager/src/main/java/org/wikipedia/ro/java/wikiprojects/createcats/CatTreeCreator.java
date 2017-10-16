package org.wikipedia.ro.java.wikiprojects.createcats;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.prependIfMissing;
import static org.wikipedia.ro.java.wikiprojects.utils.ArticleClass.A;
import static org.wikipedia.ro.java.wikiprojects.utils.ArticleClass.B;
import static org.wikipedia.ro.java.wikiprojects.utils.ArticleClass.C;
import static org.wikipedia.ro.java.wikiprojects.utils.ArticleClass.FA;
import static org.wikipedia.ro.java.wikiprojects.utils.ArticleClass.FL;
import static org.wikipedia.ro.java.wikiprojects.utils.ArticleClass.FUTURE;
import static org.wikipedia.ro.java.wikiprojects.utils.ArticleClass.GA;
import static org.wikipedia.ro.java.wikiprojects.utils.ArticleClass.HIGH;
import static org.wikipedia.ro.java.wikiprojects.utils.ArticleClass.LIST;
import static org.wikipedia.ro.java.wikiprojects.utils.ArticleClass.MEDIUM;
import static org.wikipedia.ro.java.wikiprojects.utils.ArticleClass.PORTAL;
import static org.wikipedia.ro.java.wikiprojects.utils.ArticleClass.SMALL;
import static org.wikipedia.ro.java.wikiprojects.utils.ArticleClass.START;
import static org.wikipedia.ro.java.wikiprojects.utils.ArticleClass.STUB;
import static org.wikipedia.ro.java.wikiprojects.utils.ArticleClass.TEMPLATE;
import static org.wikipedia.ro.java.wikiprojects.utils.ArticleClass.TOP;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.LoginException;

import org.wikipedia.Wiki;
import org.wikipedia.ro.java.wikiprojects.utils.ArticleClass;
import org.wikipedia.ro.java.wikiprojects.utils.Credentials;
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
            put(HIGH, "mare");
            put(MEDIUM, "medie");
            put(SMALL, "mică");
            put(TEMPLATE, "format");
            put(LIST, "listă");
            put(FUTURE, "viitor");
            put(FL, "listă de calitate");
        }
    };
    private static final Map<ArticleClass, String> catLinks = new HashMap<ArticleClass, String>() {
        {
            put(HIGH, "pentru proiectul");
            put(MEDIUM, "pentru proiectul");
            put(SMALL, "pentru proiectul");
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
            put(TOP, "Cele mai importante articole");
            put(HIGH, "Articole de importanță mare");
            put(MEDIUM, "Articole de importanță medie");
            put(SMALL, "Articole de importanță mică");
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

        Wiki wiki = new Wiki(wikiAddress);
        
        Credentials credentials = WikiprojectsUtils.identifyCredentials();

        try {
            wiki.login(credentials.username, credentials.password);

            wiki.setMarkBot(true);
            
            String mainCat = "Categorie:Articole din domeniul proiectului " + wikiprojectName;
            if (!wiki.exists(new String[]{mainCat})[0]) {
                StringBuilder sbuild = new StringBuilder("[[Categorie:Articole din domeniul proiectelor|");
                sbuild.append(wikiprojectName);
                sbuild.append("]]");
                wiki.edit(mainCat, sbuild.toString(), "Creare categorie principală pentru proiectul " + wikiprojectName);
            }
            
            for (ArticleClass eachQualClass : ArticleClass.values()) {
                String catToCreate = categoryPrefixes.get(eachQualClass);
                String parentQualCat = defaultIfNull(parentCategories.get(eachQualClass), catToCreate);
                String catLink = defaultString(catLinks.get(eachQualClass), "ale proiectului");
                if (null != catToCreate) {
                    catToCreate = "Categorie:" + catToCreate + " " + catLink + " " + wikiprojectName;
                    parentQualCat = prependIfMissing(parentQualCat, "Categorie:");
                }
                
                if (null != catToCreate && !wiki.exists(new String[]{catToCreate})[0]) {
                    String catKey = defaultIfNull(classKeys.get(eachQualClass), lowerCase(eachQualClass.name()));
                    StringBuilder sbuild = new StringBuilder();
                    sbuild.append("[[").append(mainCat).append('|').append(catKey).append("]]")
                        .append("\n[[").append(parentQualCat).append('|').append(wikiprojectName).append("]]");
                    wiki.edit(catToCreate, sbuild.toString(), "Creare categorie de evaluare pentru proiectul " + wikiprojectName);
                }
            }
            
        } finally {
            wiki.logout();
        }
    }

}
