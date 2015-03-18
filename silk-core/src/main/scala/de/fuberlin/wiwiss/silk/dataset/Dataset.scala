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

package de.fuberlin.wiwiss.silk.dataset

import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.entity.Link
import de.fuberlin.wiwiss.silk.runtime.resource.ResourceLoader
import de.fuberlin.wiwiss.silk.util.{Identifier, ValidatingXMLReader}
import scala.xml.Node

/**
 * A dataset of entities.
 */
// TODO add min and max confidence?
case class Dataset(id: Identifier, plugin: DatasetPlugin) {

  private val log = Logger.getLogger(Dataset.getClass.getName)

  def source = plugin.source

  lazy val sink = new DataSinkWrapper

  def toXML: Node = {
    val datasetXML = plugin match {
      case DatasetPlugin(pluginDesc, params) =>
        <DatasetPlugin type={pluginDesc.id}>
        { params.map {
            case (name, value) => <Param name={name} value={value}/>
        }}
        </DatasetPlugin>
    }

//    val selectionXML = {
//      <DataSelection type="sparql" var={selection.variable}>
//        { selection.toSparql }
//      </DataSelection>
//    }

    <Dataset id={id}>
      { datasetXML }
    </Dataset>
  }

  class DataSinkWrapper extends DataSink {

    private var linkCount: Int = 0

    private var isOpen = false

    private val writer = plugin.sink

    /**
     * Initializes this writer.
     */
    override def open(properties: Seq[String]) {
      require(!isOpen, "Output already open")

      writer.open(properties)
      linkCount = 0
      isOpen = true
    }



    /**
     * Writes a new link to this writer.
     */
    override def writeLink(link: Link, predicateUri: String) {
      require(isOpen, "Output must be opened befored writing statements to it")

      //        if ((minConfidence.isEmpty || link.confidence.getOrElse(-1.0) >= minConfidence.get) &&
      //            (maxConfidence.isEmpty || link.confidence.getOrElse(-1.0) < maxConfidence.get)) {
      writer.writeLink(link, predicateUri)
      linkCount += 1
      //        }

    }

    override def writeEntity(subject: String, values: Seq[Set[String]]) {
      require(isOpen, "Output must be opened befored writing statements to it")
      writer.writeEntity(subject, values)
    }

    /**
     * Closes this writer.
     */
    override def close() {
      if (isOpen) writer.close()
      isOpen = false

      log.info("Wrote " + linkCount + " links")
    }
  }
}

object Dataset {
  private val schemaLocation = "de/fuberlin/wiwiss/silk/LinkSpecificationLanguage.xsd"

  def empty = {
    Dataset("empty", EmptyDataset)
  }

  def load(resourceLoader: ResourceLoader) = {
    new ValidatingXMLReader(node => fromXML(node, resourceLoader), schemaLocation)
  }

  def fromXML(node: Node, resourceLoader: ResourceLoader): Dataset = {
    // Check if the data source still uses the old outdated XML format
    if(node.label == "DataSource" || node.label == "Output") {
      // Read old format
      new Dataset(
        id = (node \ "@id").text,
        plugin = DatasetPlugin((node \ "@type").text, readParams(node), resourceLoader)
      )
    } else {
      // Read new format
      val sourceNode = (node \ "DatasetPlugin").head
      new Dataset(
        id = (node \ "@id").text,
        plugin = DatasetPlugin((sourceNode \ "@type").text, readParams(sourceNode), resourceLoader)
      )
    }
  }

  private def readParams(element: Node): Map[String, String] = {
    (element \ "Param" map (p => ((p \ "@name").text, (p \ "@value").text))).toMap
  }
}