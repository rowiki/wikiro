package org.wikipedia.ro.java.wikiprojects.classify;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.startsWithAny;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.wikipedia.ro.java.wikiprojects.utils.Credentials;
import org.wikipedia.ro.java.wikiprojects.utils.WikiprojectsModel;
import org.wikipedia.ro.java.wikiprojects.utils.WikiprojectsUtils;
import org.wikipedia.ro.utils.PageUtils;

public class Classify {

    private Map<String, String> projects;
    private List<String> recursiveCategories;
    private List<String> categories;
    private int depth;
    private String wikiAddress;

    public Classify(String wikiAddress, Map<String, String> projects, List<String> categories,
        List<String> recursiveCategories, int depth) {
        super();
        this.wikiAddress = wikiAddress;
        this.projects = projects;
        this.categories = categories;
        this.recursiveCategories = recursiveCategories;
        this.depth = depth;
    }

    public void classify() {
        Wiki rowiki = new Wiki(wikiAddress);
        Credentials credentials = WikiprojectsUtils.identifyCredentials();
        try {
            rowiki.login(credentials.username, credentials.password);
            rowiki.setMarkBot(true);

            if (projects.size() == 0) {
                System.err.println("No projects specified");
                return;
            }
            if (recursiveCategories.size() + categories.size() == 0) {
                System.err.println("No categories specified");
                return;
            }

            Set<String> pagesToRun = new LinkedHashSet<String>();
            for (String eachCat : categories) {
                pagesToRun.addAll(Arrays.asList(rowiki.getCategoryMembers(eachCat, Wiki.MAIN_NAMESPACE)));
            }
            for (String eachRecCat : recursiveCategories) {
                pagesToRun.addAll(Arrays.asList(rowiki.getCategoryMembers(eachRecCat, depth, false, Wiki.MAIN_NAMESPACE)));
            }

            Wikibase dwiki = new Wikibase("www.wikidata.org");
            int idx = 0;
            for (String eachArticleInCat : pagesToRun) {
                idx++;
                System.out.printf("Working on page %s [ %d/%d ]", eachArticleInCat, idx, pagesToRun.size());
                String eachTalkPageOfArticleInCat = rowiki.getTalkPage(eachArticleInCat);
                String talkPageText = rowiki.getPageText(eachTalkPageOfArticleInCat);
                WikiprojectsModel projectModel = WikiprojectsModel.fromTalkPage(talkPageText);
                if (isBlank(projectModel.getQualClass())) {
                    if (startsWithAny(eachArticleInCat, "Legislatura", "Lista", "Listă", "Galerie", "Galeria")) {
                        projectModel.setQualClass("listă");
                    } else {
                        int proseSize = PageUtils.getProseSize(rowiki, eachArticleInCat);
                        String qualClass =
                            proseSize < 700 ? "ciot" : (proseSize < 2500 ? "start" : (proseSize < 10000 ? "început" : null));
                        if (null != qualClass) {
                            projectModel.setQualClass(qualClass);
                        }

                        Entity wdEntity = dwiki.getWikibaseItemBySiteAndTitle("rowiki", eachArticleInCat);
                        Map<Property, Set<Claim>> wdClaims = wdEntity.getClaims();
                        Set<Claim> instanceOfClaims = wdClaims.get(WikibasePropertyFactory.getWikibaseProperty("P31"));
                        boolean isHuman = false;
                        if (null != instanceOfClaims) {
                            for (Claim eachInstanceOfClaim : instanceOfClaims) {
                                if ("5".equals(((Item) eachInstanceOfClaim.getMainsnak().getData()).getEnt().getId())) {
                                    isHuman = true;
                                    break;
                                }
                            }
                        }
                        if (isHuman) {
                            Set<Claim> deathDateClaims = wdClaims.get(WikibasePropertyFactory.getWikibaseProperty("P570"));
                            if (null == deathDateClaims || 0 == deathDateClaims.size()) {
                                projectModel.setLivingPerson(true);
                            }
                        }
                    }
                }

                for (String eachProj : projects.keySet()) {
                    if (!projectModel.isInProject(eachProj) || isBlank(projectModel.getImportance(eachProj))) {
                        projectModel.setImportance(eachProj, projects.get(eachProj));
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

        } catch (FailedLoginException e) {
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
