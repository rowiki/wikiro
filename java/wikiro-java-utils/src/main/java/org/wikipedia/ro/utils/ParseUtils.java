package org.wikipedia.ro.utils;

import java.util.List;
import java.util.stream.Collectors;

import org.wikipedia.ro.model.WikiPart;

public class ParseUtils
{
    public String wikipartListToString(List<WikiPart> parts) {
        if (null == parts) {
            return null;
        }
        return parts.stream().map(Object::toString).collect(Collectors.joining());
    }
}
