package org.silkframework.rule.execution.local

import org.silkframework.config.{Prefixes, Task}
import org.silkframework.entity.metadata.EntityMetadata
import org.silkframework.entity.{Entity, EntitySchema, ValueType}
import org.silkframework.execution.{AbortExecutionException, ExecutionException}
import org.silkframework.failures.EntityException
import org.silkframework.rule.execution.{TransformReport, TransformReportBuilder}
import org.silkframework.rule.{RootMappingRule, TransformRule, TransformSpec}
import org.silkframework.runtime.activity.ActivityContext
import org.silkframework.runtime.iterator.CloseableIterator
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Identifier

import java.util.logging.Logger
import scala.collection.mutable
import scala.util.control.NonFatal

/**
  * Entities that come from a transform task.
  *
  * @param task              The transform task these entities originate from.
  * @param entities          The source entities the transformation is applied to.
  * @param rule              The transformation rules that are applied against the source entities.
  * @param outputSchema      The output schema of the transformation.
  * @param isRequestedSchema True, if the output schema was requested by the following task. False if this is the output
  *                          schema defined by the mapping itself. A requested schema is type agnostic.
  * @param context           The activity context.
  */
class TransformedEntities(task: Task[TransformSpec],
                          entities: CloseableIterator[Entity],
                          ruleLabel: String,
                          rule: TransformRule,
                          outputSchema: EntitySchema,
                          isRequestedSchema: Boolean,
                          abortIfErrorsOccur: Boolean,
                          context: ActivityContext[TransformReport])(implicit prefixes: Prefixes) {

  private val log: Logger = Logger.getLogger(this.getClass.getName)

  private val rules = rule.rules

  private val subjectRule = rules.find(_.target.isEmpty)

  private val propertyRules = rules.filter(_.target.nonEmpty).toIndexedSeq

  // For each schema path, collect all rules that map to it
  private val rulesPerPath = {
    if(isRequestedSchema) {
      for(path <- outputSchema.typedPaths) yield propertyRules.filter(_.target.get.asPath() == path.asUntypedPath)
    } else {
      for(path <- outputSchema.typedPaths) yield propertyRules.filter(_.target.get.asTypedPath() == path)
    }
  }

  private val updateIntervalInMS = 500

  private var count = 0

  private var errorFlag = false

  private val errors = mutable.Buffer[Throwable]()

  def iterator: CloseableIterator[Entity] = {
    val report = {
      val prevReport = context.value.get.getOrElse(TransformReport(task))
      new TransformReportBuilder(task, rules, prevReport)
    }

    count = 0
    var lastUpdateTime = System.currentTimeMillis()
    var lastCount = 0

    val mappedEntities =
      for (entity <- entities; mappedEntity <- mapEntity(entity, report)) yield {
        if (count > lastCount && (System.currentTimeMillis() - lastUpdateTime) > updateIntervalInMS) {
          context.value.update(report.build())
          context.status.updateMessage(s"Executing ($count Entities)")
          lastUpdateTime = System.currentTimeMillis()
          lastCount = count
        }
        mappedEntity
      }
    new TransformReportIterator(mappedEntities.thenClose(() => context.value.update(report.build())), report)
  }

  private def mapEntity(entity: Entity, report: TransformReportBuilder): Iterator[Entity] = {
    errorFlag = false
    errors.clear()

    val uris = subjectRule match {
      case Some(rule) => evaluateRule(entity, rule, report).iterator
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
          Entity(entity.uri, values, entity.schema.adapt(typedPaths = typedPaths))
        }

        def evalRule(rule: TransformRule): Seq[String] = { // evaluate rule on the correct entity representation
          if (rule.representsDefaultUriRule) {
            evaluateRule(objectEntity, rule, report)
          } else {
            evaluateRule(entity, rule, report) // This works even though there are still object paths mixed in, because they all are at the end
          }
        }

        val values = for (rules <- rulesPerPath) yield {
          rules.flatMap(evalRule)
        }

        updateReport(report)
        count += 1
        Entity(uri, values, outputSchema, metadata = buildErrorMetadata())
      }
    } else {
      for(uriRule <- subjectRule) {
        report.addRuleError(uriRule, entity, new ValidationException("The URI pattern did not generate any URI for this entity."))
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

  private def evaluateRule(entity: Entity, rule: TransformRule, report: TransformReportBuilder): Seq[String] = {
    try {
      val result = rule(entity)
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

  private class TransformReportIterator(iterator: CloseableIterator[Entity], report: TransformReportBuilder) extends CloseableIterator[Entity] {

    override def hasNext: Boolean = {
      try {
        if (iterator.hasNext) {
          true
        } else {
          context.value() = report.build(isDone = true)
          false
        }
      } catch {
        case NonFatal(ex) =>
          report.setExecutionError(ex.getMessage)
          context.value() = report.build(isDone = true)
          throw ex
      }
    }

    override def next(): Entity = {
      try {
        iterator.next()
      } catch {
        case NonFatal(ex) =>
          report.setExecutionError(ex.getMessage)
          context.value() = report.build(isDone = true)
          throw ex
      }
    }

    override def close(): Unit = iterator.close()
  }
}
