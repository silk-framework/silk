package org.silkframework.rule.execution.local

import org.silkframework.config.Task
import org.silkframework.entity.metadata.{EntityMetadata, EntityMetadataXml}
import org.silkframework.entity.{Entity, EntitySchema, ValueType}
import org.silkframework.execution.{AbortExecutionException, ExecutionException}
import org.silkframework.failures.EntityException
import org.silkframework.rule.execution.{TransformReport, TransformReportBuilder}
import org.silkframework.rule.{TransformRule, TransformSpec}
import org.silkframework.runtime.activity.ActivityContext
import org.silkframework.runtime.validation.ValidationException

import java.util.logging.Logger
import scala.collection.mutable
import scala.util.control.NonFatal

/**
  * Entities that come from a transform task.
  *
  * @param task              The transform task these entities originate from.
  * @param entities          The source entities the transformation is applied to.
  * @param rules             The transformation rules that are applied against the source entities.
  * @param outputSchema      The output schema of the transformation.
  * @param isRequestedSchema True, if the output schema was requested by the following task. False if this is the output
  *                          schema defined by the mapping itself. A requested schema is type agnostic.
  * @param context           The activity context.
  */
class TransformedEntities(task: Task[TransformSpec],
                          entities: Traversable[Entity],
                          rules: Seq[TransformRule],
                          outputSchema: EntitySchema,
                          isRequestedSchema: Boolean,
                          abortIfErrorsOccur: Boolean,
                          context: ActivityContext[TransformReport]) extends Traversable[Entity] {

  private val log: Logger = Logger.getLogger(this.getClass.getName)

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

  private val updateIntervalInMS = 1000

  private var count = 0

  private var errorFlag = false

  private val errors = mutable.Buffer[Throwable]()

  override def foreach[U](f: Entity => U): Unit = {
    val report = {
      val prevReport = context.value.get.getOrElse(TransformReport(task))
      new TransformReportBuilder(task, rules, prevReport)
    }

    count = 0
    var lastUpdateTime = System.currentTimeMillis()
    for(entity <- entities) {
      mapEntity(entity, f, report)
      if (count % 1000 == 0 && (System.currentTimeMillis() - lastUpdateTime) > updateIntervalInMS) {
        context.value.update(report.build())
        context.status.updateMessage(s"Executing ($count Entities)")
        lastUpdateTime = System.currentTimeMillis()
      }
    }
    context.value() = report.build()
    context.status.updateMessage(s"Finished Executing ($count Entities)")
  }

  private def mapEntity[U](entity: Entity, f: Entity => U, report: TransformReportBuilder): Unit = {
    errorFlag = false
    errors.clear()

    val uris = subjectRule match {
      case Some(rule) => evaluateRule(entity, rule, report)
      case None => Seq(entity.uri.toString)
    }

    if(uris.nonEmpty) {
      for (uri <- uris) {
        if(outputSchema.singleEntity && count > 1) {
          throw new ValidationException("Tried to generate multiple entities, but the root mapping is configured to output a single entity.")
        }

        lazy val objectEntity = { // Constructs an entity that only contains object source paths for object mappings
          val uriTypePaths = entity.schema.typedPaths.zip(entity.values).filter(_._1.valueType == ValueType.URI)
          val typedPaths = uriTypePaths.map(_._1)
          val values = uriTypePaths.map(_._2)
          Entity(entity.uri, values, entity.schema.copy(typedPaths = typedPaths))
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

        f(Entity(uri, values, outputSchema, metadata = buildErrorMetadata()))

        updateReport(report)
        count += 1
      }
    } else {
      for(uriRule <- subjectRule) {
        report.addError(uriRule, entity, new ValidationException("The URI pattern did not generate any URI for this entity."))
      }
      errorFlag = true
    }
  }

  private def buildErrorMetadata(): EntityMetadata[_] = {
    if(!errorFlag) {
      EntityMetadataXml()
    } else if(errors.size == 1) {
      EntityMetadataXml(new EntityException("", errors.head, task.id))
    } else {
      EntityMetadataXml(new EntityException("Multiple errors: " + errors.map(_.getMessage).mkString(", "), errors.head, task.id))
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
      rule(entity)
    } catch {
      case ex: ExecutionException if ex.abortExecution =>
        throw ex
      case NonFatal(ex) =>
        addError(report, rule, entity, ex)
        Seq.empty
    }
  }

  private def addError(report: TransformReportBuilder, rule: TransformRule, entity: Entity, ex: Throwable): Unit = {
    log.fine("Error during execution of transform rule " + rule.id.toString + ": " + ex.getMessage)
    report.addError(rule, entity, ex)
    errors.append(ex)
    errorFlag = true
    if(abortIfErrorsOccur) {
      throw AbortExecutionException(s"Failed to transform entity '${entity.uri}' in rule '${rule.metaData.label}': ${ex.getMessage}", Some(ex))
    }
  }
}
