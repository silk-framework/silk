package controllers.linking

import controllers.core.{Stream, Widgets}
import models.linking.EvalLink.{Correct, Generated, Incorrect, Unknown}
import models.linking._
import org.silkframework.config.LinkSpecification
import org.silkframework.learning.LearningActivity
import org.silkframework.learning.active.ActiveLearning
import org.silkframework.learning.individual.Population
import org.silkframework.rule.{LinkageRule, RuleTraverser}
import org.silkframework.rule.input.PathInput
import org.silkframework.rule.similarity.Comparison
import org.silkframework.util.DPair
import org.silkframework.util.Identifier._
import org.silkframework.workspace.{Task, User}
import org.silkframework.workspace.activity.linking.ReferenceEntitiesCache
import play.api.mvc.{Action, Controller}
import plugins.Context

object Learning extends Controller {

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

  def activeLearnCandidate(project: String, task: String) = Action { request =>
    val context = Context.get[LinkSpecification](project, task, request.path)
    val prefixes = context.project.config.prefixes
    val activeLearnState = context.task.activity[ActiveLearning].value

    if(activeLearnState.links.isEmpty) {
      Ok("No link candidates yet")
    } else {
      val linkCandidate = activeLearnState.links.head

      def paths(rule: LinkageRule, sourceOrTarget: Boolean) = {
        val comparisons = RuleTraverser(rule.operator.get).iterateAllChildren.filter(_.operator.isInstanceOf[Comparison])
        val inputs = comparisons.map(c => if (sourceOrTarget) c.iterateChildren.next() else c.iterateChildren.drop(1).next())
        val paths = comparisons.flatMap(_.iterateChildren.next().iterateAllChildren.map(_.operator)).collect { case PathInput(_, path) => path }
        paths
      }

      def sortedValues(sourceOrTarget: Boolean): (Seq[String], Seq[Seq[String]]) = {
        val rules = activeLearnState.population.individuals.map(_.node.build)
        val allSourcePaths = rules.map(rule => paths(rule, sourceOrTarget))
        val schemaPaths = activeLearnState.pool.entityDescs.select(sourceOrTarget).paths
        val sortedSchemaPaths = schemaPaths.sortBy(p => allSourcePaths.count(_ == p))
        val values = sortedSchemaPaths.map(linkCandidate.entities.get.select(sourceOrTarget).evaluate)

        (sortedSchemaPaths.map(_.serializeSimplified(prefixes)), values)
      }

      Ok(views.html.learning.linkCandidate(linkCandidate, DPair.generate(sortedValues), context))
    }
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