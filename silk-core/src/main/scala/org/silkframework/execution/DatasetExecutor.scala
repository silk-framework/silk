package org.silkframework.execution

import org.silkframework.config.Task
import org.silkframework.dataset.Dataset
import org.silkframework.entity.EntitySchema
import org.silkframework.runtime.activity.ActivityContext

/**
  * Writes and/or reads a data set.
  *
  * @tparam DatasetType   The supported data set type, e.g., CsvDataset
  * @tparam ExecType The execution type, e.g., SparkExecution
  */
trait DatasetExecutor[DatasetType <: Dataset, ExecType <: ExecutionType] extends Executor[DatasetType, ExecType] {

  protected def read(dataset: Task[DatasetType], schema: EntitySchema, execution: ExecType): ExecType#DataType

  protected def write(data: ExecType#DataType, dataset: Task[DatasetType], execution: ExecType): Unit

  /**
    * Writes all inputs into dataset first and then reads from it if an output schema is defined.
    *
    * @param task
    * @param inputs
    * @param outputSchema
    * @param execution
    * @return
    */
  final override def execute(
    task: Task[DatasetType],
    inputs: Seq[ExecType#DataType],
    outputSchema: Option[EntitySchema],
    execution: ExecType,
    context: ActivityContext[ExecutionReport]
  ): Option[ExecType#DataType] = {

    for (input <- inputs) {
      write(input, task, execution)
    }
    outputSchema map {
      read(task, _, execution)
    }
  }
}
