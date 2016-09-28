package org.silkframework.execution.local

import org.silkframework.dataset.Dataset
import org.silkframework.plugins.dataset.{InternalDataset, InternalDatasetTrait}

case class LocalInternalDataset() extends InternalDatasetTrait {
  override protected def internalDatasetPluginImpl: Dataset = InternalDataset.createInternalDataset()
}
