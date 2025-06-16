package org.wikipedia.ro.java.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class ChartSchema
{
    private List<ChartField> fields = new ArrayList<>();
}
