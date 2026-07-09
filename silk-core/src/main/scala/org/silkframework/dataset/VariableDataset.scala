package org.silkframework.dataset

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.annotations.Plugin

/**
  * A [[Dataset]] that has no own implementation, but is replaced by another implementation at request time.
  * This is used for example in Silk Workflows, where the input (or even input format) is not fixed and comes in with
  * the request. A workflow cannot be run in a normal way if it includes a dataset of this type!
  */
@Plugin(
  id = "variableDataset",
  label = "Variable dataset (deprecated)",
  categories = Array(DatasetCategories.embedded),
  description = "Dataset that acts as a placeholder in workflows and is replaced at request time. This is deprecated, please use the 'replaceable input/output dataset config' in the node menu of the workflow editor instead.")
final class VariableDataset extends Dataset {
  override def characteristics: DatasetCharacteristics = DatasetCharacteristics.attributesOnly()
}
