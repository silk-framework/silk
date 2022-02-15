package controllers.workflowApi.workflow

import org.silkframework.config.Task
import org.silkframework.runtime.activity.UserContext
import org.silkframework.workspace.Project
import org.silkframework.workspace.activity.workflow.Workflow
import play.api.libs.json.{Format, Json}

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
                        variableOutputs: Seq[String]
                       )

object WorkflowInfo {
  implicit val workflowInfoFormat: Format[WorkflowInfo] = Json.format[WorkflowInfo]

  def fromWorkflow(workflow: Task[Workflow],
                   project: Project)
                  (implicit userContext: UserContext): WorkflowInfo = {
    val variableDatasets = workflow.variableDatasets(project)
    WorkflowInfo(
      workflow.id,
      workflow.fullLabel,
      project.id,
      project.config.metaData.formattedLabel(project.id, Int.MaxValue),
      variableDatasets.dataSources,
      variableDatasets.sinks
    )
  }
}
