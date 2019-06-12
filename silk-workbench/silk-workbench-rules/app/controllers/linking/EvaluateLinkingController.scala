package controllers.linking

import akka.actor.ActorSystem
import akka.stream.Materializer
import controllers.core.{RequestUserContextAction, UserContextAction}
import controllers.util.AkkaUtils
import javax.inject.Inject
import models.linking.EvalLink.{Correct, Generated, Incorrect, Unknown}
import models.linking.{EvalLink, LinkSorter}
import org.silkframework.rule.LinkSpec
import org.silkframework.rule.evaluation.DetailedEvaluator
import org.silkframework.runtime.activity.UserContext
import org.silkframework.workbench.Context
import org.silkframework.workspace.WorkspaceFactory
import org.silkframework.workspace.activity.linking.EvaluateLinkingActivity
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, InjectedController, WebSocket}

class EvaluateLinkingController @Inject() (implicit system: ActorSystem, mat: Materializer) extends InjectedController {

  def generateLinks(project: String, task: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val context = Context.get[LinkSpec](project, task, request.path)
    Ok(views.html.evaluateLinking.evaluateLinking(context))
  }

  def links(projectName: String, taskName: String, sorting: String, filter: String, page: Int): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[LinkSpec](taskName)
    val linkSorter = LinkSorter.fromId(sorting)
    val linking = task.activity[EvaluateLinkingActivity].value()
    val schemata = task.data.entityDescriptions

    // We only show links if entities have been attached to them. We check this by looking at the first link.
    val showLinks = {
      linking.links.headOption.flatMap(_.entities) match {
        case Some(entities) =>
          // Check if the entities got all paths that are used in the linkage rule
          schemata.source.typedPaths.forall(entities.source.schema.typedPaths.contains) &&
          schemata.target.typedPaths.forall(entities.target.schema.typedPaths.contains)
        case None => false
      }
    }

    if(showLinks) {
      val referenceLinks = task.data.referenceLinks
      def links =
        for (link <- linking.links.view) yield {
          val detailedLink = DetailedEvaluator(task.data.rule, link.entities.get)
          if (referenceLinks.positive.contains(link))
            new EvalLink(detailedLink, Correct, Generated)
          else if (referenceLinks.negative.contains(link))
            new EvalLink(detailedLink, Incorrect, Generated)
          else
            new EvalLink(detailedLink, Unknown, Generated)
        }
      Ok(views.html.widgets.linksTable(project, task, links, Some(linking.statistics), linkSorter, filter, page,
        showStatus = false, showDetails = true, showEntities = false, rateButtons = true))
    } else {
      // Show an empty links table
      Ok(views.html.widgets.linksTable(project, task, Seq[EvalLink](), Some(linking.statistics), linkSorter, filter,
        page, showStatus = false, showDetails = true, showEntities = false, rateButtons = true))
    }
  }

  def linksWebsocket(projectName: String, taskName: String): WebSocket = {
    implicit val userContext = UserContext.Empty
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[LinkSpec](taskName)
    val activity = task.activity[EvaluateLinkingActivity]

    // Create a source that sends an empty object on every update
    val source = AkkaUtils.createSource(activity.value).map(_ => Json.obj())
    AkkaUtils.createWebSocket(source)
  }
}
