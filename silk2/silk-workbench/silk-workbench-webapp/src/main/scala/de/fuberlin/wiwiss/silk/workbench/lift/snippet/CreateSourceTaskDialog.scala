package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import net.liftweb.util.Helpers._
import de.fuberlin.wiwiss.silk.datasource.{DataSource, Source}
import de.fuberlin.wiwiss.silk.workbench.workspace.modules.source.SourceTask
import de.fuberlin.wiwiss.silk.workbench.workspace.User

/**
 * A dialog to create new data sources.
 */
object CreateSourceTaskDialog extends SourceTaskDialog
{
  override val title = "Create source task"

  override val fields = nameField :: uriField :: graphField :: retryCountField :: retryPauseField :: Nil

  override def onSubmit()
  {
    var params = Map("endpointURI" -> uriField.value, "retryCount" -> retryCountField.value, "retryPause" -> retryPauseField.value)
    if(graphField.value != "")
    {
      params += ("graph" -> graphField.value)
    }
    val source = Source(nameField.value, DataSource("sparqlEndpoint", params))
    val sourceTask = SourceTask(source)

    User().project.sourceModule.update(sourceTask)
  }

  /**
   * Gets a parameter.
   */
  override protected def getParam(name : String) =
  {
    val sparqlEndpointDesc = DataSource.availableStrategies.find(_.id == "sparqlEndpoint").get
    val param = sparqlEndpointDesc.parameters.find(_.name == name).get

    param.defaultValue.flatMap(Option(_)).map(_.toString).getOrElse("")
  }
}
