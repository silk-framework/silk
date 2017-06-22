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

  private var formattedLinkWriter: Writer = null

  private def write(s: String): Unit = {
    formattedLinkWriter match {
      case writer: Writer =>
        writer.write(s)
      case _ =>
        log.warning("Not initialized!")
    }
  }

  override def init(): Unit = {
    // If we got a java file, we write directly to it, otherwise we write to a temporary string
    formattedLinkWriter = javaFile match {
      case Some(file) =>
        file.getParentFile.mkdirs()
        new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true), "UTF-8"))
      case None =>
        new StringWriter()
    }
    //Write header
    write(formatter.header)
  }

  override def writeLink(link: Link, predicateUri: String) {
    write(formatter.format(link, predicateUri))
  }

  override def close() {
    write(formatter.footer)
    formattedLinkWriter match {
      case writer: Writer =>
        writer.flush()
        writer.close()
        if(writer.isInstanceOf[StringWriter]) {
          resource.writeString(writer.asInstanceOf[StringWriter].toString, append = true)
        }
      case _ =>
        log.warning("Not initialized!")
        // Nothing to be done
    }
  }

  /**
    * Makes sure that the next write will start from an empty dataset.
    */
  override def clear(): Unit = {
    resource.delete()
  }
}