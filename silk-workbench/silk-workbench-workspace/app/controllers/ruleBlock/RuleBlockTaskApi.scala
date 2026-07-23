package controllers.ruleBlock

import controllers.core.UserContextActions
import controllers.core.util.ControllerUtilsTrait
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.silkframework.config.PlainTask
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.rule.evaluation.DetailedEvaluator
import org.silkframework.rule.input.{PathInput, RuleBlockBindingExecution, RuleBlockExecution}
import org.silkframework.rule.{RuleBlockModel, RuleBlockSpec, TaskContext}
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.serialization.json.JsonSerializers.RuleBlockContentJsonFormat
import org.silkframework.serialization.json.LinkingSerializers.ValueJsonFormat
import org.silkframework.util.{Identifier, Uri}
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
      val evaluatedValues = evaluateRuleBlockModel(taskId, ruleBlockModel)(pluginContext)
      Ok(JsArray(evaluatedValues.map(ValueJsonFormat.write)))
    }
  }

  private def evaluateRuleBlockModel(taskId: String,
                                     ruleBlockModel: RuleBlockModel)
                                    (implicit pluginContext: PluginContext): Seq[org.silkframework.rule.evaluation.Value] = {
    val validatedTask = PlainTask(Identifier(taskId), RuleBlockSpec(ruleBlockModel))
    val sortedPorts = ruleBlockModel.ports.sortBy(port => (port.displayOrder, port.id.toString))
    val mockPaths = sortedPorts.zipWithIndex.map { case (port, index) =>
      port.id -> s"ruleBlockPort${index + 1}"
    }.toMap
    val schema = EntitySchema(
      typeUri = Uri("urn:rule-block-evaluation"),
      typedPaths = sortedPorts.flatMap { port =>
        mockPaths.get(port.id).map(path => UntypedPath.saveApply(path).asStringTypedPath)
      }
    )
    val bindingExecutions = sortedPorts.flatMap { port =>
      mockPaths.get(port.id).map { path =>
        RuleBlockBindingExecution(
          port.id,
          PathInput(
            id = Identifier(s"mock_${port.id}"),
            path = UntypedPath.saveApply(path)
          ).execution(TaskContext.noInput())
        )
      }
    }
    val execution = RuleBlockExecution(validatedTask, bindingExecutions, TaskContext.noInput())
    ruleBlockModel.inputExamples.flatMap { inputExample =>
      val entity = Entity(
        uri = Uri(s"urn:rule-block-evaluation/entity/${inputExample.id}"),
        values = sortedPorts.map(port => inputExample.inputs.getOrElse(port.id, Seq.empty)),
        schema = schema
      )
      execution.rootExecution.toSeq.map(rootExecution => DetailedEvaluator(rootExecution, entity))
    }
  }
}
