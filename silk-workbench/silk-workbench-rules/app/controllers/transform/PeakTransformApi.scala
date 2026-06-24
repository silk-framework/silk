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
import org.silkframework.config.{Prefixes, TaskSpec}
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset._
import org.silkframework.dataset.rdf.RdfDataset
import org.silkframework.entity._
import org.silkframework.entity.paths.{Path, TypedPath, UntypedPath}
import org.silkframework.plugins.dataset.rdf.executors.{LocalSparqlSelectExecutor, LocalSparqlSelectIterator}
import org.silkframework.plugins.dataset.rdf.tasks.SparqlSelectCustomTask
import org.silkframework.rule.TransformSpec.RuleSchemata
import org.silkframework.rule.input.Value
import org.silkframework.rule.{ComplexUriMapping, ObjectMapping, TaskContext, TransformRule, TransformRuleExecution, TransformSpec, ValueTransformRuleExecution}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.serialization.ReadContext
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.{Identifier, Uri}
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
    description = "Get transformation examples for the selected transformation rule. Value rules return their transformed values; object/resource rules return the IRI minted by their URI rule. The input task of the transformation task has to be a Dataset task. Also the Dataset task must support this feature.",
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
            includeTotal: Boolean = false): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
      val (project, task) = projectAndTask(projectName, taskName)
      val transformSpec = task.data
      val ruleSchemata = transformSpec.oneRuleEntitySchemaById(ruleName).get
      val inputTaskId = transformSpec.selection.inputId
      implicit val context: PluginContext = PluginContext.fromProject(project)

      peakRule(project, inputTaskId, ruleSchemata, limit, maxTryEntities, offset, search, includeTotal)
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
                    includeTotal: Boolean = false): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val (project, task) = projectAndTask(projectName, taskName)
    val transformSpec = task.data
    val parentRule = transformSpec.oneRuleEntitySchemaById(ruleName).get
    val inputTaskId = transformSpec.selection.inputId
    implicit val readContext: ReadContext = ReadContext.fromProject(project)

    deserializeCompileTime[TransformRule]() { rule =>
      val updatedParentRule = parentRule.transformRule.withChildren(Seq(rule)).asInstanceOf[TransformRule]
      val initialRuleSchemata = RuleSchemata.create(updatedParentRule, transformSpec.selection, parentRule.inputSchema.subPath, parentRule.outputSchema.subPath)
        .copy(transformRule = rule)
      val ruleSchemata = if(objectPath.isDefined && objectPath.get.nonEmpty) {
        val inputSchema = initialRuleSchemata.inputSchema.copy(subPath = UntypedPath(initialRuleSchemata.inputSchema.subPath.operators ++ UntypedPath.parse(objectPath.get).operators))
        RuleSchemata(initialRuleSchemata.transformRule, inputSchema, initialRuleSchemata.outputSchema)
      } else {
        initialRuleSchemata
      }
      peakRule(project, inputTaskId, ruleSchemata, limit, maxTryEntities, offset, search, includeTotal)
    }
  }

  private def peakRule(project: Project, inputTaskId: Identifier, ruleSchemata: RuleSchemata, limit: Int, maxTryEntities: Int, offset: Int, search: Option[String], includeTotal: Boolean)
                      (implicit context: PluginContext): Result = {
    if (offset < 0) {
      throw new ValidationException(s"Query parameter 'offset' must be >= 0, but was $offset.")
    }
    if (limit <= 0) {
      throw new ValidationException(s"Query parameter 'limit' must be > 0, but was $limit.")
    }
    implicit val prefixes: Prefixes = project.config.prefixes
    implicit val user: UserContext = context.user

    // An ObjectMapping is a container rule and can't be previewed directly. Preview the IRI it
    // mints instead, by peeking its URI rule (a ComplexUriMapping/PatternUriMapping value rule).
    // The uriRule is just another child of the object, so this mirrors the child-value-rule schema
    // construction in TransformSpec.RuleSchemata.hasMapping.
    val effectiveSchemata = ruleSchemata.transformRule match {
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
      case _ => ruleSchemata
    }

    val inputTask = project.anyTask(inputTaskId)
    val inputTaskLabel = inputTask.label()
    // Treat blank search input as no filter so callers can safely send `?search=` from a UI textbox.
    val effectiveSearch = search.map(_.trim).filter(_.nonEmpty)
    // Pagination/filtering implies the caller wants the total; otherwise scanning stops at `limit`.
    val computeTotal = includeTotal || offset > 0 || effectiveSearch.nonEmpty
    // Source needs to yield enough entities to cover the skipped offset plus the per-page budget.
    val sourceFetchSize = offset + maxTryEntities
    inputTask.data match {
      case dataset: GenericDatasetSpec =>
        val pluginLabel = dataset.plugin.pluginSpec.label
        DataSource.pluginSource(dataset) match {
          case peakDataSource: PeakDataSource =>
            try {
              peakDataSource.peak(effectiveSchemata.inputSchema, sourceFetchSize).use { exampleEntities =>
                generateMappingPreviewResponse(effectiveSchemata.transformRule.execution(TaskContext.forInput(inputTask)), exampleEntities, limit, offset, sourceFetchSize, effectiveSearch, computeTotal)
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
        peakIntoSparqlSelectTask(project, inputTaskLabel, effectiveSchemata, limit, sourceFetchSize, offset, effectiveSearch, computeTotal, sparqlSelectTask)
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
        case rdfDataset: RdfDataset with Dataset =>
          val entities = new LocalSparqlSelectIterator(sparqlSelectTask, rdfDataset.sparqlEndpoint, sourceFetchSize, executionReportUpdater = None)
          val entityDatasource = EntityDatasource(datasetTask, entities, sparqlSelectTask.outputSchema)
          try {
            entityDatasource.peak(ruleSchemata.inputSchema, sourceFetchSize).use { exampleEntities =>
              generateMappingPreviewResponse(ruleSchemata.transformRule.execution(TaskContext.noInput), exampleEntities, limit, offset, sourceFetchSize, search, computeTotal)
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
                                             computeTotal: Boolean)
                                            (implicit prefixes: Prefixes) = {
    val rule = ruleExecution.operator
    val (tryCounter, errorCounter, errorMessage, sourceAndTargetResults, hasMore, totalWithinBudget) =
      collectTransformationExamples(ruleExecution, exampleEntities, limit, offset, search, computeTotal)
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
    */
  def collectTransformationExamples(ruleExecution: TransformRuleExecution,
                                    exampleEntities: Iterator[Entity],
                                    limit: Int,
                                    offset: Int = 0,
                                    search: Option[String] = None,
                                    computeTotal: Boolean = false): (Int, Int, String, Seq[PeakResult], Boolean, Int) = {
    val ruleApply: Entity => Value = ruleExecution match {
      case ve: ValueTransformRuleExecution => ve.apply
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
            errorMessage = error.error.getClass.getSimpleName + ": " + Option(error.error.getMessage).getOrElse("")
          }
        }
        if (matchesSearch(entity.values, transformResult.values, needle)) {
          if (skippedCounter < offset) {
            skippedCounter += 1
          } else if (exampleCounter < limit) {
            resultBuffer.append(PeakResult(entity.values, transformResult.values))
            exampleCounter += 1
          } else {
            tailCounter += 1
          }
        }
      } catch {
        case NonFatal(ex) =>
          errorCounter += 1
          if (errorMessage.isEmpty) {
            errorMessage = ex.getClass.getSimpleName + ": " + Option(ex.getMessage).getOrElse("")
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

case class PeakResult(sourceValues: Seq[Seq[String]], transformedValues: Seq[String])

object PeakResults {
  implicit val peakStatusWrites: Format[PeakStatus] = Json.format[PeakStatus]
  implicit val peakResultWrites: Format[PeakResult] = Json.format[PeakResult]
  implicit val peakResultsWrites: Format[PeakResults] = Json.format[PeakResults]
}
