package org.silkframework.dataset.sql

import org.silkframework.config.Task
import org.silkframework.dataset.{Dataset, DatasetAccess, DatasetSpec}
import org.silkframework.execution.{ExecutionType, ExecutorRegistry}
import org.silkframework.runtime.validation.ValidationException

/**
  * A [[DatasetAccess]] for a SQL dataset that additionally exposes a SQL endpoint.
  *
  * The SQL counterpart of [[org.silkframework.dataset.rdf.RdfDatasetAccess]]: the endpoint is part of
  * the execution-scoped access built by the dataset's executor, not of the shared dataset plugin.
  */
trait SqlDatasetAccess extends DatasetAccess {

  def sqlEndpoint: SqlEndpoint

}

object SqlDatasetAccess {

  /** Resolves the SQL access of a dataset task for a specific execution. */
  def forExecution[DatasetType <: Dataset](task: Task[DatasetSpec[DatasetType]], execution: ExecutionType): SqlDatasetAccess = {
    forExecutionOption(task, execution).getOrElse(noSqlAccess(task))
  }

  /** Resolves the SQL access of a dataset task for the configured (default) execution. */
  def forExecution[DatasetType <: Dataset](task: Task[DatasetSpec[DatasetType]]): SqlDatasetAccess = {
    forExecutionOption(task).getOrElse(noSqlAccess(task))
  }

  /** Resolves the SQL access of a transient (task-less) dataset. Prefer the task-based overloads when a task is available. */
  def forExecution(dataset: Dataset): SqlDatasetAccess = {
    sqlAccessOf(ExecutorRegistry.access(dataset)).getOrElse(noSqlAccess(dataset))
  }

  /** Resolves the SQL access of a dataset task for a specific execution, or `None` if its executor provides no SQL access. */
  def forExecutionOption[DatasetType <: Dataset](task: Task[DatasetSpec[DatasetType]],
                                                 execution: ExecutionType): Option[SqlDatasetAccess] = {
    sqlAccessOf(ExecutorRegistry.access(task, execution))
  }

  /** Resolves the SQL access of a dataset task for the configured (default) execution, or `None` if its executor provides no SQL access. */
  def forExecutionOption[DatasetType <: Dataset](task: Task[DatasetSpec[DatasetType]]): Option[SqlDatasetAccess] = {
    sqlAccessOf(ExecutorRegistry.access(task))
  }

  /** Returns the access as an [[SqlDatasetAccess]] if the executor produced SQL access, else `None`. */
  private def sqlAccessOf(access: DatasetAccess): Option[SqlDatasetAccess] = {
    access match {
      case sql: SqlDatasetAccess => Some(sql)
      case _ => None
    }
  }

  private def noSqlAccess[DatasetType <: Dataset](task: Task[DatasetSpec[DatasetType]]): Nothing = {
    throw new ValidationException(s"Dataset task '${task.id}' of type ${task.data.plugin.pluginSpec.label} " +
      s"does not provide SQL data access.")
  }

  private def noSqlAccess(dataset: Dataset): Nothing = {
    throw new ValidationException(s"Dataset of type ${dataset.pluginSpec.label} does not provide SQL data access.")
  }
}
