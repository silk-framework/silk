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

    Ok.chunked(Widgets.taskStatus(stream, "cache"))
  }
  
  def sourceTypesCacheStream(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val linkingTask = project.linkingModule.task(taskName)

    val sourceTaskName = linkingTask.linkSpec.datasets.source.sourceId
    val sourceTask = project.sourceModule.task(sourceTaskName)

    val stream = Stream.taskStatus(sourceTask.cache)

    Ok.chunked(Widgets.taskStatus(stream, "sourceTypesCache"))
  }

  def targetTypesCacheStream(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val linkingTask = project.linkingModule.task(taskName)

    val sourceTaskName = linkingTask.linkSpec.datasets.target.sourceId
    val sourceTask = project.sourceModule.task(sourceTaskName)

    val stream = Stream.taskStatus(sourceTask.cache)

    Ok.chunked(Widgets.taskStatus(stream, "targetTypesCache"))
  }

  def pathCacheStream(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.linkingModule.task(taskName)

    val stream = Stream.taskStatus(task.cache.pathCache)

    Ok.chunked(Widgets.taskStatus(stream, "pathCache"))
  }

  def referenceEntitiesCacheStream(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.linkingModule.task(taskName)

    val stream = Stream.taskStatus(task.cache.referenceEntitiesCache)

    Ok.chunked(Widgets.taskStatus(stream, "referenceEntitiesCache"))
  }
}
