package controllers.transform

import config.WorkbenchConfig.WorkspaceReact
import controllers.core.UserContextActions
import controllers.util.SerializationUtils
import org.silkframework.config.Prefixes
import org.silkframework.rule.evaluation.{DetailedEvaluator, Value}
import org.silkframework.rule.{TransformRule, TransformSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.runtime.validation.NotFoundException
import org.silkframework.serialization.json.LinkingSerializers.ValueJsonFormat
import org.silkframework.util.Identifier
import org.silkframework.workbench.workspace.WorkbenchAccessMonitor
import org.silkframework.workspace.activity.transform.TransformTaskUtils._
import org.silkframework.workspace.{ProjectTask, WorkspaceFactory}
import play.api.libs.json.{JsArray, JsValue}
import play.api.mvc.{Action, AnyContent, InjectedController}

import javax.inject.Inject

/** Endpoints for evaluating transform tasks */
class EvaluateTransformApi @Inject()(implicit accessMonitor: WorkbenchAccessMonitor, workspaceReact: WorkspaceReact) extends InjectedController with UserContextActions {


  def evaluateRule(projectName: String,
                   taskName: String,
                   parentRuleId: String,
                   limit: Int): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)

    implicit val readContext: ReadContext = ReadContext.fromProject(project)
    implicit val writeContext: WriteContext[JsValue] = WriteContext[JsValue]()

    SerializationUtils.deserializeCompileTime[TransformRule](defaultMimeType = SerializationUtils.APPLICATION_JSON) { transformRule =>
      val transformedValues = evaluateRule(task, parentRuleId, transformRule, limit)
      Ok(JsArray(transformedValues.map(ValueJsonFormat.write).toSeq))
    }
  }

  private def evaluateRule(task: ProjectTask[TransformSpec], parentRuleId: Identifier, transformRule: TransformRule, limit: Int)
                          (implicit userContext: UserContext): Traversable[Value] = {
    implicit val prefixes: Prefixes = task.project.config.prefixes

    val ruleSchema = task.data.ruleSchemata
      .find(_.transformRule.id == parentRuleId)
      .getOrElse(throw new NotFoundException(s"Rule $parentRuleId is not part of task ${task.id} in project ${task.project.id}. " +
        s"Available rules: ${task.data.ruleSchemata.map(_.transformRule.id).mkString(", ")}"))

    val inputSchema = ruleSchema.inputSchema.copy(typedPaths = transformRule.sourcePaths.toIndexedSeq)

    val entities = task.dataSource.retrieve(inputSchema, Some(limit)).entities.take(limit)
    for(entity <- entities) yield {
      DetailedEvaluator(transformRule, entity)
    }
  }

}
