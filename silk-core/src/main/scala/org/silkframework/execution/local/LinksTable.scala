package org.silkframework.execution.local

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.entity._
import org.silkframework.util.Uri

case class LinksTable(links: Seq[Link], linkType: Uri, task: Task[TaskSpec]) extends EntityTable {

  val entitySchema = LinksTable.linkEntitySchema

  val entities = {
    for (link <- links) yield
      new Entity(
        uri = link.source,
        values = IndexedSeq(Seq(link.target), Seq(link.confidence.getOrElse(0.0).toString)),
        desc = entitySchema
      )
  }

}

object LinksTable {

  val linkEntitySchema = EntitySchema("", IndexedSeq(TypedPath(Path("targetUri"), UriValueType), TypedPath(Path("confidence"), DoubleValueType)))

}
