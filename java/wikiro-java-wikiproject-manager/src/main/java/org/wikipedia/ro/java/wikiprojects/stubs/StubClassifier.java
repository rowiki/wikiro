package org.wikipedia.ro.java.wikiprojects.stubs;

import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.containsAny;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

import org.wikipedia.Wiki;
import org.wikipedia.ro.java.wikiprojects.utils.Credentials;
import org.wikipedia.ro.java.wikiprojects.utils.WikiprojectsUtils;
import org.wikipedia.ro.utils.WikiTemplate;

public class StubClassifier {

    private String wikiprojectName;

    private String wikiAddress;

    private int startIndex = 0;

    private Map<String, Pattern> projectPatterns = new HashMap<String, Pattern>();

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
                System.out.println(
                    "Parcurg categorie " + crtCat + " [ " + (1 + catIdx) + '/' + visitableCategories.size() + " ] ");
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

            Pattern singleProjPattern = Pattern.compile(
                "\\{\\{\\s*(?:Wiki)?[Pp]ro[ij]ect ([^\\|\\}]+)(?:[^\\}]*\\|\\s*clasament\\s*=\\s*([^\\|\\}]+))?[^\\\\}]*\\}\\}",
                Pattern.DOTALL);
            Pattern multiProjPattern = Pattern.compile("\\{\\{\\s*Proiecte multiple[^\\}\\{]*\\}\\}", Pattern.DOTALL);
            Pattern bpvPattern = Pattern.compile("\\{\\{\\s*[Bb]pv\\s*\\}\\}\\n?", Pattern.DOTALL);
            Pattern importanceIndexedPattern = Pattern.compile("importanță(\\d+)");
            Pattern projIndexedPattern = Pattern.compile("proiect(\\d+)");
            List<String> stubList = new ArrayList<String>(stubs);
            for (int stubIdx = startIndex; stubIdx < stubList.size(); stubIdx++) {
                String eachStub = stubList.get(stubIdx);
                String stubTalkPage = rowiki.getTalkPage(eachStub);
                System.out.println("Vizitez ciot " + eachStub + " [ " + (1 + stubIdx) + '/' + stubs.size() + " ] ");
                String subproj = getSubproject(eachStub, rowiki);
                boolean talkExists = rowiki.exists(new String[] { stubTalkPage })[0];
                String newTalk = "";
                String talk = null;
                if (talkExists) {
                    talk = rowiki.getPageText(stubTalkPage);
                    Pattern thisProjPattern = getPattern(subproj);
                    Matcher projTemplateMatcher = thisProjPattern.matcher(talk);
                    StringBuffer talkBuffer = new StringBuffer();
                    if (projTemplateMatcher.find()) {
                        if (null == projTemplateMatcher.group(1)) {
                            WikiTemplate wtemplate = new WikiTemplate(projTemplateMatcher.group(0));
                            wtemplate.setTemplateTitle(trim(removeStart(wtemplate.getTemplateTitle(), "Wiki")));
                            if (!wtemplate.getParamNames().contains("clasament")
                                || 0 == wtemplate.getParams().get("clasament").length()) {
                                if (wtemplate.getParamNames().contains("clasificare")
                                    && 0 < wtemplate.getParams().get("clasificare").length()) {
                                    wtemplate.setParam("clasament", wtemplate.getParams().get("clasificare"));
                                    wtemplate.removeParam("clasificare");
                                } else {
                                    wtemplate.setParam("clasament", "ciot");
                                }
                                projTemplateMatcher.appendReplacement(talkBuffer, wtemplate.toString());
                            } else {
                                projTemplateMatcher.appendReplacement(talkBuffer, projTemplateMatcher.group(0));
                            }
                        } else {
                            projTemplateMatcher.appendReplacement(talkBuffer, projTemplateMatcher.group(0));
                        }
                        while (projTemplateMatcher.find()) {
                            projTemplateMatcher.appendReplacement(talkBuffer, "");
                        }
                        projTemplateMatcher.appendTail(talkBuffer);
                        newTalk = talkBuffer.toString();
                    } else {

                        Map<String, String> projsImportance = new HashMap<String, String>();
                        Map<String, String> otherParams = new HashMap<String, String>();
                        String qualClass = "ciot";
                        // find one other project
                        Matcher otherProjTemplateMatcher = singleProjPattern.matcher(talk);
                        while (otherProjTemplateMatcher.find()) {
                            WikiTemplate template = new WikiTemplate(trim(otherProjTemplateMatcher.group(0)));
                            projsImportance.put(trim(otherProjTemplateMatcher.group(1)),
                                defaultString(template.getParams().get("importanță"), ""));
                            if (template.getParams().containsKey("clasificare")) {
                                qualClass = defaultIfBlank(template.getParams().get("clasificare"), qualClass);
                            } else if (template.getParams().containsKey("clasament")) {
                                qualClass = defaultIfBlank(template.getParams().get("clasament"), qualClass);
                            }
                            for (Map.Entry<String, String> eachParamEntry : template.getParams().entrySet()) {
                                if (!startsWithAny(eachParamEntry.getKey(), "importanță", "proiect", "clasament",
                                    "clasificare")) {
                                    otherParams.put(eachParamEntry.getKey(), eachParamEntry.getValue());
                                }
                            }
                        }
                        otherProjTemplateMatcher.reset();
                        StringBuffer newTextBuf = new StringBuffer();
                        while (otherProjTemplateMatcher.find()) {
                            otherProjTemplateMatcher.appendReplacement(newTextBuf, "");
                        }
                        otherProjTemplateMatcher.appendTail(newTextBuf);

                        Matcher multiProjsTemplateMatcher = multiProjPattern.matcher(newTextBuf.toString());
                        if (multiProjsTemplateMatcher.find()) {
                            WikiTemplate template = new WikiTemplate(multiProjsTemplateMatcher.group(0));
                            List<String> importances =
                                new ArrayList<String>(Collections.nCopies(1 + template.getParamNames().size(), null));
                            List<String> projects =
                                new ArrayList<String>(Collections.nCopies(1 + template.getParamNames().size(), null));
                            for (Map.Entry<String, String> eachParamEntry : template.getParams().entrySet()) {
                                Matcher idxMatcherProj = projIndexedPattern.matcher(eachParamEntry.getKey());
                                if (idxMatcherProj.matches()) {
                                    int idx = Integer.parseInt(idxMatcherProj.group(1));
                                    projects.set(idx, eachParamEntry.getValue());
                                    continue;
                                }
                                Matcher idxMatcherImportance = importanceIndexedPattern.matcher(eachParamEntry.getKey());
                                if (idxMatcherImportance.matches()) {
                                    int idx = Integer.parseInt(idxMatcherImportance.group(1));
                                    importances.set(idx, eachParamEntry.getValue());
                                    continue;
                                }
                                if (eachParamEntry.getKey().equals("clasament")) {
                                    qualClass = eachParamEntry.getValue();
                                    continue;
                                }
                                otherParams.put(eachParamEntry.getKey(), eachParamEntry.getValue());
                            }
                            for (int idx = 0; idx < projects.size(); idx++) {
                                if (null != projects.get(idx)) {
                                    projsImportance.put(projects.get(idx), defaultString(importances.get(idx)));
                                }
                            }

                        }
                        if (!projsImportance.containsKey(subproj)) {
                            projsImportance.put(subproj, "");
                        }

                        boolean removeBpv = false;
                        WikiTemplate newTemplate = new WikiTemplate();
                        if (2 > projsImportance.size()) {
                            newTemplate.setTemplateTitle("Proiect " + subproj);
                            newTemplate.setParam("clasament", "ciot");
                            if (projsImportance.containsKey(subproj)) {
                                newTemplate.setParam("importanță", projsImportance.get(subproj));
                            }
                        } else {
                            newTemplate.setTemplateTitle("Proiecte multiple");
                            int idx = 1;
                            for (Map.Entry<String, String> eachProjEntry : projsImportance.entrySet()) {
                                newTemplate.setParam("proiect" + idx, eachProjEntry.getKey());
                                if (isNotEmpty(eachProjEntry.getValue())) {
                                    newTemplate.setParam("importanță" + idx, eachProjEntry.getValue());
                                }
                                idx++;
                            }
                            newTemplate.setParam("clasament", qualClass);
                            for (Map.Entry<String, String> eachOtherEntry : otherParams.entrySet()) {
                                newTemplate.setParam(eachOtherEntry.getKey(), eachOtherEntry.getValue());
                            }
                        }
                        Matcher bpvMatcher = bpvPattern.matcher(talk);
                        if (bpvMatcher.find()) {
                            newTemplate.setParam("living", "yes");
                            removeBpv = true;
                        }

                        multiProjsTemplateMatcher.reset();
                        if (multiProjsTemplateMatcher.find()) {
                            newTextBuf = new StringBuffer();
                            multiProjsTemplateMatcher.appendReplacement(newTextBuf, newTemplate.toString());
                            multiProjsTemplateMatcher.appendTail(newTextBuf);
                            newTalk = newTextBuf.toString();
                        } else {
                            newTalk = newTemplate.toString() + '\n' + newTextBuf;
                        }

                        if (removeBpv) {
                            bpvMatcher = bpvPattern.matcher(newTalk);
                            newTextBuf = new StringBuffer();
                            while (bpvMatcher.find()) {
                                bpvMatcher.appendReplacement(newTextBuf, "");
                            }
                            bpvMatcher.appendTail(newTextBuf);
                            newTalk = newTextBuf.toString();
                        }
                    }
                } else {
                    newTalk = "{{Proiect " + subproj + "|clasament=ciot}}";
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

    private Pattern getPattern(String projectName) {
        if (!projectPatterns.containsKey(projectName)) {
            projectPatterns.put(projectName, Pattern.compile("\\{\\{\\s*(?:Wiki)?[Pp]ro[ij]ect " + projectName
                + "\\s*(?:[^\\}]*\\|\\s*clasament\\s*=\\s*([^\\|\\}]+))?[^\\\\}]*\\}\\}", Pattern.DOTALL));
        }
        return projectPatterns.get(projectName);
    }
}
