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
import org.silkframework.rule.{ComplexUriMapping, ObjectMapping, TransformRule, TransformSpec, ValueTransformRule}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.iterator.CloseableIterator
import org.silkframework.runtime.plugin.PluginContext
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
      val transformedValues = evaluateRule(task, parentRuleId, transformRule, limit)(PluginContext.fromProject(project))
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
                             name = "offset",
                             description = "The number of results to skip before returning the page. Used together with 'limit' for pagination.",
                             required = false,
                             in = ParameterIn.QUERY,
                             schema = new Schema(implementation = classOf[Int], defaultValue = "0")
                           )
                           offset: Int,
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

    val safeOffset = math.max(0, offset)
    val safeLimit = math.max(0, limit)
    // Exclusive end index of the requested page, clamped to avoid Int overflow for large limits/offsets.
    val pageEnd = math.min(Int.MaxValue.toLong, safeOffset.toLong + safeLimit.toLong).toInt
    // Fetch one entity beyond the page to detect whether a next page exists (clamped to the Int range).
    val maxEntities = math.min(Int.MaxValue.toLong, pageEnd.toLong + 1L).toInt
    // Create execution task. Pagination is done here at the controller layer;
    // DataSource.retrieve has no offset and is left untouched.
    val evaluateTransform = new EvaluateTransformTask(
        source = task.dataSource,
        entitySchema = ruleSchema.inputSchema,
        rules = ruleSchema.transformRule.rules,
        maxEntities = maxEntities,
        taskContext = task.taskContext
      )
    val entities = evaluateTransform.execute()
    // FIXME: This only filters the fetched entities. Unclear how to do this in a performant way to fetch entities until the limit is met.
    val filteredEntities = if(showOnlyEntitiesWithUris) entities.filter(_.uris.nonEmpty) else entities
    val hasNextPage = filteredEntities.size > pageEnd
    val pageEntities = filteredEntities.slice(safeOffset, pageEnd)
    val jsonEntities = pageEntities.map(DetailedEntityJsonFormat.write)
    val rules: Seq[JsValue] = evaluatedRulesJson(ruleSchema)

    Ok(Json.obj(
      "rules" -> rules,
      "evaluatedEntities" -> JsArray(jsonEntities),
      "hasNextPage" -> hasNextPage,
    ))
  }

  private def evaluatedRulesJson(ruleSchema: TransformSpec.RuleSchemataExecution)
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
                            (implicit pluginContext: PluginContext): TransformSpec.RuleSchemataExecution = {
    val objectMappingId = task.data.objectMappingIdOfRule(ruleId).getOrElse(ruleId)
    task.data.ruleSchemata
      .find(_.transformRule.id.toString == objectMappingId)
      .getOrElse(throw new NotFoundException(s"Mapping rule '$ruleId' is not part of task '${task.fullLabel}' in project '${task.project.fullLabel}'. " +
        s"Available rules: ${task.data.ruleSchemata.map(_.transformRule.id).mkString(", ")}"))
      .execution(task.taskContext)
  }

  private def evaluateRule(task: ProjectTask[TransformSpec], parentRuleId: Identifier, transformRule: TransformRule, limit: Int)
                          (implicit pluginContext: PluginContext): CloseableIterator[Value] = {
    implicit val user: UserContext = pluginContext.user

    val ruleSchema = ruleSchemaById(task, parentRuleId)
    val inputSchema = ruleSchema.inputSchema.copy(typedPaths = transformRule.sourcePaths.toIndexedSeq)

    val entities = task.dataSource.retrieve(inputSchema, Some(limit)).entities.take(limit)
    val ruleExec = transformRule.execution(task.taskContext)
    for(entity <- entities) yield {
      DetailedEvaluator(ruleExec, entity)
    }
  }

}
