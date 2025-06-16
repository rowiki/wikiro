package org.wikipedia.ro.java.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class ChartDataSet
{
    private String license;
    private Map<String, String> description = new HashMap<>();
    private ChartSchema schema = new ChartSchema();
    private List<List<Object>> data = new ArrayList<>();
}
