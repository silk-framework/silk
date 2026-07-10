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
import org.silkframework.config.{PlainTask, Prefixes, Task, TaskSpec}
import org.silkframework.entity.paths.TypedPath
import org.silkframework.entity.{Entity, EntitySchema, ValueType}
import org.silkframework.execution.ExecutorOutput
import org.silkframework.execution.local.{GenericEntityTable, LocalEntities, LocalExecution}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.iterator.CloseableIterator
import org.silkframework.runtime.plugin.{PluginContext, TaskResolver}
import org.silkframework.runtime.resource.InMemoryResourceManager
import org.silkframework.runtime.templating.{ExecutionTemplateVariables, VariableScope}
import org.silkframework.runtime.validation.ValidationException

class LocalSetExecutionVariableOperatorExecutorTest extends AnyFlatSpec with Matchers {

  behavior of "LocalSetExecutionVariableOperatorExecutor"

  private val schema = EntitySchema("urn:Person", IndexedSeq(TypedPath("name", ValueType.STRING), TypedPath("city", ValueType.STRING)))

  private val executor = LocalSetExecutionVariableOperatorExecutor()

  it should "set the variable from the given source path and pass the input through unchanged" in {
    val variables = ExecutionTemplateVariables(Seq.empty)
    val result = run(SetExecutionVariableOperator("greeting", sourcePath = "name"), entities, variables)

    variables.get("greeting").value shouldBe "Alice"
    variables.get("greeting").scope shouldBe VariableScope.execution
    result.map(_.uri.uri) shouldBe Seq("urn:e1", "urn:e2")
  }

  it should "use the first value of the input when no source path is given" in {
    val variables = ExecutionTemplateVariables(Seq.empty)
    run(SetExecutionVariableOperator("greeting"), entities, variables)

    variables.get("greeting").value shouldBe "Alice"
  }

  it should "leave the variable unset but still pass through when the input is empty" in {
    val variables = ExecutionTemplateVariables(Seq.empty)
    val result = run(SetExecutionVariableOperator("greeting"), Seq.empty, variables)

    variables.all.variables shouldBe empty
    result shouldBe empty
  }

  it should "pass through all entities of a streaming input" in {
    // Simulates a streaming source (e.g. a CSV or SQL backed table): using the iterator after close() fails.
    var closed = false
    val inner = entities.iterator
    val streamingSource = new CloseableIterator[Entity] {
      override def hasNext: Boolean = { failIfClosed(); inner.hasNext }
      override def next(): Entity = { failIfClosed(); inner.next() }
      override def close(): Unit = { closed = true }
      private def failIfClosed(): Unit = {
        if (closed) throw new IllegalStateException("The iterator must not be used after it has been closed.")
      }
    }
    val variables = ExecutionTemplateVariables(Seq.empty)
    val opTask = task(SetExecutionVariableOperator("greeting", sourcePath = "name"))
    val inputTable: LocalEntities = GenericEntityTable(streamingSource, schema, opTask.asInstanceOf[Task[TaskSpec]])
    val output = executor.execute(opTask, Seq(inputTable), ExecutorOutput.empty,
      LocalExecution(useLocalInternalDatasets = false))(pluginContext(variables))
    val result = output.map(_.entities.use(_.toList)).getOrElse(Seq.empty)

    variables.get("greeting").value shouldBe "Alice"
    result.map(_.uri.uri) shouldBe Seq("urn:e1", "urn:e2")
    closed shouldBe true // consuming the pass-through closes the original source
  }

  it should "throw a ValidationException when not exactly one input is connected" in {
    val variables = ExecutionTemplateVariables(Seq.empty)
    an[ValidationException] should be thrownBy
      executor.execute(task(SetExecutionVariableOperator("greeting")), Seq.empty, ExecutorOutput.empty,
        LocalExecution(useLocalInternalDatasets = false))(pluginContext(variables))
  }

  private val entities: Seq[Entity] = Seq(
    Entity("urn:e1", IndexedSeq(Seq("Alice"), Seq("Berlin")), schema),
    Entity("urn:e2", IndexedSeq(Seq("Bob"), Seq("Hamburg")), schema)
  )

  private def task(op: SetExecutionVariableOperator): Task[SetExecutionVariableOperator] = PlainTask("setVar", op)

  private def pluginContext(variables: ExecutionTemplateVariables): PluginContext =
    PluginContext(prefixes = Prefixes.empty, resources = InMemoryResourceManager(), user = UserContext.Empty,
      templateVariables = variables, taskResolver = TaskResolver.empty)

  /** Runs the executor on the given input entities and returns the (pass-through) output entities. */
  private def run(op: SetExecutionVariableOperator, inputEntities: Seq[Entity], variables: ExecutionTemplateVariables): Seq[Entity] = {
    val opTask = task(op)
    val inputTable: LocalEntities = GenericEntityTable(inputEntities, schema, opTask.asInstanceOf[Task[TaskSpec]])
    val output = executor.execute(opTask, Seq(inputTable), ExecutorOutput.empty,
      LocalExecution(useLocalInternalDatasets = false))(pluginContext(variables))
    output.map(_.entities.use(_.toList)).getOrElse(Seq.empty)
  }
}
