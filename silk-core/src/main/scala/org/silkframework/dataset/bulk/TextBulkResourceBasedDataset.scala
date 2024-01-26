package org.silkframework.dataset.bulk

import org.silkframework.dataset.Dataset

import java.io.{InputStreamReader, Reader}
import scala.io.Codec

/**
  * A text file based data source for which the resource could also be a zip archive.
  * If the resource is a zip archive, all files in the zip archive are read.
  * If the resource is not a zip archive, the resource is read directly.
  *
  * @see BulkResourceBasedDataset
  */
trait TextBulkResourceBasedDataset extends BulkResourceBasedDataset { this: Dataset =>

  /**
   * The configured charset encoding.
   */
  def codec: Codec

  /**
   * Reads the resource as a character string. Will close the reader automatically.
   */
  def read[T](readFunc: Reader => T): T = {
    useFirstResource { resource =>
      resource.read { inputStream =>
        val reader = new InputStreamReader(inputStream, codec.charSet)
        try {
          readFunc(reader)
        } finally {
          reader.close()
        }
      }
    }
  }

}
