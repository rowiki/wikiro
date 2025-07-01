package org.wikipedia.ro.java.oldcountries.data;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UAT extends InceptedWbObject
{
    private UAT parent;
    private UATType type;
    private List<Settlement> settlements = new ArrayList<>();
    
    public UAT setName(String name)
    {
        super.setName(name);
        return this;
    }
    
    public UAT setWdId(String wdId)
    {
        super.setWdId(wdId);
        return this;
    }
}
