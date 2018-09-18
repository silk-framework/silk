package org.silkframework.workspace.activity.linking

import org.silkframework.rule.execution.{GenerateLinks, Linking}
import org.silkframework.rule.{LinkSpec, RuntimeLinkingConfig}
import org.silkframework.runtime.activity.{Activity, ActivityContext, UserContext}
import org.silkframework.runtime.plugin.{Param, Plugin}
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.TaskActivityFactory
import org.silkframework.workspace.activity.linking.LinkingTaskUtils._
import EvaluateLinkingFactory._

@Plugin(
  id = "EvaluateLinking",
  label = "Evaluate Linking",
  categories = Array("LinkSpecification"),
  description = "Evaluates the linking task by generating links."
)
case class EvaluateLinkingFactory(
  @Param("Do not generate a link for which there is a negative reference link while always generating positive reference links.")
  includeReferenceLinks: Boolean = false,
  @Param("Use a file cache. This avoids memory overflows for big files.")
  useFileCache: Boolean = true,
  @Param("The number of entities in a single partition in the cache.")
  partitionSize: Int = DEFAULT_PARTITION_SIZE,
  @Param("Generate detailed information about the matched entities. If set to false, the generated links won't be shown in the Workbench.")
  generateLinksWithEntities: Boolean = true,
  @Param("Write the generated links to the configured output of this task.")
  writeOutputs: Boolean = false,
  @Param("If defined, the execution will stop after the configured number of links is reached.\nThis is just a hint and the execution may produce slightly fewer or more links.")
  linkLimit: Int = DEFAULT_LINK_LIMIT
  ) extends TaskActivityFactory[LinkSpec, EvaluateLinkingActivity] {

  override def apply(task: ProjectTask[LinkSpec]): Activity[Linking] = {
    val runtimeConfig =
      RuntimeLinkingConfig(
        includeReferenceLinks = includeReferenceLinks,
        useFileCache = useFileCache,
        partitionSize = partitionSize,
        generateLinksWithEntities = generateLinksWithEntities,
        linkLimit = Some(linkLimit)
      )
    new EvaluateLinkingActivity(task, runtimeConfig, writeOutputs)
  }
}

class EvaluateLinkingActivity(task: ProjectTask[LinkSpec], runtimeConfig: RuntimeLinkingConfig, writeOutputs: Boolean) extends Activity[Linking] {

  @volatile
  private var generateLinks: Option[GenerateLinks] = None

  override def name: String = "EvaluateLinking"

  override def initialValue: Option[Linking] = Some(Linking())

  /**
    * Executes this activity.
    *
    * @param context Holds the context in which the activity is executed.
    */
  override def run(context: ActivityContext[Linking])
                  (implicit userContext: UserContext): Unit = {
    val linkSpec = task.data

    val inputs = task.dataSources

    val outputs = if (writeOutputs) task.linkSinks else Nil

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

object EvaluateLinkingFactory {

  val DEFAULT_PARTITION_SIZE = 500

  val DEFAULT_LINK_LIMIT = 10000

}