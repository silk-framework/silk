package org.silkframework.runtime.plugin;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TransformExamples {

    TransformExample[] value();

}
