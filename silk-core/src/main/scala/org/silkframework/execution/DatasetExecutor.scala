package org.silkframework.execution

import org.silkframework.config.{Prefixes, Task}
import org.silkframework.dataset.{Dataset, DatasetAccess, DatasetSpec}
import org.silkframework.entity.EntitySchema
import org.silkframework.runtime.activity.{ActivityContext, UserContext}

/**
  * Writes and/or reads a data set.
  *
  * @tparam DatasetType   The supported data set type, e.g., CsvDataset
  * @tparam ExecType The execution type, e.g., SparkExecution
  */
trait DatasetExecutor[DatasetType <: Dataset, ExecType <: ExecutionType] extends Executor[DatasetSpec[DatasetType], ExecType] {

  /**
    * Fetch the execution specific access to a dataset.
    */
  def access(task: Task[DatasetSpec[DatasetType]], execution: ExecType): DatasetAccess = {
    // Because the Dataset still inherits the DatasetAccess trait, we can just return it.
    // In the future, each dataset executor should overwrite this method.
    task.data
  }

  protected def read(task: Task[DatasetSpec[DatasetType]], schema: EntitySchema, execution: ExecType)
                    (implicit userContext: UserContext, context: ActivityContext[ExecutionReport], prefixes: Prefixes): ExecType#DataType

  protected def write(data: ExecType#DataType, task: Task[DatasetSpec[DatasetType]], execution: ExecType)
                     (implicit userContext: UserContext, context: ActivityContext[ExecutionReport], prefixes: Prefixes): Unit

  /**
    * Writes all inputs into dataset first and then reads from it if an output schema is defined.
    *
    * @param task
    * @param inputs
    * @param execution
    * @return
    */
  final override def execute(
    task: Task[DatasetSpec[DatasetType]],
    inputs: Seq[ExecType#DataType],
    output: ExecutorOutput,
    execution: ExecType,
    context: ActivityContext[ExecutionReport]
  )(implicit userContext: UserContext, prefixes: Prefixes): Option[ExecType#DataType] = {
    implicit val c = context
    for (input <- inputs) {
      write(input, task, execution)
    }
    output.requestedSchema.map {
      read(task, _, execution)
    }
  }
}
