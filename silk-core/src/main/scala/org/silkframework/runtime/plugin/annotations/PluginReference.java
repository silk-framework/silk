package org.silkframework.runtime.plugin.annotations;

import java.lang.annotation.*;

/**
 * Reference to another plugin.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PluginReference {

    /** The unique id of the referenced plugin */
    String id();

    /** An optional short description of the relationship to the referenced plugin. */
    String description() default "";
}
