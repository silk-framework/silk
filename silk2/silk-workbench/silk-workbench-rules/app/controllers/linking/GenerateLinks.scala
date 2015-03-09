package controllers.linking

import de.fuberlin.wiwiss.silk.config.LinkSpecification
import de.fuberlin.wiwiss.silk.dataset.Dataset
import de.fuberlin.wiwiss.silk.entity.Link
import de.fuberlin.wiwiss.silk.execution.{GenerateLinks => GenerateLinksActivity}
import de.fuberlin.wiwiss.silk.runtime.activity.Activity
import de.fuberlin.wiwiss.silk.workspace.User
import de.fuberlin.wiwiss.silk.linkagerule.evaluation.DetailedEvaluator
import controllers.core.{Stream, Widgets}
import play.api.mvc.{Controller, Action}
import models.linking.{LinkSorter, EvalLink}
import models.linking.EvalLink.{Unknown, Incorrect, Generated, Correct}
import plugins.Context

object GenerateLinks extends Controller {

  def generateLinks(project: String, task: String) = Action { request =>
    val context = Context.get[LinkSpecification](project, task, request.path)
    Ok(views.html.generateLinks.generateLinks(context))
  }

  def generateLinksDialog(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val outputs = project.tasks[Dataset].toSeq.map(_.name.toString())

    Ok(views.html.generateLinks.generateLinksDialog(projectName, taskName, outputs))
  }

  def links(projectName: String, taskName: String, sorting: String, filter: String, page: Int) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)
    val referenceLinks = task.data.referenceLinks
    val linkSorter = LinkSorter.fromId(sorting)
    val generatedLinks = task.activity[GenerateLinksActivity].value()

    println("LINKS LINKS LINKS: " + generatedLinks.size)

    def links =
      for (link <- generatedLinks.view;
           detailedLink <- DetailedEvaluator(task.data.rule, link.entities.get)) yield {
        if (referenceLinks.positive.contains(link))
          new EvalLink(detailedLink, Correct, Generated)
        else if (referenceLinks.negative.contains(link))
          new EvalLink(detailedLink, Incorrect, Generated)
        else
          new EvalLink(detailedLink, Unknown, Generated)
      }

    Ok(views.html.widgets.linksTable(project, task, links, linkSorter, filter, page, showStatus = false, showDetails = true, showEntities = false, rateButtons = true))
  }

  def linksStream(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)
    val stream = Stream.activityValue(task.activity[GenerateLinksActivity])
    Ok.chunked(Widgets.autoReload("updateLinks", stream))
  }

  def statusStream(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)
    val stream = Stream.activityStatus(task.activity[GenerateLinksActivity])
    Ok.chunked(Widgets.status(stream))
  }

}
