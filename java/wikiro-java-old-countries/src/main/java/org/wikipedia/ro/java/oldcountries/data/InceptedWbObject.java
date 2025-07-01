package org.wikipedia.ro.java.oldcountries.data;

import java.time.LocalDate;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class InceptedWbObject extends WbObject
{
    private LocalDate inception;

}
