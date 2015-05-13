package controllers.linking

import controllers.core.{Stream, Widgets}
import de.fuberlin.wiwiss.silk.config.LinkSpecification
import de.fuberlin.wiwiss.silk.learning.LearningActivity
import de.fuberlin.wiwiss.silk.learning.active.ActiveLearning
import de.fuberlin.wiwiss.silk.util.Identifier._
import de.fuberlin.wiwiss.silk.workspace.User
import de.fuberlin.wiwiss.silk.workspace.modules.linking.ReferenceEntitiesCache
import models.linking.EvalLink.{Correct, Generated, Incorrect, Unknown}
import models.linking._
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

  def rule(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)
    val referenceLinks = task.data.referenceLinks
    val population = task.activity[LearningActivity].value().population

    Ok(views.html.learning.rule(population, referenceLinks))
  }

  def ruleStream(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)
    val stream = Stream.activityValue(task.activity[ActiveLearning])
    Ok.chunked(Widgets.autoReload("reload", stream))
  }

  def links(projectName: String, taskName: String, sorting: String, filter: String, page: Int) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)
    val validLinks = task.activity[ActiveLearning].value().links
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
    }.sortBy(_.confidence.get.abs)

    Ok(views.html.widgets.linksTable(project, task, valLinks, linkSorter, filter, page, showStatus = true, showDetails = false, showEntities = true, rateButtons = true))
  }

  def linksStream(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)
    val stream = Stream.activityValue(task.activity[ActiveLearning])
    Ok.chunked(Widgets.autoReload("updateLinks", stream))
  }

  def statusStream(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)

    val stream1 = Stream.status(task.activity[LearningActivity].status)
    val stream2 = Stream.status(task.activity[ActiveLearning].status)

    Ok.chunked(Widgets.statusStream(stream1 interleave stream2))
  }

  def population(project: String, task: String) = Action { request =>
    val context = Context.get[LinkSpecification](project, task, request.path)
    Ok(views.html.learning.population(context))
  }

  def populationView(projectName: String, taskName: String, page: Int) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)
    val population = task.activity[LearningActivity].value().population

    val pageSize = 20
    val individuals = population.individuals.toSeq
    val sortedIndividuals = individuals.sortBy(-_.fitness)
    val pageIndividuals = sortedIndividuals.view(page * pageSize, (page + 1) * pageSize)

    Ok(views.html.learning.populationTable(projectName, taskName, pageIndividuals, task.activity[ReferenceEntitiesCache].value()))
  }
}