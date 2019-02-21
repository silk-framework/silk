package org.silkframework.dataset
import org.silkframework.config.Task
import org.silkframework.entity._
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
  var retrievePathsFn: (Uri, Int, Option[Int]) => IndexedSeq[TypedPath] = (_, _, _) => { IndexedSeq.empty }

  override def source(implicit userContext: UserContext): DataSource = DummyDataSource(retrieveFn, retrieveByUriFn, retrievePathsFn)

  override def linkSink(implicit userContext: UserContext): LinkSink = DummyLinkSink(writeLinkFn, clearFn)

  override def entitySink(implicit userContext: UserContext): EntitySink = DummyEntitySink(writeEntityFn, clearFn)
}

case class DummyDataSource(retrieveFn: (EntitySchema, Option[Int]) => Traversable[Entity],
                           retrieveByUriFn: (EntitySchema, Seq[Uri]) => Seq[Entity],
                           retrievePathsFn: (Uri, Int, Option[Int]) => IndexedSeq[TypedPath]) extends DataSource {
  override def retrieve(entitySchema: EntitySchema, limit: Option[Int])
                       (implicit userContext: UserContext): Traversable[Entity] = {
    retrieveFn(entitySchema, limit)
  }

  override def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri])
                            (implicit userContext: UserContext): Seq[Entity] = {
    retrieveByUriFn(entitySchema, entities)
  }

  override def retrievePaths(typeUri: Uri, depth: Int, limit: Option[Int])
                            (implicit userContext: UserContext): IndexedSeq[TypedPath] = {
    retrievePathsFn(typeUri, depth, limit)
  }

  override def retrieveTypes(limit: Option[Int])
                            (implicit userContext: UserContext): Traversable[(String, Double)] = Traversable.empty

  override def underlyingTask: Task[DatasetSpec[Dataset]] = EmptySource.underlyingTask
}

case class DummyLinkSink(writeLinkFn: (Link, String) => Unit,
                         clearFn: () => Unit) extends LinkSink {
  override def init()(implicit userContext: UserContext): Unit = {}

  override def writeLink(link: Link, predicateUri: String)
                        (implicit userContext: UserContext): Unit = {
    writeLinkFn(link, predicateUri)
  }

  override def clear()(implicit userContext: UserContext): Unit = { clearFn() }

  override def close()(implicit userContext: UserContext): Unit = {}
}

case class DummyEntitySink(writeEntityFn: (String, Seq[Seq[String]]) => Unit,
                           clearFn: () => Unit) extends EntitySink {
  override def openTable(typeUri: Uri, properties: Seq[TypedProperty])
                        (implicit userContext: UserContext): Unit = {}

  override def writeEntity(subject: String, values: Seq[Seq[String]])
                          (implicit userContext: UserContext): Unit = {
    writeEntityFn(subject, values)
  }

  override def clear()(implicit userContext: UserContext): Unit = { clearFn() }

  override def closeTable()(implicit userContext: UserContext): Unit = {}

  override def close()(implicit userContext: UserContext): Unit = {}
}