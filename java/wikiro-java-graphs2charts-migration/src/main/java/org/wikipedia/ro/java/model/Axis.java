package org.wikipedia.ro.java.model;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

@Data
public class Axis
{
    private Map<String, String> title = new HashMap<>();
    private boolean format;
}
