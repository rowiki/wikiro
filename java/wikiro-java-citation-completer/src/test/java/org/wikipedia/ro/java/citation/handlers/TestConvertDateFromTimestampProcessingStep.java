package org.wikipedia.ro.java.citation.handlers;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class TestConvertDateFromTimestampProcessingStep
{
    @Test
    public void testConversion()
    {
        Map<String, String> m = new HashMap<>();
        m.put("date", "1644407564");
        
        Map<String, String> out = new ConvertDateFromTimestampProcessingStep().processParams(m);
        Assert.assertEquals("Wrong date conversion", "2022-02-09", out.get("date"));
    }
}
