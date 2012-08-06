package com.googlecode.wikiro.was;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.wikipedia.Wiki.Revision;

import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;

public class DiffParser {
    private Revision prevRevision;
    private Revision crtRevision;
    private String addedText;
    private String removedText;

    public DiffParser(Revision prevRevision, Revision crtRevision) {
        super();
        this.prevRevision = prevRevision;
        this.crtRevision = crtRevision;
    }

    public void analyze() throws IOException {
        List<String> prevContents = Arrays.asList(prevRevision.getText().split("\\r?\\n"));
        List<String> crtContents = Arrays.asList(crtRevision.getText().split("\\r?\\n"));
        
        Patch diff = DiffUtils.diff(prevContents, crtContents);
        for (Delta delta : diff.getDeltas()) {
            System.out.println(delta);
        }
    }
}
