package de.fuberlin.wiwiss.silk.impl.writer

import de.fuberlin.wiwiss.silk.output.{Formatter, Link, AlignmentWriter}
import java.io.{Writer, OutputStreamWriter, FileOutputStream}
import java.util.logging.Logger

/**
 * A file writer.
 */
class FileWriter(val params : Map[String, String]) extends AlignmentWriter
{
    private val formatter = Formatter(params.get("format").getOrElse(throw new IllegalArgumentException("No format specified")))

    private val outputFile = params.get("file").getOrElse("output")

    private var out : Writer = null

    override def open : Unit =
    {
        out = new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8")
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
