package org.silkframework.rule.execution.local

import java.util.logging.Logger

import org.silkframework.config.{PlainTask, Task}
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.execution.local.{EntityTable, GenericEntityTable, LocalExecution}
import org.silkframework.execution.{ExecutionReport, Executor}
import org.silkframework.rule.execution._
import org.silkframework.rule.{TransformRule, TransformSpec}
import org.silkframework.runtime.activity.ActivityContext
import org.silkframework.runtime.activity.Status.Finished

import scala.util.control.NonFatal

/**
  * Created on 7/20/16.
  */
class LocalTransformSpecificationExecutor extends Executor[TransformSpec, LocalExecution] {

  private val log: Logger = Logger.getLogger(this.getClass.getName)

  override def execute(task: Task[TransformSpec],
                       inputs: Seq[EntityTable],
                       outputSchema: Option[EntitySchema],
                       execution: LocalExecution,
                       context: ActivityContext[ExecutionReport]): Option[EntityTable] = {
    val input = inputs.head
    val transformSpec = task.data.copy(selection = task.data.selection.copy(inputId = input.task.id))
    val schema = outputSchema.orElse(transformSpec.outputSchemaOpt).get
    val transformedEntities = new TransformedEntities(input.entities, task, schema, context)
    Some(GenericEntityTable(transformedEntities, schema, PlainTask(task.id, transformSpec)))
  }

  private class TransformedEntities(entities: Traversable[Entity],
                                    task: Task[TransformSpec],
                                    outputSchema: EntitySchema,
                                    context: ActivityContext[ExecutionReport]) extends Traversable[Entity] {

    private val transform = task.data

    private val subjectRule = transform.rules.find(_.target.isEmpty)

    private val propertyRules = transform.rules.filter(_.target.nonEmpty).toIndexedSeq

    private val report = new TransformReportBuilder(propertyRules)

    private var errorFlag = false

    override def foreach[U](f: (Entity) => U): Unit = {
      // For each schema path, collect all rules that map to it
      val rulesPerPath =
      for(path <- outputSchema.typedPaths.map(_.path)) yield {
        path.propertyUri match {
          case Some(property) =>
            propertyRules.filter(_.target.get.propertyUri == property)
          case None =>
            IndexedSeq.empty
        }
      }

      var count = 0
      for(entity <- entities) {
        errorFlag = false
        val uri = subjectRule.flatMap(_(entity).headOption).getOrElse(entity.uri)
        val values =
          for(rules <- rulesPerPath) yield {
            rules.flatMap(evaluateRule(entity))
          }

        f(new Entity(uri, values, outputSchema))

        if(errorFlag)
          report.incrementEntityErrorCounter()
        else
          report.incrementEntityCounter()

        count += 1
        if (count % 1000 == 0) {
          context.value.update(report.build())
          context.status.updateMessage(s"Executing ($count Entities)")
        }
      }

      context.value() = report.build()
    }

    private def evaluateRule(entity: Entity)(rule: TransformRule): Seq[String] = {
      try {
        rule(entity)
      } catch {
        case NonFatal(ex) =>
          // TODO decrease log level as the log is now in the report
          log.warning("Error during execution of transform rule " + rule.name.toString + " of transform task " + task.id.toString + ": " + ex.getMessage)
          report.addError(rule, entity, ex)
          errorFlag = true
          Seq.empty
      }
    }
  }

}


