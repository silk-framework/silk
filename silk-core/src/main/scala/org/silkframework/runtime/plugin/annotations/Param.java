package org.silkframework.runtime.plugin.annotations;

import org.silkframework.runtime.plugin.NopPluginParameterAutoCompletionProvider;
import org.silkframework.runtime.plugin.PluginParameterAutoCompletionProvider;

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

  /**
   * True, if it can be edited in the UI plugin dialogs.
   */
  boolean visibleInDialog() default true;

  /**
   * Auto-complete service provider, that returns valid values for a plugin parameter.
   */
  Class<? extends PluginParameterAutoCompletionProvider> autoCompletionProvider() default NopPluginParameterAutoCompletionProvider.class;

  /**
   * Hint to the UI that it should only allow to set values for the parameter coming from the auto-completion. The hint
   * is only forwarded to the UI if the a non-default auto-completion provider is set.
   *
   * If autoCompleteValueWithLabels is true allowOnlyAutoCompletedValues should also be true, at the moment.
   */
  boolean allowOnlyAutoCompletedValues() default false;

  /** Defines that the auto-completed values have labels that must be displayed to the user.
   *
   *  If autoCompleteValueWithLabels is true allowOnlyAutoCompletedValues should also be true, at the moment. */
  boolean autoCompleteValueWithLabels() default false;

  /** The plugin parameter values the auto-completion depends on. Without those values given no auto-completion is possible. */
  String[] autoCompletionDependsOnParameters() default {};
}
