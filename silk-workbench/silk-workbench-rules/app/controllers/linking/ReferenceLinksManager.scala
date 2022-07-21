package controllers.linking

import config.WorkbenchConfig.WorkspaceReact
import controllers.core.UserContextActions
import models.linking.EvalLink._
import models.linking.{EvalLink, LinkResolver, LinkSorter}
import org.silkframework.entity.{Entity, MinimalLink}
import org.silkframework.rule.LinkSpec
import org.silkframework.rule.evaluation.DetailedEvaluator
import org.silkframework.util.DPair
import org.silkframework.workbench.Context
import org.silkframework.workspace.WorkspaceFactory
import org.silkframework.workspace.activity.linking.ReferenceEntitiesCache
import play.api.mvc.{Action, AnyContent, InjectedController}

import javax.inject.Inject

class ReferenceLinksManager @Inject() (implicit workspaceReact: WorkspaceReact) extends InjectedController with UserContextActions {

  def referenceLinksView(project: String, task: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val context = Context.get[LinkSpec](project, task, request.path)
    Ok(views.html.referenceLinks.referenceLinks(context))
  }

  def referenceLinks(projectName: String,
                     taskName: String,
                     linkType: String,
                     sorting: String,
                     filter: String,
                     page: Int): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[LinkSpec](taskName)
    val referenceLinks = task.data.referenceLinks
    def linkSpec = task.data
    def linkageRule = linkSpec.rule
    def entities = task.activity[ReferenceEntitiesCache].value()
    val linkSorter = LinkSorter.fromId(sorting)
    val linkResolvers = LinkResolver.forLinkingTask(task)

    // Checks if a pair of entities provides values for all paths in the current linkage rule
    def hasPaths(entities: DPair[Entity]): Boolean = {
      linkSpec.entityDescriptions.source.typedPaths.forall(entities.source.schema.typedPaths.contains) &&
      linkSpec.entityDescriptions.target.typedPaths.forall(entities.target.schema.typedPaths.contains)
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
            val cleanLink = new MinimalLink(link.source, link.target)

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
            val cleanLink = new MinimalLink(link.source, link.target)

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
            val cleanLink = new MinimalLink(link.source, link.target)

            new EvalLink(
              link = cleanLink,
              correct = Unknown,
              linkType = Unlabeled
            )
          }
        }
      }
    }

    Ok(views.html.widgets.linksTable(project, task, links, None, linkResolvers, linkSorter, filter, page, showStatus = true, showDetails = true, showEntities = false, rateButtons = false))
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
