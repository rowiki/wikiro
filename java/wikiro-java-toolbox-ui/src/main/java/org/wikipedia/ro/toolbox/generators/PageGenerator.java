package org.wikipedia.ro.toolbox.generators;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PageGenerator {
    String labelKey();

    int stringsConfigNumber() default 1;

    String[] stringsConfigLabelKeys();
}
