package org.silkframework.dataset
import org.silkframework.entity.{Entity, EntitySchema, Link}
import org.silkframework.util.Uri

/**
  * Dummy dataset for tests. The main methods for retrieving and writing can be defined as parameter.
  */
case class DummyDataset(name: String = "dummy") extends Dataset {
  var retrieveFn: (EntitySchema, Option[Int]) => Traversable[Entity] = (_, _) => Seq.empty
  var retrieveByUriFn: (EntitySchema, Seq[Uri]) => Seq[Entity] = (_, _) => Seq.empty
  var writeLinkFn: (Link, String) => Unit = (_, _) => {}
  var writeEntityFn: (String, Seq[Seq[String]]) => Unit = (_, _) => {}

  override def source: DataSource = DummyDataSource(retrieveFn, retrieveByUriFn)

  override def linkSink: LinkSink = DummyLinkSink(writeLinkFn)

  override def entitySink: EntitySink = DummyEntitySink(writeEntityFn)
}

case class DummyDataSource(retrieveFn: (EntitySchema, Option[Int]) => Traversable[Entity],
                           retrieveByUriFn: (EntitySchema, Seq[Uri]) => Seq[Entity]) extends DataSource {
  override def retrieve(entitySchema: EntitySchema, limit: Option[Int]): Traversable[Entity] = {
    retrieveFn(entitySchema, limit)
  }

  override def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri]): Seq[Entity] = {
    retrieveByUriFn(entitySchema, entities)
  }
}

case class DummyLinkSink(writeLinkFn: (Link, String) => Unit) extends LinkSink {
  override def init(): Unit = {}

  override def writeLink(link: Link, predicateUri: String): Unit = {
    writeLinkFn(link, predicateUri)
  }

  override def clear(): Unit = {}

  override def close(): Unit = {}
}

case class DummyEntitySink(writeEntityFn: (String, Seq[Seq[String]]) => Unit) extends EntitySink {
  override def open(typeUri: Uri, properties: Seq[TypedProperty]): Unit = {}

  override def writeEntity(subject: String, values: Seq[Seq[String]]): Unit = {
    writeEntityFn(subject, values)
  }

  override def clear(): Unit = {}

  override def close(): Unit = {}
}