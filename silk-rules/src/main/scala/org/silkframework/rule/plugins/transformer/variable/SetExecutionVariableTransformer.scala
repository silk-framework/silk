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

package org.silkframework.rule.plugins.transformer.variable

import org.silkframework.rule.TaskContext
import org.silkframework.rule.input.{Transformer, TransformerExecution}
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.runtime.templating.{ExecutionTemplateVariables, TemplateVariable, VariableScope}
import org.silkframework.runtime.validation.ValidationException

@Plugin(
  id = "setExecutionVariable",
  categories = Array("Variables"),
  label = "Set execution variable",
  description =
    "Sets an execution variable to the first value of the (single) input and passes the input values through unchanged. " +
    "The variable is written to the 'execution' scope and can be read downstream as 'execution.<name>'. " +
    "Only works while running inside a workflow execution."
)
case class SetExecutionVariableTransformer(@Param("Name of the execution variable to set. " +
                                                  "It is written to the 'execution' scope and addressed downstream as 'execution.<name>'.")
                                           variableName: String = "myVariable") extends Transformer {

  override def execution(taskContext: TaskContext): TransformerExecution = {
    new SetExecutionVariableExecution(variableName, taskContext.pluginContext.templateVariables)
  }
}

/**
  * Captures the execution-time template variables so the variable can be set while values flow through.
  */
private class SetExecutionVariableExecution(variableName: String,
                                            templateVariables: ExecutionTemplateVariables) extends TransformerExecution {

  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    if (values.size != 1) {
      throw new ValidationException(
        s"Set execution variable expects exactly one input, but got ${values.size}.")
    }
    val inputValues = values.head
    inputValues.headOption.foreach { firstValue =>
      templateVariables.setExecutionVariable(
        TemplateVariable(name = variableName, value = firstValue, scope = VariableScope.execution))
    }
    // Output the input values unchanged
    inputValues
  }
}
