package controllers.tabs

import play.api.mvc.Controller
import play.api.mvc.Action
import controllers.util.{Widgets, Stream}
import de.fuberlin.wiwiss.silk.workspace.User

object Status extends Controller {

  def status(projectName: String, taskName: String) = Action {
    Ok(views.html.status.status(projectName, taskName))
  }

  def cacheStream(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.linkingModule.task(taskName)

    val stream = Stream.taskStatus(task.cache)

    Ok.stream(Widgets.taskStatus(stream, "cache"))
  }

  def pathCacheStream(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.linkingModule.task(taskName)

    val stream = Stream.taskStatus(task.cache.pathCache)

    Ok.stream(Widgets.taskStatus(stream, "pathCache"))
  }

  def referenceEntitiesCacheStream(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.linkingModule.task(taskName)

    val stream = Stream.taskStatus(task.cache.referenceEntitiesCache)

    Ok.stream(Widgets.taskStatus(stream, "referenceEntitiesCache"))
  }
}
