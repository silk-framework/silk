package de.fuberlin.wiwiss.silk.impl.writer

import de.fuberlin.wiwiss.silk.output.{Formatter, Link, LinkWriter}
import java.io.{Writer, OutputStreamWriter, FileOutputStream}
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation

/**
 * A file writer.
 */
@StrategyAnnotation(id = "file", label = "File")
class FileWriter(file : String = "output", format : String) extends LinkWriter
{
  private val formatter = Formatter(format)

  private var out : Writer = null

  override def open : Unit =
  {
    out = new OutputStreamWriter(new FileOutputStream(file), "UTF-8")
    out.write(formatter.header)
  }

  override def write(link : Link, predicateUri : String) : Unit =
  {
    out.write(formatter.format(link, predicateUri))
  }

  override def close : Unit =
  {
    if(out != null)
    {
      out.write(formatter.footer)
      out.close()
      out = null
    }
  }
}
