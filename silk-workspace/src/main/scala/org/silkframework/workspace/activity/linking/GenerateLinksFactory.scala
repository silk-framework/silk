package org.silkframework.workspace.activity.linking

import org.silkframework.config.{LinkSpecification, RuntimeConfig}
import org.silkframework.dataset.Dataset
import org.silkframework.entity.Link
import org.silkframework.execution.{Linking, GenerateLinks}
import org.silkframework.runtime.activity.Activity
import org.silkframework.runtime.plugin.{Param, Plugin}
import org.silkframework.workspace.Task
import org.silkframework.workspace.activity.TaskActivityFactory

@Plugin(
  id = "generateLinks",
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
    Activity.regenerating {
      var linksSpec = task.data
      if(!writeOutputs)
        linksSpec = linksSpec.copy(outputs = Seq.empty)

      GenerateLinks.fromSources(
        datasets = task.project.tasks[Dataset].map(_.data),
        linkSpec = linksSpec,
        runtimeConfig =
          RuntimeConfig(
            includeReferenceLinks = includeReferenceLinks,
            useFileCache = useFileCache,
            partitionSize = partitionSize,
            generateLinksWithEntities = generateLinksWithEntities
          )
      )
    }
  }
}
