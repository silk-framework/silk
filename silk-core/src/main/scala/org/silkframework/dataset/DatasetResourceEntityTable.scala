package org.silkframework.dataset

import org.silkframework.config.{SilkVocab, Task, TaskSpec}
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.execution.local.LocalEntities
import org.silkframework.runtime.resource.{ReadOnlyResource, Resource}
import org.silkframework.util.Uri

/**
  * An entity table that holds the input resource of a dataset and can be requested with the DatasetResourceEntitySchema schema.
  */
class DatasetResourceEntityTable(resource: Resource, val taskOption: Option[Task[TaskSpec]]) extends LocalEntities {
  override def entitySchema: EntitySchema = EntitySchema.empty

  override def entities: Traversable[Entity] = Traversable.empty

  /** The resource of the dataset this was requested from, as read-only resource. */
  def datasetResource: ReadOnlyResource = ReadOnlyResource(resource)
}

object DatasetResourceEntitySchema {
  final val schema = EntitySchema(
    typeUri = Uri(SilkVocab.DatasetResourceSchemaType),
    typedPaths = IndexedSeq.empty
  )
}