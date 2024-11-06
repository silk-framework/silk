package org.silkframework.dataset.operations

import org.silkframework.config.{CustomTask, FixedNumberOfInputs, InputPorts, Port}
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}

@Plugin(
  id = "deleteProjectFiles",
  label = "Delete project files",
  description =
    """Removes file resources from the project."""
)
case class DeleteFilesOperator(@Param(label = "File matching regex" ,value = "The regex for filtering the file names")
                               filesRegex: String) extends CustomTask {
  assert(filesRegex.trim.nonEmpty, "File regex must not be empty!")
  /**
    * The input ports and their schemata.
    */
  override def inputPorts: InputPorts = FixedNumberOfInputs(Seq.empty)

  /**
    * The output port and it's schema.
    * None, if this operator does not generate any output.
    */
  override def outputPort: Option[Port] = None
}
