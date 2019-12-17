package org.silkframework.rule.execution.local

import java.util.logging.Logger

import org.silkframework.entity.{Entity, EntitySchema, UriValueType}
import org.silkframework.execution.ExecutionException
import org.silkframework.rule.TransformRule
import org.silkframework.rule.execution.{TransformReport, TransformReportBuilder}
import org.silkframework.runtime.activity.ActivityContext

import scala.util.control.NonFatal

/**
  * Entities that come from a transform task.
  *
  * @param taskLabel         The label of the transform task these entities originate from.
  * @param entities          The source entities the transformation is applied to.
  * @param rules             The transformation rules that are applied against the source entities.
  * @param outputSchema      The output schema of the transformation.
  * @param isRequestedSchema True, if the output schema was requested by the following task. False if this is the output
  *                          schema defined by the mapping itself. A requested schema is type agnostic.
  * @param context           The activity context.
  */
class TransformedEntities(taskLabel: String,
                          entities: Traversable[Entity],
                          rules: Seq[TransformRule],
                          outputSchema: EntitySchema,
                          isRequestedSchema: Boolean,
                          context: ActivityContext[TransformReport]) extends Traversable[Entity] {

  private val log: Logger = Logger.getLogger(this.getClass.getName)

  private val subjectRule = rules.find(_.target.isEmpty)

  private val propertyRules = rules.filter(_.target.nonEmpty).toIndexedSeq

  private val updateIntervalInMS = 1000

  private val report = {
    val prevReport = context.value.get.getOrElse(TransformReport(taskLabel))
    new TransformReportBuilder(prevReport.label, rules, prevReport)
  }

  private var errorFlag = false

  override def foreach[U](f: Entity => U): Unit = {
    // For each schema path, collect all rules that map to it
    val rulesPerPath = if(isRequestedSchema) { for(path <- outputSchema.typedPaths) yield
        propertyRules.filter(_.target.get.asPath() == path.asUntypedPath)
      } else { for(path <- outputSchema.typedPaths) yield
        propertyRules.filter(_.target.get.asTypedPath() == path)
      }

    var count = 0
    var lastUpdateTime = System.currentTimeMillis()
    for(entity <- entities) {
      errorFlag = false
      val uris = subjectRule match {
        case Some(rule) => evaluateRule(entity, rule)
        case None => Seq(entity.uri.toString)
      }

      for(uri <- uris) {
        lazy val objectEntity = { // Constructs an entity that only contains object source paths for object mappings
          val uriTypePaths = entity.schema.typedPaths.zip(entity.values).filter(_._1.valueType == UriValueType)
          val typedPaths = uriTypePaths.map(_._1)
          val values = uriTypePaths.map(_._2)
          Entity(entity.uri, values, entity.schema.copy(typedPaths = typedPaths))
        }
        def evalRule(rule: TransformRule): Seq[String] = { // evaluate rule on the correct entity representation
          if(rule.representsDefaultUriRule) {
            evaluateRule(objectEntity, rule)
          } else {
            evaluateRule(entity, rule) // This works even though there are still object paths mixed in, because they all are at the end
          }
        }
        val values =
          for (rules <- rulesPerPath) yield {
            rules.flatMap(evalRule)
          }
        f(Entity(uri, values, outputSchema))

        count += 1
        if (count % 1000 == 0 && (System.currentTimeMillis() - lastUpdateTime) > updateIntervalInMS) {
          context.value.update(report.build())
          context.status.updateMessage(s"Executing ($count Entities)")
          lastUpdateTime = System.currentTimeMillis()
        }
      }
      updateReport()
    }
    context.value() = report.build()
    context.status.updateMessage(s"Finished Executing ($count Entities)")
  }

  private def updateReport(): Unit = {
    report.incrementEntityCounter()
    if (errorFlag) {
      report.incrementEntityErrorCounter()
    }
  }

  private def evaluateRule(entity: Entity, rule: TransformRule): Seq[String] = {
    try {
      rule(entity)
    } catch {
      case ex: ExecutionException if ex.abortExecution =>
        throw ex
      case NonFatal(ex) =>
        log.fine("Error during execution of transform rule " + rule.id.toString + ": " + ex.getMessage)
        report.addError(rule, entity, ex)
        errorFlag = true
        Seq.empty
    }
  }
}