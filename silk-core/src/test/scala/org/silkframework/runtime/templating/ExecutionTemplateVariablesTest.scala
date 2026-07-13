package org.silkframework.runtime.templating

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/**
  * Tests [[ExecutionTemplateVariables]]: the execution scope is independent of the other scopes and
  * holds exactly the variables that have been seeded or set for the current execution.
  */
class ExecutionTemplateVariablesTest extends AnyFlatSpec with Matchers {

  behavior of "ExecutionTemplateVariables"

  private def variable(name: String, value: String, scope: VariableScope, isSensitive: Boolean = false): TemplateVariable =
    TemplateVariable(name = name, value = value, scope = scope, isSensitive = isSensitive)

  /** A reader exposing the given variables under the given scopes. */
  private def reader(variables: TemplateVariable*): TemplateVariablesReader =
    InMemoryTemplateVariablesReader(TemplateVariables(variables), variables.map(_.scope).toSet)

  /** Wraps immutable variables and an initial execution scope into an [[ExecutionTemplateVariables]]. */
  private def executionVariables(immutable: Seq[TemplateVariable],
                                 execution: Seq[TemplateVariable] = Seq.empty): ExecutionTemplateVariables = {
    ExecutionTemplateVariables(Seq(reader(immutable: _*)), new ExecutionVariablesHolder(TemplateVariables(execution)))
  }

  /** Returns the execution-scope variable with the given name, if any. */
  private def executionVar(etv: ExecutionTemplateVariables, name: String): Option[TemplateVariable] =
    etv.all.variables.find(v => v.scope == VariableScope.execution && v.name == name)

  it should "expose seeded execution variables together with the immutable scopes" in {
    val etv = executionVariables(
      immutable = Seq(variable("foo", "projectVal", VariableScope.project)),
      execution = Seq(variable("bar", "execVal", VariableScope.execution))
    )
    executionVar(etv, "bar").map(_.value) shouldBe Some("execVal")
    etv.all.variables.find(v => v.scope == VariableScope.project && v.name == "foo").map(_.value) shouldBe Some("projectVal")
  }

  it should "not resolve execution variables from other scopes" in {
    val etv = executionVariables(immutable = Seq(
      variable("foo", "projectVal", VariableScope.project),
      variable("foo", "globalVal", VariableScope.global)
    ))
    executionVar(etv, "foo") shouldBe None
  }

  it should "make execution variables set during the execution visible" in {
    val etv = executionVariables(immutable = Seq.empty)
    etv.setExecutionVariable(variable("foo", "execVal", VariableScope.execution))
    executionVar(etv, "foo").map(_.value) shouldBe Some("execVal")
  }

  it should "replace a seeded execution variable when it is set during the execution" in {
    val etv = executionVariables(
      immutable = Seq.empty,
      execution = Seq(variable("foo", "defaultVal", VariableScope.execution))
    )
    etv.setExecutionVariable(variable("foo", "updatedVal", VariableScope.execution))
    val executionFoo = etv.all.variables.filter(v => v.scope == VariableScope.execution && v.name == "foo")
    executionFoo.map(_.value) shouldBe Seq("updatedVal")
    executionFoo should have size 1
  }

  it should "share the execution holder between contexts of the same run" in {
    val holder = new ExecutionVariablesHolder()
    val context1 = ExecutionTemplateVariables(Seq(reader()), holder)
    val context2 = ExecutionTemplateVariables(Seq(reader()), holder)
    context1.setExecutionVariable(variable("foo", "execVal", VariableScope.execution))
    executionVar(context2, "foo").map(_.value) shouldBe Some("execVal")
  }

  it should "filter sensitive execution variables" in {
    val etv = executionVariables(
      immutable = Seq.empty,
      execution = Seq(variable("secret", "shh", VariableScope.execution, isSensitive = true))
    )
    executionVar(etv, "secret").map(_.isSensitive) shouldBe Some(true)
    etv.all.withoutSensitiveVariables().variables.map(_.scopedName) should not contain "execution.secret"
  }
}
