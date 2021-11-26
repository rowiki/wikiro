package org.wikipedia.ro.java.oldcountries.data;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class WbObject
{
    private String wdId;
    private String name;

}
