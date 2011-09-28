package de.fuberlin.wiwiss.silk

import config.LinkFilter
import de.fuberlin.wiwiss.silk.util.task.Task
import output.Link
import collection.mutable.{ArrayBuffer, Buffer}

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
          val bestLinks = groupedLinks.sortWith(_.confidence > _.confidence).take(limit)

          linkBuffer.appendAll(bestLinks)
        }

        logger.info("Filtered " + links.size + " links yielding " + linkBuffer.size + " links")

        linkBuffer
      }
      case None => links.distinct
    }
  }
}
