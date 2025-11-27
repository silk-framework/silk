package org.silkframework.plugins.dataset.rdf.formatters

import org.silkframework.config.Prefixes
import org.silkframework.dataset.LinkSink
import org.silkframework.dataset.rdf.LinkFormatter
import org.silkframework.entity.Link
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.WritableResource

import java.io._
import java.util.logging.Logger

/**
 * A link sink that writes formatted links to an output stream of a resource.
 */
class FormattedLinkSink (resource: WritableResource, formatter: LinkFormatter) extends LinkSink {

  private val log: Logger = Logger.getLogger(this.getClass.getName)

  private var formattedLinkWriter: Option[Writer] = None

  private def write(s: String): Unit = {
    formattedLinkWriter match {
      case Some(writer) =>
        writer.write(s)
      case None =>
        log.warning("Tried to write to a link sink that is not open")
    }
  }

  override def init()(implicit userContext: UserContext, prefixes: Prefixes): Unit = {
    val outputStream = resource.createOutputStream(append = true)
    formattedLinkWriter = Some(new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8")))
    //Write header
    write(formatter.header)
  }

  override def writeLink(link: Link, predicateUri: String, inversePredicateUri: Option[String])
                        (implicit userContext: UserContext, prefixes: Prefixes): Unit = {
    write(formatter.formatLink(link, predicateUri, inversePredicateUri))
  }

  override def close()(implicit userContext: UserContext): Unit = {
    formattedLinkWriter match {
      case Some(writer: Writer) =>
        try {
          write(formatter.footer)
        } finally {
          writer.close()
        }
      case None =>
        log.warning("Closing link sink that is already closed")
        // Nothing to be done
    }
    formattedLinkWriter = None
  }

  /**
    * Makes sure that the next write will start from an empty dataset.
    */
  override def clear(force: Boolean = false)(implicit userContext: UserContext): Unit = {
    resource.delete()
  }
}