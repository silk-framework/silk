package org.silkframework.runtime.plugin;

import java.lang.annotation.*;

/**
 * Created by andreas on 1/14/16.
 *
 * Annotation for configuration parameters of [[Plugin]] classes.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PluginParam {
  String description() default "No description";

  String exampleValue() default "";
}
