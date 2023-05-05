package org.silkframework.dataset.rdf

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.execution.local.LocalEntities
import org.silkframework.runtime.resource.FileResource
import org.silkframework.util.CloseableIterator

/** A number of files that should be uploaded via the GraphStore protocol. */
trait GraphStoreFileUploadTable {
  def files: CloseableIterator[FileResource]
}

case class LocalGraphStoreFileUploadTable(files: CloseableIterator[FileResource], task: Task[TaskSpec]) extends LocalEntities with GraphStoreFileUploadTable {
  override def entitySchema: EntitySchema = EntitySchema.empty

  override def entities: CloseableIterator[Entity] = CloseableIterator.empty

  override def updateEntities(newEntities: CloseableIterator[Entity], newSchema: EntitySchema): LocalEntities = this // Changing entities has no effect
}