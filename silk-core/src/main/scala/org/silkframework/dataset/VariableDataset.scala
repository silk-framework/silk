package org.silkframework.dataset

import org.silkframework.runtime.plugin.Plugin

/**
  * A [[DatasetPlugin]] that has no own implementation, but is replaced by another implementation at request time.
  * This is used for example in Silk Workflows, where the input (or even input format) is not fixed and comes in with
  * the request. A workflow cannot be run in a normal way if it includes a dataset of this type!
  */
@Plugin(
  id = "variableDataset",
  label = "Variable Dataset",
  description = "Dataset that acts as a placeholder in Silk workflows and is replaced at request time.")
final class VariableDataset extends DatasetPlugin {
  /**
    * Returns a data source for reading entities from the data set.
    */
  override def source: DataSource = error()

  /**
    * Makes sure that the next write will start from an empty dataset.
    */
  override def clear(): Unit = error()

  /**
    * Returns a entity sink for writing entities to the data set.
    */
  override def entitySink: EntitySink = error()

  /**
    * Returns a link sink for writing entity links to the data set.
    */
  override def linkSink: LinkSink = error()

  private def error() = throw new RuntimeException("A Variable Dataset cannot be accessed! Only use it in workflows that replace all variable datasets before execution.")
}
