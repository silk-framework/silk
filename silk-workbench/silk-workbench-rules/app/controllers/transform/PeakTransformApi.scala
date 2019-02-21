package controllers.transform

import controllers.core.RequestUserContextAction
import controllers.util.ProjectUtils._
import controllers.util.SerializationUtils._
import org.silkframework.config.{PlainTask, Prefixes, TaskSpec}
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset._
import org.silkframework.dataset.rdf.{RdfDataset, SparqlEndpointEntityTable}
import org.silkframework.entity._
import org.silkframework.plugins.dataset.rdf.executors.LocalSparqlSelectExecutor
import org.silkframework.plugins.dataset.rdf.tasks.SparqlSelectCustomTask
import org.silkframework.rule.TransformSpec.RuleSchemata
import org.silkframework.rule.{TransformRule, TransformSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.serialization.ReadContext
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Identifier
import org.silkframework.workspace.{Project, ProjectTask}
import play.api.libs.json.{Json, Writes}
import play.api.mvc._

import scala.collection.mutable.ArrayBuffer
import scala.util.control.NonFatal

class PeakTransformApi extends Controller {

  implicit private val peakStatusWrites: Writes[PeakStatus] = Json.writes[PeakStatus]
  implicit private val peakResultWrites: Writes[PeakResult] = Json.writes[PeakResult]
  implicit private val peakResultsWrites: Writes[PeakResults] = Json.writes[PeakResults]
  // Max number of exceptions before aborting the mapping preview call
  final val MAX_TRANSFORMATION_PREVIEW_EXCEPTIONS: Int = 50
  // The number of transformation preview results that should be returned by the REST API
  final val TRANSFORMATION_PREVIEW_LIMIT: Int = 3
  // Maximum number of empty transformation results to skip during the mapping preview calculation
  final val MAX_TRANSFORMATION_PREVIEW_SKIP_EMPTY_RESULTS: Int = 500
  // Max number of entities to examine for the mapping preview
  final val MAX_TRY_ENTITIES_DEFAULT: Int = MAX_TRANSFORMATION_PREVIEW_EXCEPTIONS + TRANSFORMATION_PREVIEW_LIMIT + MAX_TRANSFORMATION_PREVIEW_SKIP_EMPTY_RESULTS
  final val NOT_SUPPORTED_STATUS_MSG = "not supported"

  /**
    * Get sample source and transformed values for a named rule.
    */
  def peak(projectName: String,
           taskName: String,
           ruleName: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val (project, task) = projectAndTask(projectName, taskName)
    val transformSpec = task.data
    val ruleSchemata = transformSpec.oneRuleEntitySchemaById(ruleName).get
    val inputTaskId = transformSpec.selection.inputId

    peakRule(project, inputTaskId, ruleSchemata)
  }

  /**
    * Get sample source and transformed values for a provided rule definition.
    */
  def peakChildRule(projectName: String,
                    taskName: String,
                    ruleName: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val (project, task) = projectAndTask(projectName, taskName)
    val transformSpec = task.data
    val parentRule = transformSpec.oneRuleEntitySchemaById(ruleName).get
    val inputTaskId = transformSpec.selection.inputId
    implicit val readContext: ReadContext = ReadContext(prefixes = project.config.prefixes, resources = project.resources)

    deserializeCompileTime[TransformRule]() { rule =>
      val updatedParentRule = parentRule.transformRule.withChildren(Seq(rule)).asInstanceOf[TransformRule]
      val ruleSchemata = RuleSchemata.create(updatedParentRule, transformSpec.selection, parentRule.inputSchema.subPath).copy(transformRule = rule)
      peakRule(project, inputTaskId, ruleSchemata)
    }
  }

  private def peakRule(project: Project, inputTaskId: Identifier, ruleSchemata: RuleSchemata)
                      (implicit request: Request[AnyContent],
                       userContext: UserContext): Result = {
    val limit = request.getQueryString("limit").map(_.toInt).getOrElse(TRANSFORMATION_PREVIEW_LIMIT)
    val maxTryEntities = request.getQueryString("maxTryEntities").map(_.toInt).getOrElse(MAX_TRY_ENTITIES_DEFAULT)
    implicit val prefixes: Prefixes = project.config.prefixes

    project.anyTask(inputTaskId).data match {
      case dataset: GenericDatasetSpec =>
        dataset.plugin.source match {
          case peakDataSource: PeakDataSource =>
            try {
              val exampleEntities = peakDataSource.peak(ruleSchemata.inputSchema, maxTryEntities)
              generateMappingPreviewResponse(ruleSchemata.transformRule, exampleEntities, limit)
            } catch {
              case pe: PeakException =>
                Ok(Json.toJson(PeakResults(None, None, PeakStatus(NOT_SUPPORTED_STATUS_MSG, "Input dataset task " + inputTaskId.toString +
                  " of type " + dataset.plugin.pluginSpec.label +
                  " raised following issue:" + pe.msg))))
            }
          case _ =>
            Ok(Json.toJson(PeakResults(None, None, PeakStatus(NOT_SUPPORTED_STATUS_MSG, "Input dataset task " + inputTaskId.toString +
              " of type " + dataset.plugin.pluginSpec.label +
              " does not support transformation preview!"))))
        }
      case sparqlSelectTask: SparqlSelectCustomTask =>
        peakIntoSparqlSelectTask(project, inputTaskId, ruleSchemata, limit, maxTryEntities, sparqlSelectTask)
      case _: TransformSpec =>
        Ok(Json.toJson(PeakResults(None, None, PeakStatus(NOT_SUPPORTED_STATUS_MSG, "Input task " + inputTaskId.toString +
          " is not a Dataset. Currently mapping preview is only supported for dataset inputs."))))
      case t: TaskSpec =>
        Ok(Json.toJson(PeakResults(None, None, PeakStatus(NOT_SUPPORTED_STATUS_MSG, s"Input task $inputTaskId of type ${t.getClass.getSimpleName} " +
            s"is not supported. Currently only dataset and transform tasks support producing example values."))))
    }
  }

  private def peakIntoSparqlSelectTask(project: Project,
                                       inputTaskId: Identifier,
                                       ruleSchemata: RuleSchemata,
                                       limit: Int,
                                       maxTryEntities: Int,
                                       sparqlSelectTask: SparqlSelectCustomTask)
                                      (implicit prefixes: Prefixes,
                                       userContext: UserContext): Result = {
    val sparqlDataset = sparqlSelectTask.optionalInputDataset.sparqlEnabledDataset
    if (sparqlDataset.toString == "") {
      Ok(Json.toJson(PeakResults(None, None, PeakStatus(NOT_SUPPORTED_STATUS_MSG, s"Input task $inputTaskId of type ${sparqlSelectTask.pluginSpec.label} " +
          s"has no input dataset configured. Please configure the 'Optional SPARQL dataset' parameter."))))
    } else {
      val datasetTask = project.task[GenericDatasetSpec](sparqlDataset)
      datasetTask.data.plugin match {
        case rdfDataset: RdfDataset with Dataset =>
          val entityTable = new SparqlEndpointEntityTable(rdfDataset.sparqlEndpoint, PlainTask(sparqlDataset, DatasetSpec(rdfDataset)))
          val executor = LocalSparqlSelectExecutor()
          val entities = executor.executeOnSparqlEndpointEntityTable(sparqlSelectTask, entityTable, maxTryEntities)
          val entityDatasource = EntityDatasource(datasetTask, entities, sparqlSelectTask.outputSchema)
          try {
            val exampleEntities = entityDatasource.peak(ruleSchemata.inputSchema, maxTryEntities)
            generateMappingPreviewResponse(ruleSchemata.transformRule, exampleEntities, limit)
          } catch {
            case pe: PeakException =>
              Ok(Json.toJson(PeakResults(None, None, PeakStatus(NOT_SUPPORTED_STATUS_MSG, "Input task " + inputTaskId.toString +
                  " of type " + sparqlSelectTask.pluginSpec.label +
                  " raised following issue:" + pe.msg))))
          }
        case _ =>
          throw new ValidationException(s"Configured dataset $sparqlDataset for task $inputTaskId offers no SPARQL endpoint!")
      }
    }
  }

  // Generate the HTTP response for the mapping transformation preview
  private def generateMappingPreviewResponse(rule: TransformRule,
                                             exampleEntities: Traversable[Entity],
                                             limit: Int)
                                            (implicit prefixes: Prefixes) = {
    val (tryCounter, errorCounter, errorMessage, sourceAndTargetResults) = collectTransformationExamples(rule, exampleEntities, limit)
    if (sourceAndTargetResults.nonEmpty) {
      Ok(Json.toJson(PeakResults(Some(rule.sourcePaths.map(serializePath)), Some(sourceAndTargetResults),
        status = PeakStatus("success", ""))))
    } else if (errorCounter > 0) {
      Ok(Json.toJson(PeakResults(Some(rule.sourcePaths.map(serializePath)), Some(sourceAndTargetResults),
        status = PeakStatus("empty with exceptions",
          s"Transformation result was always empty or exceptions occurred. $tryCounter processed and $errorCounter exceptions occurred. " +
            "First exception: " + errorMessage))))
    } else {
      Ok(Json.toJson(PeakResults(Some(rule.sourcePaths.map(serializePath)), Some(sourceAndTargetResults),
        status = PeakStatus("empty", s"Transformation result was always empty. Processed first $tryCounter entities."))))
    }
  }

  /**
    *
    * @param rule            The transformation rule to execute on the example entities.
    * @param exampleEntities Entities to try executing the tranform rule on
    * @param limit           Limit of examples to return
    * @return
    */
  def collectTransformationExamples(rule: TransformRule, exampleEntities: Traversable[Entity], limit: Int): (Int, Int, String, Seq[PeakResult]) = {
    // Number of examples collected
    var exampleCounter = 0
    // Number of exceptions occurred
    var errorCounter = 0
    // Number of example entities tried
    var tryCounter = 0
    // Record the first error message
    var errorMessage: String = ""
    val resultBuffer = ArrayBuffer[PeakResult]()
    val entityIterator = exampleEntities.toIterator
    while (entityIterator.hasNext && exampleCounter < limit) {
      tryCounter += 1
      val entity = entityIterator.next()
      try {
        val transformResult = rule(entity)
        if (transformResult.nonEmpty) {
          resultBuffer.append(PeakResult(entity.values, transformResult))
          exampleCounter += 1
        }
      } catch {
        case NonFatal(ex) =>
          errorCounter += 1
          if (errorMessage.isEmpty) {
            errorMessage = ex.getClass.getSimpleName + ": " + Option(ex.getMessage).getOrElse("")
          }
      }
    }
    (tryCounter, errorCounter, errorMessage, resultBuffer)
  }

  private def serializePath(path: TypedPath)
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
