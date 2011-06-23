package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import net.liftweb.util.Helpers._
import de.fuberlin.wiwiss.silk.datasource.{DataSource, Source}
import de.fuberlin.wiwiss.silk.workbench.workspace.modules.source.SourceTask
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import de.fuberlin.wiwiss.silk.workbench.workspace.modules.linking.LinkingTask
import de.fuberlin.wiwiss.silk.linkspec.{DatasetSpecification, LinkSpecification}
import de.fuberlin.wiwiss.silk.util.Identifier

/**
 * A dialog to edit a source task.
 */
object EditSourceTaskDialog extends SourceTaskDialog
{
  override val title = "Edit source task"

  override val fields = nameField :: uriField :: graphField :: retryCountField :: retryPauseField :: Nil

  override def onSubmit()
  {
    val sourceTask = User().sourceTask

    var params = Map("endpointURI" -> uriField.value, "retryCount" -> retryCountField.value, "retryPause" -> retryPauseField.value)
    if(graphField.value != "")
    {
      params += ("graph" -> graphField.value)
    }

    val source = Source(nameField.value, DataSource("sparqlEndpoint", params))
    val updatedSourceTask = SourceTask(source)

    if(sourceTask.name != updatedSourceTask.name)
    {
      User().project.sourceModule.remove(sourceTask.name)

      //Update all linking tasks to point to the updated task
      val linkingModule = User().project.linkingModule
      val updateFunc = new UpdateLinkingTask(sourceTask.name, updatedSourceTask.name)
      val updatedLinkingTasks = linkingModule.tasks.collect(updateFunc)
      for(linkingTask <- updatedLinkingTasks)
      {
        linkingModule.update(linkingTask)
      }
    }

    User().project.sourceModule.update(updatedSourceTask)
    User().closeTask()
  }

  override protected def getName = User().sourceTask.source.id

  /**
   * Gets a parameter from the current data source.
   */
  override protected def getParam(name : String) =
  {
    User().sourceTask.source.dataSource match
    {
      case DataSource(id, params) => params.get(name).getOrElse("")
    }
  }

  /**
   * Partial function which updates the source of a linking task.
   */
  private class UpdateLinkingTask(oldSource : Identifier, newSource : Identifier) extends PartialFunction[LinkingTask, LinkingTask]
  {
    override def isDefinedAt(task: LinkingTask) =
    {
      task.linkSpec.datasets.exists(_.sourceId == oldSource)
    }

    override def apply(task : LinkingTask) =
    {
      task.copy(linkSpec = task.linkSpec.copy(datasets = task.linkSpec.datasets.map(updateDataset)))
    }

    private def updateDataset(ds : DatasetSpecification) =
    {
      if(ds.sourceId == oldSource)
      {
        ds.copy(sourceId = newSource)
      }
      else
      {
        ds
      }
    }
  }
}
