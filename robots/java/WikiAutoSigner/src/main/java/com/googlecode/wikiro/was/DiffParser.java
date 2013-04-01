package com.googlecode.wikiro.was;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.wikipedia.Wiki.Revision;

import difflib.ChangeDelta;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.InsertDelta;
import difflib.Patch;

public class DiffParser {
    private final Revision prevRevision;
    private final Revision crtRevision;
    private boolean signed = true;
    private int line = 0;

    public DiffParser(final Revision prevRevision, final Revision crtRevision) {
        super();
        this.prevRevision = prevRevision;
        this.crtRevision = crtRevision;
    }

    public void analyze(final String ownerName) throws IOException {
        final List<String> prevContents = Arrays.asList(prevRevision.getText().split("\\r?\\n"));
        final List<String> crtContents = Arrays.asList(crtRevision.getText().split("\\r?\\n"));

        final Patch diff = DiffUtils.diff(prevContents, crtContents);
        for (final Delta delta : diff.getDeltas()) {
            //System.out.println(delta);
            List addedLines = null;
            if (delta instanceof InsertDelta) {
                final InsertDelta insertion = (InsertDelta) delta;
                line = insertion.getRevised().getPosition();
                addedLines = insertion.getRevised().getLines();
            }
            if (delta instanceof ChangeDelta) {
                final ChangeDelta insertion = (ChangeDelta) delta;
                line = insertion.getRevised().getPosition();
                addedLines = insertion.getRevised().getLines();
            }
            if (null != addedLines && addedLines.size() > 0) {
                signed = false;
                final Pattern userPageDetector = Pattern.compile("\\[\\[\\:?((Utilizator\\:)|(User\\:))" + ownerName);
                final Pattern userTalkPageDetector = Pattern.compile("\\[\\[\\:?((Discu\u021Bie Utilizator\\:)|(User talk\\:))"
                    + ownerName);
                final Pattern signedForSomeoneElseDetector = Pattern.compile("\\[\\[\\:?Ajutor\\:Semn\u0103tura personal\u0103");
                for (final Object lineObj: addedLines) {
                    final String line = lineObj.toString();
                    final Matcher userPageDetectorMatcher = userPageDetector.matcher(line);
                    final Matcher userTalkPageDetectorMatcher = userTalkPageDetector.matcher(line);
                    final Matcher signedForSomeoneElseDetectorMatcher = signedForSomeoneElseDetector.matcher(line);
                    if (userPageDetectorMatcher.find()) {
                        signed = true;
                    }
                    if (userTalkPageDetectorMatcher.find()) {
                        signed = true;
                    }
                    if (signedForSomeoneElseDetectorMatcher.find()) {
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
