package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import de.fuberlin.wiwiss.silk.workbench.evaluation.{CurrentGenerateLinksTask, GenerateLinksTask}
import de.fuberlin.wiwiss.silk.workbench.lift.util.{JS, SelectField, Dialog}
import de.fuberlin.wiwiss.silk.config.RuntimeConfig

object GenerateLinksDialog extends Dialog {

  override val title = "Generate Links"

  private val noOutputName = "Display only"

  private val output = SelectField("Output", "The output where the generated links are written", () => noOutputName :: User().project.outputModule.tasks.map(_.name.toString).toList, () => noOutputName)

  override val fields = output :: Nil

  override protected def dialogParams = ("autoOpen" -> "false") :: ("width" -> "400") :: ("modal" -> "true") :: Nil

  private val logger = Logger.getLogger(getClass.getName)

  /** We use a custom runtime config */
  private val runtimeConfig = RuntimeConfig(useFileCache = false, partitionSize = 300, generateDetailedLinks = true)

  override protected def onSubmit() = {
    val generateLinksTask =
      new GenerateLinksTask(
        sources = User().project.sourceModule.tasks.map(_.source),
        linkSpec = User().linkingTask.linkSpec,
        outputs = if(output.value == noOutputName) Traversable.empty else Traversable(User().project.outputModule.task(output.value).output),
        runtimeConfig = runtimeConfig
      )

    CurrentGenerateLinksTask() = generateLinksTask
    generateLinksTask.runInBackground()

    JS.Empty
  }
}