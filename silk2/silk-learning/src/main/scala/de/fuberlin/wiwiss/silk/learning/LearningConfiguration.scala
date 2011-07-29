package de.fuberlin.wiwiss.silk.learning

import crossover.{CrossoverConfiguration}
import generation.GenerationConfiguration
import xml.XML
import de.fuberlin.wiwiss.silk.evaluation.ReferenceInstances

case class LearningConfiguration(generation: GenerationConfiguration, crossover: CrossoverConfiguration)

object LearningConfiguration
{
  val defaultConfigFile = "de/fuberlin/wiwiss/silk/learning/config.xml"

  def load(instances : ReferenceInstances) = {

    val xml = XML.load(getClass.getClassLoader.getResourceAsStream(defaultConfigFile))

    LearningConfiguration(
      generation = GenerationConfiguration.fromXml(xml \ "GenerationConfiguration" head, instances),
      crossover = CrossoverConfiguration.fromXml(xml \ "CrossoverConfiguration" head)
    )
  }
}