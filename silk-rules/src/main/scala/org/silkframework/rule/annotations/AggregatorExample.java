package org.silkframework.rule.annotations;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(AggregatorExamples.class)
@Documented
public @interface AggregatorExample {

    // Optional description for this test case
    String description() default "";

    // Plugin parameters in pairs (key, value)
    String[] parameters() default {};

    // Input similarity scores. Double. NaN to represent an empty score (None).
    double[] inputs();

    // Input weights. Leave empty to set all weights to 1.
    int[] weights() default {};

    // Expected output score. NaN to represent an empty score (None).
    double output();

    // Thrown exception if evaluation should fail. Defaults to Object class if no exception is thrown.
    Class<?> throwsException() default Object.class;

}
