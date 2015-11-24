package org.silkframework.workspace.activity.linking

import org.silkframework.config.{LinkSpecification, RuntimeConfig}
import org.silkframework.dataset.Dataset
import org.silkframework.entity.Link
import org.silkframework.execution.GenerateLinks
import org.silkframework.runtime.activity.Activity
import org.silkframework.workspace.Task
import org.silkframework.workspace.activity.TaskActivityFactory

case class GenerateLinksFactory(useFileCache: Boolean = false, partitionSize: Int = 300, generateLinksWithEntities: Boolean = true) extends TaskActivityFactory[LinkSpecification, GenerateLinks, Seq[Link]] {

  def apply(task: Task[LinkSpecification]): Activity[Seq[Link]] = {
    Activity.regenerating {
      GenerateLinks.fromSources(
        datasets = task.project.tasks[Dataset].map(_.data),
        linkSpec = task.data,
        runtimeConfig = RuntimeConfig(useFileCache = useFileCache, partitionSize = partitionSize, generateLinksWithEntities = generateLinksWithEntities)
      )
    }
  }
}
