package de.fuberlin.wiwiss.silk

import config.LinkFilter
import de.fuberlin.wiwiss.silk.util.task.Task
import collection.mutable.{ArrayBuffer, Buffer}
import entity.Link

/**
 * Filters the links according to the link limit.
 */
class FilterTask(links: Seq[Link], filter: LinkFilter) extends Task[Seq[Link]] {
  taskName = "Filtering"

  override def execute(): Seq[Link] = {
    filter.limit match {
      case Some(limit) => {
        val linkBuffer = new ArrayBuffer[Link]()
        updateStatus("Filtering output")

        for ((sourceUri, groupedLinks) <- links.groupBy(_.source)) {
          val bestLinks = groupedLinks.sortWith(_.confidence.getOrElse(-1.0) > _.confidence.getOrElse(-1.0)).take(limit)

          linkBuffer.appendAll(bestLinks)
        }

        logger.info("Filtered " + links.size + " links yielding " + linkBuffer.size + " links")

        linkBuffer
      }
      case None => links.distinct
    }
  }
}
