package controllers.linking

import de.fuberlin.wiwiss.silk.config.LinkSpecification
import de.fuberlin.wiwiss.silk.dataset.Dataset
import de.fuberlin.wiwiss.silk.workspace.modules.dataset.TypesCache
import de.fuberlin.wiwiss.silk.workspace.modules.linking.{PathsCache, ReferenceEntitiesCache}
import play.api.mvc.Controller
import play.api.mvc.Action
import controllers.core.{Widgets, Stream}
import de.fuberlin.wiwiss.silk.workspace.User
import plugins.Context

object Status extends Controller {

  def status(project: String, task: String) = Action { request =>
    val context = Context.get[LinkSpecification](project, task, request.path)
    Ok(views.html.status.status(context))
  }

  def cacheStream(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)

    val stream = Stream.status(task.activity[ReferenceEntitiesCache].status)

    Ok.chunked(Widgets.statusStream(stream, "cache"))
  }
  
  def sourceTypesCacheStream(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val linkingTask = project.task[LinkSpecification](taskName)

    val sourceTaskName = linkingTask.data.datasets.source.datasetId
    val sourceTask = project.task[Dataset](sourceTaskName)

    val stream = Stream.status(sourceTask.activity[TypesCache].status)

    Ok.chunked(Widgets.statusStream(stream, "sourceTypesCache"))
  }

  def targetTypesCacheStream(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val linkingTask = project.task[LinkSpecification](taskName)

    val sourceTaskName = linkingTask.data.datasets.target.datasetId
    val sourceTask = project.task[Dataset](sourceTaskName)

    val stream = Stream.status(sourceTask.activity[TypesCache].status)

    Ok.chunked(Widgets.statusStream(stream, "targetTypesCache"))
  }

  def pathCacheStream(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)

    val stream = Stream.status(task.activity[PathsCache].status)

    Ok.chunked(Widgets.statusStream(stream, "pathCache"))
  }

  def referenceEntitiesCacheStream(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)

    val stream = Stream.status(task.activity[ReferenceEntitiesCache].status)

    Ok.chunked(Widgets.statusStream(stream, "referenceEntitiesCache"))
  }
}
