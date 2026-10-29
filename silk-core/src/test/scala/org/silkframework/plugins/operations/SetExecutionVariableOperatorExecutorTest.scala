/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.silkframework.plugins.operations

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config.{PlainTask, Prefixes, Task}
import org.silkframework.entity.paths.TypedPath
import org.silkframework.entity.{Entity, EntitySchema, ValueType}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{PluginContext, TaskResolver}
import org.silkframework.runtime.resource.InMemoryResourceManager
import org.silkframework.runtime.templating.{ExecutionTemplateVariables, VariableScope}
import org.silkframework.runtime.validation.ValidationException

/**
  * Shared test cases for the executors of [[SetExecutionVariableOperator]].
  * Subclasses run them against a specific execution type (local/Spark).
  */
abstract class SetExecutionVariableOperatorExecutorTest extends AnyFlatSpec with Matchers {

  protected val schema = EntitySchema("urn:Person", IndexedSeq(TypedPath("name", ValueType.STRING), TypedPath("city", ValueType.STRING)))

  protected val entities: Seq[Entity] = Seq(
    Entity("urn:e1", IndexedSeq(Seq("Alice"), Seq("Berlin")), schema),
    Entity("urn:e2", IndexedSeq(Seq("Bob"), Seq("Hamburg")), schema)
  )

  /** Runs the executor under test on the given input entity tables and returns the (pass-through) output entities. */
  protected def run(op: SetExecutionVariableOperator, inputTables: Seq[Seq[Entity]], variables: ExecutionTemplateVariables): Seq[Entity]

  it should "set the variable from the given source path and pass the input through unchanged" in {
    val variables = ExecutionTemplateVariables(Seq.empty)
    val result = run(SetExecutionVariableOperator("greeting", sourcePath = "name"), Seq(entities), variables)

    variables.get("greeting").value shouldBe "Alice"
    variables.get("greeting").scope shouldBe VariableScope.execution
    result.map(_.uri.uri) shouldBe Seq("urn:e1", "urn:e2")
  }

  it should "use the first value of the input when no source path is given" in {
    val variables = ExecutionTemplateVariables(Seq.empty)
    run(SetExecutionVariableOperator("greeting"), Seq(entities), variables)

    variables.get("greeting").value shouldBe "Alice"
  }

  it should "leave the variable unset but still pass through when the input is empty" in {
    val variables = ExecutionTemplateVariables(Seq.empty)
    val result = run(SetExecutionVariableOperator("greeting"), Seq(Seq.empty), variables)

    variables.all.variables shouldBe empty
    result shouldBe empty
  }

  it should "throw a ValidationException when not exactly one input is connected" in {
    val variables = ExecutionTemplateVariables(Seq.empty)
    an[ValidationException] should be thrownBy
      run(SetExecutionVariableOperator("greeting"), Seq.empty, variables)
  }

  protected def task(op: SetExecutionVariableOperator): Task[SetExecutionVariableOperator] = PlainTask("setVar", op)

  protected def pluginContext(variables: ExecutionTemplateVariables): PluginContext =
    PluginContext(prefixes = Prefixes.empty, resources = InMemoryResourceManager(), user = UserContext.Empty,
      templateVariables = variables, taskResolver = TaskResolver.empty)
}
