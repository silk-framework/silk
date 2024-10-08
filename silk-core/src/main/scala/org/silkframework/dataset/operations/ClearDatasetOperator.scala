package org.silkframework.dataset.operations

import org.silkframework.config._
import org.silkframework.entity.EntitySchema
import org.silkframework.execution.EmptyEntityHolder
import org.silkframework.execution.local.LocalEntities
import org.silkframework.runtime.plugin.annotations.Plugin

@Plugin(
  id = "clearDataset",
  label = "Clear dataset",
  description =
    """Clears the dataset that is connected to the output of this operator."""
)
case class ClearDatasetOperator() extends CustomTask {

  /**
    * The input ports and their schemata.
    */
  override def inputPorts: InputPorts = InputPorts.NoInputPorts

  /**
    * The output port and it's schema.
    * None, if this operator does not generate any output.
    */
  override def outputPort: Option[Port] = Some(FixedSchemaPort(ClearDatasetOperator.clearDatasetSchema))
}

object ClearDatasetOperator {
  private val clearDatasetSchema = EntitySchema(SilkVocab.ClearDatasetType, IndexedSeq.empty)

  case class ClearDatasetTable(task: Task[TaskSpec]) extends LocalEntities with EmptyEntityHolder {
    override def entitySchema: EntitySchema = clearDatasetSchema
  }
}