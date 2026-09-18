package controllers.workspace.workspaceRequests

import controllers.workspace.workspaceRequests.CopyTasksRequest.CopyTaskExecutor
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode
import org.silkframework.config.{Prefixes, TaskSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{InvalidPluginParameterValueException, PluginContext}
import org.silkframework.runtime.templating.exceptions.{TemplateVariableEvaluationException, TemplateVariablesEvaluationException, UnboundVariablesException}
import org.silkframework.runtime.templating.{GlobalTemplateVariables, TemplateVariable, TemplateVariableName, TemplateVariables, VariableScope}
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.util.Identifier
import org.silkframework.workspace.{Project, ProjectTask, WorkspaceFactory}
import play.api.libs.json.{Json, OFormat}

import scala.collection.mutable

/**
  * Request to copy a project or a task to another project.
  */
case class CopyTasksRequest(@Schema(
                              description = "If true, the copy operation will be simulated, i.e., the response listing the tasks to be copied and overwritten can be checked first.",
                              requiredMode = RequiredMode.NOT_REQUIRED,
                              nullable = true
                            )
                            dryRun: Option[Boolean],
                            @Schema(
                              description = "If true, tasks in the target project will be overwritten.",
                              requiredMode = RequiredMode.NOT_REQUIRED,
                              nullable = true
                            )
                            overwriteTasks: Option[Boolean],
                            @Schema(
                              description = "If true, all prefixes from the source project are copied to the target project.",
                              requiredMode = RequiredMode.NOT_REQUIRED,
                              nullable = true
                            )
                            copyPrefixes: Option[Boolean] = None,
                            @Schema(
                              description = "The identifier of the target project."
                            )
                            targetProject: String) {

  private def isDryRun: Boolean = dryRun.contains(true)
  private def overwriteConfirmed: Boolean = overwriteTasks.contains(true)

  /**
    * Copies all tasks in a project to the target project.
    */
  def copyProject(sourceProject: String)
                 (implicit userContext: UserContext): CopyTasksResponse = {
    val tasksToCopy = WorkspaceFactory().workspace.project(sourceProject).allTasks
    new CopyTaskExecutor(sourceProject, targetProject, isDryRun, overwriteConfirmed, copyPrefixes.contains(true)).copyTasks(tasksToCopy)
  }

  def copyTask(sourceProject: String,
               taskName: String)
              (implicit userContext: UserContext): CopyTasksResponse = {
    new CopyTaskExecutor(sourceProject, targetProject, isDryRun, overwriteConfirmed, copyPrefixes.contains(true)).copyTaskWithDependencies(taskName)
  }
}

object CopyTasksRequest {

  implicit val jsonFormat: OFormat[CopyTasksRequest] = Json.format[CopyTasksRequest]

  /**
   * Copies a task and all its referenced tasks to the target project.
   *
   * @param sourceProjectName  The source project ID.
   * @param targetProjectName  The target project ID.
   * @param isDryRun           Only return the response as if the task was copied, but do not actually do any changes.
   * @param overwriteConfirmed If true, then existing tasks will be overwritten.
   * @param copyPrefixes       If true, all prefixes from the source project are copied to the target project.
   * @param taskRenameMap      Specifies how some tasks should be named in the target project.
   */
  class CopyTaskExecutor(sourceProjectName: String,
                         targetProjectName: String,
                         isDryRun: Boolean,
                         overwriteConfirmed: Boolean,
                         copyPrefixes: Boolean = false,
                         taskRenameMap: Map[Identifier, Identifier] = Map.empty)
                        (implicit userContext: UserContext) {

    private val sourceProject = WorkspaceFactory().workspace.project(sourceProjectName)
    private val targetProject = WorkspaceFactory().workspace.project(targetProjectName)

    // Only copy resources if they are at different base paths
    private val copyResources = sourceProject.resources.basePath != targetProject.resources.basePath

    /**
     * Copies a task and all its referenced tasks.
     *
     * @param taskName The task that should be copied from the source project to the target project.
     */
    def copyTaskWithDependencies(taskName: String): CopyTasksResponse = {
      val tasksToCopy = collectTasks(sourceProject, taskName)
      copyTasks(tasksToCopy)
    }

    /**
     * Copies all provided tasks to the target project.
     */
    def copyTasks(tasksToCopy: Seq[ProjectTask[_ <: TaskSpec]]): CopyTasksResponse = {
      sourceProject.synchronized {
        targetProject.synchronized {
          // Copy prefixes
          if(copyPrefixes) {
            copyAllPrefixes()
          }

          // Copy only those tags that do not exist in the target project
          copyTags(tasksToCopy)

          // Tasks to be overwritten
          val overwrittenTasks =
            for {task <- tasksToCopy
                 overwrittenTask <- targetProject.anyTaskOption(taskRenameMap.getOrElse(task.id, task.id))} yield TaskToBeCopied.fromTask(task, Some(overwrittenTask))

          // Copy tasks
          if (!isDryRun) {
            if (overwrittenTasks.nonEmpty && !overwriteConfirmed) {
              throw BadUserInputException("Please confirm that you intend to overwrite tasks in the target project.")
            }
            val copiedVariables = mutable.Set[TemplateVariableName]()
            for (task <- tasksToCopy) {
              copyTask(task, copiedVariables)
            }
          }

          // Generate response
          val overwrittenTaskIds = overwrittenTasks.map(_.id).toSet
          val copiedTasks = for (task <- tasksToCopy if !overwrittenTaskIds.contains(task.id)) yield TaskToBeCopied.fromTask(task, None)
          CopyTasksResponse(copiedTasks.toSet, overwrittenTasks.toSet)
        }
      }
    }

    private def copyAllPrefixes(): Unit = {
      // Make sure that a prefix is not already defined with a different namespace
      val sourcePrefixes = sourceProject.config.projectPrefixes.prefixMap
      val targetPrefixes = targetProject.config.projectPrefixes.prefixMap
      val inconsistentPrefixes = for(key <- sourcePrefixes.keySet intersect targetPrefixes.keySet if sourcePrefixes(key) != targetPrefixes(key)) yield key
      if(inconsistentPrefixes.nonEmpty) {
        throw new InconsistentPrefixesException(inconsistentPrefixes)
      }

      // Update prefixes of target project
      targetProject.config = targetProject.config.copy(projectPrefixes = targetProject.config.projectPrefixes ++ sourcePrefixes)
    }

    /**
     * Copies only those tags that do not exist in the target project
     */
    private def copyTags(tasksToCopy: Seq[ProjectTask[_ <: TaskSpec]]): Unit = {
      val targetProjectTags = targetProject.tagManager.allTags().map(_.uri).toSet
      val tagsToCopy = tasksToCopy.flatMap(_.metaData.tags)
        .filter(tag => !targetProjectTags.contains(tag.uri))
        .map(tagUri => sourceProject.tagManager.getTag(tagUri.uri))
      for (tag <- tagsToCopy) {
        targetProject.tagManager.putTag(tag)
      }
    }

    private def copyTask(task: ProjectTask[_ <: TaskSpec], copiedVariables: mutable.Set[TemplateVariableName]): Unit = {
      val taskParameters = task.data.parameters(PluginContext.fromProject(sourceProject).copy(prefixes = Prefixes.empty))
      val clonedTaskSpec = copyMissingVariables(task.data, copiedVariables) {
        task.data.withParameters(taskParameters, dropExistingValues = true)(PluginContext.fromProject(targetProject))
      }
      targetProject.updateAnyTask(taskRenameMap.getOrElse(task.id, task.id), clonedTaskSpec, Some(task.metaData), Some(task.executionVariables))
      // Copy resources
      if (copyResources) {
        for (resource <- task.referencedResources if resource.exists) {
          targetProject.resources.get(resource.name).writeResource(resource)
        }
      }
    }

    /**
     * Executes a function and copies all missing variables if an UnboundVariablesException is raised.
     */
    private def copyMissingVariables[T](task: TaskSpec, copiedVariables: mutable.Set[TemplateVariableName])(f: => T): T = {
      // Copy all variables that are known to be referenced by a task
      for(variableName <- task.referencedVariables if variableName.scope == VariableScope.project && !copiedVariables.contains(variableName)) {
        copyVariable(variableName.name)
        copiedVariables += variableName
      }
      // The referenced variables are not necessarily complete, so we need to add variables that are found by an UnboundVariablesException
      try {
        f
      } catch {
        case InvalidPluginParameterValueException(_, unboundEx: UnboundVariablesException) =>
          for(missingVar <- unboundEx.missingVars if missingVar.scope == VariableScope.project) {
            copyVariable(missingVar.name)
          }
          f
      }
    }

    /**
     * Copies a variable of the source project to the target project along with the variables its template needs,
     * unless the target project defines a variable of that name already.
     */
    private def copyVariable(name: String): Unit = {
      val targetVariables = targetProject.templateVariables.all
      if(!targetVariables.map.contains(name)) {
        targetProject.templateVariables.put(resolveAndAddMissingVariables(targetVariables, sourceVariable(name)))
      }
    }

    /**
     * A variable of the source project that the copied tasks need.
     */
    private def sourceVariable(name: String): TemplateVariable = {
      sourceProject.templateVariables.all.map.getOrElse(name,
        throw BadUserInputException(s"The copied tasks reference the variable 'project.$name', which is not defined in project '${sourceProject.id}'."))
    }

    /**
     * Adds a copied variable to the target variables and resolves them, adding the variables the copied ones need from the source project.
     * Fails if the variables cannot be resolved although nothing is missing from the source project.
     */
    private def resolveAndAddMissingVariables(targetVariables: TemplateVariables, copiedVariable: TemplateVariable): TemplateVariables = {
      var currentVariables = targetVariables.withLast(copiedVariable)
      val copiedNames = mutable.Buffer(copiedVariable.name)
      var resolvedVariables: Option[TemplateVariables] = None
      while(resolvedVariables.isEmpty) {
        try {
          // Sensitive global variables are not available to project variables, as everywhere else
          resolvedVariables = Some(currentVariables.resolved(GlobalTemplateVariables.all.withoutSensitiveVariables()))
        } catch {
          case ex: TemplateVariablesEvaluationException =>
            val missingVarNames = ex.issues.collect {
              case TemplateVariableEvaluationException(_, unboundEx: UnboundVariablesException) => unboundEx.missingVars
            }.flatten.distinct.filter(name => name.scope == VariableScope.project && !currentVariables.map.contains(name.name))
            if(missingVarNames.isEmpty) {
              throw BadUserInputException(s"Cannot resolve the variables of project '${targetProject.id}' after copying " +
                s"${copiedNames.mkString("'", "', '", "'")} from project '${sourceProject.id}': ${ex.getMessage}", Some(ex))
            }
            // Add all missing variables before trying again
            for(missingVarName <- missingVarNames) {
              currentVariables = currentVariables.withFirst(sourceVariable(missingVarName.name))
              copiedNames += missingVarName.name
            }
        }
      }
      resolvedVariables.get
    }

    /**
     * Returns a task and all its referenced tasks.
     */
    private def collectTasks(project: Project, taskName: Identifier)
                            (implicit userContext: UserContext): Seq[ProjectTask[_ <: TaskSpec]] = {
      val task = project.anyTask(taskName)
      Seq(task) ++ task.data.referencedTasks.flatMap(collectTasks(project, _))
    }

  }

  class InconsistentPrefixesException(val prefixes: Set[String])
    extends BadUserInputException("Cannot copy prefixes because the target project already contains some of the same prefixes with different namespaces. " +
                                  "Inconsistent prefix keys: " + prefixes.mkString(", "))

}