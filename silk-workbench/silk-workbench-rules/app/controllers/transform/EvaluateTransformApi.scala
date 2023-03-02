package controllers.transform

import config.WorkbenchConfig.WorkspaceReact
import controllers.core.UserContextActions
import controllers.transform.doc.EvaluateTransformApiDoc
import controllers.util.SerializationUtils
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.{Operation, Parameter}
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
@Tag(name = "Transform")
class EvaluateTransformApi @Inject()(implicit accessMonitor: WorkbenchAccessMonitor, workspaceReact: WorkspaceReact) extends InjectedController with UserContextActions {

  @Operation(
    summary = "Evaluate transform rule",
    description = "Evaluates a transform rule that is send with the requests.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(
          new Content(
            mediaType = "application/json",
            examples = Array(new ExampleObject(EvaluateTransformApiDoc.evaluateRuleResponseExample))
          )
        )
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project, task or rule has not been found."
      )
    )
  )
  @RequestBody(
    content = Array(
      new Content(
        mediaType = "application/json",
        schema = new Schema(`type` = "object"),
        examples = Array(new ExampleObject(EvaluateTransformApiDoc.evaluateRuleRequestExample))
      )
    )
  )
  def evaluateRule(@Parameter(
                     name = "project",
                     description = "The project identifier",
                     required = true,
                     in = ParameterIn.PATH,
                     schema = new Schema(implementation = classOf[String])
                   )
                   projectName: String,
                   @Parameter(
                     name = "task",
                     description = "The task identifier",
                     required = true,
                     in = ParameterIn.PATH,
                     schema = new Schema(implementation = classOf[String])
                   )
                   taskName: String,
                   @Parameter(
                     name = "rule",
                     description = "The identifier of the parent rule or 'root' if there is no parent.",
                     required = true,
                     in = ParameterIn.PATH,
                     schema = new Schema(implementation = classOf[String], example = "root")
                   )
                   parentRuleId: String,
                   @Parameter(
                     name = "limit",
                     description = "The maximum number of results to be returned",
                     required = false,
                     in = ParameterIn.PATH,
                     schema = new Schema(implementation = classOf[Int], defaultValue = "3")
                   )
                   limit: Int): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)

    implicit val readContext: ReadContext = ReadContext.fromProject(project)
    implicit val writeContext: WriteContext[JsValue] = WriteContext.fromProject[JsValue](project)

    SerializationUtils.deserializeCompileTime[TransformRule](defaultMimeType = SerializationUtils.APPLICATION_JSON) { transformRule =>
      val transformedValues = evaluateRule(task, parentRuleId, transformRule, limit)
      Ok(JsArray(transformedValues.map(ValueJsonFormat.write).toSeq))
    }
  }

  private def evaluateRule(task: ProjectTask[TransformSpec], parentRuleId: Identifier, transformRule: TransformRule, limit: Int)
                          (implicit userContext: UserContext): Traversable[Value] = {
    implicit val prefixes: Prefixes = task.project.config.prefixes

    val ruleSchema = task.data.ruleSchemataWithoutEmptyObjectRules
      .find(_.transformRule.id == parentRuleId)
      .getOrElse(throw new NotFoundException(s"Mapping rule '$parentRuleId' is either an empty object rule or is not part of task '${task.id}' in project '${task.project.id}'. " +
        s"Available rules: ${task.data.ruleSchemataWithoutEmptyObjectRules.map(_.transformRule.id).mkString(", ")}"))

    val inputSchema = ruleSchema.inputSchema.copy(typedPaths = transformRule.sourcePaths.toIndexedSeq)

    val entities = task.dataSource.retrieve(inputSchema, Some(limit)).entities.take(limit)
    for(entity <- entities) yield {
      DetailedEvaluator(transformRule, entity)
    }
  }

}
