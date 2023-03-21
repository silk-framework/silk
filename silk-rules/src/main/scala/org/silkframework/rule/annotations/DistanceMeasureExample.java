package org.silkframework.rule.annotations;

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
    double output() default Double.NaN;

    // Thrown exception if evaluation should fail. Defaults to Object class if no exception is thrown.
    Class<?> throwsException() default Object.class;

}
