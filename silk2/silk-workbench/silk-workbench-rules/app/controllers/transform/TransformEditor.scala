package controllers.transform

import de.fuberlin.wiwiss.silk.config.TransformSpecification
import de.fuberlin.wiwiss.silk.workspace.modules.transform.PathsCache
import play.api.mvc.{Action, Controller}
import de.fuberlin.wiwiss.silk.workspace.User
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.evaluation.LinkageRuleEvaluator
import plugins.Context

object TransformEditor extends Controller {

  def start(project: String, task: String) = Action { request =>
    val context = Context.get[TransformSpecification](project, task, request.path)
    Ok(views.html.editor.transformRules(context))
  }

  def editor(project: String, task: String, rule: String) = Action { request =>
    val context = Context.get[TransformSpecification](project, task, request.path)
    context.task.data.rules.find(_.name == rule) match {
      case Some(r) => Ok(views.html.editor.transformEditor(context, r))
      case None => NotFound(s"No rule named '$rule' found!")
    }
  }

  def paths(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[TransformSpecification](taskName)
    val pathsCache = task.cache[PathsCache]
    val prefixes = project.config.prefixes

    if(pathsCache.status.isRunning) {
      val loadingMsg = f"Cache loading (${pathsCache.status.progress * 100}%.1f%%)"
      ServiceUnavailable(views.html.editor.paths(DPair.fill(Seq.empty), onlySource = true, loadingMsg = loadingMsg))
    } else if(pathsCache.status.failed) {
      Ok(views.html.editor.paths(DPair.fill(Seq.empty), onlySource = true, warning = pathsCache.status.message))
    } else {
      val paths = DPair(pathsCache.value.paths.map(_.serialize(prefixes)), Seq.empty)
      Ok(views.html.editor.paths(paths, onlySource = true))
    }
  }

  def score(projectName: String, taskName: String) = Action {
    Ok
  }
}
