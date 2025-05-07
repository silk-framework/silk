package org.silkframework.dataset.operations

import org.silkframework.config._
import org.silkframework.execution.typed.FileEntitySchema
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}

@Plugin(
  id = "addProjectFiles",
  label = "Add project files",
  description =
    """Add file resources to the project."""
)
case class AddProjectFilesOperator(
  @Param("File name of the uploaded file(s). If multiple files are uploaded, an index will be appended to the file name. If left empty, the existing file names will be used.")
  fileName: String = "",
  @Param("Overwrite existing files")
  overwriteStrategy: OverwriteStrategyEnum = OverwriteStrategyEnum.fail) extends CustomTask {

  /**
    * The input ports and their schemata.
    */
  override def inputPorts: InputPorts = FixedNumberOfInputs(Seq(FixedSchemaPort(FileEntitySchema.schema)))

  /**
    * The output port and it's schema.
    * None, if this operator does not generate any output.
    */
  override def outputPort: Option[Port] = None
}
