package de.fuberlin.wiwiss.silk

import de.fuberlin.wiwiss.silk.util.Task
import linkspec.LinkSpecification
import output.Link
import collection.mutable.{ArrayBuffer, Buffer}

/**
 * Filters the links according to the link limit.
 */
class FilterTask(linkSpec : LinkSpecification, links : Buffer[Link]) extends Task[Buffer[Link]]
{
  override def execute() : Buffer[Link] =
  {
    linkSpec.filter.limit match
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
