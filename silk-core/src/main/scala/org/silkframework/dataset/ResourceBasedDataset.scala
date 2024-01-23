package org.silkframework.dataset

import org.silkframework.runtime.resource.{Resource, WritableResource}

import java.io.{InputStreamReader, Reader}
import scala.io.Codec

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

  override def isFileResourceBased: Boolean = true
}

/**
 * A dataset that is based on a file resource that can be read as text.
 */
trait TextResourceBasedDataset extends ResourceBasedDataset {  this: Dataset =>

  /**
   * The configured charset encoding.
   */
  def codec: Codec

  /**
   * Reads the resource as a character string. Will close the reader automatically.
   */
  def read[T](readFunc: Reader => T): T = {
    file.read { inputStream =>
      val reader = new InputStreamReader(inputStream, codec.charSet)
      try {
        readFunc(reader)
      } finally {
        reader.close()
      }
    }
  }

}