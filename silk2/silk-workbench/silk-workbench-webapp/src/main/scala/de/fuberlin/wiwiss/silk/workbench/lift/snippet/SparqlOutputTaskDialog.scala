package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import de.fuberlin.wiwiss.silk.workbench.lift.util.Dialog
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import de.fuberlin.wiwiss.silk.output.{Output, LinkWriter}
import de.fuberlin.wiwiss.silk.workbench.workspace.modules.output.OutputTask

object SparqlOutputTaskDialog extends Dialog
{
  private val nameField = StringField("Name", "The name of this source task", () => if(User().outputTaskOpen) User().outputTask.name.toString else "")

  private val uriField = StringField("Endpoint URI", "The URI of the SPARQL endpoint", () => getParam("uri"))

  private val graphField = StringField("Graph", "The URI of the Graph", () => getParam("graphUri"))

  override val fields = nameField :: uriField :: graphField:: Nil

  override def title = if(User().sourceTaskOpen) "Edit output task" else "Create output task"

  override def onSubmit()
  {
    val newOutput = createOutput()

    User().project.outputModule.update(newOutput)

    if(User().outputTaskOpen && User().outputTask.name != newOutput.name)
    {
      val currentOutput = User().outputTask

      User().project.sourceModule.remove(currentOutput.name)
    }
  }

  /**
   * Creates a new output task from the current dialog values.
   */
  private def createOutput() =
  {
    val params = Map("uri" -> uriField.value, "graphUri" -> graphField.value)
    val source = Output(LinkWriter("sparul", params))
    OutputTask(nameField.value, source)
  }

  /**
   * Gets a parameter.
   */
  private def getParam(name : String) : String =
  {
    if(User().outputTaskOpen)
    {
      User().outputTask.output.writer match
      {
        case LinkWriter(id, params) => params.get(name).getOrElse("")
      }
    }
    else
    {
      val strategy = LinkWriter.availableStrategies.find(_.id == "sparul").get
      val param = strategy.parameters.find(_.name == name).get

      param.defaultValue.flatMap(Option(_)).map(_.toString).getOrElse("")
    }
  }
}