package org.silkframework.runtime.plugin.annotations;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TransformExamples {

    TransformExample[] value();

}
