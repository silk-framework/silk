package org.silkframework.dataset

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{AnyPlugin, PluginFactory}
import org.silkframework.runtime.resource.Resource

import scala.reflect.ClassTag

/**
 * A specific dataset.
 */
trait Dataset extends AnyPlugin with DatasetAccess {

  /** The resources that are referenced by this dataset. */
  def referencedResources: Seq[Resource] = Seq.empty
}

trait DatasetPluginAutoConfigurable[T <: Dataset] {
  /**
   * returns an auto-configured version of this plugin
   */
  def autoConfigured(implicit userContext: UserContext): T
}

/**
 * Creates new dataset olugin instances.
 */
object Dataset extends PluginFactory[Dataset]