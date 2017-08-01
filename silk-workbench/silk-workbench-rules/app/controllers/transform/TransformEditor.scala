package controllers.transform

import org.silkframework.rule.TransformSpec
import org.silkframework.util.{DPair, Uri}
import org.silkframework.workspace.User
import org.silkframework.workspace.activity.transform.{TransformPathsCache, VocabularyCache}
import play.api.mvc.{Action, AnyContent, Controller}
import plugins.Context

class TransformEditor extends Controller {

  def start(project: String, task: String, rule: String) = Action { implicit request =>
    val context = Context.get[TransformSpec](project, task, request.path)
    val vocabularies = context.task.activity[VocabularyCache].value

    // TODO: We should check whether the rule exists
    Ok(views.html.editor.transformRules(context, vocabularies, rule))
  }

  def editor(project: String, task: String, rule: String): Action[AnyContent] = Action { implicit request =>
    val context = Context.get[TransformSpec](project, task, request.path)
    val transformSpec = context.task.data
    transformSpec.nestedRuleAndSourcePath(rule) match {
      case Some((r, _)) => Ok(views.html.editor.transformEditor(context, r))
      case None =>
        val validRuleNames = transformSpec.validRuleNames(transformSpec.mappingRule).mkString(", ")
        NotFound(s"No rule named '$rule' found!. Available rules: $validRuleNames")
    }
  }

  def propertyDetails(project: String, task: String, property: String) = Action { implicit request =>
    val context = Context.get[TransformSpec](project, task, request.path)
    val vocabularies = context.task.activity[VocabularyCache].value
    val uri = Uri.parse(property, context.project.config.prefixes)

    Ok(views.html.editor.propertyDetails(property, vocabularies.findProperty(uri.uri), context.project.config.prefixes))
  }

  def paths(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)
    val pathsCache = task.activity[TransformPathsCache].control
    val prefixes = project.config.prefixes
    val sourceName = task.data.selection.inputId.toString

    if(pathsCache.status().isRunning) {
      val loadingMsg = f"Cache loading (${pathsCache.status().progress * 100}%.1f%%)"
      ServiceUnavailable(views.html.editor.paths(DPair(sourceName, ""), DPair.fill(Seq.empty), onlySource = true, loadingMsg = loadingMsg, project = project))
    } else if(pathsCache.status().failed) {
      Ok(views.html.editor.paths(DPair(sourceName, ""), DPair.fill(Seq.empty), onlySource = true, warning = pathsCache.status().message,  project = project))
    } else {
      val paths = DPair(pathsCache.value().typedPaths.map(_.path.serialize(prefixes)), Seq.empty)
      Ok(views.html.editor.paths(DPair(sourceName, ""), paths, onlySource = true,  project = project))
    }
  }

  def score(projectName: String, taskName: String) = Action {
    Ok
  }
}
