package org.silkframework.execution.local

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.entity._
import org.silkframework.execution.{EntityHolder, InterruptibleTraversable, Links}
import org.silkframework.util.Uri

class LocalLinks(
     val links: Seq[Link],
     linkType: Uri,
     override val task: Task[TaskSpec]
   ) extends Links(linkType) with LocalEntitiesWithIterator {

  override def baseEntities: EntityHolder = {
    val entities = for (link <- new InterruptibleTraversable(links)) yield Links.createEntityFromLink(link)
    new GenericEntityTable(entities, Links.linkEntitySchema, task)
  }

  override def entityIterator: Iterator[Entity] = {
    new LinkEntityIterator(links)
  }
}

object LocalLinks {

  def fromLinks(links: Seq[Link], linkType: Uri, task: Task[TaskSpec]): LocalLinks = {
    new LocalLinks(links, linkType, task)
  }

}

class LinkEntityIterator(links: Seq[Link]) extends Iterator[Entity] {

  private val linkIterator = links.iterator

  override def hasNext: Boolean = linkIterator.hasNext

  override def next(): Entity = Links.createEntityFromLink(linkIterator.next())
}
