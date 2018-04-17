package org.silkframework.execution.local

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.entity._
import org.silkframework.util.Uri

case class LinksTable(links: Seq[Link], linkType: Uri, taskOption: Option[Task[TaskSpec]]) extends LocalEntities {

  val entitySchema: EntitySchema = LinksTable.linkEntitySchema

  val entities: Seq[Entity] = {
    for (link <- links) yield
      Entity(
        uri = link.source,
        values = IndexedSeq(Seq(link.target), Seq(link.confidence.getOrElse(0.0).toString)),
        schema = entitySchema
      )
  }
}

object LinksTable {

  val linkEntitySchema = EntitySchema("", IndexedSeq(
    TypedPath(Path("targetUri"), AutoDetectValueType, isAttribute = false),
    TypedPath(Path("confidence"), DoubleValueType, isAttribute = false)))
}
