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

import org.silkframework.config.{CustomTask, FixedNumberOfInputs, FixedSchemaPort, FlexibleSchemaPort, InputPorts, Port, UnknownSchemaPort}
import org.silkframework.entity.EntitySchema
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.util.Uri

/**
  * Workflow operator that sets a single execution-scope template variable from its input and passes the input
  * through unchanged. It is the workflow-node counterpart of the `setExecutionVariable` transformer.
  */
@Plugin(
  id = "setExecutionVariableOperator",
  categories = Array("Variables"),
  label = "Set execution variable",
  description =
    "Sets an execution variable to the first value of the (single) input and passes the input through unchanged. " +
    "The variable is written to the 'execution' scope and can be read downstream as 'execution.<name>'. " +
    "Only works while running inside a workflow execution.",
  documentationFile = "SetExecutionVariableOperator.md"
)
case class SetExecutionVariableOperator(@Param("Name of the execution variable to set. " +
                                               "It is written to the 'execution' scope and addressed downstream as 'execution.<name>'.")
                                        variableName: String = "myVariable",
                                        @Param(label = "Source path",
                                          value = "Optional path/attribute of the input that supplies the value. " +
                                            "If left empty, the first value of the input is used.")
                                        sourcePath: String = "") extends CustomTask {

  /** Parsed at construction time, so an invalid path fails when the task is configured. */
  private val parsedSourcePath: Option[UntypedPath] = {
    val trimmed = sourcePath.trim
    if (trimmed.nonEmpty) Some(UntypedPath.parse(trimmed)) else None
  }

  /**
    * With a source path, a fixed schema requesting exactly that path, so datasets can be connected as input.
    * Otherwise a flexible port that adapts to the connected output.
    */
  override lazy val inputPorts: InputPorts = {
    val port = parsedSourcePath match {
      case Some(path) =>
        FixedSchemaPort(EntitySchema(typeUri = Uri(""), typedPaths = IndexedSeq(path.asStringTypedPath)))
      case None =>
        FlexibleSchemaPort()
    }
    FixedNumberOfInputs(Seq(port))
  }

  /** Passes the input through unchanged; the schema depends on the connected input. */
  override def outputPort: Option[Port] = Some(UnknownSchemaPort)
}
