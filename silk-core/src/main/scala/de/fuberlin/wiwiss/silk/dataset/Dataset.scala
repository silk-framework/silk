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

import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.entity.Link
import de.fuberlin.wiwiss.silk.runtime.resource.ResourceLoader
import de.fuberlin.wiwiss.silk.runtime.serialization.XmlFormat
import de.fuberlin.wiwiss.silk.util.Identifier

import scala.xml.Node

/**
 * A dataset of entities.
 */
case class Dataset(id: Identifier, plugin: DatasetPlugin, minConfidence: Option[Double] = None, maxConfidence: Option[Double] = None) {

  private val log = Logger.getLogger(Dataset.getClass.getName)

  def source = plugin.source

  lazy val sink = new DataSinkWrapper

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
      require(isOpen, "Output must be opened before writing statements to it")

      if ((minConfidence.isEmpty || link.confidence.getOrElse(-1.0) >= minConfidence.get) &&
          (maxConfidence.isEmpty || link.confidence.getOrElse(-1.0) < maxConfidence.get)) {
        writer.writeLink(link, predicateUri)
        linkCount += 1
      }
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

  def empty = {
    Dataset("empty", EmptyDataset)
  }

  /**
   * XML serialization format.
   */
  implicit object DatasetFormat extends XmlFormat[Dataset] {

    def read(node: Node)(implicit prefixes: Prefixes, resourceLoader: ResourceLoader): Dataset = {
      // Check if the data source still uses the old outdated XML format
      if(node.label == "DataSource" || node.label == "Output") {
        // Read old format
        val id = (node \ "@id").text
        new Dataset(
          id = if(id.nonEmpty) id else Identifier.random,
          plugin = DatasetPlugin((node \ "@type").text, readParams(node), resourceLoader),
          minConfidence = (node \ "@minConfidence").headOption.map(_.text.toDouble),
          maxConfidence = (node \ "@maxConfidence").headOption.map(_.text.toDouble)
        )
      } else {
        // Read new format
        val id = (node \ "@id").text
        val sourceNode = (node \ "DatasetPlugin").head
        new Dataset(
          id = if(id.nonEmpty) id else Identifier.random,
          plugin = DatasetPlugin((sourceNode \ "@type").text, readParams(sourceNode), resourceLoader),
          minConfidence = (node \ "@minConfidence").headOption.map(_.text.toDouble),
          maxConfidence = (node \ "@maxConfidence").headOption.map(_.text.toDouble)
        )
      }
    }

    private def readParams(element: Node): Map[String, String] = {
      (element \ "Param" map (p => ((p \ "@name").text, (p \ "@value").text))).toMap
    }

    def write(value: Dataset)(implicit prefixes: Prefixes): Node = {
      val datasetXML = value.plugin match {
        case DatasetPlugin(pluginDesc, params) =>
          <DatasetPlugin type={pluginDesc.id}>
            { params.map {
            case (name, v) => <Param name={name} value={v}/>
          }}
          </DatasetPlugin>
      }

      //    val selectionXML = {
      //      <DataSelection type="sparql" var={selection.variable}>
      //        { selection.toSparql }
      //      </DataSelection>
      //    }

      <Dataset id={value.id}>
        { datasetXML }
      </Dataset>
    }
  }
}