package controllers.workflowApi.workflow

import org.silkframework.config.Task
import org.silkframework.dataset.operations.ClearDatasetOperator
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Identifier
import org.silkframework.workspace.Project
import org.silkframework.workspace.activity.workflow.{AllReplaceableDatasets, ClearDatasetOrderingCheck, Workflow, WorkflowDataset, WorkflowOperator, WorkflowNode}
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
case class WorkflowWarningInfo(message: String,
                               nodeIds: Option[Seq[String]] = None)

case class WorkflowInfo(id: String,
                        label: String,
                        projectId: String,
                        projectLabel: String,
                        variableInputs: Seq[String],
                        variableOutputs: Seq[String],
                        warnings: Seq[WorkflowWarningInfo]
                       )

object WorkflowInfo {
  implicit val workflowWarningInfoFormat: Format[WorkflowWarningInfo] = Json.format[WorkflowWarningInfo]
  implicit val workflowInfoFormat: Format[WorkflowInfo] = Json.format[WorkflowInfo]

  private def taskLabel(taskId: Identifier, project: Project)
                       (implicit userContext: UserContext): String = {
    project.anyTaskOption(taskId).map(_.fullLabel).getOrElse(taskId.toString)
  }

  private def formatLabelList(labels: Seq[String]): String = {
    labels.distinct.sorted.map(label => s"'$label'").mkString(", ")
  }

  private def summarizeClearOrderWarnings(workflow: Workflow, project: Project)
                                         (implicit userContext: UserContext): Seq[WorkflowWarningInfo] = {
    val unorderedPairs = ClearDatasetOrderingCheck.unorderedPairsIfValid(workflow, project)
    if (unorderedPairs.isEmpty) {
      Seq.empty
    } else {
      val workflowNodesById: Map[String, WorkflowNode] = workflow.nodes.map(node => node.nodeId -> node).toMap
      def clearOperatorNodesForPair(pair: ClearDatasetOrderingCheck.UnorderedClearWrite): Seq[WorkflowOperator] = {
        workflowNodesById.get(pair.clearNodeId).toSeq
          .collect { case datasetNode: WorkflowDataset => datasetNode }
          .flatMap(_.inputs.flatten)
          .flatMap(nodeId => workflowNodesById.get(nodeId).collect { case operatorNode: WorkflowOperator => operatorNode })
          .filter(operatorNode => project.anyTaskOption(operatorNode.task).exists(_.data.isInstanceOf[ClearDatasetOperator]))
      }

      val summaryMessage =
        s"Discovered ${unorderedPairs.size} case(s) of undefined clear/write operation order for the highlighted nodes. " +
          s"Add dependency connections between the mentioned nodes in order to make each clear/write operation order explicit."
      val perCaseWarnings = unorderedPairs.zipWithIndex.map { case (pair, idx) =>
        val pairClearOperators = clearOperatorNodesForPair(pair)
        val pairClearOperatorLabels = formatLabelList(pairClearOperators.map(operatorNode => taskLabel(operatorNode.task, project)))
        val pairNodeIds = (pair.nodeIds ++ pairClearOperators.map(_.nodeId)).distinct
        WorkflowWarningInfo(
          s"Case ${idx+1}: dataset '${pair.datasetLabel}' and 'Clear dataset' operator '$pairClearOperatorLabels' are not explicitly ordered.",
          Some(pairNodeIds)
        )
      }
      WorkflowWarningInfo(summaryMessage) +: perCaseWarnings
    }
  }

  def fromWorkflow(workflow: Task[Workflow],
                   project: Project)
                  (implicit userContext: UserContext): WorkflowInfo = {
    var warning: Option[WorkflowWarningInfo] = None
    val variableDatasets = try{
      workflow.allReplaceableDatasets(project)
    } catch {
      case NonFatal(ex) =>
        warning = if(ex.getMessage != null) {
          Some(WorkflowWarningInfo("Variable inputs and outputs could not be retrieved! Details: " + ex.getMessage))
        } else {
          Some(WorkflowWarningInfo("Variable inputs and outputs could not be retrieved!"))
        }
        AllReplaceableDatasets(Seq.empty, Seq.empty)
    }
    // Clear-dataset nodes whose order relative to writers of the same dataset is undefined.
    val clearOrderWarnings = summarizeClearOrderWarnings(workflow.data, project)
    WorkflowInfo(
      workflow.id,
      workflow.fullLabel,
      project.id,
      project.config.metaData.formattedLabel(project.id, Int.MaxValue),
      variableDatasets.dataSources,
      variableDatasets.sinks,
      warning.toSeq ++ clearOrderWarnings
    )
  }
}
