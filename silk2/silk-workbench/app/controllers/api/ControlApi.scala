package controllers.api

import play.api.mvc.Action
import de.fuberlin.wiwiss.silk.config.{Dataset, RuntimeConfig}
import de.fuberlin.wiwiss.silk.workspace.User
import de.fuberlin.wiwiss.silk.execution.{EvaluateTransform, GenerateLinksTask, ExecuteTransform}
import play.api.mvc.Controller
import de.fuberlin.wiwiss.silk.learning.active.ActiveLearningTask
import java.util.logging.ConsoleHandler
import models._
import de.fuberlin.wiwiss.silk.runtime.task.{TaskFinished, TaskStatus}
import de.fuberlin.wiwiss.silk.learning.{LearningResult, LearningInput, LearningTask}
import de.fuberlin.wiwiss.silk.datasource.Source
import de.fuberlin.wiwiss.silk.linkagerule.TransformRule
import de.fuberlin.wiwiss.silk.output.Output

object ControlApi extends Controller {

  def reloadTransformCache(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.transformModule.task(taskName)
    task.cache.clear()
    task.cache.load(project, task)
    Ok
  }

  def reloadLinkingCache(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.linkingModule.task(taskName)
    task.cache.reload(project, task)
    Ok
  }

  def startGenerateLinksTask(projectName: String, taskName: String) = Action { request =>
    val project = User().workspace.project(projectName)
    val task = project.linkingModule.task(taskName)

    //Retrieve parameters
    val params = request.body.asFormUrlEncoded.getOrElse(Map.empty)
    val outputNames = params.get("outputs[]").toSeq.flatten
    val outputs = outputNames.map(project.outputModule.task(_).output)

    /** We use a custom runtime config */
    val runtimeConfig = RuntimeConfig(useFileCache = false, partitionSize = 300, generateLinksWithEntities = true)

    val generateLinksTask =
      new GenerateLinksTask(
        sources = project.sourceModule.tasks.map(_.source),
        linkSpec = task.linkSpec,
        outputs = outputs,
        runtimeConfig = runtimeConfig
      )

    CurrentGenerateLinksTask() = generateLinksTask
    generateLinksTask.runInBackground()

    Ok
  }

  def stopGenerateLinksTask(projectName: String, taskName: String) = Action {
    CurrentGenerateLinksTask().cancel()
    Ok
  }

  def executeTransformTask(projectName: String, taskName: String) = Action { request =>
    val project = User().workspace.project(projectName)
    val task = project.transformModule.task(taskName)

    // Retrieve parameters
    val params = request.body.asFormUrlEncoded.getOrElse(Map.empty)
    val outputNames = params.get("outputs[]").toSeq.flatten
    val outputs = outputNames.map(project.outputModule.task(_).output)

    // Create execution task
    val executeTransformTask =
      new ExecuteTransform(
        source = project.sourceModule.task(task.dataset.sourceId).source,
        dataset= task.dataset,
        rule = task.rule,
        outputs = outputs
      )

    // Start task in the background
    CurrentExecuteTransformTask() = executeTransformTask
    executeTransformTask.runInBackground()

    Ok
  }

  def writeReferenceLinks(projectName: String, taskName: String) = Action { request =>
    val project = User().workspace.project(projectName)
    val task = project.linkingModule.task(taskName)
    val params = request.body.asFormUrlEncoded.get

    for(posOutputName <- params.get("positiveOutput")) {
      val posOutput = project.outputModule.task(posOutputName.head).output
      posOutput.writeAll(task.referenceLinks.positive, params("positiveProperty").head)
    }

    for(negOutputName <- params.get("negativeOutput")) {
      val negOutput = project.outputModule.task(negOutputName.head).output
      negOutput.writeAll(task.referenceLinks.negative, params("negativeProperty").head)
    }

    Ok
  }
  
  def learningTask(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.linkingModule.task(taskName)
    
    //Start passive learning task
    val input =
      LearningInput(
        trainingEntities = task.cache.entities,
        seedLinkageRules = task.linkSpec.rule :: Nil
      )
    val learningTask = new LearningTask(input, CurrentConfiguration())
    CurrentLearningTask() = learningTask
    learningTask.runInBackground()
    Ok
  }
  
  def activeLearningTask(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.linkingModule.task(taskName)
    
    //Start active learning task
    val activeLearningTask =
      new ActiveLearningTask(
        config = CurrentConfiguration(),
        sources = project.sourceModule.tasks.map(_.source),
        linkSpec = task.linkSpec,
        paths = task.cache.entityDescs.map(_.paths),
        referenceEntities = task.cache.entities,
        pool = CurrentPool(),
        population = CurrentPopulation()
      )
    CurrentValidationLinks() = Nil
    CurrentActiveLearningTask() = activeLearningTask
    activeLearningTask.runInBackground()
    Ok
  }

  private val learningTaskListener = new CurrentTaskValueListener(CurrentLearningTask) {
    override def onUpdate(result: LearningResult) {
      CurrentPopulation() = result.population
    }
  }

  private val activeLearningTaskListener = new CurrentTaskStatusListener(CurrentActiveLearningTask) {
    override def onUpdate(status: TaskStatus) {
      status match {
        case _: TaskFinished => {
          CurrentPool() = task.pool
          CurrentPopulation() = task.population
          CurrentValidationLinks() = task.links
        }
        case _ =>
      }
    }
  }
}