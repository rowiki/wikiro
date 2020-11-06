package org.wikipedia.ro.model.table;

import java.util.stream.Collectors;

import org.wikipedia.ro.model.WikiPart;

public class WikiTable extends WikiTableElement {

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("{|");
        if (null != attribs) {
            builder.append(' ').append(attribs.stream().map(eachPart -> eachPart.toString()).collect(Collectors.joining()));
        }
        
        for (WikiPart eachSubPart: subParts) {
            builder.append(eachSubPart);
        }
        builder.append("\n|}");
        return builder.toString();
    }


}
