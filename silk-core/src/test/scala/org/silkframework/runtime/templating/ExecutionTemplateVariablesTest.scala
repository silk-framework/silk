package org.silkframework.runtime.templating

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/**
  * Tests the execution-scope fallback of [[ExecutionTemplateVariables]]: when a variable is addressed
  * as `execution.X` but has not been set directly in the execution scope, it falls back to the task,
  * project, then global variable of the same name (task highest precedence).
  */
class ExecutionTemplateVariablesTest extends AnyFlatSpec with Matchers {

  behavior of "ExecutionTemplateVariables"

  private def variable(name: String, value: String, scope: Seq[String], isSensitive: Boolean = false): TemplateVariable =
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
    etv.all.variables.find(v => v.scope == TemplateVariableScopes.execution && v.name == name)

  it should "let a directly set execution variable win over the fallback" in {
    val etv = executionVariables(
      immutable = Seq(variable("foo", "taskVal", TemplateVariableScopes.task)),
      execution = Seq(variable("foo", "execVal", TemplateVariableScopes.execution))
    )
    val executionFoo = etv.all.variables.filter(v => v.scope == TemplateVariableScopes.execution && v.name == "foo")
    executionFoo.map(_.value) shouldBe Seq("execVal")
    executionFoo should have size 1
  }

  it should "fall back to the task variable, preferring it over project and global" in {
    val etv = executionVariables(immutable = Seq(
      variable("foo", "taskVal", TemplateVariableScopes.task),
      variable("foo", "projectVal", TemplateVariableScopes.project),
      variable("foo", "globalVal", TemplateVariableScopes.global)
    ))
    executionVar(etv, "foo").map(_.value) shouldBe Some("taskVal")
  }

  it should "fall back to the project variable, preferring it over global" in {
    val etv = executionVariables(immutable = Seq(
      variable("foo", "projectVal", TemplateVariableScopes.project),
      variable("foo", "globalVal", TemplateVariableScopes.global)
    ))
    executionVar(etv, "foo").map(_.value) shouldBe Some("projectVal")
  }

  it should "fall back to the global variable when it is the only definition" in {
    val etv = executionVariables(immutable = Seq(variable("foo", "globalVal", TemplateVariableScopes.global)))
    executionVar(etv, "foo").map(_.value) shouldBe Some("globalVal")
  }

  it should "not create an execution variable when the name is defined in no scope" in {
    val etv = executionVariables(immutable = Seq(variable("bar", "taskVal", TemplateVariableScopes.task)))
    executionVar(etv, "foo") shouldBe None
  }

  it should "preserve sensitivity of fallback variables so they are still filtered out" in {
    val etv = executionVariables(immutable = Seq(variable("secret", "shh", TemplateVariableScopes.task, isSensitive = true)))
    executionVar(etv, "secret").map(_.isSensitive) shouldBe Some(true)

    val nonSensitiveNames = etv.all.withoutSensitiveVariables().variables.map(_.scopedName).toSet
    nonSensitiveNames should not contain "task.secret"
    nonSensitiveNames should not contain "execution.secret"
  }

  it should "not fall back for variables whose scope is not exactly task, project or global" in {
    val etv = executionVariables(immutable = Seq(variable("foo", "nestedVal", Seq("project", "metaData"))))
    executionVar(etv, "foo") shouldBe None
  }

  it should "produce exactly one execution entry per name without duplicate scoped names" in {
    val etv = executionVariables(immutable = Seq(
      variable("foo", "projectVal", TemplateVariableScopes.project),
      variable("foo", "globalVal", TemplateVariableScopes.global),
      variable("bar", "taskVal", TemplateVariableScopes.task)
    ))
    // Constructing all must not throw a duplicate-name validation error.
    val executionVars = etv.all.variables.filter(_.scope == TemplateVariableScopes.execution)
    executionVars.map(_.name).sorted shouldBe Seq("bar", "foo")
  }
}
