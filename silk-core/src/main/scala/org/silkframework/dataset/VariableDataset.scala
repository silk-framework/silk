package org.silkframework.dataset

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.Plugin

/**
  * A [[Dataset]] that has no own implementation, but is replaced by another implementation at request time.
  * This is used for example in Silk Workflows, where the input (or even input format) is not fixed and comes in with
  * the request. A workflow cannot be run in a normal way if it includes a dataset of this type!
  */
@Plugin(
  id = "variableDataset",
  label = "Variable Dataset",
  description = "Dataset that acts as a placeholder in Silk workflows and is replaced at request time.")
final class VariableDataset extends Dataset {
  /**
    * Returns a data source for reading entities from the data set.
    */
  override def source(implicit userContext: UserContext): DataSource = EmptyDataset.source// TODO: error()

  /**
    * Returns a entity sink for writing entities to the data set.
    */
  override def entitySink(implicit userContext: UserContext): EntitySink = error()

  /**
    * Returns a link sink for writing entity links to the data set.
    */
  override def linkSink(implicit userContext: UserContext): LinkSink = error()

  private def error() = throw new RuntimeException("A Variable Dataset cannot be accessed! Only use it in workflows that replace all variable datasets before execution.")
}
