package de.fuberlin.wiwiss.silk.workspace.activity.linking

import de.fuberlin.wiwiss.silk.config.LinkSpecification
import de.fuberlin.wiwiss.silk.dataset.Dataset
import de.fuberlin.wiwiss.silk.learning.LearningConfiguration
import de.fuberlin.wiwiss.silk.learning.active.{ActiveLearning, ActiveLearningState}
import de.fuberlin.wiwiss.silk.runtime.activity.Activity
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.workspace.activity.TaskActivityFactory
import de.fuberlin.wiwiss.silk.workspace.{Task}

class ActiveLearningFactory extends TaskActivityFactory[LinkSpecification, ActiveLearning, ActiveLearningState] {

  def apply(task: Task[LinkSpecification]): Activity[ActiveLearningState] = {
    Activity.regenerating {
      new ActiveLearning(
        config = LearningConfiguration.default,
        datasets = DPair.fromSeq(task.data.dataSelections.map(ds => task.project.tasks[Dataset].map(_.data).find(_.id == ds.datasetId).getOrElse(Dataset.empty).source)),
        linkSpec = task.data,
        paths = task.activity[LinkingPathsCache].value().map(_.paths),
        referenceEntities = task.activity[ReferenceEntitiesCache].value()
      )
    }
  }

}
