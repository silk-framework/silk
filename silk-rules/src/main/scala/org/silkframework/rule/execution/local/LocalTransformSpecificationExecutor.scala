package org.silkframework.rule.execution.local

import org.silkframework.config.{PlainTask, Task}
import org.silkframework.entity.{EntityTrait, SchemaTrait}
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
    input match {
      case flatTable: FlatEntityTable =>
        val transformedEntities = new TransformedEntities(flatTable.entities, task, schema, context)
        Some(GenericEntityTable(transformedEntities, schema, PlainTask(task.id, transformSpec)))
      case nestedTable: NestedEntityTableTrait =>
        // TODO: Support transforming
        ???
    }
  }

}
