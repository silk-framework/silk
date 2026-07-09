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

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config.Prefixes
import org.silkframework.rule.TaskContext
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{PluginContext, TaskResolver}
import org.silkframework.runtime.resource.InMemoryResourceManager
import org.silkframework.runtime.templating.{ExecutionTemplateVariables, TemplateVariableScopes}
import org.silkframework.runtime.validation.ValidationException

class SetExecutionVariableTransformerTest extends AnyFlatSpec with Matchers {

  behavior of "SetExecutionVariableTransformer"

  it should "set the execution variable to the first value of the single input and pass the input through unchanged" in {
    val variables = ExecutionTemplateVariables(Seq.empty)
    val transformer = SetExecutionVariableTransformer("myVar").execution(taskContext(variables))

    val result = transformer(Seq(Seq("first", "second")))

    result shouldBe Seq("first", "second")
    variables.get("myVar").value shouldBe "first"
    variables.get("myVar").scope shouldBe TemplateVariableScopes.execution
  }

  it should "overwrite the variable on each invocation, so the last processed value wins" in {
    val variables = ExecutionTemplateVariables(Seq.empty)
    val transformer = SetExecutionVariableTransformer("myVar").execution(taskContext(variables))

    transformer(Seq(Seq("entity1")))
    transformer(Seq(Seq("entity2")))

    variables.get("myVar").value shouldBe "entity2"
    variables.all.variables should have size 1
  }

  it should "leave the variable untouched but still pass through when the input has no values" in {
    val variables = ExecutionTemplateVariables(Seq.empty)
    val transformer = SetExecutionVariableTransformer("myVar").execution(taskContext(variables))

    val result = transformer(Seq(Seq.empty))

    result shouldBe Seq.empty
    variables.all.variables shouldBe empty
  }

  it should "throw a ValidationException when not exactly one input is connected" in {
    val variables = ExecutionTemplateVariables(Seq.empty)
    val transformer = SetExecutionVariableTransformer("myVar").execution(taskContext(variables))

    an[ValidationException] should be thrownBy transformer(Seq(Seq("a"), Seq("b")))
    an[ValidationException] should be thrownBy transformer(Seq.empty)
  }

  private def taskContext(variables: ExecutionTemplateVariables): TaskContext = {
    val pluginContext = PluginContext(
      prefixes = Prefixes.empty,
      resources = InMemoryResourceManager(),
      user = UserContext.Empty,
      templateVariables = variables,
      taskResolver = TaskResolver.empty
    )
    TaskContext(Seq.empty, pluginContext)
  }
}
