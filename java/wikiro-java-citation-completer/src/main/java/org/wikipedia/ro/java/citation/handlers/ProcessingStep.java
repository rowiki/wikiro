package org.wikipedia.ro.java.citation.handlers;

import java.util.Map;

public interface ProcessingStep
{
    Map<String, String> processParams(Map<String, String> params);
}
