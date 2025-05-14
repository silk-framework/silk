package org.silkframework.plugins.dataset

import org.silkframework.dataset._
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.runtime.resource.WritableResource

@Plugin(
  id = "binaryFile",
  label = "Binary File",
  description= "Reads and writes binary files.")
case class BinaryFileDataset(
    @Param("The plain text file. May also be a zip archive containing multiple text files.")
    file: WritableResource,
    @Param(value = "A type name that represents this file.", advanced = true)
    typeName: String = "document") extends Dataset with ResourceBasedDataset {

  override def mimeType: Option[String] = Some("application/octet-stream")

  /**
   * Creates a new data source for reading entities from the data set.
   */
  override def source(implicit userContext: UserContext): DataSource = ???

  /**
   * Returns a link sink for writing entity links to the data set.
   */
  override def linkSink(implicit userContext: UserContext): LinkSink = ??? //new TextFileSink(this)

  /**
   * Returns a entity sink for writing entities to the data set.
   */
  override def entitySink(implicit userContext: UserContext): EntitySink = ??? //new TextFileSink(this)

  //TODO
  override def characteristics: DatasetCharacteristics = DatasetCharacteristics.attributesOnly(explicitSchema = true)
}
