package org.silkframework.dataset.rdf

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.execution.local.LocalEntities
import org.silkframework.runtime.resource.FileResource

/** A number of files that should be uploaded via the GraphStore protocol. */
trait GraphStoreFileUploadTable {
  def files: Traversable[FileResource]
}

case class LocalGraphStoreFileUploadTable(files: Traversable[FileResource], task: Task[TaskSpec]) extends LocalEntities with GraphStoreFileUploadTable {
  override def entitySchema: EntitySchema = EntitySchema.empty

  override def entities: Traversable[Entity] = Traversable.empty
}