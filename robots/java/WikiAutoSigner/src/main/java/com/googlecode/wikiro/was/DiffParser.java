package com.googlecode.wikiro.was;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
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
    private String addedMessage = null;

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
                addedLines = new ArrayList(insertion.getRevised().getLines());
                final List originalLines = new ArrayList(insertion.getOriginal().getLines());
                final List<Integer> indicesToRemove = new ArrayList<Integer>();
                for (int i = 0; i < addedLines.size() && i < originalLines.size(); i++) {
                    if (StringUtils.getCommonPrefix(addedLines.get(i).toString(), originalLines.get(i).toString()).length() > 2) {
                        indicesToRemove.add(i);
                    }
                }
                Collections.sort(indicesToRemove);
                int removedIndicesCnt = 0;
                for (final Integer indexToRemove: indicesToRemove) {
                    addedLines.remove(indexToRemove - removedIndicesCnt);
                    removedIndicesCnt++;
                }
            }
            if (null != addedLines && addedLines.size() > 0) {
                signed = false;
                final Pattern userPageDetector = Pattern.compile("\\[\\[\\:?((Utilizator\\:)|(User\\:))" + ownerName);
                final Pattern userTalkPageDetector = Pattern.compile("\\[\\[\\:?((Discu\u021Bie (U|u)tilizator\\:)|(User talk\\:))"
                    + ownerName);
                final Pattern signedForSomeoneElseDetector = Pattern.compile("\\[\\[\\:?Ajutor\\:Semn\u0103tura personal\u0103");
                final Pattern talkbackDetector = Pattern.compile("\\{\\{(Format\\:)?(((R|r)\u0103spuns)|((T|t)alkback)|((M|m)esaj))");

                for (final Object lineObj: addedLines) {
                    final String line = lineObj.toString();
                    final Matcher userPageDetectorMatcher = userPageDetector.matcher(line);
                    final Matcher userTalkPageDetectorMatcher = userTalkPageDetector.matcher(line);
                    final Matcher signedForSomeoneElseDetectorMatcher = signedForSomeoneElseDetector.matcher(line);
                    final Matcher talkbackDetectorMatcher = talkbackDetector.matcher(line);
                    if (userPageDetectorMatcher.find()) {
                        signed = true;
                    }
                    if (userTalkPageDetectorMatcher.find()) {
                        signed = true;
                    }
                    if (signedForSomeoneElseDetectorMatcher.find()) {
                        signed = true;
                    }
                    signed = signed || talkbackDetectorMatcher.find();
                }
                if (!signed) {
                    line = line + addedLines.size();
                }
            }
            if (null != addedLines) {
                addedMessage = StringUtils.join(addedLines, System.getProperty("line.separator"));
            }
        }
    }

    public boolean isSigned() {
        return signed;
    }

    public int getLine() {
        return line;
    }

    public String getAddedMessage() {
        return addedMessage;
    }
}
