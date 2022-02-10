package org.wikipedia.ro.java.citation.handlers;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RemoveParamsProcessingStep implements ProcessingStep
{
    private List<String> paramsToRemove;

    public RemoveParamsProcessingStep(String... params)
    {
        paramsToRemove = Arrays.stream(params).collect(Collectors.toList());
    }

    @Override
    public Map<String, String> processParams(Map<String, String> params)
    {
        paramsToRemove.stream().forEach(p -> params.remove(p));
        return params;
    }

}
