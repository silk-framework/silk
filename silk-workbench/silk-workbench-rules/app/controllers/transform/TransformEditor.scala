package controllers.transform

import controllers.core.util.ControllerUtilsTrait
import controllers.core.{RequestUserContextAction, UserContextAction}
import javax.inject.Inject
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.rule.TransformSpec
import org.silkframework.runtime.validation.NotFoundException
import org.silkframework.util.{DPair, Uri}
import org.silkframework.workbench.Context
import org.silkframework.workbench.workspace.WorkbenchAccessMonitor
import org.silkframework.workspace.WorkspaceFactory
import org.silkframework.workspace.activity.transform.{TransformPathsCache, VocabularyCache}
import play.api.mvc.{Action, AnyContent, InjectedController}

class TransformEditor @Inject() (accessMonitor: WorkbenchAccessMonitor) extends InjectedController with ControllerUtilsTrait {

  def start(project: String, task: String, rule: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val context = Context.get[TransformSpec](project, task, request.path)
    val vocabularies = context.task.activity[VocabularyCache].value()
    accessMonitor.saveProjectTaskAccess(project, task)

    // TODO: We should check whether the rule exists
    Ok(views.html.editor.transformRules(context, vocabularies, rule))
  }

  def editor(project: String, task: String, rule: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val context = Context.get[TransformSpec](project, task, request.path)
    val transformSpec = context.task.data
    transformSpec.nestedRuleAndSourcePath(rule) match {
      case Some((r, _)) => Ok(views.html.editor.transformEditor(context, r))
      case None =>
        val validRuleNames = transformSpec.ruleSchemata.map(_.transformRule.id).mkString(", ")
        NotFound(s"No rule named '$rule' found!. Available rules: $validRuleNames")
    }
  }

  def propertyDetails(project: String, task: String, property: String): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    val context = Context.get[TransformSpec](project, task, request.path)
    val vocabularies = context.task.activity[VocabularyCache].value()
    val uri = Uri.parse(property, context.project.config.prefixes)

    Ok(views.html.editor.propertyDetails(property, vocabularies.findProperty(uri.uri), context.project.config.prefixes))
  }

  /** Fetch relative source paths for a specific rule and render widget. */
  def rulePaths(projectName: String, taskName: String, ruleName: String, groupPaths: Boolean): Action[AnyContent] = UserContextAction { implicit userContext =>
    val (project, transformTask) = projectAndTask[TransformSpec](projectName, taskName)
    val sourceName = transformTask.data.selection.inputId.toString
    val prefixes = project.config.prefixes
    transformTask.data.nestedRuleAndSourcePath(ruleName) match {
      case Some((_, sourcePath)) =>
        val pathsCache = transformTask.activity[TransformPathsCache]
        pathsCache.control.waitUntilFinished()
        if(pathsCache.status().failed) {
          Ok(views.html.editor.paths(DPair(sourceName, ""), DPair.fill(Seq.empty), onlySource = true,
            warning = pathsCache.status().message,  project = project))
        } else {
          val relativePaths = pathsCache.value().configuredSchema.typedPaths. // FIXME: This won't work inside nested object rules for RDF datasets
              filter(tp => tp.operators.startsWith(sourcePath) && tp.operators.size > sourcePath.size).
              map(tp => UntypedPath(tp.operators.drop(sourcePath.size)))
          val paths = DPair(relativePaths.map(_.serialize()(prefixes)), Seq.empty)
          if (groupPaths) {
            Ok(views.html.editor.paths(DPair(sourceName, ""), paths, onlySource = true, project = project))
          } else {
            Ok(views.html.editor.pathsList(DPair(sourceName, ""), paths, onlySource = true, project = project))
          }
        }
      case None =>
        throw new NotFoundException("No rule found with name " + ruleName)
    }
  }

  def paths(projectName: String, taskName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)
    val pathsCache = task.activity[TransformPathsCache].control
    val prefixes = project.config.prefixes
    val sourceName = task.data.selection.inputId.toString

    if(pathsCache.status().isRunning) {
      val loadingMsg = f"Cache loading (${pathsCache.status().progress.getOrElse(0.0) * 100}%.1f%%)"
      ServiceUnavailable(views.html.editor.paths(DPair(sourceName, ""), DPair.fill(Seq.empty), onlySource = true, loadingMsg = loadingMsg, project = project))
    } else if(pathsCache.status().failed) {
      Ok(views.html.editor.paths(DPair(sourceName, ""), DPair.fill(Seq.empty), onlySource = true, warning = pathsCache.status().message,  project = project))
    } else {
      val paths = DPair(pathsCache.value().configuredSchema.typedPaths.map(_.toUntypedPath.serialize()(prefixes)), Seq.empty)
      Ok(views.html.editor.paths(DPair(sourceName, ""), paths, onlySource = true,  project = project))
    }
  }

  def score(projectName: String, taskName: String): Action[AnyContent] = Action {
    Ok
  }
}
