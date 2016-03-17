package org.silkframework.workspace.activity.linking

import com.sun.xml.internal.bind.v2.runtime.output.FastInfosetStreamWriterOutput
import org.silkframework.config.{TransformSpecification, LinkSpecification, RuntimeConfig}
import org.silkframework.dataset.{DataSource, Dataset}
import org.silkframework.entity.{Restriction, Path, Entity, EntitySchema}
import org.silkframework.execution.{ExecuteTransform, GenerateLinks, Linking}
import org.silkframework.plugins.dataset.InternalDataset
import org.silkframework.rule.{TransformedDataSource, TransformRule, TypeMapping}
import org.silkframework.runtime.activity.{ActivityContext, Activity}
import org.silkframework.runtime.plugin.{Param, Plugin}
import org.silkframework.util.{Uri, Identifier}
import org.silkframework.workspace.Task
import org.silkframework.workspace.activity.TaskActivityFactory

import scala.reflect.ClassTag

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
  writeOutputs: Boolean = true) extends TaskActivityFactory[LinkSpecification, GenerateLinks] {

  def apply(task: Task[LinkSpecification]): Activity[Linking] = {
    val runtimeConfig =
      RuntimeConfig(
        includeReferenceLinks = includeReferenceLinks,
        useFileCache = useFileCache,
        partitionSize = partitionSize,
        generateLinksWithEntities = generateLinksWithEntities
      )
    new GenerateLinksActivity(task, runtimeConfig, writeOutputs)
  }
}

class GenerateLinksActivity(task: Task[LinkSpecification], runtimeConfig: RuntimeConfig, writeOutputs: Boolean) extends Activity[Linking] {

  @volatile
  private var generateLinks: Option[GenerateLinks] = None

  override def name = "GenerateLinks"

  override def initialValue = Some(Linking())

  /**
    * Executes this activity.
    *
    * @param context Holds the context in which the activity is executed.
    */
  override def run(context: ActivityContext[Linking]): Unit = {
    val linkSpec = task.data

    val inputs = linkSpec.dataSelections.map(ds => getDataSource(ds.datasetId))

    val outputs =
      if (writeOutputs) linkSpec.outputs.flatMap(o => task.project.taskOption[Dataset](o)).map(_.data.linkSink)
      else Nil

    generateLinks = Some(
      new GenerateLinks(
        inputs = inputs,
        linkSpec = linkSpec,
        outputs = outputs,
        runtimeConfig = runtimeConfig
      )
    )
    generateLinks.get.run(context)
    generateLinks = None
  }

  override def cancelExecution() = generateLinks.foreach(_.cancelExecution())

  private def getDataSource(sourceId: String): DataSource = {
    task.project.taskOption[TransformSpecification](sourceId) match {
      case Some(transformTask) =>
        val source = task.project.task[Dataset](transformTask.data.selection.datasetId).data.source
        new TransformedDataSource(source, transformTask.data)
      case None =>
        task.project.task[Dataset](sourceId).data.source
    }
  }

}
