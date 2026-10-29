package controllers.ruleBlock

import controllers.core.UserContextActions
import controllers.core.util.ControllerUtilsTrait
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.silkframework.rule.evaluation.RuleBlockEvaluation
import org.silkframework.rule.{RuleBlockModel, RuleBlockSpec}
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.serialization.json.JsonSerializers.RuleBlockContentJsonFormat
import org.silkframework.serialization.json.LinkingSerializers.ValueJsonFormat
import org.silkframework.util.Identifier
import play.api.libs.json.{JsArray, JsValue}
import play.api.mvc.{Action, InjectedController}

import javax.inject.Inject

@Tag(
  name = "Rule Block",
  description = "Endpoints related to reusable rule block tasks."
)
class RuleBlockTaskApi @Inject()() extends InjectedController with UserContextActions with ControllerUtilsTrait {

  @Operation(
    summary = "Evaluate reusable rule block",
    description = "Evaluates the provided rule block model against its stored input examples.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(new Content(mediaType = "application/json"))
      )
    )
  )
  @RequestBody(
    required = true,
    content = Array(new Content(mediaType = "application/json"))
  )
  def evaluateRuleBlock(@Parameter(
                          name = "projectId",
                          description = "The project identifier",
                          required = true,
                          in = ParameterIn.PATH,
                          schema = new Schema(implementation = classOf[String])
                        )
                        projectId: String,
                        @Parameter(
                          name = "taskId",
                          description = "The rule block task identifier",
                          required = true,
                          in = ParameterIn.PATH,
                          schema = new Schema(implementation = classOf[String])
                        )
                        taskId: String): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request => implicit userContext =>
    val project = getProject(projectId)
    project.task[RuleBlockSpec](taskId)
    implicit val readContext: ReadContext = ReadContext.fromProject(project)
    implicit val writeContext: WriteContext[JsValue] = WriteContext.fromProject[JsValue](project)
    implicit val pluginContext: PluginContext = PluginContext.fromProject(project)

    validateJsonFromJsonFormat[RuleBlockModel] { ruleBlockModel =>
      val evaluated = RuleBlockEvaluation.evaluateInputExamples(Identifier(taskId), ruleBlockModel)(pluginContext)
      Ok(JsArray(evaluated.map(example => ValueJsonFormat.write(example.value))))
    }
  }
}
