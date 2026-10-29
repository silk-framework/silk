package org.silkframework.workspace.activity.workflow

import org.silkframework.runtime.plugin.{ParameterObjectValue, ParameterValues}
import org.silkframework.runtime.templating.{TemplateVariables, TemplateVariablesParameter}

/**
  * Common trait of the factories that create workflow-executor activities.
  * Each factory declares an `executionVariables` plugin parameter (the constructor parameter implementing
  * [[executionVariables]]); build the start configuration that sets it with
  * [[WorkflowExecutorFactory.executionVariablesConfig]].
  */
trait WorkflowExecutorFactory {

  /** The execution-variable overrides applied to runs started from this factory instance. */
  def executionVariables: TemplateVariablesParameter
}

object WorkflowExecutorFactory {

  /** Name of the execution-variables plugin parameter declared by every [[WorkflowExecutorFactory]]. */
  final val EXECUTION_VARIABLES_PARAMETER = "executionVariables"

  /**
    * Start configuration that sets the execution-variable overrides for a run. Pass it on every start
    * (possibly empty), so that overrides from a previous start are reset. The start re-instantiates the
    * activity's factory from it; a factory without the parameter fails the start.
    */
  def executionVariablesConfig(overrides: TemplateVariables): ParameterValues = {
    ParameterValues(Map(EXECUTION_VARIABLES_PARAMETER -> ParameterObjectValue(Left(TemplateVariablesParameter(overrides)))))
  }
}
