package org.silkframework.dataset

import org.silkframework.config.Prefixes
import org.silkframework.runtime.resource.WritableResource

/**
  * A dataset that has a writable resource.
  */
trait WritableResourceDataset extends Dataset {
  def file: WritableResource

  def replaceWritableResource(writableResource: WritableResource): WritableResourceDataset

  override def searchTags(prefixes: Prefixes): Seq[String] = {
    Seq(file.name)
  }
}
