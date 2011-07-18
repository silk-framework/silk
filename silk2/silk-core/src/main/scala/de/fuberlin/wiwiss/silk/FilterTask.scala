package de.fuberlin.wiwiss.silk

import de.fuberlin.wiwiss.silk.util.task.Task
import linkspec.LinkFilter
import output.Link
import collection.mutable.{ArrayBuffer, Buffer}
import java.util.logging.Logger

/**
 * Filters the links according to the link limit.
 */
class FilterTask(links : Buffer[Link], filter : LinkFilter) extends Task[Buffer[Link]]
{
  taskName = "Filtering"

  private val logger = Logger.getLogger(classOf[MatchTask].getName)

  override def execute() : Buffer[Link] =
  {
    filter.limit match
    {
      case Some(limit) =>
      {
        val linkBuffer = new ArrayBuffer[Link]()
        updateStatus("Filtering output")

        for((sourceUri, groupedLinks) <- links.groupBy(_.source))
        {
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
