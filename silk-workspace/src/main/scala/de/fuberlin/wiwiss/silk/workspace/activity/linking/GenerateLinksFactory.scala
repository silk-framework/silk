package de.fuberlin.wiwiss.silk.workspace.activity.linking

import de.fuberlin.wiwiss.silk.config.{LinkSpecification, RuntimeConfig}
import de.fuberlin.wiwiss.silk.dataset.Dataset
import de.fuberlin.wiwiss.silk.entity.Link
import de.fuberlin.wiwiss.silk.execution.GenerateLinks
import de.fuberlin.wiwiss.silk.runtime.activity.Activity
import de.fuberlin.wiwiss.silk.workspace.activity.TaskActivityFactory
import de.fuberlin.wiwiss.silk.workspace.{Task}

class GenerateLinksFactory extends TaskActivityFactory[LinkSpecification, GenerateLinks, Seq[Link]] {

  def apply(task: Task[LinkSpecification]): Activity[Seq[Link]] = {
    Activity.regenerating {
      GenerateLinks.fromSources(
        datasets = task.project.tasks[Dataset].map(_.data),
        linkSpec = task.data,
        runtimeConfig = RuntimeConfig(useFileCache = false, partitionSize = 300, generateLinksWithEntities = true)
      )
    }
  }
}
