package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import de.fuberlin.wiwiss.silk.workbench.workspace.User
import de.fuberlin.wiwiss.silk.workbench.learning.{CurrentLearningTask}
import de.fuberlin.wiwiss.silk.learning.{LearningConfiguration, LearningInput, LearningTask}
import de.fuberlin.wiwiss.silk.learning.reproduction._
import de.fuberlin.wiwiss.silk.learning.LearningConfiguration.{Parameters, Components}
import de.fuberlin.wiwiss.silk.workbench.lift.util._
import java.util.logging.Logger

/**
 * Dialog which allows the user to configure and start a new learning task.
 */
object StartLearningDialog extends Dialog {

  override val title = "Start learning task"

  private val mode = RadioField("Mode", "", "New Linkage Rule" :: "Improve Linkage Rule" :: Nil, () => "New Linkage Rule")

  private val populationSize = IntField("Population Size", "The number of individuals in the population", 1, 10000, () => 500)

  private val iterations = IntField("Iterations", "The number of iterations to be performed", 0, 1000, () => 50)

  private val components = CheckboxesField("Components", "Which components of the link specification should be learned", "Transformations" :: "Aggregations" :: Nil, () => Set("Transformations", "Aggregations"))

  override val fields = mode :: populationSize :: iterations :: components :: Nil

  override protected def dialogParams = ("autoOpen" -> "false") :: ("width" -> "600") :: ("modal" -> "true") :: Nil

  private val logger = Logger.getLogger(getClass.getName)

  override protected def onSubmit() = {
    startNewTask()
    JS.Empty
  }

  /**
   * Starts a new learning task.
   */
  private def startNewTask() {
    val input = createInput()
    val config = createConfig()

    logger.info("Starting learning task with parameters: \n" + config)

    val task = new LearningTask(input, config)
    CurrentLearningTask() = task
    task.runInBackground()
  }

  private def createInput() = {
    LearningInput(
      trainingInstances = User().linkingTask.cache.instances,
      seedLinkageRules = if(mode.value == "Improve Linkage Rule") List(User().linkingTask.linkSpec.rule) else Nil
    )
  }

  private def createConfig() = {
    LearningConfiguration(
      components = Components(components.value.contains("Transformations"), components.value.contains("Aggregations")),
      reproduction = ReproductionConfiguration(),
      parameters = Parameters(populationSize = populationSize.value, maxIterations = iterations.value)
    )
  }
}