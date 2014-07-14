package controllers.tabs

import de.fuberlin.wiwiss.silk.workspace.modules.linking.LinkingTask
import play.api.mvc.Controller
import play.api.mvc.Action
import de.fuberlin.wiwiss.silk.linkagerule.evaluation.DetailedEvaluator
import models.EvalLink
import de.fuberlin.wiwiss.silk.entity.Link
import models.EvalLink._
import de.fuberlin.wiwiss.silk.workspace.User
import models.LinkSorter
import plugins.Context

object ReferenceLinks extends Controller {

  def referenceLinksView(project: String, task: String) = Action { request =>
    val context = Context.get[LinkingTask](project, task, request.path)
    Ok(views.html.referenceLinks.referenceLinks(context))
  }

  def referenceLinks(projectName: String, taskName: String, linkType: String, sorting: String, filter: String, page: Int) = Action {
    val project = User().workspace.project(projectName)
    val task = project.linkingModule.task(taskName)
    val referenceLinks = task.referenceLinks
    def linkageRule = task.linkSpec.rule
    def entities = task.cache.entities
    val linkSorter = LinkSorter.fromId(sorting)

    val links = linkType match {
      case "positive" => {
        for (link <- referenceLinks.positive.toSeq.view) yield entities.positive.get(link) match {
          case Some(entities) => {
            val evaluatedLink = DetailedEvaluator(linkageRule, entities, -1.0).get

            new EvalLink(
              link = evaluatedLink,
              correct = if (evaluatedLink.confidence.getOrElse(-1.0) >= 0.0) Correct else Incorrect,
              linkType = Positive
            )
          }
          case None => {
            val cleanLink = new Link(link.source, link.target)

            new EvalLink(
              link = cleanLink,
              correct = Unknown,
              linkType = Positive
            )
          }
        }
      }
      case "negative" => {
        for (link <- referenceLinks.negative.toSeq.view) yield entities.negative.get(link) match {
          case Some(entities) => {
            val evaluatedLink = DetailedEvaluator(linkageRule, entities, -1.0).get

            new EvalLink(
              link = evaluatedLink,
              correct = if (evaluatedLink.confidence.getOrElse(-1.0) >= 0.0) Incorrect else Correct,
              linkType = Negative
            )
          }
          case None => {
            val cleanLink = new Link(link.source, link.target)

            new EvalLink(
              link = cleanLink,
              correct = Unknown,
              linkType = Negative
            )
          }
        }
      }
    }

    Ok(views.html.widgets.linksTable(project, task, links, linkSorter, filter, page, showStatus = true, showDetails = true, showEntities = false, rateButtons = false))
  }

  def importDialog(project: String, task: String) = Action {
    Ok(views.html.referenceLinks.importDialog(project, task))
  }

  def exportDialog(project: String, task: String) = Action {
    Ok(views.html.referenceLinks.exportDialog(project, task))
  }
}
