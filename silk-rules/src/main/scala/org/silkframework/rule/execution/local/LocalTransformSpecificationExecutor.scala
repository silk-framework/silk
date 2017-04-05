package org.silkframework.rule.execution.local

import org.silkframework.config.{PlainTask, Task}
import org.silkframework.entity.EntitySchema
import org.silkframework.execution.local.{EntityTable, GenericEntityTable, LocalExecution}
import org.silkframework.execution.{ExecutionReport, Executor}
import org.silkframework.rule.TransformSpec
import org.silkframework.runtime.activity.ActivityContext

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
    val transformedEntities = new TransformedEntities(input.entities, task, schema, context)
    Some(GenericEntityTable(transformedEntities, schema, PlainTask(task.id, transformSpec)))
  }

}
