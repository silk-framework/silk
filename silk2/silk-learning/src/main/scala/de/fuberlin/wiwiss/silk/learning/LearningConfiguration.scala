package de.fuberlin.wiwiss.silk.learning

import reproduction.{ReproductionConfiguration}
import xml.XML
import de.fuberlin.wiwiss.silk.learning.LearningConfiguration._

case class LearningConfiguration(components: Components, reproduction: ReproductionConfiguration, parameters: Parameters)

object LearningConfiguration {

  val defaultConfigFile = "de/fuberlin/wiwiss/silk/learning/config.xml"

  def empty = load(LearningInput())

  def load(input: LearningInput) = {

    val xml = XML.load(getClass.getClassLoader.getResourceAsStream(defaultConfigFile))

    LearningConfiguration(
      components = Components(),
      reproduction = ReproductionConfiguration(),
      parameters = Parameters()
    )
  }

  case class Components(transformations: Boolean = true, aggregations: Boolean = true)

  /**
   * The parameters of the learning algorithm.
   *
   * @param maxIterations The maximum number of iterations before giving up.
   * @param maxIneffectiveIterations The maximum number of subsequent iterations without any increase in fitness before giving up.
   * @param cleanFrequency The number of iterations between two runs of the cleaning algorithm.
   * @param destinationfMeasure The desired fMeasure. The algorithm will stop after reaching it.
   */
  case class Parameters(populationSize: Int = 500,
                        maxIterations: Int = 50,
                        maxIneffectiveIterations: Int = 50,
                        cleanFrequency: Int = 5,
                        destinationfMeasure: Double = 0.999)
}