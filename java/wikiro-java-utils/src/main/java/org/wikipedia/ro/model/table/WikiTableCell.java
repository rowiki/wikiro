package org.wikipedia.ro.model.table;

import java.util.stream.Collectors;

import org.wikipedia.ro.model.WikiPart;

public class WikiTableCell extends WikiTableElement {

    private String cellSeparator = "\n|";

    public String getCellSeparator() {
        return cellSeparator;
    }

    public void setCellSeparator(String cellSeparator) {
        this.cellSeparator = cellSeparator;
    }

    @Override
    public String toString() {

        StringBuilder sbuild = new StringBuilder(cellSeparator);

        if (null != attribs) {
            sbuild.append(' ').append(attribs.stream().map(eachPart -> eachPart.toString()).collect(Collectors.joining()))
                .append(" |");
        }

        if (null != subParts && 0 < subParts.size()) {
            sbuild.append(' ');
            for (WikiPart eachSubPart : subParts) {
                sbuild.append(eachSubPart);
            }
        }

        return sbuild.toString();
    }

}
