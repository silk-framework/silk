package de.fuberlin.wiwiss.silk.plugins.writer

import de.fuberlin.wiwiss.silk.output.{Formatter, Link, LinkWriter}
import java.io.{Writer, OutputStreamWriter, FileOutputStream}
import de.fuberlin.wiwiss.silk.util.plugin.Plugin

/**
 * A file writer.
 */
@Plugin(id = "file", label = "File")
case class FileWriter(file: String = "output.nt", format: String = "ntriples") extends LinkWriter {
  private val formatter = Formatter(format)

  private var out: Writer = null

  override def open() {
    out = new OutputStreamWriter(new FileOutputStream(file), "UTF-8")
    out.write(formatter.header)
  }

  override def write(link: Link, predicateUri: String) {
    out.write(formatter.format(link, predicateUri))
  }

  override def close() {
    if (out != null) {
      out.write(formatter.footer)
      out.close()
      out = null
    }
  }
}
