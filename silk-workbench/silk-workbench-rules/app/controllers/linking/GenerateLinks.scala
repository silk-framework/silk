package controllers.linking

import controllers.core.{Stream, Widgets}
import org.silkframework.config.LinkSpecification
import org.silkframework.dataset.Dataset
import org.silkframework.execution.{GenerateLinks => GenerateLinksActivity}
import org.silkframework.rule.evaluation.DetailedEvaluator
import org.silkframework.workspace.User
import models.linking.EvalLink.{Correct, Generated, Incorrect, Unknown}
import models.linking.{EvalLink, LinkSorter}
import play.api.mvc.{Action, Controller}
import plugins.Context

object GenerateLinks extends Controller {

  def generateLinks(project: String, task: String) = Action { request =>
    val context = Context.get[LinkSpecification](project, task, request.path)
    Ok(views.html.generateLinks.generateLinks(context))
  }

  def generateLinksDialog(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val outputs = project.tasks[Dataset].map(_.name.toString())

    Ok(views.html.generateLinks.generateLinksDialog(projectName, taskName, outputs))
  }

  def links(projectName: String, taskName: String, sorting: String, filter: String, page: Int) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)
    val linkSorter = LinkSorter.fromId(sorting)
    val linking = task.activity[GenerateLinksActivity].value

    // We only show links if entities have been attached to them. We check this by looking at the first link.
    if(linking.links.headOption.exists(_.entities.nonEmpty)) {
      val referenceLinks = task.data.referenceLinks
      def links =
        for (link <- linking.links.view;
             detailedLink <- DetailedEvaluator(task.data.rule, link.entities.get)) yield {
          if (referenceLinks.positive.contains(link))
            new EvalLink(detailedLink, Correct, Generated)
          else if (referenceLinks.negative.contains(link))
            new EvalLink(detailedLink, Incorrect, Generated)
          else
            new EvalLink(detailedLink, Unknown, Generated)
        }
      Ok(views.html.widgets.linksTable(project, task, links, Some(linking.statistics), linkSorter, filter, page, showStatus = false, showDetails = true, showEntities = false, rateButtons = true))
    } else {
      // Show an empty links table
      Ok(views.html.widgets.linksTable(project, task, Seq[EvalLink](), Some(linking.statistics), linkSorter, filter, page, showStatus = false, showDetails = true, showEntities = false, rateButtons = true))
    }
  }

  def linksStream(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)
    val stream = Stream.activityValue(task.activity[GenerateLinksActivity].control)
    Ok.chunked(Widgets.autoReload("updateLinks", stream))
  }

  def statusStream(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)
    val stream = Stream.status(task.activity[GenerateLinksActivity].control.status)
    Ok.chunked(Widgets.statusStream(stream))
  }

}
