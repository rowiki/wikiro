package org.wikipedia.ro.java.wikiprojects.createproj;

import java.io.IOException;

import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.wikipedia.Wiki;
import org.wikipedia.ro.java.wikiprojects.createcats.CatTreeCreator;
import org.wikipedia.ro.java.wikiprojects.utils.Credentials;
import org.wikipedia.ro.java.wikiprojects.utils.WikiprojectsUtils;

public class WikiprojectCreator {
    private String wikiprojectName;

    private String wikiAddress;

    private String wikiprojectDescr;

    private String wikiprojectImage;

    public WikiprojectCreator(String wikiprojectName, String wikiAddress, String wikiprojectDescr, String wikiprojectImage) {
        this.wikiprojectName = wikiprojectName;
        this.wikiAddress = wikiAddress;
        this.wikiprojectDescr = wikiprojectDescr;
        this.wikiprojectImage = wikiprojectImage;
    }

    public void createWikiproject() {
        Wiki wiki = Wiki.newSession(wikiAddress);

        Credentials credentials = WikiprojectsUtils.identifyCredentials();

        try {
            wiki.login(credentials.username, credentials.password);

            wiki.setMarkBot(true);

            String[] projPageTitles = new String[] { "Proiect:" + this.wikiprojectName,
                "Proiect:" + this.wikiprojectName + "/Participanți", "Format:Proiect " + this.wikiprojectName, };

            String[] projPageDefaultTexts = new String[] { "[[Fișier:" + this.wikiprojectImage + "|right|200px]]\n" 
                + "Acest proiect are ca scop crearea unor articole despre "
                + this.wikiprojectDescr + ".\n" + "== Participanți ==\n" + "{{/Participanți}}\n" + "\n" + "== Articole ==\n"
                + "Puteți marca articolele de interes pentru proiectul acesta introducând formatul {{f|Proiect "
                + this.wikiprojectName + "}} în ''pagina de discuții'' a articolelor respective, sau adăugând proiectul ''"
                + this.wikiprojectName + "'' în lista de proiecte specificată cu formatul {{f|Proiecte multiple}}.\n" + "\n"
                + "== Sumar articole ==\n" + "{{/rezumat}}\n" + "\n" + "[[Categorie:WikiProiecte|" + this.wikiprojectName
                + "]]\n",

                "Vă puteți declara interesul față de acest proiect înscriindu-vă semnătura în [{{fullurl:Proiect:"
                    + this.wikiprojectName + "/Participanți|action=edit}} lista următoare]:\n" + "\n" + "* ...\n",

                "<includeonly>{{#invoke:Wikiproject|fromFrame|nume_proiect=" + this.wikiprojectName
                    + "}}</includeonly><noinclude>{{Proiect " + this.wikiprojectName
                    + "|clasament=format|importanță=mare}}</noinclude>\n" };

            boolean[] projectPagesExistance = wiki.exists(projPageTitles);
            for (int i = 0; i < projPageTitles.length; i++) {
                if (!projectPagesExistance[i]) {
                    wiki.edit(projPageTitles[i], projPageDefaultTexts[i], "Robot: creare pagină proiect");
                }
            }
            
            String wikiprojDataTxt = wiki.getPageText("Modul:Wikiproject/data");
            int startOfProjects = StringUtils.indexOf(wikiprojDataTxt, "local projects = {");
            if (0 <= startOfProjects) {
                if (0 > StringUtils.indexOf(wikiprojDataTxt, "[" + this.wikiprojectName + "] = ", startOfProjects)) {
                    String projDeclaration = new StringBuilder("\t['")
                        .append(this.wikiprojectName).append("'] = {")
                        .append("\n\t\t['descriere'] = ").append("'un spațiu de organizare pentru dezvoltarea articolelor despre ").append(this.wikiprojectDescr).append("',")
                        .append("\n\t\t['imagine'] = '").append(this.wikiprojectImage).append('\'')
                        .append("\n\t},\n\n").toString();
                    StringBuilder projDataBuilder = new StringBuilder(wikiprojDataTxt);
                    int placeToInsertProj = 1 + StringUtils.indexOf(projDataBuilder, "\n", startOfProjects);
                    projDataBuilder.insert(placeToInsertProj, projDeclaration);
                    wiki.edit("Modul:Wikiproject/data", projDataBuilder.toString(), "Robot: declarat nou proiect");
                }
            }

            CatTreeCreator catCreator = new CatTreeCreator(wikiprojectName, wikiAddress);
            catCreator.createCats();
        } catch (FailedLoginException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (LoginException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            wiki.logout();
        }
    }
}
