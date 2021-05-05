package org.silkframework.dataset

import org.silkframework.config.TaskLink
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{AnyPlugin, PluginFactory}
import org.silkframework.runtime.resource.Resource

/**
 * A specific dataset.
 */
trait Dataset extends AnyPlugin with DatasetAccess {

  /** The resources that are referenced by this dataset. */
  def referencedResources: Seq[Resource] = Seq.empty

  /** A dataset that is based on a file repository based resource, e.g. CSV, XML files. */
  def isFileResourceBased: Boolean = false

  /** Related links for this dataset. */
  def datasetLinks: Seq[TaskLink] = Seq.empty

  /** Characteristics of the data source. */
  def characteristics: DataSourceCharacteristics

}

trait DatasetPluginAutoConfigurable[T <: Dataset] {
  /**
   * returns an auto-configured version of this plugin
   */
  def autoConfigured(implicit userContext: UserContext): T
}

/**
 * Creates new dataset plugin instances.
 */
object Dataset extends PluginFactory[Dataset]
