package org.silkframework.workspace.activity.linking

import org.silkframework.config.LinkSpec
import org.silkframework.learning.{LearningActivity, LearningConfiguration, LearningInput, LearningResult}
import org.silkframework.runtime.activity.Activity
import org.silkframework.runtime.plugin.Plugin
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.TaskActivityFactory

@Plugin(
  id = "SupervisedLearning",
  label = "Supervised Learning",
  categories = Array("LinkSpecification"),
  description = "Executes the supervised learning."
)
case class LearningFactory() extends TaskActivityFactory[LinkSpec, LearningActivity] {

  def apply(task: ProjectTask[LinkSpec]): Activity[LearningResult] = {
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
