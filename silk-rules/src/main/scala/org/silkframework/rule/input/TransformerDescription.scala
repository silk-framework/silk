package org.silkframework.rule.input

import org.silkframework.rule.OperatorExampleValues
import org.silkframework.runtime.plugin.{CustomPluginDescription, CustomPluginDescriptionGenerator}

case class TransformerDescription(examples: OperatorExampleValues[TransformExampleValue]) extends CustomPluginDescription {

  def generateDocumentation(sb: StringBuilder): Unit = {
    examples.markdownFormatted(sb)
  }
}

class TransformerDescriptionGenerator extends CustomPluginDescriptionGenerator {

  override def generate(pluginClass: Class[_]): Option[CustomPluginDescription] = {
    Some(TransformerDescription(OperatorExampleValues(TransformExampleValue.retrieve(pluginClass))))
  }
}

