package controllers.linking

import models.linking.EvalLink._
import models.linking.{EvalLink, LinkSorter}
import org.silkframework.config.LinkSpecification
import org.silkframework.entity.{Entity, Link}
import org.silkframework.rule.evaluation.DetailedEvaluator
import org.silkframework.util.DPair
import org.silkframework.workspace.User
import org.silkframework.workspace.activity.linking.ReferenceEntitiesCache
import play.api.mvc.{Action, Controller}
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
    def linkSpec = task.data
    def linkageRule = linkSpec.rule
    def entities = task.activity[ReferenceEntitiesCache].value
    val linkSorter = LinkSorter.fromId(sorting)

    // Checks if a pair of entities provides values for all paths in the current linkage rule
    def hasPaths(entities: DPair[Entity]): Boolean = {
      linkSpec.entityDescriptions.source.paths.forall(entities.source.desc.paths.contains) &&
      linkSpec.entityDescriptions.target.paths.forall(entities.target.desc.paths.contains)
    }

    val links = linkType match {
      case "positive" => {
        for (link <- referenceLinks.positive.toSeq.view) yield entities.positiveLinkToEntities(link) match {
          case Some(entities) if hasPaths(entities) => {
            val evaluatedLink = DetailedEvaluator(linkageRule, entities, -1.0).get

            new EvalLink(
              link = evaluatedLink,
              correct = if (evaluatedLink.confidence.getOrElse(-1.0) >= 0.0) Correct else Incorrect,
              linkType = Positive
            )
          }
          case _ => {
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
        for (link <- referenceLinks.negative.toSeq.view) yield entities.negativeLinkToEntities(link) match {
          case Some(entities) if hasPaths(entities) => {
            val evaluatedLink = DetailedEvaluator(linkageRule, entities, -1.0).get

            new EvalLink(
              link = evaluatedLink,
              correct = if (evaluatedLink.confidence.getOrElse(-1.0) >= 0.0) Incorrect else Correct,
              linkType = Negative
            )
          }
          case _ => {
            val cleanLink = new Link(link.source, link.target)

            new EvalLink(
              link = cleanLink,
              correct = Unknown,
              linkType = Negative
            )
          }
        }
      }
      case "unlabeled" => {
        for (link <- referenceLinks.unlabeled.toSeq.view) yield entities.unlabeledLinkToEntities(link) match {
          case Some(entities) if hasPaths(entities) => {
            val evaluatedLink = DetailedEvaluator(linkageRule, entities, -1.0).get

            new EvalLink(
              link = evaluatedLink,
              correct = Unknown,
              linkType = Unlabeled
            )
          }
          case _ => {
            val cleanLink = new Link(link.source, link.target)

            new EvalLink(
              link = cleanLink,
              correct = Unknown,
              linkType = Unlabeled
            )
          }
        }
      }
    }

    Ok(views.html.widgets.linksTable(project, task, links, None, linkSorter, filter, page, showStatus = true, showDetails = true, showEntities = false, rateButtons = false))
  }

  def addLinkDialog(project: String, task: String) = Action {
    Ok(views.html.referenceLinks.addLinkDialog(project, task))
  }

  def importDialog(project: String, task: String) = Action {
    Ok(views.html.referenceLinks.importDialog(project, task))
  }

  def removeLinksDialog(project: String, task: String) = Action {
    Ok(views.html.referenceLinks.removeLinksDialog(project, task))
  }
}
