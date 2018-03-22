package org.silkframework.dataset

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

  override def referencedResources: Seq[Resource] = Seq(file)
}
