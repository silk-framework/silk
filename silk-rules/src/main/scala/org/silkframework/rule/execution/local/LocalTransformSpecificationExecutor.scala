package org.silkframework.rule.execution.local

import org.silkframework.config.{PlainTask, Task}
import org.silkframework.entity._
import org.silkframework.execution.local._
import org.silkframework.execution.{ExecutionReport, Executor}
import org.silkframework.rule.TransformSpec
import org.silkframework.runtime.activity.ActivityContext

/**
  * Created on 7/20/16.
  */
class LocalTransformSpecificationExecutor extends Executor[TransformSpec, LocalExecution] {

  override def execute(task: Task[TransformSpec],
                       inputs: Seq[EntityTable[EntityTrait, SchemaTrait]],
                       outputSchema: Option[SchemaTrait],
                       execution: LocalExecution,
                       context: ActivityContext[ExecutionReport]): Option[EntityTable[EntityTrait, SchemaTrait]] = {
    val input = inputs.head
    val transformSpec = task.data.copy(selection = task.data.selection.copy(inputId = input.task.id))
    val schema = outputSchema.orElse(transformSpec.outputSchemaOpt).get
    (input, schema) match {
      case (flatTable: FlatEntityTable, flatSchema: EntitySchema) =>
        val transformedEntities = new TransformedEntities(flatTable.entities, task, flatSchema, context)
        Some(GenericEntityTable(transformedEntities, flatSchema, PlainTask(task.id, transformSpec)))
      case (flatTable: FlatEntityTable, nestedSchema: NestedEntitySchema) =>
        val nestedEntityWrapper = flatTable.entities map (e => NestedEntity(e.uri, e.values, IndexedSeq.empty))
        val transformedNestedEntities = new TransformedNestedEntities(nestedEntityWrapper, task, context)
        Some(NestedEntityTable(transformedNestedEntities, SchemaTrait.toNestedSchema(transformSpec.outputSchema), task))
      case (nestedTable: NestedEntityTableTrait, _) =>
        /* TODO: At the moment this always returns a NestedEntityTable. In theory this could return a flat entity table.
                 This could be changed with small effort.
         */
        val transformedNestedEntities = new TransformedNestedEntities(nestedTable.entities, task, context)
        Some(NestedEntityTable(transformedNestedEntities, SchemaTrait.toNestedSchema(transformSpec.outputSchema), task))
    }
  }
}
