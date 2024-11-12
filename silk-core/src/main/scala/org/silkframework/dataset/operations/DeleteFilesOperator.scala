package org.silkframework.dataset.operations

import org.silkframework.config.{CustomTask, FixedNumberOfInputs, FixedSchemaPort, InputPorts, Port}
import org.silkframework.entity.{EntitySchema, ValueType}
import org.silkframework.entity.paths.TypedPath
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}

@Plugin(
  id = "deleteProjectFiles",
  label = "Delete project files",
  description =
    """Removes file resources from the project based on a regular expression."""
)
case class DeleteFilesOperator(@Param(label = "File matching regex",
  value = "The regex for filtering the file names. The regex needs to match the full path (i.e. from beginning to end, including sub-directories) in order for the file to be deleted.")
                               filesRegex: String,
                              @Param(label = "Output deleted files", value = "If enabled the operator outputs entities, one entity for each deleted file, with the path of the file as attribute 'filePath'.")
                               outputEntities: Boolean = false) extends CustomTask {

  /**
    * The input ports and their schemata.
    */
  override def inputPorts: InputPorts = FixedNumberOfInputs(Seq.empty)

  /**
    * The output port and it's schema.
    * None, if this operator does not generate any output.
    */
  override def outputPort: Option[Port] = if(outputEntities) Some(FixedSchemaPort(DeleteFilesOperator.schema)) else None
}

object DeleteFilesOperator {
  final val schema = EntitySchema("DeletedFile", IndexedSeq(TypedPath("filePath", ValueType.STRING, isAttribute = true)))
}