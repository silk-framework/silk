/*
 * Copyright 2011 Freie Universität Berlin, MediaEvent Services GmbH & Co. KG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

  override val title = "Learning Configuration"

  private val mode_newRule = "New Linkage Rule"

  private val mode_improveRule = "Improve Linkage Rule"

  private val mode = RadioField("Mode", "", mode_newRule :: mode_improveRule :: Nil, () => if(CurrentConfiguration().params.seed) mode_improveRule else mode_newRule)

  private val populationSize = IntField("Population Size", "The number of individuals in the population", 1, 10000, () => CurrentConfiguration().params.populationSize)

  private val iterations = IntField("Iterations", "The number of iterations to be performed", 0, 1000, () => CurrentConfiguration().params.maxIterations)

  private val components = CheckboxesField("Components", "Which components of the link specification should be learned", "Transformations" :: "Aggregations" :: Nil, () => Set("Transformations", "Aggregations"))

  override val fields = mode :: populationSize :: iterations :: Nil

  override protected def dialogParams = ("autoOpen" -> "false") :: ("width" -> "600") :: ("modal" -> "true") :: Nil

  override protected def onSubmit() = {
    CurrentConfiguration() = createConfig()
    JS.Empty
  }

  private def createConfig() = {
    LearningConfiguration(
      components = Components(components.value.contains("Transformations"), components.value.contains("Aggregations")),
      reproduction = ReproductionConfiguration(),
      params = Parameters(seed = (mode.value == mode_improveRule), populationSize = populationSize.value, maxIterations = iterations.value)
    )
  }
}