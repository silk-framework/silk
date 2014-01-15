package controllers.tabs

import play.api.mvc.Controller
import play.api.mvc.Action
import de.fuberlin.wiwiss.silk.workspace.User
import de.fuberlin.wiwiss.silk.evaluation.LinkageRuleEvaluator

object Editor extends Controller {

  def editor(project: String, task: String) = Action {
    Ok(views.html.editor.editor(project, task))
  }

  def score(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.linkingModule.task(taskName)
    val entitiesCache = task.cache.referenceEntitiesCache

    if(entitiesCache.status.isRunning) {
      ServiceUnavailable(f"Cache loading (${entitiesCache.status.progress * 100}%.1f%%)")
    } else if(entitiesCache.status.failed) {
      Ok("Cache loading failed")
    } else if (entitiesCache.value.positive.isEmpty || entitiesCache.value.negative.isEmpty) {
      Ok("No reference links")
    } else {
      val result = LinkageRuleEvaluator(task.linkSpec.rule, entitiesCache.value)
      Ok(f"Precision: ${result.precision}%.2f | Recall: ${result.recall}%.2f | F-measure: ${result.fMeasure}%.2f")
    }
  }

}
