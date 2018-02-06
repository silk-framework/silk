package org.silkframework.plugins.dataset.rdf.formatters

import java.io._
import java.util.logging.Logger

import org.silkframework.dataset.LinkSink
import org.silkframework.entity.Link
import org.silkframework.runtime.resource.{FileResource, WritableResource}

/**
 * A link sink that writes formatted links to an output stream of a resource.
 */
class FormattedLinkSink (resource: WritableResource, formatter: LinkFormatter) extends LinkSink {

  private val log: Logger = Logger.getLogger(this.getClass.getName)

  // We optimize cases in which the resource is a file resource
  private val javaFile = resource match {
    case f: FileResource => Some(f.file)
    case _ => None
  }

  private var formattedLinkWriter: Option[Writer] = None

  private def write(s: String): Unit = {
    formattedLinkWriter match {
      case Some(writer) =>
        writer.write(s)
      case None =>
        log.warning("Tried to write to a link sink that is not open")
    }
  }

  override def init(): Unit = {
    // If we got a java file, we write directly to it, otherwise we write to a temporary string
    formattedLinkWriter = javaFile match {
      case Some(file) =>
        file.getParentFile.mkdirs()
        Some(new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(file, true)), "UTF-8"))
      case None =>
        Some(new StringWriter())
    }
    //Write header
    write(formatter.header)
  }

  override def writeLink(link: Link, predicateUri: String) {
    write(formatter.format(link, predicateUri))
  }

  override def close() {
    formattedLinkWriter match {
      case Some(writer: StringWriter) =>
        write(formatter.footer)
        resource.writeString(writer.toString, append = true)
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
    formattedLinkWriter = null
  }

  /**
    * Makes sure that the next write will start from an empty dataset.
    */
  override def clear(): Unit = {
    resource.delete()
  }
}