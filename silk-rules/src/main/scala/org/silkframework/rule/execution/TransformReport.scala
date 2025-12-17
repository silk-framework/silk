package org.silkframework.rule.execution

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.dataset.DatasetSpec.{EntitySinkWrapper, GenericDatasetSpec}
import org.silkframework.dataset.rdf.RdfDataset
import org.silkframework.dataset.{CombinedEntitySink, DataSink, TripleSink}
import org.silkframework.execution.ExecutionReport
import org.silkframework.execution.report.SampleEntities
import org.silkframework.rule.TransformSpec
import org.silkframework.rule.execution.TransformReport._
import org.silkframework.util.Identifier

import java.time.Instant

/**
  * Holds the state of the transform execution.
  *
  * @param entityCount The number of entities that have been transformed, including erroneous entities.
  * @param entityErrorCount The number of entities that have been erroneous.
  * @param ruleResults The transformation statistics for each mapping rule by name.
  */
case class TransformReport(task: Task[TransformSpec],
                           entityCount: Int = 0,
                           entityErrorCount: Int = 0,
                           ruleResults: Map[Identifier, RuleResult] = Map.empty,
                           globalErrors: Seq[String] = Seq.empty,
                           isDone: Boolean = false,
                           override val error: Option[String] = None,
                           override val sampleOutputEntities: Vector[SampleEntities] = Vector.empty,
                           context: Option[TransformReportExecutionContext] = None) extends ExecutionReport {

  lazy val summary: Seq[(String, String)] = {
    Seq(
      "Number of entities" -> entityCount.toString,
      "Entities with issues" -> entityErrorCount.toString
    )
  }

  def warnings: Seq[String] = {
    var allErrors = globalErrors
    if(entityErrorCount != 0) {
      allErrors :+= s"Validation issues occurred on $entityErrorCount entities."
    }
    allErrors
  }

  /**
    * Short description of the operation (plural, past tense).
    */
  override def operationDesc: String = "entities generated"

  /**
    * Returns a done version of this report.
    */
  def asDone(): ExecutionReport = copy(isDone = true)

  /** Add more sample entities to the report. */
  override def withSampleOutputEntities(sampleEntities: SampleEntities): ExecutionReport = this.copy(
    sampleOutputEntities = TransformReport.updateOutputSampleEntities(sampleEntities, this.sampleOutputEntities)
  )
}

object TransformReport {

  /**
    * The transformation statistics for a single mapping rule.
    *
    * @param errorCount The number of (validation) errors for this rule.
    * @param sampleErrors Samples of erroneous values. This is just an excerpt. If all erroneous values are needed,
    *                     the transform executor needs to be configured with an error output.
    * @param started     The time when the rule has started executing, i.e. when the first input entity has been requested.
    * @param finished    The time when the rule has finished executing successfully. This will be probably missing when
    *                    the execution has been aborted.
    */
  case class RuleResult(errorCount: Long = 0L,
                        sampleErrors: IndexedSeq[RuleError] = IndexedSeq.empty,
                        started: Option[Instant] = None,
                        finished: Option[Instant] = None) {

    /**
      * Increases the error counter, but does not add a new sample error.
      */
    def withError(): RuleResult = {
      copy(
        errorCount = errorCount + 1
      )
    }

    /**
      * Increases the error counter and adds a new sample error.
      */
    def withError(error: RuleError): RuleResult = {
      copy(
        errorCount = errorCount + 1,
        sampleErrors :+ error
      )
    }

    def withStarted(): RuleResult = {
      copy(
        started = Some(Instant.now())
      )
    }

    def withFinished(): RuleResult = {
      copy(
        finished = Some(Instant.now())
      )
    }
  }

  /**
    * A single transformation error.
    *
    * @param entity The URI of the entity for which the error occurred.
    * @param value The erroneous value
    * @param message The error description
    */
  case class RuleError(entity: String, value: Seq[Seq[String]], message: String, operatorId: Option[Identifier] = None, exception: Option[Throwable])

  object RuleError {
    def fromException(entity: String, value: Seq[Seq[String]], exception: Throwable, operatorId: Option[Identifier] = None): RuleError = {
      new RuleError(entity, value, exception.getMessage, operatorId, Some(exception))
    }
  }

  def updateOutputSampleEntities(newSampleEntities: SampleEntities, currentSampleEntities: Vector[SampleEntities]): Vector[SampleEntities] = {
    if (currentSampleEntities.isEmpty ||
      (newSampleEntities.schema != currentSampleEntities.last.schema || newSampleEntities.id != currentSampleEntities.last.id)) {
      // Create new entry if either the schema or ID is different than the last entry
      currentSampleEntities :+ newSampleEntities
    } else {
      // Else replace last entry
      currentSampleEntities.dropRight(1) :+ newSampleEntities
    }
  }
}

/** Additional information about the context the transformation was executed in
  *
  * @param entityUriOutput If the entity URI is output in any way. This might be relevant when displaying the report.
  */
case class TransformReportExecutionContext(entityUriOutput: Boolean)

object TransformReportExecutionContext {
  final val default: TransformReportExecutionContext = TransformReportExecutionContext(false)

  def apply(outputTask: Task[TaskSpec]): TransformReportExecutionContext = {
    outputTask.data match {
      case datasetSpec: GenericDatasetSpec => apply(datasetSpec)
      case _ => TransformReportExecutionContext.default
    }
  }

  def apply(sink: DataSink): TransformReportExecutionContext = {
    sink match {
      case combined: CombinedEntitySink =>
        val reports = combined.sinks.map(sink => apply(sink))
        TransformReportExecutionContext(reports.exists(r => r.entityUriOutput))
      case wrapper: EntitySinkWrapper =>
        apply(wrapper.datasetSpec)
      case _: TripleSink =>
        TransformReportExecutionContext(true)
      case _ =>
        TransformReportExecutionContext(false)
    }

  }

  def apply(outputDataset: GenericDatasetSpec): TransformReportExecutionContext = {
    val isRdfOutput = outputDataset.plugin.isInstanceOf[RdfDataset]
    val outputsUri = outputDataset.uriAttribute.isDefined
    TransformReportExecutionContext(isRdfOutput || outputsUri)
  }
}