package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import xml.NodeSeq
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import de.fuberlin.wiwiss.silk.output.{Output, LinkWriter}
import de.fuberlin.wiwiss.silk.workbench.workspace.modules.output.OutputTask
import de.fuberlin.wiwiss.silk.workbench.lift.util.{StringField, PluginDialog}

object OutputDialog extends PluginDialog[LinkWriter] {
  override def title = "Output"

  private val nameField = StringField("name", "The name of this source task", () => if (User().outputTaskOpen) User().outputTask.name.toString else "")

  override protected val fields = nameField :: Nil

  override protected val plugins = LinkWriter.plugin("file") :: LinkWriter.plugin("sparul") :: Nil

  //Close the current task if the window is closed
  override protected def dialogParams = ("close" -> "closeTask") :: super.dialogParams

  override protected def currentObj = {
    if (User().outputTaskOpen) {
      Some(User().outputTask.output.writer)
    }
    else {
      None
    }
  }

  override protected def onSubmit(linkWriter: LinkWriter) {
    val newOutput = OutputTask(Output(nameField.value, linkWriter))

    User().project.outputModule.update(newOutput)

    if (User().outputTaskOpen && User().outputTask.name != newOutput.name) {
      User().project.outputModule.remove(User().outputTask.name)
    }
  }

  override def render(in: NodeSeq): NodeSeq = super.render(in)
}