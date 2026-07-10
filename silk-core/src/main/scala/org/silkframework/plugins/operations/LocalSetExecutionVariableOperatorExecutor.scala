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

import org.silkframework.config.{Prefixes, Task}
import org.silkframework.execution.local.{GenericEntityTable, LocalEntities, LocalExecution, LocalExecutor}
import org.silkframework.execution.{ExecutionReport, ExecutorOutput}
import org.silkframework.runtime.activity.{ActivityContext, ActivityMonitor}
import org.silkframework.runtime.iterator.CloseableIterator
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.templating.{TemplateVariable, VariableScope}
import org.silkframework.runtime.validation.ValidationException

import scala.util.control.NonFatal

/**
  * Executes [[SetExecutionVariableOperator]]: reads the first value of the (single) input, sets it as an
  * execution-scope template variable on the shared per-run holder (so downstream nodes can read it via
  * `{{execution.<name>}}`), and forwards the input entities unchanged.
  */
case class LocalSetExecutionVariableOperatorExecutor() extends LocalExecutor[SetExecutionVariableOperator] {

  override def execute(task: Task[SetExecutionVariableOperator],
                       inputs: Seq[LocalEntities],
                       output: ExecutorOutput,
                       execution: LocalExecution,
                       context: ActivityContext[ExecutionReport] = new ActivityMonitor(getClass.getSimpleName))
                      (implicit pluginContext: PluginContext): Option[LocalEntities] = {
    if (inputs.size != 1) {
      throw new ValidationException(s"Set execution variable operator expects exactly one input, but got ${inputs.size}.")
    }
    implicit val prefixes: Prefixes = pluginContext.prefixes
    val spec = task.data
    val input = inputs.head
    val source = input.entities

    // Pull the first entity eagerly so the variable is set before downstream nodes execute.
    // Peek with next() instead of headOption, since headOption would close the source, which is still needed for the pass-through.
    val passThrough =
      if (source.hasNext) {
        try {
          val first = source.next()
          val value =
            if (spec.sourcePath.trim.nonEmpty) first.singleValue(spec.sourcePath.trim)
            else first.values.flatten.headOption
          value.foreach { v =>
            pluginContext.templateVariables.setExecutionVariable(
              TemplateVariable(name = spec.variableName, value = v, scope = VariableScope.execution))
          }
          // Pass the input through unchanged, re-attaching the peeked first entity.
          CloseableIterator(Iterator(first) ++ source, source)
        } catch {
          case NonFatal(ex) =>
            source.close()
            throw ex
        }
      } else {
        source
      }
    Some(GenericEntityTable(passThrough, input.entitySchema, task))
  }
}
