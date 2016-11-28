package controllers.transform

import org.silkframework.rule.TransformSpec
import org.silkframework.util.{DPair, Uri}
import org.silkframework.workspace.User
import org.silkframework.workspace.activity.transform.{TransformPathsCache, VocabularyCache}
import play.api.mvc.{Action, Controller}
import plugins.Context

class TransformEditor extends Controller {

  def start(project: String, task: String) = Action { implicit request =>
    val context = Context.get[TransformSpec](project, task, request.path)
    val vocabularies = context.task.activity[VocabularyCache].value

    Ok(views.html.editor.transformRules(context, vocabularies))
  }

  def editor(project: String, task: String, rule: String) = Action { implicit request =>
    val context = Context.get[TransformSpec](project, task, request.path)
    context.task.data.rules.find(_.name == rule) match {
      case Some(r) => Ok(views.html.editor.transformEditor(context, r))
      case None => NotFound(s"No rule named '$rule' found!. Available rules: ${context.task.data.rules.map(_.name).mkString(", ")}")
    }
  }

  def propertyDetails(project: String, task: String, property: String) = Action { implicit request =>
    val context = Context.get[TransformSpec](project, task, request.path)
    val vocabularies = context.task.activity[VocabularyCache].value
    val uri = Uri.parse(property, context.project.config.prefixes)

    Ok(views.html.editor.propertyDetails(property, vocabularies.findProperty(uri.uri)))
  }

  def paths(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)
    val pathsCache = task.activity[TransformPathsCache].control
    val prefixes = project.config.prefixes
    val sourceName = task.data.selection.inputId.toString

    if(pathsCache.status().isRunning) {
      val loadingMsg = f"Cache loading (${pathsCache.status().progress * 100}%.1f%%)"
      ServiceUnavailable(views.html.editor.paths(DPair(sourceName, ""), DPair.fill(Seq.empty), onlySource = true, loadingMsg = loadingMsg))
    } else if(pathsCache.status().failed) {
      Ok(views.html.editor.paths(DPair(sourceName, ""), DPair.fill(Seq.empty), onlySource = true, warning = pathsCache.status().message))
    } else {
      val paths = DPair(pathsCache.value().paths.map(_.serialize(prefixes)), Seq.empty)
      Ok(views.html.editor.paths(DPair(sourceName, ""), paths, onlySource = true))
    }
  }

  def score(projectName: String, taskName: String) = Action {
    Ok
  }
}
