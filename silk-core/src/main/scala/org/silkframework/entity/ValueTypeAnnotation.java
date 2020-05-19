package org.silkframework.entity;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ValueTypeAnnotation {

    String[] validValues();

    String[] invalidValues();

}
