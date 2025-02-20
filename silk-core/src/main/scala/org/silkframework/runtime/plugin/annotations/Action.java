package org.silkframework.runtime.plugin.annotations;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Action {

    /** A human-readable label for this action */
    String label() default "";

    /** A short (few sentence) description of this action. */
    String description() default "";

    /**
     * Optional icon. If not set the plugin will have a generic icon.
     * This icon is rendered in the UI.
     */
    String iconFile() default "";
}
