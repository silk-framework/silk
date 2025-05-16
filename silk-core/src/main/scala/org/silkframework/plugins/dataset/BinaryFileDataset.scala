package org.silkframework.plugins.dataset

import org.silkframework.config.{Prefixes, Task}
import org.silkframework.dataset._
import org.silkframework.execution.local.{LocalDatasetExecutor, LocalExecution}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.runtime.resource.WritableResource
import org.silkframework.util.Uri

@Plugin(
  id = BinaryFileDataset.id,
  label = "Binary File",
  description= "Reads and writes binary files.")
case class BinaryFileDataset(
    @Param("The plain text file. May also be a zip archive containing multiple text files.")
    file: WritableResource) extends Dataset with ResourceBasedDataset {

  override def mimeType: Option[String] = Some(BinaryFileDataset.mimeType)

  /**
   * Creates a new data source for reading entities from the data set.
   */
  // TODO
  override def source(implicit userContext: UserContext): DataSource = ???

  /**
   * Returns a link sink for writing entity links to the data set.
   */
  override def linkSink(implicit userContext: UserContext): LinkSink = {
    throw new RuntimeException("Only file entities can be written to this dataset. Links are not supported")
  }

  /**
   * Returns an entity sink for writing entities to the data set.
   */
  override def entitySink(implicit userContext: UserContext): EntitySink = new FileSink(file)

  //TODO
  override def characteristics: DatasetCharacteristics = DatasetCharacteristics.attributesOnly(explicitSchema = true)
}

object BinaryFileDataset {

  final val id = "binaryFile"
  final val mimeType = "application/octet-stream"
}

class LocalBinaryDatasetExecutor extends LocalDatasetExecutor[BinaryFileDataset] {

  override def access(task: Task[DatasetSpec[BinaryFileDataset]], execution: LocalExecution): DatasetAccess = {
    new BinaryFileDatasetAccess(task.data.plugin.file)
  }
}


class BinaryFileDatasetAccess(file: WritableResource) extends DatasetAccess {

  /**
   * Creates a new data source for reading entities from the data set.
   */
  // TODO
  override def source(implicit userContext: UserContext): DataSource = ???

  /**
   * Returns a link sink for writing entity links to the data set.
   */
  override def linkSink(implicit userContext: UserContext): LinkSink = {
    throw new RuntimeException("Only file entities can be written to this dataset. Links are not supported")
  }

  /**
   * Returns an entity sink for writing entities to the data set.
   */
  override def entitySink(implicit userContext: UserContext): EntitySink = new FileSink(file)
}

class FileSink(file: WritableResource) extends EntitySink {

  override def openTable(typeUri: Uri, properties: Seq[TypedProperty], singleEntity: Boolean)
                        (implicit userContext: UserContext, prefixes: Prefixes): Unit = {
    throwException
  }

  override def closeTable()(implicit userContext: UserContext): Unit = {
    throwException
  }

  override def writeEntity(subject: String, values: IndexedSeq[Seq[String]])(implicit userContext: UserContext): Unit = {
    throwException
  }

  override def clear()(implicit userContext: UserContext): Unit = {
    file.delete()
  }

  private def throwException: Nothing = {
    throw new RuntimeException("Only file entities can be written to this dataset.")
  }
}