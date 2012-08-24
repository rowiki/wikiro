package com.googlecode.wikiro.was;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.wikipedia.Wiki.Revision;

import difflib.Delta;
import difflib.DiffUtils;
import difflib.InsertDelta;
import difflib.Patch;

public class DiffParser {
    private Revision prevRevision;
    private Revision crtRevision;
    private boolean signed = true;
    private int line = 0;

    public DiffParser(Revision prevRevision, Revision crtRevision) {
        super();
        this.prevRevision = prevRevision;
        this.crtRevision = crtRevision;
    }

    public void analyze(String ownerName) throws IOException {
        List<String> prevContents = Arrays.asList(prevRevision.getText().split("\\r?\\n"));
        List<String> crtContents = Arrays.asList(crtRevision.getText().split("\\r?\\n"));
        
        Patch diff = DiffUtils.diff(prevContents, crtContents);
        for (Delta delta : diff.getDeltas()) {
            //System.out.println(delta);
            if (delta instanceof InsertDelta) {
                InsertDelta insertion = (InsertDelta) delta;
                line = insertion.getRevised().getPosition();
                List addedLines = insertion.getRevised().getLines();
                signed = false;
                Pattern userPageDetector = Pattern.compile("\\[\\[\\:?((Utilizator\\:)|(User\\:))" + ownerName);
                Pattern userTalkPageDetector = Pattern.compile("\\[\\[\\:?((Discu\u021Bie Utilizator\\:)|(User talk\\:))"
                    + ownerName);
                for (Object lineObj: addedLines) {
                    String line = lineObj.toString();
                    Matcher userPageDetectorMatcher = userPageDetector.matcher(line);
                    Matcher userTalkPageDetectorMatcher = userTalkPageDetector.matcher(line);
                    if (userPageDetectorMatcher.find()) {
                        signed = true;
                    }
                    if (userTalkPageDetectorMatcher.find()) {
                        signed = true;
                    }
                }
                if (!signed) {
                    line = line + addedLines.size();
                }
            }
        }
    }

    public boolean isSigned() {
        return signed;
    }

    public int getLine() {
        return line;
    }
}
