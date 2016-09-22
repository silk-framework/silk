package org.silkframework.execution.local

import org.silkframework.dataset.Dataset
import org.silkframework.execution.ExecutionType
import org.silkframework.plugins.dataset.{InternalDataset, InternalDatasetTrait}

import scala.collection.mutable

/**
  * Tasks are executed on a local machine.
  *
  * @param useLocalInternalDatasets If set to true the LocalExecution object will maintain an own map of internal
  *                                 datasets, instead of using the global ones.
  */
case class LocalExecution(useLocalInternalDatasets: Boolean) extends ExecutionType {

  type DataType = EntityTable

  private val internalDatasets: mutable.Map[Option[String], InternalDatasetTrait] = mutable.Map.empty

  def createInternalDataset(internalDatasetId: Option[String]): Dataset = {
    if (useLocalInternalDatasets) {
      internalDatasets.synchronized {
        internalDatasets.getOrElseUpdate(
          internalDatasetId,
          LocalInternalDataset()
        )
      }
    } else {
      InternalDataset.byGraph(Option(generateGraphUri(internalDatasetId)))
    }
  }

  private def generateGraphUri(internalDatasetId: Option[String]): String = {
    val uriOpt = internalDatasetId map { id =>
      InternalDataset.internalDatasetGraphPrefix + id
    }
    uriOpt.orNull // graphUri must be set to null for the default
  }
}
