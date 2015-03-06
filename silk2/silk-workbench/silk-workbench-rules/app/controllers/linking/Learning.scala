package controllers.linking

import controllers.core.{Stream, Widgets}
import de.fuberlin.wiwiss.silk.config.LinkSpecification
import de.fuberlin.wiwiss.silk.runtime.oldtask.{TaskFinished, TaskStatus}
import de.fuberlin.wiwiss.silk.util.Identifier._
import de.fuberlin.wiwiss.silk.workspace.User
import de.fuberlin.wiwiss.silk.workspace.modules.linking.LinkingCaches
import models.CurrentTaskStatusListener
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
    val population = CurrentPopulation()

    Ok(views.html.learning.rule(population, referenceLinks))
  }

  def ruleStream(projectName: String, taskName: String) = Action {
    val stream = Stream.taskData(CurrentPopulation)
    Ok.chunked(Widgets.autoReload("reload", stream))
  }

  def links(projectName: String, taskName: String, sorting: String, filter: String, page: Int) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)
    def refLinks = task.data.referenceLinks
    val linkSorter = LinkSorter.fromId(sorting)

    val valLinks = {
      for (link <- CurrentValidationLinks().view) yield {
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
    val stream = Stream.taskData(CurrentValidationLinks)
    Ok.chunked(Widgets.autoReload("updateLinks", stream))
  }

  def statusStream(project: String, task: String) = Action {
    val stream1 = Stream.currentTaskStatus(CurrentLearningTask)
    val stream2 = Stream.currentTaskStatus(CurrentActiveLearningTask)

    Ok.chunked(Widgets.taskStatus(stream1 interleave stream2))
  }

  def population(project: String, task: String) = Action { request =>
    val context = Context.get[LinkSpecification](project, task, request.path)
    Ok(views.html.learning.population(context))
  }

  def populationView(projectName: String, taskName: String, page: Int) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)

    val pageSize = 20
    val individuals = CurrentPopulation().individuals.toSeq
    val sortedIndividuals = individuals.sortBy(-_.fitness)
    val pageIndividuals = sortedIndividuals.view(page * pageSize, (page + 1) * pageSize)

    Ok(views.html.learning.populationTable(projectName, taskName, pageIndividuals, task.cache[LinkingCaches].entities))
  }

  /**
   * Listens to changes of the current active learning task.
   */
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