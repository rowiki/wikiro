package org.wikipedia.ro.java.citation.data;

import lombok.Data;

@Data public class Zotero
{
    private String DOI;
    private String ISSN;
    private String ISBN;
    private String abstractNote;
    private Creator[] creators;
    private String date;
    private String extra;
    private String issue;
    private String itemKey;
    private String itemType;
    private int itemVersion;
    private String journalAbbreviation;
    private String language;
    private String libraryCatalog;
    private String pages;
    private String publicationTitle;
    private String title;
    private String volume;
    private String websiteTitle;
    private ZoteroTag[] tags;
}
