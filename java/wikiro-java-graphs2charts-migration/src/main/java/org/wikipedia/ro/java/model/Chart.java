package org.wikipedia.ro.java.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class Chart
{
    private int version;
    private String license;
    private String source;
    private String type;
    private Axis xAxis;
    private Axis yAxis;
    private Map<String, String> title = new HashMap<>();
    private List<String> mediawikiCategories = new ArrayList<>();
}
