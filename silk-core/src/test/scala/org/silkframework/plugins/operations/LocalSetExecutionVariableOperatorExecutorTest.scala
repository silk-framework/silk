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

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.entity.Entity
import org.silkframework.execution.ExecutorOutput
import org.silkframework.execution.local.{GenericEntityTable, LocalEntities, LocalExecution}
import org.silkframework.runtime.iterator.CloseableIterator
import org.silkframework.runtime.templating.ExecutionTemplateVariables

class LocalSetExecutionVariableOperatorExecutorTest extends SetExecutionVariableOperatorExecutorTest {

  private val executor = LocalSetExecutionVariableOperatorExecutor()

  override protected def run(op: SetExecutionVariableOperator,
                             inputTables: Seq[Seq[Entity]],
                             variables: ExecutionTemplateVariables): Seq[Entity] = {
    val opTask = task(op)
    val inputs: Seq[LocalEntities] = inputTables.map(GenericEntityTable(_, schema, opTask.asInstanceOf[Task[TaskSpec]]))
    val output = executor.execute(opTask, inputs, ExecutorOutput.empty,
      LocalExecution(useLocalInternalDatasets = false))(pluginContext(variables))
    output.map(_.entities.use(_.toList)).getOrElse(Seq.empty)
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
}
