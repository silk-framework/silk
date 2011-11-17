package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import de.fuberlin.wiwiss.silk.learning.LearningConfiguration
import de.fuberlin.wiwiss.silk.learning.reproduction._
import de.fuberlin.wiwiss.silk.learning.LearningConfiguration.{Parameters, Components}
import de.fuberlin.wiwiss.silk.workbench.lift.util._
import de.fuberlin.wiwiss.silk.workbench.learning.CurrentConfiguration

/**
 * Dialog which allows the user to configure the learning.
 */
object LearnConfigDialog extends Dialog {

  override val title = "Configuration"

  //private val mode = RadioField("Mode", "", "New Linkage Rule" :: "Improve Linkage Rule" :: Nil, () => "New Linkage Rule")

  private val populationSize = IntField("Population Size", "The number of individuals in the population", 1, 10000, () => CurrentConfiguration().parameters.populationSize)

  private val iterations = IntField("Iterations", "The number of iterations to be performed", 0, 1000, () => CurrentConfiguration().parameters.maxIterations)

  private val components = CheckboxesField("Components", "Which components of the link specification should be learned", "Transformations" :: "Aggregations" :: Nil, () => Set("Transformations", "Aggregations"))

  override val fields = populationSize :: iterations :: Nil

  override protected def dialogParams = ("autoOpen" -> "false") :: ("width" -> "600") :: ("modal" -> "true") :: Nil

  override protected def onSubmit() = {
    CurrentConfiguration() = createConfig()
    JS.Empty
  }

  private def createConfig() = {
    LearningConfiguration(
      components = Components(components.value.contains("Transformations"), components.value.contains("Aggregations")),
      reproduction = ReproductionConfiguration(),
      parameters = Parameters(populationSize = populationSize.value, maxIterations = iterations.value)
    )
  }
}