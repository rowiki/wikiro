package org.wikipedia.ro.java.wikiprojects.classify;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.startsWithAny;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

import org.apache.commons.lang3.StringUtils;
import org.wikibase.Wikibase;
import org.wikibase.WikibaseException;
import org.wikibase.WikibasePropertyFactory;
import org.wikibase.data.Claim;
import org.wikibase.data.Entity;
import org.wikibase.data.Item;
import org.wikibase.data.Property;
import org.wikipedia.Wiki;
import org.wikipedia.ro.java.wikiprojects.WikiprojectsHierarchy;
import org.wikipedia.ro.java.wikiprojects.model.WikiprojectsModel;
import org.wikipedia.ro.utils.CredentialUtils;
import org.wikipedia.ro.utils.Credentials;
import org.wikipedia.ro.utils.PageUtils;

public class Classify {

    private Map<String, String> projects;
    private List<String> recursiveCategories;
    private List<String> categories;
    private List<String> tmplCats;
    private int depth;
    private String wikiAddress;
    private boolean autoEvaluateBySize;

    public Classify(String wikiAddress, Map<String, String> projects, List<String> categories,
        List<String> templateCategories, List<String> recursiveCategories, int depth, boolean autoEvaluateBySize) {
        super();
        this.wikiAddress = wikiAddress;
        this.projects = projects;
        this.categories = categories;
        this.tmplCats = templateCategories;
        this.recursiveCategories = recursiveCategories;
        this.depth = depth;
        this.autoEvaluateBySize = autoEvaluateBySize;
    }

    public void classify() {
        Wiki rowiki = Wiki.newSession(wikiAddress);
        Credentials credentials = CredentialUtils.identifyCredentials();
        try {
            rowiki.login(credentials.username, credentials.password);
            rowiki.setMarkBot(true);

            if (projects.size() == 0) {
                System.err.println("No projects specified");
                return;
            }
            if (recursiveCategories.size() + categories.size() + tmplCats.size() == 0) {
                System.err.println("No categories specified");
                return;
            }

            Set<String> pagesToRun = new LinkedHashSet<String>();
            for (String eachCat : categories) {
                pagesToRun.addAll(rowiki.getCategoryMembers(eachCat, Wiki.MAIN_NAMESPACE));
            }
            for (String eachRecCat : recursiveCategories) {
                System.out.printf("Analyzing category %s in depth %d%n", eachRecCat, depth);
                pagesToRun.addAll(rowiki.getCategoryMembers(eachRecCat, depth, false, Wiki.MAIN_NAMESPACE));
            }

            Wikibase dwiki = new Wikibase("www.wikidata.org");
            int idx = 0;
            Pattern faPattern = Pattern.compile("\\{\\{\\s*[Aa]rticol de calitate\\s*\\}\\}");
            Pattern gaPattern = Pattern.compile("\\{\\{\\s*[Aa]rticol bun\\s*\\}\\}");
            Pattern flPattern = Pattern.compile("\\{\\{\\s*[Ll]istă de calitate\\s*\\}\\}");
            Pattern stubPattern = Pattern.compile("\\{\\{\\s*[Cc]iot\\-");
            Pattern blpPattern = Pattern.compile("\\{\\{\\s*[Bb]pv\\s*\\}\\}");

            for (String eachArticleInCat : pagesToRun) {
                idx++;

                System.out.printf("Working on page %s [ %d/%d ]%n", eachArticleInCat, idx, pagesToRun.size());
                String eachTalkPageOfArticleInCat = rowiki.getTalkPage(eachArticleInCat);
                String talkPageText = rowiki.getPageText(List.of(eachTalkPageOfArticleInCat)).stream().filter(Objects::nonNull).findFirst().orElse("");
                WikiprojectsModel projectModel = WikiprojectsModel.fromTalkPage(talkPageText);

                String articleText = rowiki.getPageText(List.of(eachArticleInCat)).stream().findFirst().orElse("");
                Matcher faMatcher = faPattern.matcher(articleText);
                if (faMatcher.find()) {
                    projectModel.setQualClass("AC");
                } else {
                    Matcher flMatcher = flPattern.matcher(articleText);
                    if (flMatcher.find()) {
                        projectModel.setQualClass("LC");
                    } else {
                        Matcher gaMatcher = gaPattern.matcher(articleText);
                        if (gaMatcher.find() && !"A".equals(projectModel.getQualClass())) {
                            projectModel.setQualClass("AB");
                        }
                    }
                }
                if (isBlank(projectModel.getQualClass())) {
                    if (startsWithAny(eachArticleInCat, "Legislatura", "Lista", "Listă", "Galerie", "Galeria")) {
                        projectModel.setQualClass("listă");
                    } else {
                        if (autoEvaluateBySize) {
                            int proseSize = PageUtils.getProseSize(rowiki, eachArticleInCat);
                            String qualClass = proseSize < 700 ? "ciot"
                                : (proseSize < 2500 ? "start" : (proseSize < 10000 ? "început" : null));
                            if (null != qualClass) {
                                projectModel.setQualClass(qualClass);
                            }
                        } else {
                            Matcher stubMatcher = stubPattern.matcher(articleText);
                            if (stubMatcher.find()) {
                                projectModel.setQualClass("ciot");
                            }
                        }
                        Matcher blpMatcher = blpPattern.matcher(defaultString(talkPageText));
                        if (blpMatcher.find()) {
                            projectModel.setLivingPerson(true);
                        }
                        Entity wdEntity = dwiki.getWikibaseItemBySiteAndTitle("rowiki", eachArticleInCat);
                        if (null != wdEntity) {
                            Map<Property, Set<Claim>> wdClaims = wdEntity.getClaims();
                            if (null != wdClaims) {
                                Set<Claim> instanceOfClaims =
                                    wdClaims.get(WikibasePropertyFactory.getWikibaseProperty("P31"));
                                boolean isHuman = false;
                                if (null != instanceOfClaims) {
                                    for (Claim eachInstanceOfClaim : instanceOfClaims) {
                                        if ("5"
                                            .equals(((Item) eachInstanceOfClaim.getMainsnak().getData()).getEnt().getId())) {
                                            isHuman = true;
                                            break;
                                        }
                                    }
                                }
                                if (isHuman) {
                                    Set<Claim> deathDateClaims =
                                        wdClaims.get(WikibasePropertyFactory.getWikibaseProperty("P570"));
                                    if (null == deathDateClaims || deathDateClaims.isEmpty()
                                        || deathDateClaims.stream().noneMatch(claim -> "statement".equals(claim.getType())
                                            && "value".equals(claim.getMainsnak().getSnaktype()))) {
                                        projectModel.setLivingPerson(true);
                                    }
                                }
                            }
                        }
                    }
                }

                for (String eachProj : projects.keySet()) {
                    if (!projectModel.isInProject(eachProj) || isBlank(projectModel.getImportance(eachProj))) {
                        if (projectModel.getImportanceMap().keySet().stream()
                            .anyMatch(projkey -> WikiprojectsHierarchy.isParent(eachProj, projkey))) {
                            System.out.println("Already in a child project. Skipping...");
                            continue;
                        }

                        Set<String> parents = projectModel.getImportanceMap().keySet().stream()
                            .filter(projkey -> WikiprojectsHierarchy.isParent(projkey, eachProj))
                            .collect(Collectors.toSet());
                        Optional<String> importanceFromParent = projectModel.getImportanceMap().keySet().stream()
                            .filter(projkey -> WikiprojectsHierarchy.isParent(projkey, eachProj))
                            .filter(p -> isNotBlank(projectModel.getImportance(p))).findFirst()
                            .map(p -> projectModel.getImportance(p));

                        parents.stream().forEach(projectModel::removeFromProject);

                        projectModel.setImportance(eachProj, importanceFromParent.orElse(projects.get(eachProj)));
                    }
                }

                String newTalkPageText = projectModel.saveToTalkPage(talkPageText);
                if (!StringUtils.equals(newTalkPageText, talkPageText)) {
                    if (!"listă".equals(projectModel.getQualClass())) {
                        newTalkPageText = newTalkPageText.replaceAll("\\{\\{[Bb]pv\\}\\}\\s*", "");
                    }
                    rowiki.edit(eachTalkPageOfArticleInCat, newTalkPageText, "Robot: luat în evidență pentru wikiproiecte");
                }
            }

            List<String> templatesToRun = new ArrayList<>();
            for (String eachTmplCat : tmplCats) {
                templatesToRun.addAll(rowiki.getCategoryMembers(eachTmplCat, Wiki.TEMPLATE_NAMESPACE));
            }
            int tmplIdx = 0;
            for (String eachTmpl : templatesToRun) {
                tmplIdx++;
                System.out.printf("Running on template %s [ %d/%d ]%n", eachTmpl, tmplIdx, templatesToRun.size());

                String tmplTalkPageTitle = rowiki.getTalkPage(eachTmpl);
                String tmplTalkPageText = rowiki.getPageText(List.of(tmplTalkPageTitle)).stream().findFirst().orElse("");
                WikiprojectsModel projectModel = WikiprojectsModel.fromTalkPage(tmplTalkPageText);
                projectModel.setQualClass("format");

                for (String eachProj : projects.keySet()) {
                    if (!projectModel.isInProject(eachProj) || isBlank(projectModel.getImportance(eachProj))) {
                        projectModel.setImportance(eachProj, projects.get(eachProj));
                    }
                }

                String newTalkPageText = projectModel.saveToTalkPage(tmplTalkPageText);
                if (!StringUtils.equals(newTalkPageText, tmplTalkPageText)) {
                    rowiki.edit(tmplTalkPageTitle, newTalkPageText, "Robot: luat în evidență pentru wikiproiecte");
                }
            }

        } catch (

        FailedLoginException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (LoginException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (WikibaseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            rowiki.logout();
        }
    }
}
