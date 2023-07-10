package org.silkframework.dataset
import org.silkframework.config.{Prefixes, Task}
import org.silkframework.entity._
import org.silkframework.entity.paths.TypedPath
import org.silkframework.execution.EntityHolder
import org.silkframework.execution.local.GenericEntityTable
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.iterator.CloseableIterator
import org.silkframework.runtime.plugin.annotations.Param
import org.silkframework.util.Uri

/**
  * Mock dataset for tests. The main methods for retrieving and writing can be defined as parameter.
  */
case class MockDataset(@Param(label = "person name", value = "The full name of a person")
                       name: String = "dummy") extends Dataset {
  var retrieveFn: (EntitySchema, Option[Int]) => CloseableIterator[Entity] = (_, _) => CloseableIterator.empty
  var retrieveByUriFn: (EntitySchema, Seq[Uri]) => CloseableIterator[Entity] = (_, _) => CloseableIterator.empty
  var writeLinkFn: (Link, String) => Unit = (_, _) => {}
  var writeEntityFn: (String, Seq[Seq[String]]) => Unit = (_, _) => {}
  var clearFn: () => Unit = () => {}
  var retrievePathsFn: (Uri, Int, Option[Int]) => IndexedSeq[TypedPath] = (_, _, _) => { IndexedSeq.empty }

  override def source(implicit userContext: UserContext): DataSource = DummyDataSource(retrieveFn, retrieveByUriFn, retrievePathsFn)

  override def linkSink(implicit userContext: UserContext): LinkSink = DummyLinkSink(writeLinkFn, clearFn)

  override def entitySink(implicit userContext: UserContext): EntitySink = DummyEntitySink(writeEntityFn, clearFn)

  override def characteristics: DatasetCharacteristics = DatasetCharacteristics.attributesOnly()
}

case class DummyDataSource(retrieveFn: (EntitySchema, Option[Int]) => CloseableIterator[Entity],
                           retrieveByUriFn: (EntitySchema, Seq[Uri]) => CloseableIterator[Entity],
                           retrievePathsFn: (Uri, Int, Option[Int]) => IndexedSeq[TypedPath]) extends DataSource {
  override def retrieve(entitySchema: EntitySchema, limit: Option[Int])
                       (implicit userContext: UserContext, prefixes: Prefixes): EntityHolder = {
    GenericEntityTable(retrieveFn(entitySchema, limit), entitySchema, underlyingTask)
  }

  override def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri])
                            (implicit userContext: UserContext, prefixes: Prefixes): EntityHolder = {
    GenericEntityTable(retrieveByUriFn(entitySchema, entities), entitySchema, underlyingTask)
  }

  override def retrievePaths(typeUri: Uri, depth: Int, limit: Option[Int])
                            (implicit userContext: UserContext, prefixes: Prefixes): IndexedSeq[TypedPath] = {
    retrievePathsFn(typeUri, depth, limit)
  }

  override def retrieveTypes(limit: Option[Int])
                            (implicit userContext: UserContext, prefixes: Prefixes): Iterable[(String, Double)] = Iterable.empty

  override def underlyingTask: Task[DatasetSpec[Dataset]] = EmptySource.underlyingTask
}

case class DummyLinkSink(writeLinkFn: (Link, String) => Unit,
                         clearFn: () => Unit) extends LinkSink {
  override def init()(implicit userContext: UserContext, prefixes: Prefixes): Unit = {}

  override def writeLink(link: Link, predicateUri: String, inversePredicateUri: Option[String])
                        (implicit userContext: UserContext, prefixes: Prefixes): Unit = {
    writeLinkFn(link, predicateUri)
  }

  override def clear()(implicit userContext: UserContext): Unit = { clearFn() }

  override def close()(implicit userContext: UserContext): Unit = {}
}

case class DummyEntitySink(writeEntityFn: (String, Seq[Seq[String]]) => Unit,
                           clearFn: () => Unit) extends EntitySink {
  override def openTable(typeUri: Uri, properties: Seq[TypedProperty], singleEntity: Boolean = false)
                        (implicit userContext: UserContext, prefixes: Prefixes): Unit = {}

  override def writeEntity(subject: String, values: IndexedSeq[Seq[String]])
                          (implicit userContext: UserContext): Unit = {
    writeEntityFn(subject, values)
  }

  override def clear()(implicit userContext: UserContext): Unit = { clearFn() }

  override def closeTable()(implicit userContext: UserContext): Unit = {}

  override def close()(implicit userContext: UserContext): Unit = {}
}