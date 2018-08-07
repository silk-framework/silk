package org.silkframework.dataset
import org.silkframework.config.Task
import org.silkframework.entity.{Entity, EntitySchema, Link, Path}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Uri

/**
  * Mock dataset for tests. The main methods for retrieving and writing can be defined as parameter.
  */
case class MockDataset(name: String = "dummy") extends Dataset {
  var retrieveFn: (EntitySchema, Option[Int]) => Traversable[Entity] = (_, _) => Seq.empty
  var retrieveByUriFn: (EntitySchema, Seq[Uri]) => Seq[Entity] = (_, _) => Seq.empty
  var writeLinkFn: (Link, String) => Unit = (_, _) => {}
  var writeEntityFn: (String, Seq[Seq[String]]) => Unit = (_, _) => {}
  var clearFn: () => Unit = () => {}
  var retrievePathsFn: (Uri, Int, Option[Int]) => IndexedSeq[Path] = (_, _, _) => { IndexedSeq.empty }

  override def source(implicit userContext: UserContext): DataSource = DummyDataSource(retrieveFn, retrieveByUriFn, retrievePathsFn)

  override def linkSink(implicit userContext: UserContext): LinkSink = DummyLinkSink(writeLinkFn, clearFn)

  override def entitySink(implicit userContext: UserContext): EntitySink = DummyEntitySink(writeEntityFn, clearFn)
}

case class DummyDataSource(retrieveFn: (EntitySchema, Option[Int]) => Traversable[Entity],
                           retrieveByUriFn: (EntitySchema, Seq[Uri]) => Seq[Entity],
                           retrievePathsFn: (Uri, Int, Option[Int]) => IndexedSeq[Path]) extends DataSource {
  override def retrieve(entitySchema: EntitySchema, limit: Option[Int]): Traversable[Entity] = {
    retrieveFn(entitySchema, limit)
  }

  override def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri]): Seq[Entity] = {
    retrieveByUriFn(entitySchema, entities)
  }

  override def retrievePaths(typeUri: Uri, depth: Int, limit: Option[Int]): IndexedSeq[Path] = {
    retrievePathsFn(typeUri, depth, limit)
  }

  override def retrieveTypes(limit: Option[Int]): Traversable[(String, Double)] = Traversable.empty

  override def underlyingTask: Task[DatasetSpec[Dataset]] = EmptySource.underlyingTask
}

case class DummyLinkSink(writeLinkFn: (Link, String) => Unit,
                         clearFn: () => Unit) extends LinkSink {
  override def init()(implicit userContext: UserContext): Unit = {}

  override def writeLink(link: Link, predicateUri: String): Unit = {
    writeLinkFn(link, predicateUri)
  }

  override def clear(): Unit = { clearFn() }

  override def close(): Unit = {}
}

case class DummyEntitySink(writeEntityFn: (String, Seq[Seq[String]]) => Unit,
                           clearFn: () => Unit) extends EntitySink {
  override def openTable(typeUri: Uri, properties: Seq[TypedProperty]): Unit = {}

  override def writeEntity(subject: String, values: Seq[Seq[String]]): Unit = {
    writeEntityFn(subject, values)
  }

  override def clear(): Unit = { clearFn() }

  override def closeTable(): Unit = {}

  override def close(): Unit = {}
}