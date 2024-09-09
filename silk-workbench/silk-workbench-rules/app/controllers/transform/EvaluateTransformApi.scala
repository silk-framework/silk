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
import org.silkframework.rule.execution.{EvaluateTransform => EvaluateTransformTask}
import org.silkframework.rule.{ObjectMapping, TransformRule, TransformSpec, ValueTransformRule}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.iterator.CloseableIterator
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.runtime.validation.NotFoundException
import org.silkframework.serialization.json.JsonSerializers.TransformRuleJsonFormat
import org.silkframework.serialization.json.LinkingSerializers.{DetailedEntityJsonFormat, ValueJsonFormat}
import org.silkframework.util.Identifier
import org.silkframework.workbench.workspace.WorkbenchAccessMonitor
import org.silkframework.workspace.activity.transform.TransformTaskUtils._
import org.silkframework.workspace.{ProjectTask, WorkspaceFactory}
import play.api.libs.json.{JsArray, JsValue, Json}
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
                     in = ParameterIn.QUERY,
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

  @Operation(
    summary = "Evaluate transform rule by ID",
    description = "Evaluates a transform rule with the given ID.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(
          new Content(
            mediaType = "application/json",
            examples = Array(new ExampleObject(EvaluateTransformApiDoc.evaluatedRuleResponseExample))
          )
        )
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project, task or rule has not been found."
      )
    )
  )
  def evaluateSpecificRule(@Parameter(
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
                             description = "The identifier of the rule that should be evaluated.",
                             required = true,
                             in = ParameterIn.PATH,
                             schema = new Schema(implementation = classOf[String], example = "root")
                           )
                           ruleId: String,
                           @Parameter(
                             name = "limit",
                             description = "The maximum number of results to be returned",
                             required = false,
                             in = ParameterIn.QUERY,
                             schema = new Schema(implementation = classOf[Int], defaultValue = "50")
                           )
                           limit: Int,
                           @Parameter(
                             name = "showOnlyEntitiesWithUris",
                             description = "If true, only entities are returned that generated a valid entity URI.",
                             required = false,
                             in = ParameterIn.QUERY,
                             schema = new Schema(implementation = classOf[Int], defaultValue = "false")
                           )
                           showOnlyEntitiesWithUris: Boolean): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)

    implicit val writeContext: WriteContext[JsValue] = WriteContext.fromProject[JsValue](project)

    implicit val prefixes: Prefixes = project.config.prefixes
    val ruleSchema = ruleSchemaById(task, ruleId)

    // Create execution task
    val evaluateTransform = new EvaluateTransformTask(
        source = task.dataSource,
        entitySchema = ruleSchema.inputSchema,
        rules = ruleSchema.transformRule.rules,
        maxEntities = limit
      )
    val entities = evaluateTransform.execute()
    // FIXME: This only filters the limit# entities. Unclear how to do this in a performant way to fetch entities until the limit is met.
    val filteredEntities = if(showOnlyEntitiesWithUris) entities.filter(_.uris.nonEmpty) else entities
    val jsonEntities = filteredEntities.map(DetailedEntityJsonFormat.write)
    val rules: Seq[JsValue] = evaluatedRulesJson(ruleSchema)

    Ok(Json.obj(
      "rules" -> rules,
      "evaluatedEntities" -> JsArray(jsonEntities),
    ))
  }

  private def evaluatedRulesJson(ruleSchema: TransformSpec.RuleSchemata)
                                (implicit writeContext: WriteContext[JsValue]): Seq[JsValue] = {
    ruleSchema.transformRule.rules.allRules
      .map(r => {
        val rule = r match {
          case om: ObjectMapping =>
            val uriRule = om.rules.uriRule.orElse(om.uriRule()).map(_.asComplexMapping)
            // Return only the URI rule as complex rule for the object mapping
            om.copy(rules = om.rules.copy(
              uriRule = None,
              typeRules = Seq.empty,
              propertyRules = uriRule.toSeq)
            )
          case vr: ValueTransformRule => vr.asComplexMapping
          case or: TransformRule => or
        }
        TransformRuleJsonFormat.write(rule)
      })
  }

  private def ruleSchemaById(task: ProjectTask[TransformSpec], ruleId: String)
                            (implicit userContext: UserContext): TransformSpec.RuleSchemata = {
    val objectMappingId = task.data.objectMappingIdOfRule(ruleId).getOrElse(ruleId)
    task.data.ruleSchemataWithoutEmptyObjectRules
      .find(_.transformRule.id.toString == objectMappingId)
      .getOrElse(throw new NotFoundException(s"Mapping rule '$ruleId' is either an empty object rule, i.e. it has at most a URI rule,  or is not part of task '${task.fullLabel}' in project '${task.project.fullLabel}'. " +
        s"Available rules: ${task.data.ruleSchemataWithoutEmptyObjectRules.map(_.transformRule.id).mkString(", ")}"))
      .withContext(task.taskContext)
  }

  private def evaluateRule(task: ProjectTask[TransformSpec], parentRuleId: Identifier, transformRule: TransformRule, limit: Int)
                          (implicit userContext: UserContext): CloseableIterator[Value] = {
    implicit val prefixes: Prefixes = task.project.config.prefixes

    val ruleSchema = ruleSchemaById(task, parentRuleId)
    val inputSchema = ruleSchema.inputSchema.copy(typedPaths = transformRule.sourcePaths.toIndexedSeq)
    val ruleWithContext = transformRule.withContext(task.taskContext)

    val entities = task.dataSource.retrieve(inputSchema, Some(limit)).entities.take(limit)
    for(entity <- entities) yield {
      DetailedEvaluator(ruleWithContext, entity)
    }
  }

}
