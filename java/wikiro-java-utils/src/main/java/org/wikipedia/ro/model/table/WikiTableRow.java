package org.wikipedia.ro.model.table;

import java.util.stream.Collectors;

import org.wikipedia.ro.model.WikiPart;

public class WikiTableRow extends WikiTableElement {

    @Override
    public String toString() {
        StringBuilder sbuild = new StringBuilder("\n|-");
        if (null != attribs) {
            sbuild.append(' ').append(attribs.stream().map(eachPart -> eachPart.toString()).collect(Collectors.joining()));
        }
        
        for (WikiPart eachSubPart: subParts) {
            sbuild.append(eachSubPart);
        }
        
        return sbuild.toString();
    }

}
