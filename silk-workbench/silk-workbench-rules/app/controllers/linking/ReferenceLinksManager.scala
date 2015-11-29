package controllers.linking

import org.silkframework.config.LinkSpecification
import org.silkframework.workspace.activity.linking.ReferenceEntitiesCache
import play.api.mvc.Controller
import play.api.mvc.Action
import org.silkframework.rule.evaluation.DetailedEvaluator
import models.linking.EvalLink
import org.silkframework.entity.Link
import models.linking.EvalLink._
import org.silkframework.workspace.User
import models.linking.LinkSorter
import plugins.Context

object ReferenceLinksManager extends Controller {

  def referenceLinksView(project: String, task: String) = Action { request =>
    val context = Context.get[LinkSpecification](project, task, request.path)
    Ok(views.html.referenceLinks.referenceLinks(context))
  }

  def referenceLinks(projectName: String, taskName: String, linkType: String, sorting: String, filter: String, page: Int) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)
    val referenceLinks = task.data.referenceLinks
    def linkageRule = task.data.rule
    def entities = task.activity[ReferenceEntitiesCache].value
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

  def addLinkDialog(project: String, task: String) = Action {
    Ok(views.html.referenceLinks.addLinkDialog(project, task))
  }

  def importDialog(project: String, task: String) = Action {
    Ok(views.html.referenceLinks.importDialog(project, task))
  }
}
