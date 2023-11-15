package controllers.workflowApi.workflow

import org.silkframework.config.Task
import org.silkframework.runtime.activity.UserContext
import org.silkframework.workspace.Project
import org.silkframework.workspace.activity.workflow.{AllReplaceableDatasets, Workflow}
import play.api.libs.json.{Format, Json}

import scala.util.control.NonFatal

/** Workflow information.
  *
  * @param id              Workflow ID
  * @param label           Workflow label
  * @param projectId       Project the workflow in located in.
  * @param projectLabel    Label of the project.
  * @param variableInputs  IDs of the variable input datasets used in this workflow.
  * @param variableOutputs IDs of the variable output datasets used in this workflow.
  */
case class WorkflowInfo(id: String,
                        label: String,
                        projectId: String,
                        projectLabel: String,
                        variableInputs: Seq[String],
                        variableOutputs: Seq[String],
                        warnings: Seq[String]
                       )

object WorkflowInfo {
  implicit val workflowInfoFormat: Format[WorkflowInfo] = Json.format[WorkflowInfo]

  def fromWorkflow(workflow: Task[Workflow],
                   project: Project)
                  (implicit userContext: UserContext): WorkflowInfo = {
    var warning: Option[String] = None
    val variableDatasets = try{
      workflow.allReplaceableDatasets(project)
    } catch {
      case NonFatal(ex) =>
        warning = if(ex.getMessage != null) {
          Some("Variable inputs and outputs could not be retrieved! Details: " + ex.getMessage)
        } else {
          Some("Variable inputs and outputs could not be retrieved!")
        }
        AllReplaceableDatasets(Seq.empty, Seq.empty)
    }
    WorkflowInfo(
      workflow.id,
      workflow.fullLabel,
      project.id,
      project.config.metaData.formattedLabel(project.id, Int.MaxValue),
      variableDatasets.dataSources,
      variableDatasets.sinks,
      warning.toSeq
    )
  }
}
