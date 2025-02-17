package org.silkframework.execution

import org.silkframework.config.{FixedSchemaPort, FlexibleSchemaPort, Prefixes, Task}
import org.silkframework.dataset.{Dataset, DatasetAccess, DatasetSpec}
import org.silkframework.entity.EntitySchema
import org.silkframework.runtime.activity.{ActivityContext, UserContext}
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.validation.ValidationException

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
                    (implicit pluginContext: PluginContext, context: ActivityContext[ExecutionReport]): ExecType#DataType

  protected def write(data: ExecType#DataType, task: Task[DatasetSpec[DatasetType]], execution: ExecType)
                     (implicit pluginContext: PluginContext, context: ActivityContext[ExecutionReport]): Unit

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
  )(implicit pluginContext: PluginContext): Option[ExecType#DataType] = {
    implicit val c: ActivityContext[ExecutionReport] = context
    // Write all inputs into the dataset
    for (input <- inputs) {
      write(input, task, execution)
    }
    // Determine output schema
    val outputSchema = {
      output.connectedPort match {
        case Some(FixedSchemaPort(schema)) =>
          Some(schema)
        case Some(FlexibleSchemaPort(_)) if task.data.characteristics.explicitSchema =>
          Some(retrieveSchema(task, execution))
        case _ =>
          None
      }
    }
    // Read from dataset
    for(schema <- outputSchema) yield {
      read(task, schema, execution)
    }
  }

  /**
    * Retrieves the schema of the dataset if no output schema has been provided.
    */
  protected def retrieveSchema(dataset: Task[DatasetSpec[DatasetType]], execution: ExecType)(implicit pluginContext: PluginContext): EntitySchema = {
    implicit val prefixes: Prefixes = pluginContext.prefixes
    implicit val user: UserContext = pluginContext.user

    val source = access(dataset, execution).source(pluginContext.user)
    val types = source.retrieveTypes()

    if(types.isEmpty) {
      throw new ValidationException(s"No types found in dataset ${dataset.labelAndId()}")
    } else {
      val typeUri = types.head._1
      EntitySchema(typeUri, source.retrievePaths(typeUri))
    }
  }
}

object DatasetExecutor {

  /**
   * Checks if a dataset can read data for a given output port.
   */
  def canRead(dataset: Dataset, output: ExecutorOutput): Boolean = {
    output.connectedPort match {
      case Some(FixedSchemaPort(_)) =>
        true
      case Some(FlexibleSchemaPort(_)) if dataset.characteristics.explicitSchema =>
        true
      case _ =>
        false
    }
  }

}
