package org.silkframework.rule.execution.local

import org.silkframework.config.Task
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.execution.local.{EntityTable, GenericEntityTable, LocalExecution}
import org.silkframework.execution.{ExecutionReport, Executor}
import org.silkframework.rule.execution._
import org.silkframework.rule.TransformSpec
import org.silkframework.runtime.activity.ActivityContext

import scala.util.control.NonFatal

/**
  * Created on 7/20/16.
  */
class LocalTransformSpecificationExecutor extends Executor[TransformSpec, LocalExecution] {

  override def execute(task: Task[TransformSpec],
                       inputs: Seq[EntityTable],
                       outputSchema: Option[EntitySchema],
                       execution: LocalExecution,
                       context: ActivityContext[ExecutionReport]): Option[EntityTable] = {
    val input = inputs.head
    val transformSpec = task.data
    val transformedEntities = mapEntities(transformSpec, input.entities)
    assert(transformSpec.outputSchemaOpt.isDefined)
    Some(GenericEntityTable(transformedEntities, transformSpec.outputSchemaOpt.get))
  }

  private def mapEntities(transform: TransformSpec, entities: Traversable[Entity]) = {
    val subjectRule = transform.rules.find(_.target.isEmpty)
    val propertyRules = transform.rules.filter(_.target.nonEmpty).toIndexedSeq

    for(entity <- entities.view) yield {
      val uri = subjectRule.flatMap(_(entity).headOption).getOrElse(entity.uri)
      val values =
        for(rules <- propertyRules) yield {
          try {
            rules(entity)
          } catch {
            case NonFatal(ex) =>
              // TODO forward error
              Seq.empty
          }
        }

      new Entity(uri, values, transform.outputSchemaOpt.get)
    }
  }
}


