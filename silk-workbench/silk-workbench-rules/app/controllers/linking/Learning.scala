package controllers.linking

import akka.stream.Materializer
import controllers.core.UserContextActions
import models.learning.{PathValue, PathValues}
import models.linking.EvalLink.{Correct, Generated, Incorrect, Unknown}
import models.linking._
import org.silkframework.config.Prefixes
import org.silkframework.learning.LearningActivity
import org.silkframework.learning.active.ActiveLearning
import org.silkframework.learning.individual.Population
import org.silkframework.rule.LinkSpec
import org.silkframework.rule.evaluation.ReferenceLinks
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.util.CollectionUtils.ExtendedSeq
import org.silkframework.util.Identifier._
import org.silkframework.workbench.Context
import org.silkframework.workbench.utils.ErrorResult
import org.silkframework.workspace.activity.linking.ReferenceEntitiesCache
import org.silkframework.workspace.{ProjectTask, WorkspaceFactory}
import play.api.mvc.{Action, AnyContent, InjectedController}

import java.util.logging.Logger
import javax.inject.Inject

class Learning @Inject() (implicit mat: Materializer) extends InjectedController with UserContextActions {

  private val log = Logger.getLogger(getClass.getName)

  def start(project: String, task: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val context = Context.get[LinkSpec](project, task, request.path)
    Ok(views.html.learning.start(context))
  }

  def learn(project: String, task: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val context = Context.get[LinkSpec](project, task, request.path)
    Ok(views.html.learning.learn(context))
  }

  def activeLearn(project: String, task: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val context = Context.get[LinkSpec](project, task, request.path)
    Ok(views.html.learning.activeLearn(context))
  }

  def activeLearnDetails(project: String, task: String): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    val context = Context.get[LinkSpec](project, task, request.path)
    val activeLearnState = context.task.activity[ActiveLearning].value()
    Ok(views.html.learning.activeLearnDetails(activeLearnState, context.project.config.prefixes))
  }

  def activeLearnCandidate(project: String, task: String): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    val context = Context.get[LinkSpec](project, task, request.path)
    implicit val prefixes: Prefixes = context.project.config.prefixes
    val activeLearn = context.task.activity[ActiveLearning].control

    request.body.asFormUrlEncoded match {
      case Some(p) =>
        val params = p.mapValues(_.head)
        val nextLinkCandidate = ActiveLearningIterator.nextActiveLearnCandidate(params("decision"), params("source"), params("target"), context.task)
        nextLinkCandidate match {
          case Some(link) =>
            val comparisonPaths = activeLearn.value().comparisonPaths
            // Generate all source values for this link
            val sourceValues =
              for(path <- comparisonPaths.map(_.source).distinctBy(_.normalizedSerialization)) yield {
                PathValues(path.serialize(), link.sourceEntity.evaluate(path).map(PathValue(_)))
              }
            // Generate all target values for this link
            val targetValues =
              for(path <- comparisonPaths.map(_.target).distinctBy(_.normalizedSerialization)) yield {
                PathValues(path.serialize(), link.targetEntity.evaluate(path).map(PathValue(_)))
              }
            // Find matching values for highlighting
            addHighlighting(sourceValues, targetValues)

            Ok(views.html.learning.linkCandidate(link, sourceValues, targetValues, context))
          case None =>
            Ok("No link candidate generated, please wait for completion or restart...")
        }
      case None =>
        ErrorResult(BadUserInputException("query parameters missing"))
    }
  }

  private def addHighlighting(sourceValues: Seq[PathValues], targetValues: Seq[PathValues]): Unit = {
    var currentIndex = 1
    for(sourcePath <- sourceValues;
        sourceValue <- sourcePath.values;
        targetPath <- targetValues;
        targetValue <- targetPath.values) {
      if(sourceValue.value == targetValue.value && currentIndex <= 5) {
        // Check if this value already got an index
        sourceValues.flatMap(_.values).find(_.value == sourceValue.value).flatMap(_.similarityClass) match {
          case Some(index) =>
            sourceValue.similarityClass = Some(index)
            targetValue.similarityClass = Some(index)
          case None if currentIndex <= 5 =>
            sourceValue.similarityClass = Some(currentIndex)
            targetValue.similarityClass = Some(currentIndex)
            currentIndex += 1
          case None =>
        }
      }
    }
  }

  /**
    * Renders the top linkage rule in the current population.
    */
  def rule(projectName: String, taskName: String): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    val context = Context.get[LinkSpec](projectName, taskName, request.path)
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[LinkSpec](taskName)
    val referenceLinks = task.data.referenceLinks
    val population = getPopulation(task)


    Ok(views.html.learning.rule(population, referenceLinks, context))
  }

  /**
    * Shows the dialog for resetting the active learning activity.
    */
  def resetActiveLearningDialog(projectName: String, taskName: String): Action[AnyContent] = Action {
    Ok(views.html.learning.resetDialog(projectName, taskName))
  }

  /**
    * Resets the active learning activity.
    */
  def resetActiveLearning(projectName: String, taskName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[LinkSpec](taskName)

    // Reset reference links
    task.activity[ReferenceEntitiesCache].control.reset()
    task.update(task.data.copy(referenceLinks = ReferenceLinks.empty))

    // Restart active learning activity
    val activeLearning = task.activity[ActiveLearning]
    activeLearning.control.cancel()
    activeLearning.control.waitUntilFinished()
    activeLearning.control.reset()
    activeLearning.start(Map("fixedRandomSeed" -> "false"))

    Ok
  }

  def links(projectName: String, taskName: String, sorting: String, filter: String, page: Int): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[LinkSpec](taskName)
    val validLinks = task.activity[ActiveLearning].value().links
    def refLinks = task.data.referenceLinks
    val linkSorter = LinkSorter.fromId(sorting)
    val linkResolvers = LinkResolver.forLinkingTask(task)

    val valLinks = {
      for (link <- validLinks.view) yield {
        if (refLinks.positive.contains(link))
          new EvalLink(link, Correct, Generated)
        else if (refLinks.negative.contains(link))
          new EvalLink(link, Incorrect, Generated)
        else
          new EvalLink(link, Unknown, Generated)
      }
    }

    Ok(views.html.widgets.linksTable(project, task, valLinks, None, linkResolvers, linkSorter, filter, page, showStatus = true, showDetails = false, showEntities = true, rateButtons = true))
  }

  def population(project: String, task: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val context = Context.get[LinkSpec](project, task, request.path)
    Ok(views.html.learning.population(context))
  }

  def populationView(projectName: String, taskName: String, page: Int): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[LinkSpec](taskName)
    val population = getPopulation(task)

    val pageSize = 20
    val individuals = population.individuals
    val sortedIndividuals = individuals.sortBy(-_.fitness)
    val pageIndividuals = sortedIndividuals.view(page * pageSize, (page + 1) * pageSize)

    Ok(views.html.learning.populationTable(projectName, taskName, pageIndividuals, task.activity[ReferenceEntitiesCache].value()))
  }

  private def getPopulation(task: ProjectTask[LinkSpec]): Population = {
    val population1 = task.activity[ActiveLearning].value().population
    val population2 = task.activity[LearningActivity].value().population
    if(population1.isEmpty)
      population2
    else if(population2.isEmpty)
      population1
    else if(population1.bestIndividual.fitness >= population2.bestIndividual.fitness)
      population1
    else
      population2
  }
}