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

        String attribsStr = null;
        if (null != attribs) {
            attribsStr = attribs.stream().map(eachPart -> eachPart.toString()).collect(Collectors.joining());
            if (null != attribsStr && 0 < attribsStr.trim().length()) {
                sbuild.append(' ').append(attribsStr.trim()).append(" |");
            }
        }

        if (null != subParts && 0 < subParts.size()) {
            StringBuilder subPartsBuilder = new StringBuilder();
            for (WikiPart eachSubPart : subParts) {
                subPartsBuilder.append(eachSubPart);
            }
            if (!Character.isWhitespace(subPartsBuilder.charAt(0))) {
                sbuild.append(' ');
            }
            sbuild.append(subPartsBuilder);
        }

        return sbuild.toString();
    }

}
