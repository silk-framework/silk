package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import de.fuberlin.wiwiss.silk.workbench.workspace.User
import de.fuberlin.wiwiss.silk.workbench.learning.{CurrentLearningTask}
import de.fuberlin.wiwiss.silk.learning.{LearningConfiguration, LearningInput, LearningTask}
import de.fuberlin.wiwiss.silk.learning.reproduction._
import de.fuberlin.wiwiss.silk.workbench.lift.util.{IntField, BooleanField, Dialog}
import de.fuberlin.wiwiss.silk.learning.LearningConfiguration.{Parameters, Components}

/**
 * Dialog which allows the user to configure and start a new learning task.
 */
object StartLearningDialog extends Dialog {

  override val title = "Start learning task"

  private val improveExisting = BooleanField("Improve existing rule", "")

  //private val iterations = IntField("Iterations", "The number of iterations to be performed")

  private val learnTransformations = BooleanField("Learn Transformations", "Learn transformations", () => true)

  private val learnAggregations = BooleanField("Learn Aggregations", "Learn aggregations", () => true)

  override val fields = improveExisting :: learnTransformations :: learnAggregations :: Nil

  override protected def onSubmit() {
    startNewTask()
  }

  /**
   * Starts a new learning task.
   */
  private def startNewTask() {
    val task = new LearningTask(createInput(), createConfig())
    CurrentLearningTask() = task
    task.runInBackground()
  }

  private def createInput() = {
    LearningInput(
      trainingInstances = User().linkingTask.cache.instances,
      seedConditions = if(improveExisting.value) List(User().linkingTask.linkSpec.condition) else Nil
    )
  }

  private def createConfig() = {
    LearningConfiguration(
      components = Components(learnTransformations.value, learnAggregations.value),
      reproduction = ReproductionConfiguration(),
      parameters = Parameters(maxIterations = 50)
    )
  }
}