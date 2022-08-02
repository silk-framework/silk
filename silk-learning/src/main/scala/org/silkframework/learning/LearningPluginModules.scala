package org.silkframework.learning

import org.silkframework.learning.active.comparisons.ComparisonPairGeneratorFactory
import org.silkframework.learning.active.comparisons.ComparisonPairs.ComparisonPairsJsonFormat
import org.silkframework.learning.active.{ActiveLearningFactory, LearningFactory}
import org.silkframework.runtime.plugin.PluginModule

/**
  * Active learning plugins.
  */
class LearningPluginModules extends PluginModule {
  override def pluginClasses: Seq[Class[_]] = classOf[LearningFactory] ::
      classOf[ActiveLearningFactory] ::
      classOf[ComparisonPairGeneratorFactory] ::
      ComparisonPairsJsonFormat.getClass ::
      Nil
}
