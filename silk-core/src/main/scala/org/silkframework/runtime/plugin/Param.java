package org.silkframework.runtime.plugin;

import java.lang.annotation.*;

/**
 * Annotation for configuration parameters of [[Plugin]] classes.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Param {

  /**
   * A human-readable label for the annotated parameter.
   * If not provided the label will be generated from the parameter name.
   * Thus, overwriting the label is usually not necessary.
   */
  String label() default "";

  /**
   * The description for the annotated parameter.
   */
  String value();

  /**
   * Example value for the annotated parameter.
   */
  String example() default "";

  /**
   * True, if this is an advanced parameter that should not be shown by default.
   */
  boolean advanced() default false;
}
