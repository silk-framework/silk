package controllers.transform.transformTask

import controllers.util.ProjectUtils.{connectedSubgraph, inMemoryModelSink}
import org.apache.jena.rdf.model.Model
import org.silkframework.config.{DefaultConfig, PlainTask, Prefixes}
import org.silkframework.dataset.{EntitySink, TypedProperty}
import org.silkframework.execution.TaskException
import org.silkframework.rule.execution.ExecuteTransform
import org.silkframework.rule.{MappingRuleTreePruning, TransformSpec}
import org.silkframework.runtime.activity.{Activity, UserContext}
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.validation.{BadUserInputException, RequestException}
import org.silkframework.util.{Identifier, Uri}
import org.silkframework.workspace.activity.transform.TransformTaskUtils.{TransformTask => TransformTaskOps}
import org.silkframework.workspace.{Project, ProjectTask}

import scala.collection.mutable
import scala.jdk.CollectionConverters._

/**
  * Assembles the paged Turtle debug output for a selection of mapping rules (nodes) of a transform task:
  * rule id validation, mapping tree pruning, transform execution into a bounded in-memory Jena model and
  * assembly of the per-record subgraph. Used by the `.../transform/tasks/:project/:task/turtle` endpoint.
  */
object TurtleOutputService {

  /** Config key for the maximum allowed value of the `limit` query parameter (root records per page). */
  final val maxLimitConfigKey = "workbench.transform.turtleOutput.maxLimit"
  final val defaultMaxLimit = 1000

  /** Config key for the maximum number of statements that may be materialized into the in-memory debug model. */
  final val maxStatementsConfigKey = "workbench.transform.turtleOutput.maxStatements"
  final val defaultMaxStatements = 100000L

  /** Read per request so config changes (and test overrides) take effect without re-initialization. */
  def maxLimit: Int = DefaultConfig.instance.extendedTypesafeConfig().getIntOrElse(maxLimitConfigKey, defaultMaxLimit)

  /** Read per request so config changes (and test overrides) take effect without re-initialization. */
  def maxStatements: Long = DefaultConfig.instance.extendedTypesafeConfig().getLongOrElse(maxStatementsConfigKey, defaultMaxStatements)

  /**
    * Thrown when the debug transform materializes more statements than [[maxStatements]] allows.
    * Results in a 413 (Payload Too Large) problem-details response.
    */
  case class TurtleOutputTooLargeException(msg: String) extends RequestException(msg, None) {
    override def errorTitle: String = "Turtle debug output too large"
    override def httpErrorCode: Option[Int] = Some(413) // Payload Too Large
  }

  /**
    * Executes the transformation restricted to the selected rules and returns the RDF of the page
    * `[offset, offset + limit)` of top-level input records: each root subject together with all of its
    * nested, linked entities.
    *
    * @param project         The project of the transform task.
    * @param task            The transform task. Its input task must be a dataset task that supports reading entities.
    * @param selectedRuleIds The ids of the selected mapping rules (nodes). Must be non-empty and exist in the task.
    * @param offset          The number of top-level input records to skip before the returned page. Must be >= 0.
    * @param limit           The number of top-level input records in the page. Must be in (0, maxLimit].
    * @return The Jena model of the requested page, with the project prefixes applied.
    */
  def outputPageModel(project: Project,
                      task: ProjectTask[TransformSpec],
                      selectedRuleIds: Set[Identifier],
                      offset: Int,
                      limit: Int)
                     (implicit userContext: UserContext): Model = {
    validatePaging(offset, limit)
    validateRuleIds(task, selectedRuleIds)
    val prunedRoot = MappingRuleTreePruning.pruneRoot(task.data.mappingRule, selectedRuleIds)
      .getOrElse(throw new BadUserInputException("The selected rules do not produce any output."))
    val prunedTask = PlainTask(task.id, task.data.copy(mappingRule = prunedRoot), task.metaData)

    // Resolve the data source up front so that an unsupported input task (e.g. a non-dataset 'Other'
    // task) surfaces as a 400 instead of escaping the activity as a 500.
    val dataSource =
      try {
        task.dataSource
      } catch {
        case ex: TaskException =>
          throw new BadUserInputException(ex.getMessage)
      }

    val (model, baseSink) = inMemoryModelSink()
    // Records the subjects minted for the root (top-level) entities so we can page by input record.
    // Also enforces the statement cap: the model is in-memory, so an unbounded transform is an OOM vector.
    val recordingSink = new RootSubjectRecordingSink(baseSink, maxStatements)
    // Bound the root-record scan to the page window instead of materializing the whole dataset on every
    // (single-record) page request. This is only safe when the pruned transform produces a single output
    // table (no object/nested rules): there are no child levels that could be truncated, and with no object
    // links there is no cross-record bleed that the full root set (foreign-root boundary in connectedSubgraph)
    // would have to guard against. For multi-level transforms we keep the full materialization so complete
    // records and the anti-bleed boundary stay correct. The stop condition counts DISTINCT root subjects (the
    // recording sink de-duplicates by IRI), so heavy root-IRI de-duplication cannot under-fill the
    // page - it keeps reading root records until enough distinct roots for [offset, offset + limit) are seen.
    val singleOutputTable = prunedTask.data.ruleSchemataWithoutEmptyObjectRules.lengthCompare(1) == 0
    val requiredRootCount = offset.toLong + limit.toLong // widened so a near-Int.MaxValue offset can't overflow
    val rootTableStopCondition: () => Boolean =
      if (singleOutputTable) () => recordingSink.rootSubjects.size.toLong >= requiredRootCount
      else () => false
    val transform = new ExecuteTransform(
      task = prunedTask,
      inputTask = (uc: UserContext) => project.anyTask(task.data.selection.inputId)(uc),
      input = _ => dataSource,
      output = _ => recordingSink,
      pluginContext = (uc: UserContext) => PluginContext.fromProject(project)(uc),
      rootTableStopCondition = rootTableStopCondition
    )
    Activity(transform).startBlocking()

    // A page = the complete output of the root records [offset, offset + limit): each root subject
    // plus all entities reachable from it (its nested, linked children). Passing all root subjects as
    // boundaries keeps records that merely share a referenced entity (same author, publisher, ...) from
    // bleeding into the page through the backward/inverse link traversal.
    val pageRootSubjects = recordingSink.rootSubjects.drop(offset).take(limit)
    // Follow inverse links backward only for predicates that are actually backward/inverse property
    // mappings in this transform. Otherwise the walk would hop through shared object IRIs (e.g. the
    // rdf:type class shared by every entity of a type, or a shared author) into other records.
    val backwardPredicates: Set[String] =
      prunedRoot.rules.allRulesRecursive.flatMap(_.target).collect {
        case target if target.isBackwardProperty => target.propertyUri.uri
      }.toSet
    val pageModel = connectedSubgraph(model, pageRootSubjects, recordingSink.rootSubjects.toSet, Some(backwardPredicates))
    // Add the project prefixes so the Turtle output uses readable @prefix declarations.
    pageModel.setNsPrefixes(project.config.prefixes.prefixMap.asJava)
    pageModel
  }

  private def validatePaging(offset: Int, limit: Int): Unit = {
    if (offset < 0) {
      throw new BadUserInputException(s"Query parameter 'offset' must be >= 0, but was $offset.")
    }
    if (limit <= 0) {
      throw new BadUserInputException(s"Query parameter 'limit' must be > 0, but was $limit.")
    }
    val maxLimitValue = maxLimit
    if (limit > maxLimitValue) {
      throw new BadUserInputException(s"Query parameter 'limit' must be <= $maxLimitValue, but was $limit. " +
        s"The maximum page size of this debug endpoint can be configured via '$maxLimitConfigKey'.")
    }
  }

  private def validateRuleIds(task: ProjectTask[TransformSpec], selectedRuleIds: Set[Identifier]): Unit = {
    if (selectedRuleIds.isEmpty) {
      throw new BadUserInputException("No rule ids provided in 'ruleIds'.")
    }
    val knownRuleIds = task.data.allRulesRecursive.map(_.id).toSet
    val unknownRuleIds = selectedRuleIds.diff(knownRuleIds)
    if (unknownRuleIds.nonEmpty) {
      throw new BadUserInputException(s"The following rule ids do not exist in transform task '${task.id}': " +
        unknownRuleIds.map(_.toString).mkString(", ") + ".")
    }
  }

  /**
    * Entity sink that records the subjects written for the first (root) table, so the turtle endpoint
    * can page by top-level input record. ExecuteTransform writes one table per object level, the root
    * level first; subjects written before the first [[closeTable]] are therefore the root subjects.
    *
    * Additionally enforces the statement cap on the in-memory model: once more than `maxStatements`
    * statements have been written, a [[TurtleOutputTooLargeException]] (413) aborts the transform. The
    * count tracks the statements as written (one per value; the underlying SparqlSink buffers up to 200
    * statements before flushing into the model, so the model's own size would lag behind). This bounds
    * the memory use of multi-level transforms, whose full materialization cannot be stopped early.
    */
  private class RootSubjectRecordingSink(inner: EntitySink, maxStatements: Long) extends EntitySink {
    private val recordedSubjects = mutable.ListBuffer[String]()
    private val seen = mutable.Set[String]()
    private var firstTableClosed = false
    private var statementCount = 0L

    /** The root subjects in the order they were written, de-duplicated. */
    def rootSubjects: Seq[String] = recordedSubjects.toSeq

    override def openTable(typeUri: Uri, properties: Seq[TypedProperty], singleEntity: Boolean)
                          (implicit userContext: UserContext, prefixes: Prefixes): Unit =
      inner.openTable(typeUri, properties, singleEntity)

    override def closeTable()(implicit userContext: UserContext): Unit = {
      firstTableClosed = true
      inner.closeTable()
    }

    override def writeEntity(subject: String, values: IndexedSeq[Seq[String]])
                            (implicit userContext: UserContext): Unit = {
      if (!firstTableClosed && seen.add(subject)) {
        recordedSubjects += subject
      }
      inner.writeEntity(subject, values)
      statementCount += values.iterator.map(_.size.toLong).sum // the sink writes one statement per value
      if (statementCount > maxStatements) {
        throw TurtleOutputTooLargeException(s"The debug output exceeded the maximum of $maxStatements statements " +
          s"that this endpoint materializes in memory. Select fewer rules, use a smaller (sample) input dataset, " +
          s"or raise the limit via the config key '$maxStatementsConfigKey'.")
      }
    }

    override def clear(force: Boolean)(implicit userContext: UserContext): Unit = inner.clear(force)

    override def close()(implicit userContext: UserContext): Unit = inner.close()
  }
}
