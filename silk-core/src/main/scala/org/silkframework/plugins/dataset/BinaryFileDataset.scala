package org.silkframework.plugins.dataset

import org.silkframework.config.{PlainTask, Prefixes, Task}
import org.silkframework.dataset._
import org.silkframework.entity.EntitySchema
import org.silkframework.entity.paths.TypedPath
import org.silkframework.execution.EntityHolder
import org.silkframework.execution.local.EmptyEntityTable
import org.silkframework.execution.typed.{FileEntity, FileEntitySchema, FileType}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.runtime.resource.WritableResource
import org.silkframework.util.{Identifier, Uri}

@Plugin(
  id = BinaryFileDataset.id,
  label = "Binary file",
  description= "Reads and writes binary files.",
  documentationFile = "BinaryFileDataset.md"
)
case class BinaryFileDataset(
    @Param("The file to read or write.")
    file: WritableResource) extends Dataset with ResourceBasedDataset {

  override def mimeType: Option[String] = Some(BinaryFileDataset.mimeType)

  /**
   * Creates a new data source for reading entities from the data set.
   */
  override def source(implicit userContext: UserContext): DataSource = new FileSource(file)

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

  override def characteristics: DatasetCharacteristics = DatasetCharacteristics.attributesOnly(explicitSchema = true)
}

object BinaryFileDataset {

  final val id = "binaryFile"
  final val mimeType = "application/octet-stream"
}

/**
 * A data source for reading files.
 */
class FileSource(file: WritableResource) extends DataSource with PeakDataSource {

  override def retrieveTypes(limit: Option[Int])(implicit userContext: UserContext, prefixes: Prefixes): Iterable[(String, Double)] = {
    Seq((FileEntitySchema.schema.typeUri, 1.0))
  }

  override def retrievePaths(typeUri: Uri, depth: Int, limit: Option[Int])(implicit userContext: UserContext, prefixes: Prefixes): IndexedSeq[TypedPath] = {
    FileEntitySchema.schema.typedPaths
  }

  override def retrieve(entitySchema: EntitySchema, limit: Option[Int])(implicit pluginContext: PluginContext): EntityHolder = {
    val fileEntity = FileEntity(file, FileType.Project, Some(BinaryFileDataset.mimeType))
    FileEntitySchema.create(Iterable(fileEntity), underlyingTask)
  }

  override def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri])(implicit pluginContext: PluginContext): EntityHolder = {
    if(entities.isEmpty) {
      EmptyEntityTable(underlyingTask)
    } else {
      val uriSet = entities.map(_.uri).toSet
      retrieve(entitySchema).filter(entity => uriSet.contains(entity.uri.toString))
    }
  }

  override def underlyingTask: Task[DatasetSpec[Dataset]] = PlainTask(Identifier.fromAllowed(file.name), DatasetSpec(EmptyDataset))
}

/**
 * The [[BinaryFileDataset]] does not support writing files using the generic sink interface.
 * Files can only be written using the [[org.silkframework.execution.local.LocalDatasetExecutor]].
 * This sink will throw an exception if the user tries to write to it.
 * It does support clearing the file, though.
 *
 */
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