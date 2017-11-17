package org.silkframework.runtime.plugin;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(TransformExamples.class)
@Documented
public @interface TransformExample {

    String[] parameters() default {};

    String[] input1() default {};

    String[] input2() default {};

    String[] input3() default {};

    String[] input4() default {};

    String[] input5() default {};

    String[] output() default {};

    // The full class path or empty string
    String throwsException() default "";

}

