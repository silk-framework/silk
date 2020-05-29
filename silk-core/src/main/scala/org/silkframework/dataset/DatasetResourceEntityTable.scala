package org.silkframework.dataset

import org.silkframework.config.{SilkVocab, Task, TaskSpec}
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.execution.EmptyEntityHolder
import org.silkframework.execution.local.{LocalOnlyEntities, LocalEntities}
import org.silkframework.runtime.resource.{ReadOnlyResource, Resource}
import org.silkframework.util.Uri

/**
  * An entity table that holds the input resource of a dataset and can be requested with the DatasetResourceEntitySchema schema.
  */
trait DatasetResourceEntityTable {

  /** The resource of the dataset this was requested from, as read-only resource. */
  def datasetResource: ReadOnlyResource
}

object DatasetResourceEntitySchema {
  final val schema = EntitySchema(
    typeUri = Uri(SilkVocab.DatasetResourceSchemaType),
    typedPaths = IndexedSeq.empty
  )
}

/**
  * Local implementation of the DatasetResourceEntityTable.
  */
class LocalOnlyDatasetResourceEntityTable(resource: Resource, val task: Task[TaskSpec]) extends LocalOnlyEntities with DatasetResourceEntityTable with EmptyEntityHolder {

  override def entitySchema: EntitySchema = EntitySchema.empty

  /** The resource of the dataset this was requested from, as read-only resource. */
  def datasetResource: ReadOnlyResource = ReadOnlyResource(resource)
}
