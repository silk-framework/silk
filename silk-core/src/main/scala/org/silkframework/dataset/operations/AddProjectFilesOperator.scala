package org.silkframework.dataset.operations

import org.silkframework.config._
import org.silkframework.execution.typed.FileEntitySchema
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.runtime.resource.ResourceManager

@Plugin(
  id = "addProjectFiles",
  label = "Add project files",
  iconFile = "AddProjectFilesOperator.svg",
  description =
    """Adds file resources to the project that are piped into the input port."""
)
case class AddProjectFilesOperator(
  @Param("File name of the uploaded file(s). If multiple files are uploaded, an index will be appended to the file name. If left empty, the existing file names will be used.")
  fileName: String = "",
  @Param("Directory to which the files should be written. If left empty, the files will be uploaded to the project root directory. Note that all files will be written to this directory even if they have been read from a different project directory initially.")
  directory: String = "",
  @Param("The strategy to use if a file with the same name already exists.")
  overwriteStrategy: OverwriteStrategyEnum = OverwriteStrategyEnum.fail) extends CustomTask {

  /**
   * The clean target directory name without superfluous slashes, but with a trailing slash.
   */
  val targetDirectory: String = {
    val dir = directory.split('/').filter(_.nonEmpty).mkString("/")
    if(dir.isEmpty) "" else dir + '/'
  }

  /**
    * The input ports and their schemata.
    */
  override def inputPorts: InputPorts = FlexibleNumberOfInputs(FixedSchemaPort(FileEntitySchema.schema))

  /**
    * The output port and it's schema.
    * None, if this operator does not generate any output.
    */
  override def outputPort: Option[Port] = None

  /**
   * Returns the directory to which the files should be written.
   */
  def getDirectory(resources: ResourceManager): ResourceManager = {
    val folders = directory.split('/')
    var currentFolder = resources
    for(folder <- folders if folder.nonEmpty) {
      currentFolder = currentFolder.child(folder)
    }
    currentFolder
  }
}
