package de.fuberlin.wiwiss.silk.output.outputs

import de.fuberlin.wiwiss.silk.output.{Formatter, Link, Output}
import de.fuberlin.wiwiss.silk.util.StringUtils._
import java.io.{Writer, OutputStreamWriter, FileOutputStream, OutputStream}
import java.util.logging.Logger

/**
 * A file output.
 */
class FileOutput(val params : Map[String, String]) extends Output
{
    private val logger = Logger.getLogger(classOf[FileOutput].getName)

    private val formatter = Formatter(params.get("format").getOrElse(throw new IllegalArgumentException("No format specified")))

    private val minConfidence = params.get("minConfidence").map
    {
        case DoubleLiteral(c) => c
        case _ => throw new IllegalArgumentException("Parameter 'minConfidence' must be a number")
    }

    private val maxConfidence = params.get("maxConfidence").map
    {
        case DoubleLiteral(c) => c
        case _ => throw new IllegalArgumentException("Parameter 'maxConfidence' must be a number")
    }

    private val outputFile = params.get("file").getOrElse("output")

    private var out : Writer = null

    private var linkCount : Int = 0

    override def open : Unit =
    {
        out = new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8")
        out.write(formatter.header)
        linkCount = 0
    }

    override def write(link : Link, predicateUri : String) : Unit =
    {
        require(out != null, "Output must be opened befored writing statements to it")

        if((minConfidence.isEmpty || link.confidence > minConfidence.get) &&
           (maxConfidence.isEmpty || link.confidence <= maxConfidence.get))
        {
            out.write(formatter.format(link, predicateUri))
            linkCount += 1
        }
    }

    override def close : Unit =
    {
        out.write(formatter.footer)
        out.close()
        logger.info("Wrote " + linkCount + " links to " + outputFile)
    }
}
