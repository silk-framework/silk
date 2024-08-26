package org.silkframework.workspace.activity.dataset

import org.silkframework.dataset.{Dataset, DatasetCharacteristics, ResourceBasedDataset}
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset.rdf.RdfDataset
import org.silkframework.rule.DatasetSelection
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.workspace.Project

object DatasetUtils {
  /** Get the plugin IDs of all resource based datasets. */
  def resourceBasedDatasetPluginIds: Seq[String] = {
    PluginRegistry.availablePlugins[Dataset]
      .filter(plugin => classOf[ResourceBasedDataset].isAssignableFrom(plugin.pluginClass))
      .map(_.id.toString)
  }

  /** Returns the dataset characteristics of the dataset selection. */
  def datasetCharacteristics(project: Project,
                             datasetSelection: DatasetSelection)
                            (implicit userContext: UserContext): Option[DatasetCharacteristics] = {
    project.taskOption[GenericDatasetSpec](datasetSelection.inputId)
      .map(_.data.characteristics)
  }

  def supportsAsteriskOperator(project: Project,
                               datasetSelection: DatasetSelection)
                              (implicit userContext: UserContext): Boolean = {
    datasetCharacteristics(project, datasetSelection).exists(_.supportsAsteriskPathOperator)
  }

  def isRdfInput(project: Project,
                 datasetSelection: DatasetSelection)
                (implicit userContext: UserContext): Boolean = {
    project.taskOption[GenericDatasetSpec](datasetSelection.inputId).exists(_.data.plugin.isInstanceOf[RdfDataset])
  }
}
