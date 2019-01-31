package org.silkframework.learning.active

import org.silkframework.learning.{LearningActivity, LearningConfiguration, LearningInput, LearningResult}
import org.silkframework.rule.LinkSpec
import org.silkframework.runtime.activity.Activity
import org.silkframework.runtime.plugin.Plugin
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.TaskActivityFactory
import org.silkframework.workspace.activity.linking.ReferenceEntitiesCache

@Plugin(
  id = "SupervisedLearning",
  label = "Supervised Learning",
  categories = Array("LinkSpecification"),
  description = "Executes the supervised learning."
)
case class LearningFactory() extends TaskActivityFactory[LinkSpec, LearningActivity] {

  override def apply(task: ProjectTask[LinkSpec]): Activity[LearningResult] = {
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
