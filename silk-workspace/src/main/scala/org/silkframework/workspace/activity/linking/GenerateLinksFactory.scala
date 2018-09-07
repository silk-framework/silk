package org.silkframework.workspace.activity.linking

import org.silkframework.rule.execution.{GenerateLinks, Linking}
import org.silkframework.rule.{LinkSpec, RuntimeLinkingConfig}
import org.silkframework.runtime.activity.{Activity, ActivityContext, UserContext}
import org.silkframework.runtime.plugin.{Param, Plugin}
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.TaskActivityFactory
import org.silkframework.workspace.activity.linking.LinkingTaskUtils._

@Plugin(
  id = "GenerateLinks",
  label = "Generate Links",
  categories = Array("LinkSpecification"),
  description = "Executes the link specification."
)
case class GenerateLinksFactory(
  @Param("Do not generate a link for which there is a negative reference link while always generating positive reference links.")
  includeReferenceLinks: Boolean = false,
  @Param("Use a file cache. This avoids memory overflows for big files.")
  useFileCache: Boolean = false,
  @Param("The number of entities in a single partition in the cache.")
  partitionSize: Int = 300,
  @Param("Generate detailed information about the matched entities. If set to false, the generated links won't be shown in the Workbench.")
  generateLinksWithEntities: Boolean = true,
  @Param("Write the generated links to the configured output of this task.")
  writeOutputs: Boolean = true) extends TaskActivityFactory[LinkSpec, GenerateLinks] {

  override def apply(task: ProjectTask[LinkSpec]): Activity[Linking] = {
    val runtimeConfig =
      RuntimeLinkingConfig(
        includeReferenceLinks = includeReferenceLinks,
        useFileCache = useFileCache,
        partitionSize = partitionSize,
        generateLinksWithEntities = generateLinksWithEntities
      )
    new GenerateLinksActivity(task, runtimeConfig, writeOutputs)
  }
}

class GenerateLinksActivity(task: ProjectTask[LinkSpec], runtimeConfig: RuntimeLinkingConfig, writeOutputs: Boolean) extends Activity[Linking] {

  @volatile
  private var generateLinks: Option[GenerateLinks] = None

  override def name = "GenerateLinks"

  override def initialValue = Some(Linking())

  /**
    * Executes this activity.
    *
    * @param context Holds the context in which the activity is executed.
    */
  override def run(context: ActivityContext[Linking])
                  (implicit userContext: UserContext): Unit = {
    val linkSpec = task.data

    val inputs = task.dataSources

    val outputs =
      if (writeOutputs) task.linkSinks
      else Nil

    generateLinks = Some(
      new GenerateLinks(
        task.id,
        inputs = inputs,
        linkSpec = linkSpec,
        outputs = outputs,
        runtimeConfig = runtimeConfig
      )
    )
    generateLinks.get.run(context)
    generateLinks = None
  }

  override def cancelExecution()(implicit userContext: UserContext): Unit = generateLinks.foreach(_.cancelExecution())

}
