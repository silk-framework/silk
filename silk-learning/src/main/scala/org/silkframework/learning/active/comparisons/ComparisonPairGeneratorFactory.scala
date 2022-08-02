package org.silkframework.learning.active.comparisons

import org.silkframework.learning.LearningConfiguration
import org.silkframework.rule.LinkSpec
import org.silkframework.runtime.activity.Activity
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.TaskActivityFactory

import scala.util.Random

@Plugin(
  id = "ActiveLearning-ComparisonPairs",
  label = "Active learning (find comparison pairs)",
  categories = Array("LinkSpecification"),
  description = "Executes an active learning iteration."
)
case class ComparisonPairGeneratorFactory(fixedRandomSeed: Boolean = true) extends TaskActivityFactory[LinkSpec, ComparisonPairGenerator] {

  override def apply(task: ProjectTask[LinkSpec]): Activity[ComparisonPairs] = {
    val randomSeed = if(fixedRandomSeed) 0L else Random.nextLong()
    new ComparisonPairGenerator(
      task,
      config = LearningConfiguration.default,
      initialState = ComparisonPairs.initial(randomSeed)
    )
  }

}


