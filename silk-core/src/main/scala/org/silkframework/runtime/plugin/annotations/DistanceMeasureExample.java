package org.silkframework.runtime.plugin.annotations;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(DistanceMeasureExamples.class)
@Documented
public @interface DistanceMeasureExample {

    // Optional description for this test case
    String description() default "";

    // Plugin parameters in pairs (key, value)
    String[] parameters() default {};

    // Source values
    String[] input1();

    // Target values
    String[] input2();

    // Expected distance
    double output();

}
