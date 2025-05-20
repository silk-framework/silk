package org.silkframework.dataset.operations

import org.silkframework.config._
import org.silkframework.execution.typed.FileEntitySchema
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{AutoCompletionResult, ParamValue, PluginContext, PluginParameterAutoCompletionProvider}
import org.silkframework.runtime.plugin.annotations.{Action, Param, Plugin}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.workspace.WorkspaceReadTrait

@Plugin(
  id = "getProjectFiles",
  label = "Get project files",
  iconFile = "GetProjectFilesOperator.svg",
  description =
    """Get file resources from the project."""
)
case class GetProjectFilesOperator(
  @Param(value = "The path of the project file to retrieve. Leave empty if the file regex parameter should be used.",
    autoCompletionProvider = classOf[ProjectFilesAutoCompletionProvider])
  fileName: String = "",
  @Param("Optional regular expression for retrieving files. The regex needs to match the full path (i.e. from beginning to end, including sub-directories).")
  filesRegex: String = "") extends CustomTask {

  assert(fileName.nonEmpty || filesRegex.nonEmpty, "Either the file name or the file regex must be set")
  assert(!(fileName.nonEmpty && filesRegex.nonEmpty), "Only one of the file name or the file regex must be set")

  private val regex = filesRegex.trim.r

  /**
    * The input ports and their schemata.
    */
  override def inputPorts: InputPorts = FixedNumberOfInputs(Seq.empty)

  /**
    * The output port and it's schema.
    * None, if this operator does not generate any output.
    */
  override def outputPort: Option[Port] = Some(FixedSchemaPort(FileEntitySchema.schema))

  /**
   * Retrieves the files that should be retrieved.
   */
  def getFiles()(implicit pluginContext: PluginContext): Seq[String] = {
    val resourceManager = pluginContext.resources
    if(fileName.nonEmpty && !resourceManager.getInPath(fileName).exists) {
      throw new ValidationException(s"File $fileName does not exist.")
    }
    if(fileName.isEmpty) {
      resourceManager.listRecursive.filter(f => regex.matches(f))
    } else {
      // File name and regex are mutually exclusive, so we can just return the file name.
      Seq(fileName)
    }
  }

  @Action(
    label = "Preview",
    description = "Lists the files that would be retrieved.",
    iconFile = "DryRun.svg"
  )
  def dryRun(implicit pluginContext: PluginContext): GetProjectFilesPreview = {
    new GetProjectFilesPreview(getFiles())
  }
}

case class ProjectFilesAutoCompletionProvider() extends PluginParameterAutoCompletionProvider {
  override def autoComplete(searchQuery: String, dependOnParameterValues: Seq[ParamValue],
                            workspace: WorkspaceReadTrait)
                           (implicit context: PluginContext): Iterable[AutoCompletionResult] = {
    val multiSearchWords = extractSearchTerms(searchQuery)
    implicit val userContext: UserContext = context.user
    val projectId = context.projectId.getOrElse(throw new ValidationException("Project not provided"))
    workspace.project(projectId).resources.listRecursive
      .filter(r => matchesSearchTerm(multiSearchWords, r.toLowerCase))
      .map(r => AutoCompletionResult(r, None))
  }

  override def valueToLabel(value: String, dependOnParameterValues: Seq[ParamValue],
                            workspace: WorkspaceReadTrait)
                           (implicit context: PluginContext): Option[String] = {
    None
  }
}

class GetProjectFilesPreview(val files: Seq[String]) {

  override def toString: String = {
    if(files.isEmpty) {
      "No files would be retrieved because the regex does not match any project file."
    } else {
      val result = new StringBuilder()
      result ++= "The following files would be retrieved:\n"
      for(file <- files) {
        result ++= s"* $file\n"
      }
      result.toString()
    }

  }
}