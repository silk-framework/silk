package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.workbench.lift.util.{SelectField, Dialog}
import de.fuberlin.wiwiss.silk.workbench.workspace.{CurrentStatusListener, User}
import de.fuberlin.wiwiss.silk.util.task.{Finished, Started, Status}
import de.fuberlin.wiwiss.silk.workbench.evaluation.{CurrentGenerateLinksTask, GenerateLinksTask}

object GenerateLinksDialog extends Dialog {

  override val title = "Generate Links"

  private val noOutputName = "Display only"

  private val output = SelectField("Output", "The output where the generated links are written", () => noOutputName :: User().project.outputModule.tasks.map(_.name.toString).toList, () => noOutputName)

  override val fields = output :: Nil

  override protected def dialogParams = ("autoOpen" -> "false") :: ("width" -> "400") :: ("modal" -> "true") :: Nil

  private val logger = Logger.getLogger(getClass.getName)

  override protected def onSubmit() {
    val generateLinksTask = new GenerateLinksTask(User())

    if(output.value == noOutputName) {
      generateLinksTask.output = None
    } else {
      generateLinksTask.output = Some(User().project.outputModule.task(output.value).output)
    }

    CurrentGenerateLinksTask() = generateLinksTask
    generateLinksTask.runInBackground()
  }
}