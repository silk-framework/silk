package org.silkframework.rule.input

import org.silkframework.runtime.plugin.{CustomPluginDescription, CustomPluginDescriptionGenerator, TransformExampleValue}

case class TransformerDescription(examples: Seq[TransformExampleValue]) extends CustomPluginDescription {

  def generateDocumentation(sb: StringBuilder): Unit = {
    if (examples.nonEmpty) {
      sb ++= "### Examples"
      sb ++= "\n\n"
      sb ++= "#### Notation\n\n"
      sb ++= "List of values are represented via square brackets. Example: `[first, second]` represents a list of two values \"first\" and \"second\".\n\n"
      for ((example, idx) <- examples.zipWithIndex) {
        sb ++= s"#### Example ${idx + 1}\n\n"
        sb ++= example.markdownFormatted
        sb ++= "\n\n"
      }
    }
  }
}

class TransformerDescriptionGenerator extends CustomPluginDescriptionGenerator {

  override def generate(pluginClass: Class[_]): Option[CustomPluginDescription] = {
    Some(TransformerDescription(TransformExampleValue.retrieve(pluginClass)))
  }
}

