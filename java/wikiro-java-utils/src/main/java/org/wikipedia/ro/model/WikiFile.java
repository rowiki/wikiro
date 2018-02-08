package org.wikipedia.ro.model;

import java.util.List;

public class WikiFile extends WikiPart{
    private String fileName;
    private String sizeDescriptor;
    private String locationDescriptor;
    private String borderFlag;
    private String namespaceName;
    private List<WikiPart> altText;
    private List<WikiPart> description;
    private List<WikiPart> extraDescriptions;
    
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getSizeDescriptor() {
        return sizeDescriptor;
    }

    public void setSizeDescriptor(String sizeDescriptor) {
        this.sizeDescriptor = sizeDescriptor;
    }

    public String getLocationDescriptor() {
        return locationDescriptor;
    }

    public void setLocationDescriptor(String locationDescriptor) {
        this.locationDescriptor = locationDescriptor;
    }

    public String getBorderFlag() {
        return borderFlag;
    }

    public void setBorderFlag(String borderFlag) {
        this.borderFlag = borderFlag;
    }

    public List<WikiPart> getDescription() {
        return description;
    }

    public void setDescription(List<WikiPart> description) {
        this.description = description;
    }

}
