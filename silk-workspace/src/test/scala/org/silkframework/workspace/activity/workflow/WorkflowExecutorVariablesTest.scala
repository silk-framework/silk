package org.silkframework.workspace.activity.workflow

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.runtime.templating.{TemplateVariable, VariableScope, TemplateVariables}

/**
  * Tests [[WorkflowExecutor.buildExecutionVariables]]: the execution variables of the executed task
  * provide the defaults, overridden by name by the variables provided for the run.
  */
class WorkflowExecutorVariablesTest extends AnyFlatSpec with Matchers {

  behavior of "WorkflowExecutor.buildExecutionVariables"

  private def variable(name: String, value: String, scope: VariableScope = VariableScope.execution): TemplateVariable =
    TemplateVariable(name = name, value = value, scope = scope)

  private def valuesByName(variables: TemplateVariables): Map[String, String] =
    variables.variables.map(v => v.name -> v.value).toMap

  it should "use the defaults when no overrides are provided" in {
    val result = WorkflowExecutor.buildExecutionVariables(
      defaults = TemplateVariables(Seq(variable("foo", "defaultVal"))),
      overrides = TemplateVariables.empty
    )
    valuesByName(result) shouldBe Map("foo" -> "defaultVal")
  }

  it should "override defaults by name and add new names" in {
    val result = WorkflowExecutor.buildExecutionVariables(
      defaults = TemplateVariables(Seq(variable("foo", "defaultVal"), variable("bar", "barVal"))),
      overrides = TemplateVariables(Seq(variable("foo", "overrideVal"), variable("extra", "extraVal")))
    )
    valuesByName(result) shouldBe Map("foo" -> "overrideVal", "bar" -> "barVal", "extra" -> "extraVal")
  }

  it should "re-scope all variables to the execution scope" in {
    val result = WorkflowExecutor.buildExecutionVariables(
      defaults = TemplateVariables(Seq(variable("foo", "defaultVal", VariableScope.empty))),
      overrides = TemplateVariables(Seq(variable("bar", "barVal", VariableScope("other"))))
    )
    all(result.variables.map(_.scope)) shouldBe VariableScope.execution
  }
}
