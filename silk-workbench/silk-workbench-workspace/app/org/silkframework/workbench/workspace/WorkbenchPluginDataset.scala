package org.silkframework.workbench.workspace

import controllers.workspace.routes.Assets
import org.silkframework.config.TaskSpec
import org.silkframework.dataset.DatasetSpec
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset.rdf.RdfDataset
import org.silkframework.util.Identifier
import org.silkframework.workbench.WorkbenchPlugin
import org.silkframework.workbench.WorkbenchPlugin.{Tab, TaskActions, TaskType}
import org.silkframework.workbench.workspace.WorkbenchPluginDataset.{DatasetTaskActions, DatasetTaskType}
import org.silkframework.workspace.ProjectTask

import scala.language.existentials

case class WorkbenchPluginDataset() extends WorkbenchPlugin[GenericDatasetSpec] {

  override def taskType: TaskType = DatasetTaskType

  override def taskActions(task: ProjectTask[_ <: TaskSpec]): TaskActions = {
    DatasetTaskActions(task)
  }
}

object WorkbenchPluginDataset {

  object DatasetTaskType extends TaskType {

    /** The name of the task type */
    override def typeName: String = "Dataset"

    /** Path to the task icon */
    override def icon: String = Assets.at("img/server.png").url

    override def folderIcon: String = Assets.at("img/dataset-folder.png").url

    /** The path to the dialog for creating a new task. */
    override def createDialog(project: String): Option[String] =
      Some(s"workspace/dialogs/newDataset/$project")

    override def index: Int = 0
  }

  case class DatasetTaskActions(task: ProjectTask[_ <: TaskSpec]) extends TaskActions {

    private val project = task.project.name

    private val taskId = task.id

    /** The name of the task type */
    override def taskType: TaskType = DatasetTaskType

    /** The path to the dialog for editing an existing task. */
    override def propertiesDialog: Option[String] = {
      Some(s"workspace/dialogs/editDataset/$project/$taskId")
    }

    /** The path to redirect to when the task is opened. */
    override def openPath(inWorkflow: Option[Identifier], workflowOperatorId: Option[String]): Option[String] = {
      Some(s"workspace/datasets/$project/$taskId/dataset")
    }

    /**
      * Lists the shown tabs.
      */
    override def tabs: Seq[Tab] = {
      task.data match {
        case dataset: GenericDatasetSpec =>
          if (dataset.plugin.isInstanceOf[RdfDataset]) {
            Seq(Tab("SPARQL", s"workspace/datasets/$project/$taskId/sparql"))
          } else {
            Seq(Tab("Tableview", s"workspace/datasets/$project/$taskId/table"))
          }
      }
    }
  }
}