package org.silkframework.rule.annotations;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistanceMeasureExamples {

    DistanceMeasureExample[] value();

}
