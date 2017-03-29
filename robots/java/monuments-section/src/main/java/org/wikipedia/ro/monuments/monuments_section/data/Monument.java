package org.wikipedia.ro.monuments.monuments_section.data;

import java.util.ArrayList;
import java.util.List;

public class Monument {
    public String name;
    public String settlement;
    public String dating;
    public int type;
    public String code;
    public char structure;
    public String codeNumber;
    public String county;
    public String supplementalCodeNumber;
    public List<Monument> submonuments = new ArrayList<Monument>();
}
