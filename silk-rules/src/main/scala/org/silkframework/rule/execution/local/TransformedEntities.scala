package org.silkframework.rule.execution.local

import java.util.logging.Logger

import org.silkframework.entity.{Entity, EntitySchema, UriValueType}
import org.silkframework.execution.ExecutionReport
import org.silkframework.rule.TransformRule
import org.silkframework.rule.execution.TransformReportBuilder
import org.silkframework.runtime.activity.ActivityContext

import scala.util.control.NonFatal

class TransformedEntities(entities: Traversable[Entity],
                          rules: Seq[TransformRule],
                          outputSchema: EntitySchema,
                          context: ActivityContext[ExecutionReport]) extends Traversable[Entity] {

  private val log: Logger = Logger.getLogger(this.getClass.getName)

  private val subjectRule = rules.find(_.target.isEmpty)

  private val propertyRules = rules.filter(_.target.nonEmpty).toIndexedSeq

  private val report = new TransformReportBuilder(propertyRules)

  private var errorFlag = false

  override def foreach[U](f: (Entity) => U): Unit = {
    // For each schema path, collect all rules that map to it
    val rulesPerPath =
      for(path <- outputSchema.typedPaths.map(_.path)) yield {
        propertyRules.filter(_.target.get.asPath() == path)
      }

    var count = 0
    for(entity <- entities) {
      errorFlag = false

      val uriOption = subjectRule match {
        case Some(rule) => rule(entity).headOption
        case None => Some(entity.uri.toString)
      }

      for(uri <- uriOption) {
        lazy val objectEntity = { // Constructs an entity that only contains object source paths for object mappings
          val uriTypePaths = entity.desc.typedPaths.zip(entity.values).filter(_._1.valueType == UriValueType)
          val typedPaths = uriTypePaths.map(_._1)
          val values = uriTypePaths.map(_._2)
          Entity(entity.uri, values, entity.desc.copy(typedPaths = typedPaths))
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

        f(new Entity(uri, values, outputSchema))

        report.incrementEntityCounter()
        if (errorFlag) {
          report.incrementEntityErrorCounter()
        }
        count += 1
        if (count % 1000 == 0) {
          context.value.update(report.build())
          context.status.updateMessage(s"Executing ($count Entities)")
        }
      }
    }
    context.value() = report.build()
  }

  private def evaluateRule(entity: Entity, rule: TransformRule): Seq[String] = {
    try {
      rule(entity)
    } catch {
      case NonFatal(ex) =>
        log.fine("Error during execution of transform rule " + rule.id.toString + ": " + ex.getMessage)
        report.addError(rule, entity, ex)
        errorFlag = true
        Seq.empty
    }
  }
}