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

package org.silkframework.rule

import org.silkframework.config._
import org.silkframework.dataset.{Dataset, DatasetSpec}
import org.silkframework.runtime.serialization.XmlSerialization._
import org.silkframework.runtime.serialization._
import org.silkframework.util.Identifier
import org.silkframework.workspace.LoadedTask

import scala.xml.Node

/**
 * A Silk linking configuration.
 * Specifies how multiple sources are interlinked by defining a link specification for each type of entity to be interlinked.
 *
 * @param prefixes The prefixes which are used throughout the configuration to shorten URIs
 * @param sources The sources which should be interlinked
 * @param linkSpecs The Silk link specifications
 * @param output  The output
 */
case class LinkingConfig(prefixes: Prefixes,
                         runtime: RuntimeLinkingConfig,
                         sources: Iterable[Task[DatasetSpec[Dataset]]],
                         linkSpecs: Iterable[Task[LinkSpec]],
                         output: Option[Task[DatasetSpec[Dataset]]] = None,
                         transforms: Iterable[Task[TransformSpec]] = Seq.empty) {

  private val sourceMap = sources.map(s => (s.id, s)).toMap
  private val linkSpecMap = linkSpecs.map(s => (s.id, s)).toMap

  /**
   * Selects a datasource by id.
   */
  def source(id: Identifier) = sourceMap(id)

  /**
   * Selects a link specification by id.
   */
  def linkSpec(id: Identifier) = linkSpecMap(id)

  /**
   * Get a link specification given an Identifier. This method is similar to *linkSpec* but returns an Option.
   *
   * @since 2.6.1
    * @see linkSpec
    * @param id The identifier.
   * @return A LinkSpecification instance.
   */
  def interlink(id: Identifier) = linkSpecs.find(id == _.id)

  /**
   * Select a transform given an Id.
   *
   * @since 2.6.1
    * @param id The transform identifier.
   * @return A transform instance or None.
   */
  def transform(id: Identifier) = transforms.find(id == _.id)

}

object LinkingConfig {

  def empty = LinkingConfig(Prefixes.empty, RuntimeLinkingConfig(), Nil, Nil, None)

  /**
   * XML serialization format.
   * Reference links are currently not serialized and need to be serialize separably.
   */
  implicit object LinkingConfigFormat extends XmlFormat[LinkingConfig] {

    private val schemaLocation = "org/silkframework/LinkSpecificationLanguage.xsd"

    /**
     * Deserializes a LinkingConfig from XML.
     */
    def read(node: Node)(implicit readContext: ReadContext): LinkingConfig = {
      // Validate against XSD Schema
      ValidatingXMLReader.validate(node, schemaLocation)

      implicit val prefixes = Prefixes.fromXML((node \ "Prefixes").head)

      readWithPrefixes(node)(readContext.copy(prefixes = prefixes))
    }

    private def readWithPrefixes(node: Node)(implicit readContext: ReadContext) = {
      val oldSources = (node \ "DataSources" \ "DataSource").map(n => fromXml[Task[DatasetSpec[Dataset]]](n)).toSet
      val newSources = (node \ "DataSources" \ "Dataset").map(n => fromXml[Task[DatasetSpec[Dataset]]](n)).toSet
      val sources = oldSources ++ newSources
      val blocking = (node \ "Blocking").headOption match {
        case Some(blockingNode) => Blocking.fromXML(blockingNode)
        case None => Blocking()
      }
      val linkSpecifications = (node \ "Interlinks" \ "Interlink").map(p => fromXml[Task[LinkSpec]](p))
      val transforms = (node \ "Transforms" \ "Transform").map(p => fromXml[Task[TransformSpec]](p))

      implicit val globalThreshold = None

      val oldOutputs = (node \ "Outputs" \ "Output").map(fromXml[Task[DatasetSpec[Dataset]]])
      val newOutputs = (node \ "Outputs" \ "Dataset").map(fromXml[Task[DatasetSpec[Dataset]]])
      val output = (oldOutputs ++ newOutputs).headOption

      LinkingConfig(readContext.prefixes, RuntimeLinkingConfig(blocking = blocking), sources, linkSpecifications, output, transforms)
    }

    /**
     * Serializes a LinkingConfig to XML.
     */
    def write(value: LinkingConfig)(implicit writeContext: WriteContext[Node]): Node = {
      <Silk>
        {value.prefixes.toXML}
        <DataSources>
          {value.sources.map(toXml[Task[DatasetSpec[Dataset]]])}
        </DataSources>
        <Interlinks>
          {value.linkSpecs.map(spec => XmlSerialization.toXml(spec))}
        </Interlinks>
        <Transforms>
          {value.transforms.map(spec => XmlSerialization.toXml(spec))}
        </Transforms>
        <Outputs>
          {value.output.toSeq.map(toXml[Task[DatasetSpec[Dataset]]])}
        </Outputs>
      </Silk>
    }
  }
}
