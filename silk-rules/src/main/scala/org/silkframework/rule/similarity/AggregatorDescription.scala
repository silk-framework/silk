package org.silkframework.rule.similarity

import org.silkframework.rule.OperatorExampleValues
import org.silkframework.rule.similarity.DistanceMeasureDescription._
import org.silkframework.runtime.plugin.{CustomPluginDescription, CustomPluginDescriptionGenerator}

case class AggregatorDescription(examples: OperatorExampleValues[AggregatorExampleValue]) extends CustomPluginDescription {

  def generateDocumentation(sb: StringBuilder): Unit = {
    examples.markdownFormatted(sb)
  }

}

class AggregatorDescriptionGenerator extends CustomPluginDescriptionGenerator {

  override def generate(pluginClass: Class[_]): Option[CustomPluginDescription] = {
    Some(AggregatorDescription(OperatorExampleValues(AggregatorExampleValue.retrieve(pluginClass))))
  }
}
