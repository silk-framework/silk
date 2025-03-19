package org.silkframework.runtime.plugin.annotations;

import java.lang.annotation.*;

/**
 * Annotates a method that can be executed as an action.
 * The method can have one optional parameter of type PluginContext.
 * The return value of the method will be converted to a string and displayed in the UI.
 * The string may use Markdown formatting.
 * The method may return an Option, in which case None will result in no message being displayed.
 * It may raise an exception to signal an error to the user.
 * Note that actions are not sorted at the moment and can appear in the UI any order.
 */
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

    /**
     * The optional index of this action. Actions are sorted by this index.
     * Actions with a lower index are displayed first.
     * If not set, the order is undefined.
     */
    int index() default Integer.MAX_VALUE;
}
