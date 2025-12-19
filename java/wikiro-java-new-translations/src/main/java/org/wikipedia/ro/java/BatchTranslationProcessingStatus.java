package org.wikipedia.ro.java;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;

@Getter
public class BatchTranslationProcessingStatus
{
    private List<String> successfulProcessings = new ArrayList<>();
    private Map<String, String> processingFailures = new HashMap<>();

    public void addSuccessfulProcessing(String pageName)
    {
        successfulProcessings.add(pageName);
    }
    
    public void addProcessingFailure(String pageName, String errorMessage)
    {
        processingFailures.put(pageName, errorMessage);
    }
}
