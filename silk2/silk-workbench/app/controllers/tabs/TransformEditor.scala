package controllers.tabs

import play.api.mvc.{Action, Controller}
import de.fuberlin.wiwiss.silk.workspace.User
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.evaluation.LinkageRuleEvaluator

object TransformEditor extends Controller {

  def editor(projectName: String, taskName: String, rule: Int) = Action {
    val project = User().workspace.project(projectName)
    val task = project.transformModule.task(taskName)
    Ok(views.html.editor.transformEditor(project, task, task.rules(rule)))
  }

  def paths(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.transformModule.task(taskName)
    val pathsCache = task.cache
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
