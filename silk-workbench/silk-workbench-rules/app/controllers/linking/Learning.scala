package controllers.linking

import java.util.logging.Logger

import controllers.core.{Stream, Widgets}
import models.linking.EvalLink.{Correct, Generated, Incorrect, Unknown}
import models.linking._
import org.silkframework.config.LinkSpecification
import org.silkframework.entity.{Link, Path}
import org.silkframework.learning.LearningActivity
import org.silkframework.learning.active.ActiveLearning
import org.silkframework.learning.individual.Population
import org.silkframework.rule.{LinkageRule, RuleTraverser}
import org.silkframework.rule.input.PathInput
import org.silkframework.rule.similarity.Comparison
import org.silkframework.runtime.activity.Status
import org.silkframework.runtime.activity.Status.{Idle, Finished}
import org.silkframework.util.DPair
import org.silkframework.util.Identifier._
import org.silkframework.workspace.{Task, User}
import org.silkframework.workspace.activity.linking.ReferenceEntitiesCache
import play.api.mvc.{Action, Controller}
import plugins.Context

object Learning extends Controller {

  private val log = Logger.getLogger(getClass.getName)

  def start(project: String, task: String) = Action { request =>
    val context = Context.get[LinkSpecification](project, task, request.path)
    Ok(views.html.learning.start(context))
  }

  def learn(project: String, task: String) = Action { request =>
    val context = Context.get[LinkSpecification](project, task, request.path)
    Ok(views.html.learning.learn(context))
  }

  def activeLearn(project: String, task: String) = Action { request =>
    val context = Context.get[LinkSpecification](project, task, request.path)
    Ok(views.html.learning.activeLearn(context))
  }

  def activeLearnDetails(project: String, task: String) = Action { request =>
    val context = Context.get[LinkSpecification](project, task, request.path)
    val activeLearnState = context.task.activity[ActiveLearning].value
    Ok(views.html.learning.activeLearnDetails(activeLearnState, context.project.config.prefixes))
  }

  /**
    * Iterates the active learning and selects the next link candidate
    *
    * @param project project name
    * @param task linking task name4
    * @param decision The decision for the link candidate. One of [[LinkCandidateDecision]].
    * @param linkSource source URI of the current link candidate
    * @param linkTarget target URI of the current link candidate
    */
  def activeLearnCandidate(project: String, task: String, decision: String, linkSource: String, linkTarget: String) = Action { request =>
    val context = Context.get[LinkSpecification](project, task, request.path)
    val prefixes = context.project.config.prefixes
    val activeLearn = context.task.activity[ActiveLearning].control
    val linkCandidate = new Link(linkSource, linkTarget)

    // Commit link candidate
    decision match {
      case LinkCandidateDecision.positive =>
        context.task.update(context.task.data.copy(referenceLinks = context.task.data.referenceLinks.withPositive(linkCandidate)))
      case LinkCandidateDecision.negative =>
        context.task.update(context.task.data.copy(referenceLinks = context.task.data.referenceLinks.withNegative(linkCandidate)))
      case LinkCandidateDecision.pass =>
    }

    // Assert that a learning task is running
    val finished = !activeLearn.status().isRunning
    if(finished)
      activeLearn.start()

    // Pick the next link candidate
    val links = activeLearn.value().links
    val nextLinkCandidate =
      if(links.isEmpty) {
        log.info("Selecting link candidate: No previous candidates available, waiting until learning task is finished.")
        activeLearn.waitUntilFinished()
        activeLearn.value().links.head
      } else if(finished) {
        log.info("Selecting link candidate: A learning task finished, thus selecting its top link candidate (if it hasn't been selected just before).")
        links.find(_ != linkCandidate).get
      } else if(links.last == linkCandidate) {
        log.info("Selecting link candidate: No remaining link candidates in current learning task, waiting for the next task to finish.")
        activeLearn.waitUntilFinished()
        activeLearn.value().links.head
      } else {
        val currentIndex = links.indexOf(linkCandidate)
        log.info(s"Selecting link candidate: Learning task still running, thus selecting next candidate with index ${currentIndex + 1} from list.")
        links(currentIndex + 1)
      }

    /**
      * Collects all paths of a single linkage rule.
      */
    def collectPaths(rule: LinkageRule, sourceOrTarget: Boolean): Iterator[Path] = {
      val comparisons = RuleTraverser(rule.operator.get).iterateAllChildren.filter(_.operator.isInstanceOf[Comparison])
      val inputs = comparisons.map(c => if (sourceOrTarget) c.iterateChildren.next() else c.iterateChildren.drop(1).next())
      val paths = inputs.flatMap(_.iterateAllChildren.map(_.operator)).collect { case PathInput(_, path) => path }
      paths
    }

    /**
      * Collects paths of all linkage rules in the population, sorted by frequency
      */
    def sortedPaths(sourceOrTarget: Boolean): Seq[Path] = {
      val rules = activeLearn.value().population.individuals.map(_.node.build)
      val allSourcePaths = rules.map(rule => collectPaths(rule, sourceOrTarget))
      val schemaPaths = activeLearn.value().pool.entityDescs.select(sourceOrTarget).paths
      val sortedSchemaPaths = schemaPaths.sortBy(p => allSourcePaths.count(_ == p))
      sortedSchemaPaths
    }

    def values(sourceOrTarget: Boolean) = {
      val paths = sortedPaths(sourceOrTarget)
      for(path <- paths) yield (path.serializeSimplified(prefixes), nextLinkCandidate.entities.get.select(sourceOrTarget).evaluate(path))
    }

    Ok(views.html.learning.linkCandidate(nextLinkCandidate, DPair.generate(values), context))
  }

  def rule(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)
    val referenceLinks = task.data.referenceLinks
    val population = getPopulation(task)

    Ok(views.html.learning.rule(population, referenceLinks))
  }

  def ruleStream(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)
    val stream1 = Stream.status(task.activity[LearningActivity].control.status)
    val stream2 = Stream.status(task.activity[ActiveLearning].control.status)
    Ok.chunked(Widgets.autoReload("reload", stream1 interleave stream2))
  }

  def links(projectName: String, taskName: String, sorting: String, filter: String, page: Int) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)
    val validLinks = task.activity[ActiveLearning].value.links
    def refLinks = task.data.referenceLinks
    val linkSorter = LinkSorter.fromId(sorting)

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

    Ok(views.html.widgets.linksTable(project, task, valLinks, None, linkSorter, filter, page, showStatus = true, showDetails = false, showEntities = true, rateButtons = true))
  }

  def linksStream(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)
    val stream = Stream.activityValue(task.activity[ActiveLearning].control)
    Ok.chunked(Widgets.autoReload("reload", stream))
  }

  def statusStream(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)

    val stream1 = Stream.status(task.activity[LearningActivity].control.status)
    val stream2 = Stream.status(task.activity[ActiveLearning].control.status)

    Ok.chunked(Widgets.statusStream(stream1 interleave stream2))
  }

  def population(project: String, task: String) = Action { request =>
    val context = Context.get[LinkSpecification](project, task, request.path)
    Ok(views.html.learning.population(context))
  }

  def populationView(projectName: String, taskName: String, page: Int) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)
    val population = getPopulation(task)

    val pageSize = 20
    val individuals = population.individuals.toSeq
    val sortedIndividuals = individuals.sortBy(-_.fitness)
    val pageIndividuals = sortedIndividuals.view(page * pageSize, (page + 1) * pageSize)

    Ok(views.html.learning.populationTable(projectName, taskName, pageIndividuals, task.activity[ReferenceEntitiesCache].value))
  }

  private def getPopulation(task: Task[LinkSpecification]): Population = {
    val population1 = task.activity[ActiveLearning].value.population
    val population2 = task.activity[LearningActivity].value.population
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