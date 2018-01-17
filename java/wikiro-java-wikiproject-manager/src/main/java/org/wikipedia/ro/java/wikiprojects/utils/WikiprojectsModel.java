package org.wikipedia.ro.java.wikiprojects.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.*;

public class WikiprojectsModel {
    private String qualClass;
    private Map<String, String> projectsImportance = new LinkedHashMap<String, String>();
    private boolean livingPerson;

    private static final Pattern moreProjectsFinderPattern =
        Pattern.compile("\\{\\{\\s*[Pp]roiecte multiple\\s*(\\|([^\\}]*))*\\}\\}", Pattern.DOTALL);

    private static final Pattern oneProjectFinderPattern =
        Pattern.compile("\\{\\{\\s*(?:Wiki)?[Pp]ro[ij]ect ([^\\|\\}]+)(\\|([^\\}]*))*\\}\\}", Pattern.DOTALL);

    private static final Pattern argsPattern =
        Pattern.compile("\\|\\s*([^=\\|\\}]+)\\s*=\\s*([^\\|\\}]+)\\s*", Pattern.DOTALL);

    private static final Pattern projArgPattern = Pattern.compile("proiect(\\d+)");
    private static final Pattern importanceArgPattern = Pattern.compile("importanță(\\d+)");

    public void setImportance(String project, String importance) {
        projectsImportance.put(project, importance);
    }

    public String getImportance(String project) {
        return projectsImportance.get(project);
    }

    public String getQualClass() {
        return qualClass;
    }

    public void setQualClass(String qualClass) {
        this.qualClass = qualClass;
    }

    public boolean isLivingPerson() {
        return livingPerson;
    }

    public void setLivingPerson(boolean livingPerson) {
        this.livingPerson = livingPerson;
    }
    
    public boolean isInProject(String project) {
        return this.projectsImportance.containsKey(project);
    }
    
    public void removeFromProject(String project) {
        this.projectsImportance.remove(project);
    }

    public static WikiprojectsModel fromTalkPage(String talkPageText) {
        WikiprojectsModel model = new WikiprojectsModel();
        if (null == talkPageText) {
            return model;
        }
        Matcher moreProjectsMatcher = moreProjectsFinderPattern.matcher(defaultString(talkPageText));
        if (moreProjectsMatcher.find()) {
            Matcher argsMatcher = argsPattern.matcher(moreProjectsMatcher.group(1));
            Map<Integer, String> projsIndices = new HashMap<Integer, String>();
            Map<Integer, String> importanceIndices = new HashMap<Integer, String>();
            while (argsMatcher.find()) {
                if (argsMatcher.group(1).trim().equals("clasament")) {
                    model.setQualClass(argsMatcher.group(2).trim());
                    continue;
                }
                if (Arrays.asList("living", "în viață").contains(argsMatcher.group(1).trim())) {
                    model.setLivingPerson(Arrays.asList("da", "yes").contains(argsMatcher.group(2).trim()));
                    continue;
                }
                Matcher projArgMatcher = projArgPattern.matcher(argsMatcher.group(1));
                if (projArgMatcher.find()) {
                    Integer projectIdx = Integer.valueOf(projArgMatcher.group(1));
                    projsIndices.put(projectIdx, argsMatcher.group(2).trim());
                    continue;
                }
                Matcher importanceArgMatcher = importanceArgPattern.matcher(argsMatcher.group(1));
                if (importanceArgMatcher.find()) {
                    Integer importanceIdx = Integer.valueOf(importanceArgMatcher.group(1));
                    importanceIndices.put(importanceIdx, argsMatcher.group(2).trim());
                    continue;
                }
            }

            List<Integer> indicesList = new ArrayList<Integer>(projsIndices.keySet());
            Collections.sort(indicesList);
            for (Integer eachIdx : indicesList) {
                model.projectsImportance.put(projsIndices.get(eachIdx), defaultString(importanceIndices.get(eachIdx)));
            }
        } else {
            Matcher oneProjectMatcher = oneProjectFinderPattern.matcher(defaultString(talkPageText));
            if (oneProjectMatcher.find()) {
                String projName = oneProjectMatcher.group(1).trim();
                Matcher argsMatcher = argsPattern.matcher(defaultString(oneProjectMatcher.group(2)));
                while (argsMatcher.find()) {
                    switch (argsMatcher.group(1).trim()) {
                    case "clasament":
                        model.setQualClass(argsMatcher.group(2).trim());
                        break;
                    case "living":
                    case "în viață":
                        model.setLivingPerson(Arrays.asList("da", "yes").contains(argsMatcher.group(2).trim()));
                        break;
                    case "importanță":
                        model.projectsImportance.put(projName, argsMatcher.group(2).trim());
                        break;
                    }
                }

            }
        }
        return model;
    }

    public String toString() {
        if (0 == projectsImportance.size()) {
            return "";
        }
        StringBuilder sbuild = new StringBuilder("{{");
        if (1 == projectsImportance.size()) {
            for (Map.Entry<String, String> theEntry : projectsImportance.entrySet()) {
                sbuild.append("Proiect ").append(theEntry.getKey());
                if (livingPerson) {
                    sbuild.append("\n| living = yes");
                }
                sbuild.append("\n| clasament = ").append(null != qualClass ? qualClass : "");
                sbuild.append("\n| importanță = ").append(theEntry.getValue());
            }
        } else {
            sbuild.append("Proiecte multiple");
            if (livingPerson) {
                sbuild.append("\n| living = yes");
            }
            if (null != qualClass) {
                sbuild.append("\n| clasament = ").append(null != qualClass ? qualClass : "");
            }
            int idx = 0;
            for (Map.Entry<String, String> eachEntry : projectsImportance.entrySet()) {
                idx++;
                sbuild.append("\n| proiect").append(idx).append(" = ").append(eachEntry.getKey());
                if (isNotBlank(eachEntry.getValue())) {
                    sbuild.append("\n| importanță").append(idx).append(" = ").append(eachEntry.getValue());
                }
            }
        }
        sbuild.append("\n}}");
        return sbuild.toString();
    }
    
    public String saveToTalkPage(String talkPageText) {
        if (isBlank(talkPageText)) {
            return this.toString();
        }
        StringBuffer newTalkText = new StringBuffer();
        int replacementsDone = 0;
        Matcher moreProjectsMatcher = moreProjectsFinderPattern.matcher(defaultString(talkPageText));
        if (moreProjectsMatcher.find()) {
            moreProjectsMatcher.appendReplacement(newTalkText, this.toString());
            replacementsDone++;
        }
        moreProjectsMatcher.appendTail(newTalkText);

        talkPageText = newTalkText.toString();
        newTalkText = new StringBuffer();

        if (0 == replacementsDone) {
            Matcher oneProjectMatcher = oneProjectFinderPattern.matcher(defaultString(talkPageText));
            if (oneProjectMatcher.find()) {
                oneProjectMatcher.appendReplacement(newTalkText, this.toString());
                replacementsDone++;
            }
            oneProjectMatcher.appendTail(newTalkText);
            talkPageText = newTalkText.toString();
        }

        if (0 == replacementsDone) {
            talkPageText = join(new String[] { this.toString(), trim(defaultString(talkPageText)) }, "\n");
        }
        return talkPageText;
    }

}
