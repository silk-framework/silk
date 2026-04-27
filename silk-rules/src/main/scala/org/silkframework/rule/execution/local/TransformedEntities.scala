package org.silkframework.rule.execution.local

import org.silkframework.config.{Prefixes, Task}
import org.silkframework.entity.metadata.EntityMetadata
import org.silkframework.entity.{Entity, EntitySchema, ValueType}
import org.silkframework.execution.report.EntitySample
import org.silkframework.execution.{AbortExecutionException, ExecutionException, ExecutionReport}
import org.silkframework.failures.EntityException
import org.silkframework.rule.execution.TransformReportBuilder
import org.silkframework.rule.{RootMappingRule, TransformRule, TransformRuleExecution, TransformSpec, ValueTransformRuleExecution}
import org.silkframework.runtime.iterator.CloseableIterator
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.{Identifier, Uri}

import java.util.logging.Logger
import scala.collection.mutable
import scala.util.control.NonFatal

/**
  * Entities that come from a transform task.
  *
  * @param task              The transform task these entities originate from.
  * @param entities          The source entities the transformation is applied to.
  * @param ruleExecution     The contextualized container rule that is applied against the source entities.
  * @param outputSchema      The output schema of the transformation.
  * @param isRequestedSchema True, if the output schema was requested by the following task. False if this is the output
  *                          schema defined by the mapping itself. A requested schema is type agnostic.
  */
class TransformedEntities(task: Task[TransformSpec],
                          entities: CloseableIterator[Entity],
                          ruleLabel: String,
                          ruleExecution: TransformRuleExecution,
                          outputSchema: EntitySchema,
                          isRequestedSchema: Boolean,
                          abortIfErrorsOccur: Boolean,
                          report: TransformReportBuilder)(implicit prefixes: Prefixes) {

  private val log: Logger = Logger.getLogger(this.getClass.getName)

  private val rule: TransformRule = ruleExecution.operator

  private val rules = rule.rules

  private val ruleExecutions: Seq[TransformRuleExecution] = ruleExecution.childExecutions

  private val subjectRuleExecution = ruleExecutions.find(_.operator.target.isEmpty)

  private val propertyRuleExecutions = ruleExecutions.filter(_.operator.target.nonEmpty).toIndexedSeq

  // For each schema path, collect all rule executions that map to it
  private val ruleExecutionsPerPath = {
    if(isRequestedSchema) {
      for(path <- outputSchema.typedPaths) yield propertyRuleExecutions.filter(_.operator.target.get.asPath() == path.asUntypedPath)
    } else {
      for(path <- outputSchema.typedPaths) yield propertyRuleExecutions.filter(_.operator.target.get.asTypedPath() == path)
    }
  }

  private val updateIntervalInMS = 500

  private var count = 0

  private var errorFlag = false

  private val errors = mutable.Buffer[Throwable]()

  def iterator: CloseableIterator[Entity] = {
    count = 0
    var lastUpdateTime = System.currentTimeMillis()
    var lastCount = 0
    report.addRules(rules)

    val mappedEntities =
      for (entity <- entities; mappedEntity <- mapEntity(entity, report)) yield {
        if(count < ExecutionReport.SAMPLE_ENTITY_LIMIT) {
          if(count == 1) {
            report.setContainerRule(rule.id.toString, outputSchema)
          }
          report.sampleOutputEntity(EntitySample.entityToEntitySample(mappedEntity))
        }
        if (count > lastCount && (System.currentTimeMillis() - lastUpdateTime) > updateIntervalInMS) {
          report.build(logMessage = true)
          lastUpdateTime = System.currentTimeMillis()
          lastCount = count
        }
        mappedEntity
      }
    new TransformReportIterator(mappedEntities.thenClose(() => report.build()), report, rules.map(_.id.toString).toSet)
  }

  private def mapEntity(entity: Entity, report: TransformReportBuilder): Iterator[Entity] = {
    errorFlag = false
    errors.clear()

    val uris = subjectRuleExecution match {
      case Some(subjectExec) => evaluateRule(entity, subjectExec, report).iterator
      case None => Iterator(entity.uri.toString)
    }

    if(uris.nonEmpty) {
      for (uri <- uris) yield {
        if(count > 0 && outputSchema.singleEntity && rule.isInstanceOf[RootMappingRule]) {
          throw new MultipleValuesException(s"Tried to generate multiple entities, but the '$ruleLabel' mapping is configured to output a single entity.")
        }

        lazy val objectEntity = { // Constructs an entity that only contains object source paths for object mappings
          val uriTypePaths = entity.schema.typedPaths.zip(entity.values).filter(_._1.valueType == ValueType.URI)
          val typedPaths = uriTypePaths.map(_._1)
          val values = uriTypePaths.map(_._2)
          Entity(entity.uri, values, entity.schema.copy(typedPaths = typedPaths))
        }

        def evalRule(exec: TransformRuleExecution): Seq[String] = { // evaluate rule on the correct entity representation
          if (exec.operator.representsDefaultUriRule) {
            evaluateRule(objectEntity, exec, report)
          } else {
            evaluateRule(entity, exec, report) // This works even though there are still object paths mixed in, because they all are at the end
          }
        }

        val values = for (rulesAtPath <- ruleExecutionsPerPath) yield {
          rulesAtPath.flatMap(evalRule)
        }

        updateReport(report)
        count += 1
        Entity(uri, values, outputSchema, metadata = buildErrorMetadata())
      }
    } else {
      for(uriRuleExec <- subjectRuleExecution) {
        report.addRuleError(uriRuleExec.operator, entity, new ValidationException("The URI pattern did not generate any URI for this entity."))
      }
      errorFlag = true
      Iterator.empty
    }
  }

  private def buildErrorMetadata(): EntityMetadata = {
    if(!errorFlag) {
      EntityMetadata()
    } else if(errors.size == 1) {
      EntityMetadata(new EntityException("", errors.head, task.id))
    } else {
      EntityMetadata(new EntityException("Multiple errors: " + errors.map(_.getMessage).mkString(", "), errors.head, task.id))
    }
  }

  private def updateReport(report: TransformReportBuilder): Unit = {
    report.incrementEntityCounter()
    if (errorFlag) {
      report.incrementEntityErrorCounter()
    }
  }

  private def evaluateRule(entity: Entity, exec: TransformRuleExecution, report: TransformReportBuilder): Seq[String] = {
    val rule = exec.operator
    try {
      val result = exec match {
        case ve: ValueTransformRuleExecution => ve(entity)
        case _ =>
          throw new IllegalStateException(s"Cannot evaluate non-value rule '${rule.id}' on an entity.")
      }
      for(error <- result.errors) {
        addError(report, rule, entity, error.error, Some(error.operatorId))
      }
      result.values
    } catch {
      case ex: ExecutionException if ex.abortExecution =>
        throw ex
      case ex: MultipleValuesException =>
        throw ex
      case NonFatal(ex) =>
        addError(report, rule, entity, ex)
        Seq.empty
    }
  }

  private def addError(report: TransformReportBuilder, rule: TransformRule, entity: Entity, ex: Throwable, operatorId: Option[Identifier] = None): Unit = {
    log.fine("Error during execution of transform rule " + rule.id.toString + ": " + ex.getMessage)
    report.addRuleError(rule, entity, ex, operatorId)
    errors.append(ex)
    errorFlag = true
    if(abortIfErrorsOccur) {
      val message = "Transform task is configured to fail if any error occurs: " +
        s"Failed to transform entity '${entity.uri}' with rule '${rule.label()}' in '$ruleLabel': ${ex.getMessage}"
      throw AbortExecutionException(message, Some(ex))
    }
  }

  private class TransformReportIterator(iterator: CloseableIterator[Entity], report: TransformReportBuilder, coveredRules: Set[String]) extends CloseableIterator[Entity] {
    @volatile
    private var reportDone = false
    @volatile
    private var started = false

    private def closeReport(): Unit = {
      if(!reportDone) {
        report.build(isDone = true)
        reportDone = true
      }
    }

    override def hasNext: Boolean = {
      if(!started) {
        started = true
        report.setStarted(coveredRules)
      }
      try {
        if (iterator.hasNext) {
          true
        } else {
          report.setFinished(coveredRules)
          closeReport()
          false
        }
      } catch {
        case NonFatal(ex) =>
          report.setExecutionError(ex.getMessage)
          closeReport()
          throw ex
      }
    }

    override def next(): Entity = {
      try {
        iterator.next()
      } catch {
        case NonFatal(ex) =>
          report.setExecutionError(ex.getMessage)
          closeReport()
          throw ex
      }
    }

    override def close(): Unit = {
      closeReport()
      iterator.close()
    }
  }
}
