package org.silkframework.execution.local

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.entity._
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.execution.InterruptibleIterator
import org.silkframework.runtime.iterator.CloseableIterator
import org.silkframework.util.Uri

case class LinksTable(
     links: Seq[Link],
     linkType: Uri,
     task: Task[TaskSpec]
   ) extends LocalEntitiesWithIterator {

  val entitySchema: EntitySchema = LinksTable.linkEntitySchema

  lazy val entities: CloseableIterator[Entity] = {
    for (link <- new InterruptibleIterator(CloseableIterator(links.iterator))) yield LinksTable.convertLinkToEntity(link, entitySchema)
  }

  override def entityIterator: Iterator[Entity] = {
    new LinkEntityIterator(links, entitySchema)
  }

  override def updateEntities(newEntities: CloseableIterator[Entity], newSchema: EntitySchema): GenericEntityTable = {
    new GenericEntityTable(newEntities, newSchema, task)
  }
}

class LinkEntityIterator(links: Seq[Link], entitySchema: EntitySchema) extends Iterator[Entity] {
  private val linkIterator = links.iterator

  override def hasNext: Boolean = linkIterator.hasNext

  override def next(): Entity = LinksTable.convertLinkToEntity(linkIterator.next(), entitySchema)
}

object LinksTable {

  val linkEntitySchema = EntitySchema("", IndexedSeq(
    TypedPath(UntypedPath("targetUri"), ValueType.URI, isAttribute = false),
    TypedPath(UntypedPath("confidence"), ValueType.DOUBLE, isAttribute = false)))

  def convertLinkToEntity(link: Link, entitySchema: EntitySchema): Entity = {
    Entity(
      uri = link.source,
      values = IndexedSeq(Seq(link.target), Seq(link.confidence.getOrElse(0.0).toString)),
      schema = entitySchema
    )
  }
}
