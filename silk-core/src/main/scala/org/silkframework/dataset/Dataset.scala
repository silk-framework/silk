package org.silkframework.dataset

import org.silkframework.config.{Prefixes, TaskLink}
import org.silkframework.runtime.plugin.annotations.PluginType
import org.silkframework.runtime.plugin.{AnyPlugin, PluginContext, PluginFactory}
import org.silkframework.runtime.resource.Resource

/**
 * A specific dataset.
 */
@PluginType()
trait Dataset extends AnyPlugin with DatasetAccess {

  /** The resources that are referenced by this dataset. */
  def referencedResources: Seq[Resource] = Seq.empty

  /** A dataset that is based on a file repository based resource, e.g. CSV, XML files. */
  def isFileResourceBased: Boolean = false

  /** Generates a browser URl for a given resource URI. */
  def entityLink(entityUri: String): Option[String] = None

  /** Related links for this dataset. */
  def datasetLinks: Seq[TaskLink] = Seq.empty

  /** Characteristics of the data source. */
  def characteristics: DatasetCharacteristics

  /** Additional tags that will be displayed in the UI for this task. These tags are covered by the workspace search. */
  def searchTags(prefixes: Prefixes): Seq[String] = Seq.empty
}

trait DatasetPluginAutoConfigurable[T <: Dataset] {
  /**
   * returns an auto-configured version of this plugin
   */
  def autoConfigured(implicit pluginContext: PluginContext): T
}

/**
 * Creates new dataset plugin instances.
 */
object Dataset extends PluginFactory[Dataset]
