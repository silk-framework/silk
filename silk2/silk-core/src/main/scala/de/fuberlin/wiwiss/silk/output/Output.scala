/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.output

import java.util.logging.Logger
import xml.Node
import de.fuberlin.wiwiss.silk.util.{Identifier, ValidatingXMLReader}
import de.fuberlin.wiwiss.silk.entity.Link

/**
 * Represents an abstraction over an output of links.
 */
case class Output(id: Identifier, writer: LinkWriter, minConfidence: Option[Double] = None, maxConfidence: Option[Double] = None) {
  private val logger = Logger.getLogger(classOf[Output].getName)

  private var linkCount: Int = 0

  private var isOpen = false

  /**
   * Initializes this writer.
   */
  def open() {
    require(!isOpen, "Output already open")

    writer.open()
    linkCount = 0
    isOpen = true
  }

  /**
   * Writes a new link to this writer.
   */
  def write(link: Link, predicateUri: String) {
    require(isOpen, "Output must be opened befored writing statements to it")

    if ((minConfidence.isEmpty || link.confidence.getOrElse(-1.0) >= minConfidence.get) &&
        (maxConfidence.isEmpty || link.confidence.getOrElse(-1.0) < maxConfidence.get)) {
      writer.write(link, predicateUri)
      linkCount += 1
    }

  }

  /**
   * Closes this writer.
   */
  def close() {
    if (isOpen) writer.close()
    isOpen = false

    logger.info("Wrote " + linkCount + " links")
  }

  def writeAll(links: Traversable[Link], predicateUri: String) {
    open()
    for (link <- links) write(link, predicateUri)
    close()
  }

  //TODO write minConfidence, maxConfidence
  def toXML: Node = writer match {
    case LinkWriter(outputType, params) => {
      <Output id={id} type={outputType}>
        {params.map {
        case (name, value) => <Param name={name} value={value}/>
      }}
      </Output>
    }
  }
}

object Output {
  private val schemaLocation = "de/fuberlin/wiwiss/silk/LinkSpecificationLanguage.xsd"

  def load = {
    new ValidatingXMLReader(node => fromXML(node), schemaLocation)
  }

  def fromXML(node: Node)(implicit globalThreshold: Option[Double] = None) = {
    Output(
      id = Identifier((node \ "@id").headOption.map(_.text).getOrElse("id")),
      writer = LinkWriter(node \ "@type" text, readParams(node)),
      minConfidence = (node \ "@minConfidence").headOption.map(_.text.toDouble).map(convertConfidence),
      maxConfidence = (node \ "@maxConfidence").headOption.map(_.text.toDouble).map(convertConfidence)
    )
  }

  private def convertConfidence(confidence: Double)(implicit globalThreshold: Option[Double]) = globalThreshold match {
    case Some(t) => (confidence - t) / (1.0 - t)
    case None => confidence
  }

  private def readParams(element: Node): Map[String, String] = {
    (element \ "Param").map(p => (p \ "@name" text, p \ "@value" text)).toMap
  }
}
