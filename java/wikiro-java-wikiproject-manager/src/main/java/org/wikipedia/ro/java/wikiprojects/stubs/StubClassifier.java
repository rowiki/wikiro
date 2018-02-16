package org.wikipedia.ro.java.wikiprojects.stubs;

import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.containsAny;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.apache.commons.lang3.StringUtils.startsWithAny;
import static org.apache.commons.lang3.StringUtils.startsWithIgnoreCase;
import static org.apache.commons.lang3.StringUtils.substring;
import static org.apache.commons.lang3.StringUtils.trim;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

import org.wikipedia.Wiki;
import org.wikipedia.ro.java.wikiprojects.WikiprojectsHierarchy;
import org.wikipedia.ro.java.wikiprojects.utils.Credentials;
import org.wikipedia.ro.java.wikiprojects.utils.WikiprojectsModel;
import org.wikipedia.ro.java.wikiprojects.utils.WikiprojectsUtils;

public class StubClassifier {

    private String wikiprojectName;

    private String wikiAddress;

    private int startIndex = 0;

    private List<String> synonyms;

    public StubClassifier(String wikiprojectName, String wikiAddress, int startIndex, List<String> synonyms) {
        super();
        this.wikiprojectName = wikiprojectName;
        this.wikiAddress = wikiAddress;
        this.startIndex = startIndex;
        this.synonyms = synonyms;
    }

    public StubClassifier(String wikiprojectName, String wikiAddress) {
        this(wikiprojectName, wikiAddress, 0, Collections.<String> emptyList());
    }

    public void classifyStubs() {

        Wiki rowiki = new Wiki(wikiAddress);
        Credentials credentials = WikiprojectsUtils.identifyCredentials();
        try {
            rowiki.login(credentials.username, credentials.password);
            rowiki.setMarkBot(true);

            List<String> visitableCategories = new ArrayList<String>();
            Set<String> projectIdentifiers = new LinkedHashSet<String>();
            projectIdentifiers.add(wikiprojectName);
            projectIdentifiers.addAll(synonyms);
            for (String eachProjSynonym : projectIdentifiers) {
                visitableCategories.add("Cioturi " + capitalize(eachProjSynonym));
                visitableCategories
                    .add("Cioturi " + lowerCase(substring(eachProjSynonym, 0, 1)) + substring(eachProjSynonym, 1));
                visitableCategories
                    .add("Cioturi legate de " + lowerCase(substring(eachProjSynonym, 0, 1)) + substring(eachProjSynonym, 1));
            }

            Set<String> stubs = new LinkedHashSet<String>();

            int catIdx = 0;
            while (catIdx < visitableCategories.size()) {
                String crtCat = visitableCategories.get(catIdx);
                System.out.printf("Parcurg categorie %s [ %d/%d ] %n", crtCat, (1 + catIdx), visitableCategories.size());
                stubs.addAll(Arrays.asList(rowiki.getCategoryMembers(crtCat, Wiki.MAIN_NAMESPACE)));

                String[] subCats = rowiki.getCategoryMembers(crtCat, Wiki.CATEGORY_NAMESPACE);
                for (String eachSubCat : subCats) {
                    String eachSubCatName = removeStart(eachSubCat, "Categorie:");
                    if (startsWithIgnoreCase(eachSubCatName, "cioturi")) {
                        visitableCategories.add(eachSubCatName);
                    }
                }

                catIdx++;
            }

            Pattern bpvPattern = Pattern.compile("\\{\\{\\s*[Bb]pv\\s*\\}\\}\\n?", Pattern.DOTALL);
            List<String> stubList = new ArrayList<String>(stubs);
            for (int stubIdx = startIndex; stubIdx < stubList.size(); stubIdx++) {
                String eachStub = stubList.get(stubIdx);
                String stubTalkPage = rowiki.getTalkPage(eachStub);
                System.out.printf("Vizitez ciot %s [ %d/%d ] %n", eachStub, (1 + stubIdx), stubList.size());
                String subproj = getSubproject(eachStub, rowiki);
                boolean talkExists = rowiki.exists(new String[] { stubTalkPage })[0];
                String newTalk = "";
                String talk = null;
                if (talkExists) {
                    talk = rowiki.getPageText(stubTalkPage);
                    WikiprojectsModel projModel = WikiprojectsModel.fromTalkPage(talk);
                    if (isBlank(projModel.getQualClass())) {
                        projModel.setQualClass("ciot");
                    }

                    if (projModel.getImportanceMap().keySet().stream()
                        .anyMatch(projkey -> WikiprojectsHierarchy.isParent(wikiprojectName, projkey))) {
                        System.out.println("Already in a child project. Skipping...");
                        continue;
                    }

                    Set<String> parents = projModel.getImportanceMap().keySet().stream()
                        .filter(projkey -> WikiprojectsHierarchy.isParent(projkey, wikiprojectName))
                        .collect(Collectors.toSet());
                    Optional<String> importanceFromParent = projModel.getImportanceMap().keySet().stream()
                        .filter(projkey -> WikiprojectsHierarchy.isParent(projkey, wikiprojectName))
                        .filter(p -> isNotBlank(projModel.getImportance(p))).findFirst()
                        .map(p -> projModel.getImportance(p));

                    parents.stream().forEach(projModel::removeFromProject);

                    if (!projModel.isInProject(wikiprojectName)) {
                        projModel.setImportance(wikiprojectName, importanceFromParent.orElse(""));
                    }

                    boolean removeBpv = false;
                    Matcher bpvMatcher = bpvPattern.matcher(talk);
                    if (bpvMatcher.find()) {
                        projModel.setLivingPerson(true);
                        removeBpv = true;
                    }

                    newTalk = projModel.saveToTalkPage(talk);
                    if (removeBpv) {
                        bpvMatcher = bpvPattern.matcher(newTalk);
                        StringBuffer newTextBuf = new StringBuffer();
                        while (bpvMatcher.find()) {
                            bpvMatcher.appendReplacement(newTextBuf, "");
                        }
                        bpvMatcher.appendTail(newTextBuf);
                        newTalk = newTextBuf.toString();
                    }

                } else {
                    WikiprojectsModel projModel = new WikiprojectsModel();
                    projModel.setImportance(wikiprojectName, "");
                    projModel.setQualClass("ciot");
                    newTalk = projModel.saveToTalkPage(null);
                }
                if (!newTalk.equals(talk)) {
                    rowiki.edit(stubTalkPage, newTalk, "Robot: clasificat ciot pentru Proiect " + subproj);
                }
            }

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
            rowiki.logout();
        }
    }

    private String getSubproject(String page, Wiki wiki) {
        if ("Istorie".equals(wikiprojectName)) {
            if (startsWithAny(lowerCase(page), "bătălia", "asediul", "regimentul", "divizia", "batalionul", "războiul",
                "războaiele", "prima bătălie", "a doua bătălie", "a treia bătălie", "primul război", "al doilea război",
                "al treilea război", "campania", "lupta", "operațiunea")) {
                return "Istorie militară";
            }
        }
        if ("Transport".equals(wikiprojectName)) {
            if (startsWithAny(lowerCase(page), "dn", "dj", "autostrada", "șoseaua", "drumul")
                || containsAny(lowerCase(page), "highway", "interstate")) {
                return "Drumuri";
            }
            if (startsWithAny(lowerCase(page), "aeroport", "airbus", "boeing", "embraer", "american airlines")) {
                return "Aviație";
            }
        }
        return trim(wikiprojectName);
    }
}
