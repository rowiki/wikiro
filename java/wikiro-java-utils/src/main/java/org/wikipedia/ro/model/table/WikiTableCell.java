package org.wikipedia.ro.model.table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.wikipedia.ro.model.WikiPart;

public class WikiTableCell extends WikiPart {
    private List<WikiPart> subParts;
    private boolean header;
    private boolean onOwnRow;

    public List<WikiPart> getSubParts() {
        return Collections.unmodifiableList(subParts);
    }

    public void setSubParts(List<WikiPart> subParts) {
        if (null == subParts) {
            return;
        }
        if (null != this.subParts) {
            this.subParts.clear();
        }
        for (WikiPart eachSubPart : subParts) {
            this.addSubPart(eachSubPart);
        }
    }

    public void addSubPart(WikiPart subPart) {
        if (null == this.subParts) {
            this.subParts = new ArrayList<>();
        }
        this.subParts.add(subPart);
    }

    public boolean isHeader() {
        return header;
    }

    public void setHeader(boolean header) {
        this.header = header;
    }

    public boolean isOnOwnRow() {
        return onOwnRow;
    }

    public void setOnOwnRow(boolean onOwnRow) {
        this.onOwnRow = onOwnRow;
    }

}
