package org.silkframework.workspace.activity.linking

import org.silkframework.config.LinkSpecification
import org.silkframework.dataset.Dataset
import org.silkframework.learning.LearningConfiguration
import org.silkframework.learning.active.{ActiveLearning, ActiveLearningState}
import org.silkframework.runtime.activity.Activity
import org.silkframework.runtime.plugin.Plugin
import org.silkframework.util.DPair
import org.silkframework.workspace.Task
import org.silkframework.workspace.activity.TaskActivityFactory

@Plugin(
  id = "activeLearning",
  label = "Active Learning",
  categories = Array("LinkSpecification"),
  description = "Executes an active learning iteration."
)
case class ActiveLearningFactory() extends TaskActivityFactory[LinkSpecification, ActiveLearning] {

  def apply(task: Task[LinkSpecification]): Activity[ActiveLearningState] = {
    Activity.regenerating {
      new ActiveLearning(
        config = LearningConfiguration.default,
        datasets = DPair.fromSeq(task.data.dataSelections.map(ds => task.project.tasks[Dataset].map(_.data).find(_.id == ds.datasetId).getOrElse(Dataset.empty).source)),
        linkSpec = task.data,
        paths = task.activity[LinkingPathsCache].value.map(_.paths),
        referenceEntities = task.activity[ReferenceEntitiesCache].value
      )
    }
  }

}
