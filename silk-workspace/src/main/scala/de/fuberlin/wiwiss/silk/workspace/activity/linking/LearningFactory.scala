package de.fuberlin.wiwiss.silk.workspace.activity.linking

import de.fuberlin.wiwiss.silk.config.LinkSpecification
import de.fuberlin.wiwiss.silk.learning.{LearningActivity, LearningConfiguration, LearningInput, LearningResult}
import de.fuberlin.wiwiss.silk.runtime.activity.Activity
import de.fuberlin.wiwiss.silk.workspace.activity.TaskActivityFactory
import de.fuberlin.wiwiss.silk.workspace.{Task}

class LearningFactory extends TaskActivityFactory[LinkSpecification, LearningActivity, LearningResult] {

  def apply(task: Task[LinkSpecification]): Activity[LearningResult] = {
    Activity.regenerating {
      val input =
        LearningInput(
          trainingEntities = task.activity[ReferenceEntitiesCache].value(),
          seedLinkageRules = task.data.rule :: Nil
        )
      new LearningActivity(input, LearningConfiguration.default)
    }
  }

}
