package controllers.tabs

import de.fuberlin.wiwiss.silk.workspace.modules.linking.LinkingTask
import play.api.mvc.Controller
import play.api.mvc.Action
import controllers.util.{Widgets, Stream}
import de.fuberlin.wiwiss.silk.workspace.User
import plugins.Context

object Status extends Controller {

  def status(project: String, task: String) = Action { request =>
    val context = Context.get[LinkingTask](project, task, request.path)
    Ok(views.html.status.status(context))
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
