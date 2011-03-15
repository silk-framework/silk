package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import net.liftweb.util.Helpers._
import de.fuberlin.wiwiss.silk.datasource.{DataSource, Source}
import de.fuberlin.wiwiss.silk.workbench.workspace.modules.source.SourceTask
import de.fuberlin.wiwiss.silk.workbench.workspace.User

/**
 * A dialog to edit a source task.
 */
object EditSourceTaskDialog extends SourceTaskDialog
{
  override val title = "Edit source task"

  override val fields = uriField :: graphField :: retryCountField :: retryPauseField :: Nil

  override def onSubmit()
  {
    val sourceTask = User().sourceTask

    var params = Map("endpointURI" -> uriField.value, "retryCount" -> retryCountField.value, "retryPause" -> retryPauseField.value)
    if(graphField.value != "")
    {
      params += ("graph" -> graphField.value)
    }
    val source = Source(sourceTask.name, DataSource("sparqlEndpoint", params))
    val updatedSourceTask = SourceTask(source)

    User().project.sourceModule.update(updatedSourceTask)
    User().closeTask()
  }

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
}
