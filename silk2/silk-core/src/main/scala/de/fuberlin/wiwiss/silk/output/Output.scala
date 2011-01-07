package de.fuberlin.wiwiss.silk.output

import de.fuberlin.wiwiss.silk.util.strategy.{Factory, Strategy}
import java.util.logging.Logger

/**
 * Represents an abstraction over an writer of links.
 *
 * Implementing classes of this trait must override the write method.
 */
case class Output(writer : LinkWriter, minConfidence : Option[Double] = None, maxConfidence : Option[Double] = None)
{
    private val logger = Logger.getLogger(classOf[Output].getName)

    private var linkCount : Int = 0

    private var isOpen = false

    /**
     * Initializes this writer.
     */
    def open()
    {
        require(!isOpen, "Output already open")

        writer.open()
        linkCount = 0
        isOpen = true
    }

    /**
     * Writes a new link to this writer.
     */
    def write(link : Link, predicateUri : String)
    {
        require(isOpen, "Output must be opened befored writing statements to it")

        if((minConfidence.isEmpty || link.confidence > minConfidence.get) &&
           (maxConfidence.isEmpty || link.confidence <= maxConfidence.get))
        {
            writer.write(link, predicateUri)
            linkCount += 1
        }

    }

    /**
     * Closes this writer.
     */
    def close()
    {
        if(isOpen) writer.close()
        isOpen = false

        logger.info("Wrote " + linkCount + " links")
    }
}
