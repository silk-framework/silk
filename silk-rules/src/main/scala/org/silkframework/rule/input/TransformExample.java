package org.silkframework.rule.input;

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

    String[] output();

}

