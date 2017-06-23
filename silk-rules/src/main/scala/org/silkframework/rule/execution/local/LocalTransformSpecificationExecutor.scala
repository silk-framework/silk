package org.silkframework.rule.execution.local

import org.silkframework.config.{PlainTask, Task}
import org.silkframework.entity.{EntitySchema, Path, TypedPath}
import org.silkframework.execution.local.{EntityTable, GenericEntityTable, LocalExecution, MultiEntityTable}
import org.silkframework.execution.{ExecutionReport, Executor}
import org.silkframework.rule._
import org.silkframework.runtime.activity.ActivityContext

import scala.collection.mutable

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
    val transformSpec = task.data.copy(selection = task.data.selection.copy(inputId = input.task.id))
    val schema = outputSchema.orElse(transformSpec.outputSchemaOpt).get

    input match {
      case mt: MultiEntityTable =>
        val output = mutable.Buffer[EntityTable]()
        val transformer = new EntityTransformer(task, (mt.asInstanceOf[EntityTable] +: mt.subTables).to[mutable.Buffer], output)
        transformer.transformEntities(task.rules, task.outputSchema, context)
        Some(MultiEntityTable(output.head.entities, output.head.entitySchema, task, output.tail))
      case _ =>
        val output = mutable.Buffer[EntityTable]()
        val transformer = new EntityTransformer(task, mutable.Buffer(input), output)
        transformer.transformEntities(task.rules, task.outputSchema, context)
        Some(MultiEntityTable(output.head.entities, output.head.entitySchema, task, output.tail))
    }
  }


  private class EntityTransformer(task: Task[TransformSpec], inputTables: mutable.Buffer[EntityTable], outputTables: mutable.Buffer[EntityTable]) {

    def transformEntities(rules: Seq[TransformRule], outputSchema: EntitySchema,
                                  context: ActivityContext[ExecutionReport]): Unit = {

      val entities = inputTables.remove(0).entities

      val transformedEntities = new TransformedEntities(entities, rules, outputSchema, context)
      outputTables.append(GenericEntityTable(transformedEntities, outputSchema, task))

      for(objectMapping @ ObjectMapping(_, relativePath, _, childRules, _) <- rules) {
        val childOutputSchema =
          EntitySchema(
            typeUri = childRules.collect { case tm: TypeMapping => tm.typeUri }.headOption.getOrElse(""),
            typedPaths = childRules.flatMap(_.target).map(mt => TypedPath(mt.asPath(), mt.valueType)).toIndexedSeq
          )

        val updatedChildRules = childRules.copy(uriRule = childRules.uriRule.orElse(objectMapping.uriRule))

        transformEntities(updatedChildRules, childOutputSchema, context)
      }
    }

  }

}
