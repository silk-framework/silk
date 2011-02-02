package de.fuberlin.wiwiss.silk

import de.fuberlin.wiwiss.silk.util.Task
import linkspec.LinkFilter
import output.Link
import collection.mutable.{ArrayBuffer, Buffer}

/**
 * Filters the links according to the link limit.
 */
class FilterTask(links : Buffer[Link], filter : LinkFilter) extends Task[Buffer[Link]]
{
  override def execute() : Buffer[Link] =
  {
    filter.limit match
    {
      case Some(limit) =>
      {
        val linkBuffer = new ArrayBuffer[Link]()
        updateStatus("Filtering output")

        for((sourceUri, groupedLinks) <- links.groupBy(_.sourceUri))
        {
          val bestLinks = groupedLinks.sortWith(_.confidence > _.confidence).take(limit)

          linkBuffer.appendAll(bestLinks)
        }

        linkBuffer
      }
      case None => links.distinct
    }
  }
}
