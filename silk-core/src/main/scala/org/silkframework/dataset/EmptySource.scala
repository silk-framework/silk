package org.silkframework.dataset
import org.silkframework.config.{PlainTask, Prefixes, Task}
import org.silkframework.entity.EntitySchema
import org.silkframework.entity.paths.TypedPath
import org.silkframework.execution.EntityHolder
import org.silkframework.execution.local.GenericEntityTable
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Uri

/**
  * An empty data source.
  */
object EmptySource extends DataSource {

  override def retrieveTypes(limit: Option[Int] = None)
                            (implicit userContext: UserContext, prefixes: Prefixes): Traversable[(String, Double)] = {
    Traversable.empty
  }

  override def retrievePaths(typeUri: Uri, depth: Int = 1, limit: Option[Int] = None)
                            (implicit userContext: UserContext, prefixes: Prefixes): IndexedSeq[TypedPath] = {
    IndexedSeq.empty
  }

  override def retrieve(entitySchema: EntitySchema, limit: Option[Int])
                       (implicit userContext: UserContext, prefixes: Prefixes): EntityHolder = {
    GenericEntityTable(Traversable.empty, entitySchema, underlyingTask)
  }

  override def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri])
                            (implicit userContext: UserContext, prefixes: Prefixes): EntityHolder= {
    GenericEntityTable(Seq.empty, entitySchema, underlyingTask)
  }

  override def underlyingTask: Task[DatasetSpec[Dataset]] = PlainTask("empty_dataset", DatasetSpec(EmptyDataset))
}
