package org.silkframework.rule.execution.local

import java.util.logging.Logger

import org.silkframework.config.{PlainTask, Task}
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.execution.local.{EntityTable, GenericEntityTable, LocalExecution}
import org.silkframework.execution.{ExecutionReport, Executor}
import org.silkframework.rule.execution._
import org.silkframework.rule.{TransformRule, TransformSpec}
import org.silkframework.runtime.activity.ActivityContext

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
    val transformedEntities = mapEntities(task, input.entities, schema)
    Some(GenericEntityTable(transformedEntities, schema, PlainTask(task.id, transformSpec)))
  }

  private def mapEntities(task: Task[TransformSpec], entities: Traversable[Entity], outputSchema: EntitySchema) = {
    val transform = task.data
    val subjectRule = transform.rules.find(_.target.isEmpty)
    val propertyRules = transform.rules.filter(_.target.nonEmpty).toIndexedSeq

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

    for(entity <- entities.view) yield {
      val uri = subjectRule.flatMap(_(entity).headOption).getOrElse(entity.uri)
      val values =
        for(rules <- rulesPerPath) yield {
          rules.flatMap(evaluateRule(task, entity))
        }

      new Entity(uri, values, outputSchema)
    }
  }

  private def evaluateRule(task: Task[TransformSpec], entity: Entity)(rule: TransformRule): Seq[String] = {
    try {
      rule(entity)
    } catch {
      case NonFatal(ex) =>
        // TODO forward error
        log.warning("Error during execution of transform rule " + rule.name.toString + " of transform task " + task.id.toString + ": " + ex.getMessage)
        Seq.empty
    }
  }
}


