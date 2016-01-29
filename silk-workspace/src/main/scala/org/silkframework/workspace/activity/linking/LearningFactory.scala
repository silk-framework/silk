package org.silkframework.workspace.activity.linking

import org.silkframework.config.LinkSpecification
import org.silkframework.learning.{LearningActivity, LearningConfiguration, LearningInput, LearningResult}
import org.silkframework.runtime.activity.Activity
import org.silkframework.runtime.plugin.Plugin
import org.silkframework.workspace.Task
import org.silkframework.workspace.activity.TaskActivityFactory

@Plugin(
  id = "supervisedLearning",
  label = "Supervised Learning",
  categories = Array("LinkSpecification"),
  description = "Executes the supervised learning."
)
case class LearningFactory() extends TaskActivityFactory[LinkSpecification, LearningActivity] {

  def apply(task: Task[LinkSpecification]): Activity[LearningResult] = {
    Activity.regenerating {
      val input =
        LearningInput(
          trainingEntities = task.activity[ReferenceEntitiesCache].value,
          seedLinkageRules = task.data.rule :: Nil
        )
      new LearningActivity(input, LearningConfiguration.default)
    }
  }

}
