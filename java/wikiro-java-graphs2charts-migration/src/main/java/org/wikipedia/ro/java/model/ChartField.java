package org.wikipedia.ro.java.model;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

@Data
public class ChartField
{
    private String name;
    private String type;
    private Map<String, String> title = new HashMap<>();
}
