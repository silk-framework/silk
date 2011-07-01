package de.fuberlin.wiwiss.silk.output

import de.fuberlin.wiwiss.silk.util.strategy.{Factory, Strategy}
import java.util.logging.Logger
import xml.Node

/**
 * Represents an abstraction over an output of links.
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

    if((minConfidence.isEmpty || link.confidence >= minConfidence.get) &&
       (maxConfidence.isEmpty || link.confidence < maxConfidence.get))
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

  //TODO write minConfidence, maxConfidence
  def toXML : Node = writer match
  {
    case LinkWriter(outputType, params) =>
    {
      <Output type={outputType}>
        { params.map{case (name, value) => <Param name={name} value={value} /> } }
      </Output>
    }
  }
}

object Output
{
  def fromXML(node : Node)(implicit globalThreshold : Option[Double]) =
  {
    new Output(
      writer = LinkWriter(node \ "@type" text, readParams(node)),
      minConfidence = (node \ "@minConfidence").headOption.map(_.text.toDouble).map(convertConfidence),
      maxConfidence = (node \ "@maxConfidence").headOption.map(_.text.toDouble).map(convertConfidence)
    )
  }

  private def convertConfidence(confidence : Double)(implicit globalThreshold : Option[Double]) = globalThreshold match
  {
    case Some(t) => (confidence - t) / (1.0 - t)
    case None => confidence
  }

  private def readParams(element : Node) : Map[String, String] =
  {
    (element \ "Param").map(p => (p \ "@name" text, p \ "@value" text)).toMap
  }
}
