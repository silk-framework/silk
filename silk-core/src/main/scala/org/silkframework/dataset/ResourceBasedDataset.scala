package org.silkframework.dataset

import org.silkframework.config.Prefixes
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.{Resource, WritableResource}

/**
  * A dataset that uses resources, e.g. a file from the file repository, to read from.
  */
trait ResourceBasedDataset { this: Dataset =>
  /** The resource the dataset is reading from */
  def file: Resource

  def writableResource: Option[WritableResource] = file match {
    case wr: WritableResource => Some(wr)
    case _ => None
  }

  /**
   * The MIME type of the configured resource, if any.
   * If this is a bulk (i.e., zipped) resource, this should be the MIME type of the zip content.
   */
  def mimeType: Option[String]

  override def referencedResources: Seq[Resource] = Seq(file)

  override def isFileResourceBased: Boolean = true

  override def searchTags(pluginContext: PluginContext): Seq[String] = {
    Seq(file.relativePath(pluginContext.resources))
  }
}
