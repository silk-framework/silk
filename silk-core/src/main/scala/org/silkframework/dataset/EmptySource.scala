package org.silkframework.dataset
import org.silkframework.config.{PlainTask, Task}
import org.silkframework.entity.{Entity, EntitySchema, Path}
import org.silkframework.util.Uri

/**
  * An empty data source.
  */
object EmptySource extends DataSource {

  override def retrieveTypes(limit: Option[Int] = None): Traversable[(String, Double)] = {
    Traversable.empty
  }

  override def retrievePaths(typeUri: Uri, depth: Int = 1, limit: Option[Int] = None): IndexedSeq[Path] = {
    IndexedSeq.empty
  }

  override def retrieve(entitySchema: EntitySchema, limit: Option[Int]): Traversable[Entity] = {
    Traversable.empty
  }

  override def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri]): Seq[Entity] = {
    Seq.empty
  }

  override def underlyingTask: Task[DatasetSpec[Dataset]] = PlainTask("empty_dataset", DatasetSpec(EmptyDataset))
}
