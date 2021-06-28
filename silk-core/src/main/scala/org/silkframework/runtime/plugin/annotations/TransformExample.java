package org.silkframework.runtime.plugin.annotations;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(TransformExamples.class)
@Documented
public @interface TransformExample {

    // Optional description for this test case
    String description() default "";

    String[] parameters() default {};

    String[] input1() default {"  __UNINITIALIZED__  "};

    String[] input2() default {"  __UNINITIALIZED__  "};

    String[] input3() default {"  __UNINITIALIZED__  "};

    String[] input4() default {"  __UNINITIALIZED__  "};

    String[] input5() default {"  __UNINITIALIZED__  "};

    String[] output() default {};

    // The full class path or empty string
    String throwsException() default "";

}

