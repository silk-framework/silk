package controllers.transform

import controllers.core.UserContextActions
import controllers.transform.PeakTransformApi._
import controllers.transform.doc.PeakApiDoc
import controllers.util.ProjectUtils._
import controllers.util.SerializationUtils._
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.silkframework.config.{Prefixes, Task, TaskSpec}
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset._
import org.silkframework.dataset.rdf.{RdfDataset, RdfDatasetAccess}
import org.silkframework.entity._
import org.silkframework.entity.paths.{Path, TypedPath, UntypedPath}
import org.silkframework.plugins.dataset.rdf.executors.LocalSparqlSelectIterator
import org.silkframework.plugins.dataset.rdf.tasks.SparqlSelectCustomTask
import org.silkframework.rule.TransformSpec.RuleSchemata
import org.silkframework.rule._
import org.silkframework.rule.input.{PathInput, Value}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.serialization.ReadContext
import org.silkframework.runtime.validation.{BadUserInputException, ValidationException}
import org.silkframework.util.Identifier
import org.silkframework.workspace.{Project, ProjectTask}
import play.api.libs.json.{Format, Json}
import play.api.mvc._

import javax.inject.Inject
import scala.collection.mutable.ArrayBuffer
import scala.util.control.NonFatal

@Tag(name = "Transform")
class PeakTransformApi @Inject() () extends InjectedController with UserContextActions {

    /**
    * Get sample source and transformed values for a named rule.
    */
  @Operation(
    summary = "Mapping Rule Transformation Examples",
    description = "Get transformation examples for the selected transformation rule. Value rules return their transformed values; object and root rules return the IRI minted by their URI rule. The input task of the transformation task has to be a Dataset task. Also the Dataset task must support this feature.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = PeakApiDoc.peakResultDoc,
        content = Array(
          new Content(
            mediaType = "application/json",
            examples = Array(new ExampleObject(PeakApiDoc.peakExample))
          )
        )
      )
  ))
  def peak( @Parameter(
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
              description = "The value rule identifier",
              required = true,
              in = ParameterIn.PATH,
              schema = new Schema(implementation = classOf[String])
            )
            ruleName: String,
            @Parameter(
              name = "limit",
              description = "The maximum number of transformed example entities.",
              required = false,
              in = ParameterIn.QUERY,
              schema = new Schema(implementation = classOf[Int], defaultValue = TRANSFORMATION_PREVIEW_LIMIT_STR)
            )
            limit: Int = TRANSFORMATION_PREVIEW_LIMIT,
            @Parameter(
              name = "maxTryEntities",
              description = "Per-page budget of example entities to try to transform before giving up. The actual number of source entities fetched is `offset + maxTryEntities`, so paginating into the result set scans proportionally more entities.",
              required = false,
              in = ParameterIn.QUERY,
              schema = new Schema(implementation = classOf[Int], defaultValue = MAX_TRY_ENTITIES_DEFAULT_STR)
            )
            maxTryEntities: Int = MAX_TRY_ENTITIES_DEFAULT,
            @Parameter(
              name = "offset",
              description = "Number of successful example results to skip before collecting the page. Defaults to 0 (first page). Setting this implies `includeTotal=true`.",
              required = false,
              in = ParameterIn.QUERY,
              schema = new Schema(implementation = classOf[Int], defaultValue = "0")
            )
            offset: Int = 0,
            @Parameter(
              name = "search",
              description = "Optional case-insensitive substring filter. When set, only rows whose source values or transformed values contain the substring count toward offset/limit/total. Setting this implies `includeTotal=true`.",
              required = false,
              in = ParameterIn.QUERY,
              schema = new Schema(implementation = classOf[String])
            )
            search: Option[String] = None,
            @Parameter(
              name = "includeTotal",
              description = "If true, scan the full `offset + maxTryEntities` budget so the response can report `total`/`totalIsExact`/`nextOffset`. When false (the default) and no `offset`/`search` is set, scanning stops once `limit` results are collected, and the pagination metadata is omitted.",
              required = false,
              in = ParameterIn.QUERY,
              schema = new Schema(implementation = classOf[Boolean], defaultValue = "false")
            )
            includeTotal: Boolean = false,
            @Parameter(
              name = "perRecord",
              description = "If true, return one row per tried source record - including records whose transform " +
                "produced no value (per-record grouping, used by the new mapping editor). If false (the default), " +
                "records with an empty transform result are skipped so only real transformed examples are returned " +
                "(legacy mapping editor behaviour).",
              required = false,
              in = ParameterIn.QUERY,
              schema = new Schema(implementation = classOf[Boolean], defaultValue = "false")
            )
            perRecord: Boolean = false): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
      val (project, task) = projectAndTask(projectName, taskName)
      val transformSpec = task.data
      val ruleSchemata = transformSpec.oneRuleEntitySchemaById(ruleName).get
      val inputTaskId = transformSpec.selection.inputId
      implicit val context: PluginContext = PluginContext.fromProject(project)

      peakRule(project, inputTaskId, ruleSchemata, limit, maxTryEntities, offset, search, includeTotal, perRecord)
  }

  /**
    * Get sample source and transformed values for a provided rule definition.
    */
  @Operation(
    summary = "Mapping Rule from Request Transformation Examples",
    description = "Get transformation examples for the transformation rule that is attached in the body of this request. The input task of the transformation task has to be a Dataset task. Also the Dataset task must support this feature.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = PeakApiDoc.peakResultDoc,
        content = Array(
          new Content(
            mediaType = "application/json",
            examples = Array(new ExampleObject(PeakApiDoc.peakExample))
          )
        )
      )
  ))
  @RequestBody(
    description = "The rule to be used for retrieving example values.",
    required = true,
    content = Array(
      new Content(
        mediaType = "application/json"
      ),
      new Content(
        mediaType = "application/xml"
      )
    )
  )
  def peakChildRule(@Parameter(name = "project", description = "The project identifier",
                      required = true,
                      in = ParameterIn.PATH,
                      schema = new Schema(implementation = classOf[String])
                    )
                    projectName: String,
                    @Parameter(name = "task", description = "The task identifier",
                      required = true,
                      in = ParameterIn.PATH,
                      schema = new Schema(implementation = classOf[String])
                    )
                    taskName: String,
                    @Parameter(name = "rule", description = "The rule identifier",
                      required = true,
                      in = ParameterIn.PATH,
                      schema = new Schema(implementation = classOf[String])
                    )
                    ruleName: String,
                    @Parameter(name = "limit", description = "The maximum number of transformed example entities.",
                      required = false,
                      in = ParameterIn.QUERY,
                      schema = new Schema(implementation = classOf[Int], defaultValue = TRANSFORMATION_PREVIEW_LIMIT_STR)
                    )
                    limit: Int = TRANSFORMATION_PREVIEW_LIMIT,
                    @Parameter(name = "maxTryEntities", description = "Per-page budget of example entities to try to transform before giving up. The actual number of source entities fetched is `offset + maxTryEntities`, so paginating into the result set scans proportionally more entities.",
                      required = false,
                      in = ParameterIn.QUERY,
                      schema = new Schema(implementation = classOf[Int], defaultValue = MAX_TRY_ENTITIES_DEFAULT_STR)
                    )
                    maxTryEntities: Int = MAX_TRY_ENTITIES_DEFAULT,
                    @Parameter(name = "objectPath", description = "An additional object path this auto-completion should be the context of.", required = false,
                      in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String]))
                    objectPath: Option[String],
                    @Parameter(
                      name = "offset",
                      description = "Number of successful example results to skip before collecting the page. Defaults to 0 (first page). Setting this implies `includeTotal=true`.",
                      required = false,
                      in = ParameterIn.QUERY,
                      schema = new Schema(implementation = classOf[Int], defaultValue = "0")
                    )
                    offset: Int = 0,
                    @Parameter(
                      name = "search",
                      description = "Optional case-insensitive substring filter. When set, only rows whose source values or transformed values contain the substring count toward offset/limit/total. Setting this implies `includeTotal=true`.",
                      required = false,
                      in = ParameterIn.QUERY,
                      schema = new Schema(implementation = classOf[String])
                    )
                    search: Option[String] = None,
                    @Parameter(
                      name = "includeTotal",
                      description = "If true, scan the full `offset + maxTryEntities` budget so the response can report `total`/`totalIsExact`/`nextOffset`. When false (the default) and no `offset`/`search` is set, scanning stops once `limit` results are collected, and the pagination metadata is omitted.",
                      required = false,
                      in = ParameterIn.QUERY,
                      schema = new Schema(implementation = classOf[Boolean], defaultValue = "false")
                    )
                    includeTotal: Boolean = false,
                    @Parameter(
                      name = "perRecord",
                      description = "If true, return one row per tried source record - including records whose transform " +
                        "produced no value (per-record grouping, used by the new mapping editor). If false (the default), " +
                        "records with an empty transform result are skipped (legacy mapping editor behaviour).",
                      required = false,
                      in = ParameterIn.QUERY,
                      schema = new Schema(implementation = classOf[Boolean], defaultValue = "false")
                    )
                    perRecord: Boolean = false): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val (project, task) = projectAndTask(projectName, taskName)
    val transformSpec = task.data
    val parentRule = transformSpec.oneRuleEntitySchemaById(ruleName).get
    val inputTaskId = transformSpec.selection.inputId
    implicit val readContext: ReadContext = ReadContext.fromProject(project)

    deserializeCompileTime[TransformRule]() { rule =>
      val ruleSchemata = childRuleSchemata(parentRule, transformSpec, rule, objectPath)
      peakRule(project, inputTaskId, ruleSchemata, limit, maxTryEntities, offset, search, includeTotal, perRecord)
    }
  }

  /**
    * Get sample values of a source path grouped per source entity, without a saved rule or target.
    */
  @Operation(
    summary = "Source Path Preview Values",
    description = "Get example values for a bare source path that has no saved rule yet. A trivial identity value rule is " +
      "synthesized on the path and previewed through the same machinery as a mapping rule, so each result groups one source " +
      "entity's values for the path (matching the grouping shown once the path is bound to a target). The input task of the " +
      "transformation task has to be a Dataset task that supports this feature.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = PeakApiDoc.peakResultDoc,
        content = Array(
          new Content(
            mediaType = "application/json",
            examples = Array(new ExampleObject(PeakApiDoc.peakExample))
          )
        )
      )
  ))
  def peakSourcePath(@Parameter(name = "project", description = "The project identifier",
                       required = true, in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]))
                     projectName: String,
                     @Parameter(name = "task", description = "The task identifier",
                       required = true, in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]))
                     taskName: String,
                     @Parameter(name = "rule", description = "The identifier of the rule whose entity scope the path is relative to (e.g. the containing object mapping or the root rule).",
                       required = true, in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]))
                     ruleName: String,
                     @Parameter(name = "path", description = "The source path to preview, relative to the rule's (and any objectPath's) entity scope.",
                       required = true, in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String]))
                     path: String,
                     @Parameter(name = "objectPath", description = "An additional object path this preview should be the context of.",
                       required = false, in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String]))
                     objectPath: Option[String],
                     @Parameter(name = "limit", description = "The maximum number of example entities.",
                       required = false, in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[Int], defaultValue = TRANSFORMATION_PREVIEW_LIMIT_STR))
                     limit: Int = TRANSFORMATION_PREVIEW_LIMIT,
                     @Parameter(name = "maxTryEntities", description = "Per-page budget of example entities to try before giving up. The actual number of source entities fetched is `offset + maxTryEntities`.",
                       required = false, in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[Int], defaultValue = MAX_TRY_ENTITIES_DEFAULT_STR))
                     maxTryEntities: Int = MAX_TRY_ENTITIES_DEFAULT,
                     @Parameter(name = "offset", description = "Number of example results to skip before collecting the page. Setting this implies `includeTotal=true`.",
                       required = false, in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[Int], defaultValue = "0"))
                     offset: Int = 0,
                     @Parameter(name = "search", description = "Optional case-insensitive substring filter on the source values. Setting this implies `includeTotal=true`.",
                       required = false, in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String]))
                     search: Option[String] = None,
                     @Parameter(name = "includeTotal", description = "If true, scan the full `offset + maxTryEntities` budget so the response can report `total`/`totalIsExact`/`nextOffset`.",
                       required = false, in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[Boolean], defaultValue = "false"))
                     includeTotal: Boolean = false,
                     @Parameter(name = "perRecord", description = "If true, return one row per tried source record - including records whose transform produced no value (per-record grouping, used by the new mapping editor). If false (the default), records with an empty result are skipped (legacy behaviour).",
                       required = false, in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[Boolean], defaultValue = "false"))
                     perRecord: Boolean = false): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val (project, task) = projectAndTask(projectName, taskName)
    val transformSpec = task.data
    val parentRule = transformSpec.oneRuleEntitySchemaById(ruleName).get
    val inputTaskId = transformSpec.selection.inputId
    implicit val context: PluginContext = PluginContext.fromProject(project)

    // Synthesize a target-less identity value rule on the path so a bare source node (no saved rule,
    // no target) is previewed through the very same peek machinery. The source side of each result is
    // the entity's grouped values for the path; transformedValues just echo them and are ignored by
    // a source-only preview.
    val childRule = ComplexMapping(id = "sourcePathPreview", operator = PathInput(path = UntypedPath.parse(path)), target = None)
    val ruleSchemata = childRuleSchemata(parentRule, transformSpec, childRule, objectPath)
    peakRule(project, inputTaskId, ruleSchemata, limit, maxTryEntities, offset, search, includeTotal, perRecord)
  }

  /**
    * Attaches a child rule to a parent rule's schema context and applies an optional object path,
    * mirroring how a real child value rule is scoped. Shared by [[peakChildRule]] (rule supplied in the
    * request body) and [[peakSourcePath]] (a synthesized identity rule on a bare source path).
    */
  private def childRuleSchemata(parentRule: RuleSchemata, transformSpec: TransformSpec, childRule: TransformRule, objectPath: Option[String]): RuleSchemata = {
    val updatedParentRule = parentRule.transformRule.withChildren(Seq(childRule)).asInstanceOf[TransformRule]
    val initialRuleSchemata = RuleSchemata.create(updatedParentRule, transformSpec.selection, parentRule.inputSchema.subPath, parentRule.outputSchema.subPath)
      .copy(transformRule = childRule)
    if (objectPath.exists(_.nonEmpty)) {
      val inputSchema = initialRuleSchemata.inputSchema.copy(subPath = UntypedPath(initialRuleSchemata.inputSchema.subPath.operators ++ UntypedPath.parse(objectPath.get).operators))
      RuleSchemata(initialRuleSchemata.transformRule, inputSchema, initialRuleSchemata.outputSchema)
    } else {
      initialRuleSchemata
    }
  }

  private def peakRule(project: Project, inputTaskId: Identifier, ruleSchemata: RuleSchemata, limit: Int, maxTryEntities: Int, offset: Int, search: Option[String], includeTotal: Boolean, perRecord: Boolean)
                      (implicit context: PluginContext): Result = {
    if (offset < 0) {
      throw BadUserInputException(s"Query parameter 'offset' must be >= 0, but was $offset.")
    }
    if (limit <= 0) {
      throw BadUserInputException(s"Query parameter 'limit' must be > 0, but was $limit.")
    }
    if (maxTryEntities < 1) {
      // BadUserInputException (a RequestException) maps to HTTP 400, consistent with the offset/limit checks above.
      throw BadUserInputException(s"Query parameter 'maxTryEntities' must be >= 1, but was $maxTryEntities.")
    }
    implicit val prefixes: Prefixes = project.config.prefixes
    implicit val user: UserContext = context.user

    // Container rules (root/object mappings) can't be previewed directly — preview the IRI they
    // mint instead, by peeking their URI rule (a value rule). Objects get an auto-generated URI
    // rule filled in when none is defined, so the minted @id can be previewed either way; the
    // input schema is narrowed to the URI rule's own source paths.
    val peakSchemata = ruleSchemata.transformRule match {
      case objectMapping: ObjectMapping =>
        objectMapping.fillEmptyUriRule.rules.uriRule match {
          case Some(uriRule) =>
            val inputPaths = uriRule.sourcePaths
              .map(p => TypedPath(p.operators, ValueType.STRING, isAttribute = false))
              .toIndexedSeq
            ruleSchemata.copy(
              transformRule = uriRule,
              inputSchema = ruleSchemata.inputSchema.copy(typedPaths = inputPaths)
            )
          case None => ruleSchemata
        }
      case container: ContainerTransformRule =>
        container.rules.uriRule.map(uriRule => ruleSchemata.copy(transformRule = uriRule)).getOrElse(ruleSchemata)
      case _ =>
        ruleSchemata
    }

    val inputTask = project.anyTask(inputTaskId)
    val inputTaskLabel = inputTask.label()
    // Treat blank search input as no filter so callers can safely send `?search=` from a UI textbox.
    val effectiveSearch = search.map(_.trim).filter(_.nonEmpty)
    // Pagination/filtering implies the caller wants the total; otherwise scanning stops at `limit`.
    val computeTotal = includeTotal || offset > 0 || effectiveSearch.nonEmpty
    // Source needs to yield enough entities to cover the skipped offset plus the per-page budget. Compute in
    // Long and saturate so a large offset (near Int.MaxValue) can't overflow into a negative fetch size.
    val sourceFetchSize = math.min(offset.toLong + maxTryEntities.toLong, Int.MaxValue.toLong).toInt
    inputTask.data match {
      case dataset: GenericDatasetSpec =>
        val pluginLabel = dataset.plugin.pluginSpec.label
        DataSource.pluginSource(inputTask.asInstanceOf[Task[GenericDatasetSpec]]) match {
          case peakDataSource: PeakDataSource =>
            try {
              peakDataSource.peak(peakSchemata.inputSchema, sourceFetchSize).use { exampleEntities =>
                generateMappingPreviewResponse(
                  peakSchemata.transformRule.execution(TaskContext.forInput(inputTask)),
                  exampleEntities,
                  limit, offset, sourceFetchSize, effectiveSearch, computeTotal, perRecord
                )
              }
            } catch {
              case pe: PeakException =>
                Ok(Json.toJson(PeakResults(None, None, PeakStatus(NOT_SUPPORTED_STATUS_MSG, s"Input dataset '$inputTaskLabel'" +
                  s" of type '$pluginLabel' raised following issue:" + pe.msg))))
            }
          case _ =>
            Ok(Json.toJson(PeakResults(None, None, PeakStatus(NOT_SUPPORTED_STATUS_MSG, s"Input dataset '$inputTaskLabel'" +
              s" of type '$pluginLabel' does not support transformation preview!"))))
        }
      case sparqlSelectTask: SparqlSelectCustomTask =>
        peakIntoSparqlSelectTask(project, inputTaskLabel, peakSchemata, limit, sourceFetchSize, offset, effectiveSearch, computeTotal, perRecord, sparqlSelectTask)
      case _: TransformSpec =>
        Ok(Json.toJson(PeakResults(None, None, PeakStatus(NOT_SUPPORTED_STATUS_MSG, s"Input task '$inputTaskLabel'" +
          " is not a Dataset. Currently mapping preview is only supported for dataset inputs."))))
      case t: TaskSpec =>
        Ok(Json.toJson(PeakResults(None, None, PeakStatus(NOT_SUPPORTED_STATUS_MSG, s"Input task '$inputTaskLabel' of type ${t.getClass.getSimpleName} " +
          s"is not supported. Currently only dataset and transform tasks support producing example values."))))
    }
  }

  private def peakIntoSparqlSelectTask(project: Project,
                                       inputTaskLabel: String,
                                       ruleSchemata: RuleSchemata,
                                       limit: Int,
                                       sourceFetchSize: Int,
                                       offset: Int,
                                       search: Option[String],
                                       computeTotal: Boolean,
                                       perRecord: Boolean,
                                       sparqlSelectTask: SparqlSelectCustomTask)
                                      (implicit userContext: UserContext, prefixes: Prefixes): Result = {
    implicit val context: PluginContext = PluginContext.fromProject(project)
    val sparqlDataset = sparqlSelectTask.optionalInputDataset.sparqlEnabledDataset
    if (sparqlDataset == "") {
      Ok(Json.toJson(PeakResults(None, None, PeakStatus(NOT_SUPPORTED_STATUS_MSG, s"Input task '$inputTaskLabel' of type ${sparqlSelectTask.pluginSpec.label} " +
        s"has no input dataset configured. Please configure the 'Optional SPARQL dataset' parameter."))))
    } else {
      val datasetTask = project.task[GenericDatasetSpec](sparqlDataset)
      datasetTask.data.plugin match {
        case _: RdfDataset =>
          val sparqlEndpoint = RdfDatasetAccess.forExecution(datasetTask).sparqlEndpoint
          val entities = new LocalSparqlSelectIterator(sparqlSelectTask, sparqlEndpoint, Some(datasetTask), None, sourceFetchSize, executionReportUpdater = None)
          val entityDatasource = EntityDatasource(datasetTask, entities, sparqlSelectTask.outputSchema)
          try {
            entityDatasource.peak(ruleSchemata.inputSchema, sourceFetchSize).use { exampleEntities =>
              generateMappingPreviewResponse(
                ruleSchemata.transformRule.execution(TaskContext.noInput()),
                exampleEntities,
                limit, offset, sourceFetchSize, search, computeTotal, perRecord
              )
            }
          } catch {
            case pe: PeakException =>
              Ok(Json.toJson(PeakResults(None, None, PeakStatus(NOT_SUPPORTED_STATUS_MSG, s"Input task '$inputTaskLabel'" +
                " of type '" + sparqlSelectTask.pluginSpec.label +
                "' raised following issue:" + pe.msg))))
          }
        case _ =>
          throw new ValidationException(s"Configured dataset $sparqlDataset for task '$inputTaskLabel' offers no SPARQL endpoint!")
      }
    }
  }

  // Generate the HTTP response for the mapping transformation preview
  private def generateMappingPreviewResponse(ruleExecution: TransformRuleExecution,
                                             exampleEntities: Iterator[Entity],
                                             limit: Int,
                                             offset: Int,
                                             sourceFetchSize: Int,
                                             search: Option[String],
                                             computeTotal: Boolean,
                                             perRecord: Boolean)
                                            (implicit prefixes: Prefixes) = {
    val rule = ruleExecution.operator
    val (tryCounter, errorCounter, errorMessage, sourceAndTargetResults, hasMore, totalWithinBudget) =
      collectTransformationExamples(ruleExecution, exampleEntities, limit, offset, search, computeTotal, perRecord)
    // Only expose pagination metadata when the caller asked for total/pagination. Otherwise scanning
    // stopped at `limit` and the counts wouldn't be meaningful.
    val nextOffset = if (computeTotal && hasMore) Some(offset + limit) else None
    val total = if (computeTotal) Some(totalWithinBudget) else None
    // The source returns at most `sourceFetchSize` entities. If we consumed fewer, the iterator
    // ran dry naturally and `total` is the true count. If we hit the cap, more results may exist.
    val totalIsExact = if (computeTotal) Some(tryCounter < sourceFetchSize) else None
    if (sourceAndTargetResults.nonEmpty && errorMessage.nonEmpty) {
      Ok(Json.toJson(PeakResults(Some(rule.sourcePaths.map(serializePath)), Some(sourceAndTargetResults),
        status = PeakStatus("with exceptions", errorMessage), nextOffset = nextOffset, total = total, totalIsExact = totalIsExact)))
    } else if (sourceAndTargetResults.nonEmpty) {
      Ok(Json.toJson(PeakResults(Some(rule.sourcePaths.map(serializePath)), Some(sourceAndTargetResults),
        status = PeakStatus("success", ""), nextOffset = nextOffset, total = total, totalIsExact = totalIsExact)))
    } else if (errorCounter > 0) {
      Ok(Json.toJson(PeakResults(Some(rule.sourcePaths.map(serializePath)), Some(sourceAndTargetResults),
        status = PeakStatus("empty with exceptions",
          s"Transformation result has always been empty or exceptions occurred. $tryCounter processed and $errorCounter exceptions occurred. " +
            "First exception: " + errorMessage), nextOffset = nextOffset, total = total, totalIsExact = totalIsExact)))
    } else {
      Ok(Json.toJson(PeakResults(Some(rule.sourcePaths.map(serializePath)), Some(sourceAndTargetResults),
        status = PeakStatus("empty", s"Transformation result has always been empty. Processed first $tryCounter entities."), nextOffset = nextOffset, total = total, totalIsExact = totalIsExact)))
    }
  }

  private def serializePath(path: Path)
                           (implicit prefixes: Prefixes): Seq[String] = {
    path.operators.map { op =>
      op.serialize
    }
  }

  private def projectAndTask(projectName: String, taskName: String)
                            (implicit userContext: UserContext): (Project, ProjectTask[TransformSpec]) = {
    getProjectAndTask[TransformSpec](projectName, taskName)
  }

}

object PeakTransformApi {

  // Max number of exceptions before aborting the mapping preview call
  final val MAX_TRANSFORMATION_PREVIEW_EXCEPTIONS: Int = 50
  // The number of transformation preview results that should be returned by the REST API
  final val TRANSFORMATION_PREVIEW_LIMIT: Int = 3
  final val TRANSFORMATION_PREVIEW_LIMIT_STR = "3"
  // Maximum number of empty transformation results to skip during the mapping preview calculation
  final val MAX_TRANSFORMATION_PREVIEW_SKIP_EMPTY_RESULTS: Int = 500
  // Max number of entities to examine for the mapping preview
  final val MAX_TRY_ENTITIES_DEFAULT: Int = MAX_TRANSFORMATION_PREVIEW_EXCEPTIONS + TRANSFORMATION_PREVIEW_LIMIT + MAX_TRANSFORMATION_PREVIEW_SKIP_EMPTY_RESULTS
  final val MAX_TRY_ENTITIES_DEFAULT_STR = "553"

  final val NOT_SUPPORTED_STATUS_MSG = "not supported"

  /**
    * @param ruleExecution   The contextualized transformation rule to execute on the example entities.
    * @param exampleEntities Entities to try executing the transform rule on
    * @param limit           Limit of examples to return
    * @param perRecord       When false (default), only entities whose transform produced a non-empty result
    *                        count as examples - empty results are scanned over and skipped. This is the legacy
    *                        Mapping Editor behaviour: the preview shows real transformed values, not blank rows.
    *                        When true, every tried entity is kept as its own row (including empty results), so
    *                        callers get one grouped row per source record. MappingCreatorV2 relies on this.
    */
  def collectTransformationExamples(ruleExecution: TransformRuleExecution,
                                    exampleEntities: Iterator[Entity],
                                    limit: Int,
                                    offset: Int = 0,
                                    search: Option[String] = None,
                                    computeTotal: Boolean = false,
                                    perRecord: Boolean = false): (Int, Int, String, Seq[PeakResult], Boolean, Int) = {
    // Use the non-throwing apply so a record whose transformed values fail target validation (e.g. a
    // multi-valued source mapped onto a single-cardinality target) still surfaces as a row, with the
    // validation error attached, instead of being silently dropped into the catch (NonFatal) branch.
    val ruleApply: Entity => Value = ruleExecution match {
      case ve: ValueTransformRuleExecution => ve.applyKeepingValidationErrors
      case other =>
        throw new IllegalArgumentException(s"Cannot generate a mapping preview for non-value rule '${other.operator.id}'.")
    }
    // Number of examples collected (after skipping `offset` successful ones)
    var exampleCounter = 0
    // Number of successful (and matching) results skipped to honor `offset`
    var skippedCounter = 0
    // Number of successful (and matching) results encountered after the page was filled
    var tailCounter = 0
    // Number of exceptions occurred
    var errorCounter = 0
    // Number of example entities tried
    var tryCounter = 0
    // Record the first error message
    var errorMessage: String = ""
    val resultBuffer = ArrayBuffer[PeakResult]()
    // Lower-cased needle once; None means "no filter".
    val needle = search.map(_.toLowerCase)
    // When computeTotal is true we drain the iterator so we can report a true count of matching
    // results within the budget passed to the underlying source. Otherwise stop as soon as the
    // page is filled to avoid scanning the tail.
    while (exampleEntities.hasNext && (computeTotal || skippedCounter < offset || exampleCounter < limit)) {
      tryCounter += 1
      val entity = exampleEntities.next()
      try {
        val transformResult = ruleApply(entity)
        for(error <- transformResult.errors) {
          errorCounter += 1
          if (errorMessage.isEmpty) {
            errorMessage = formatError(error.error)
          }
        }
        // Flag the individual row with its first error (if any) so a client can distinguish an empty
        // target caused by validation failure from one caused by the record simply having no value.
        val rowError = transformResult.errors.headOption.map(e => formatError(e.error))
        // In legacy (non per-record) mode an entity that transformed to no value is not an example row at
        // all: it is scanned over just like a search miss, so the preview only ever shows real results.
        val countsAsResult = perRecord || transformResult.values.nonEmpty
        if (countsAsResult && matchesSearch(entity.values, transformResult.values, needle)) {
          if (skippedCounter < offset) {
            skippedCounter += 1
          } else if (exampleCounter < limit) {
            resultBuffer.append(PeakResult(entity.values, transformResult.values, rowError))
            exampleCounter += 1
          } else {
            tailCounter += 1
          }
        }
      } catch {
        case NonFatal(ex) =>
          errorCounter += 1
          if (errorMessage.isEmpty) {
            errorMessage = formatError(ex)
          }
      }
    }
    // In drain mode `hasMore` is exact: it counts matching results past the page. In lazy mode
    // we fall back to "is the source iterator exhausted?" - that may over-report (more entities
    // exist but none of them would have matched) but never under-reports.
    val hasMore = if (computeTotal) tailCounter > 0 else exampleEntities.hasNext
    val totalWithinBudget = skippedCounter + exampleCounter + tailCounter
    (tryCounter, errorCounter, errorMessage, resultBuffer.toSeq, hasMore, totalWithinBudget)
  }

  // Format a throwable as "SimpleClassName: message" - the shape used both for the global first-error
  // message and for the per-row error flag.
  private def formatError(error: Throwable): String = {
    error.getClass.getSimpleName + ": " + Option(error.getMessage).getOrElse("")
  }

  // Case-insensitive substring match against any source value or transformed value.
  private def matchesSearch(sourceValues: Seq[Seq[String]], transformedValues: Seq[String], needle: Option[String]): Boolean = {
    needle match {
      case None => true
      case Some(n) =>
        sourceValues.exists(_.exists(v => v != null && v.toLowerCase.contains(n))) ||
          transformedValues.exists(v => v != null && v.toLowerCase.contains(n))
    }
  }
}

// Peak API
case class PeakResults(sourcePaths: Option[Seq[Seq[String]]],
                       results: Option[Seq[PeakResult]],
                       status: PeakStatus,
                       nextOffset: Option[Int] = None,
                       total: Option[Int] = None,
                       totalIsExact: Option[Boolean] = None)

case class PeakStatus(id: String, msg: String)

// `error`, when set, flags a row whose transformed values failed target validation (or otherwise
// errored). It is an optional field: a JSON Option serializes as omitted/null, so existing clients
// that don't know about it are unaffected.
case class PeakResult(sourceValues: Seq[Seq[String]], transformedValues: Seq[String], error: Option[String] = None)

object PeakResults {
  implicit val peakStatusWrites: Format[PeakStatus] = Json.format[PeakStatus]
  implicit val peakResultWrites: Format[PeakResult] = Json.format[PeakResult]
  implicit val peakResultsWrites: Format[PeakResults] = Json.format[PeakResults]
}
