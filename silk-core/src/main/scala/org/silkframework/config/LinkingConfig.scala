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

package org.silkframework.config

import org.silkframework.dataset.Dataset
import org.silkframework.runtime.resource.{ResourceManager, ResourceLoader}
import org.silkframework.runtime.serialization.{XmlFormat, Serialization, ValidatingXMLReader}
import org.silkframework.util.Identifier
import org.silkframework.runtime.serialization.Serialization._
import scala.xml.Node

/**
 * A Silk linking configuration.
 * Specifies how multiple sources are interlinked by defining a link specification for each type of entity to be interlinked.
 *
 * @param prefixes The prefixes which are used throughout the configuration to shorten URIs
 * @param sources The sources which should be interlinked
 * @param linkSpecs The Silk link specifications
 * @param outputs The outputs
 */
case class LinkingConfig(prefixes: Prefixes,
                         runtime: RuntimeConfig,
                         sources: Traversable[Dataset],
                         linkSpecs: Traversable[LinkSpecification],
                         outputs: Seq[Dataset] = Seq.empty,
                         transforms: Traversable[TransformSpecification] = Seq.empty) {

  private val sourceMap = sources.map(s => (s.id, s)).toMap
  private val linkSpecMap = linkSpecs.map(s => (s.id, s)).toMap
  private val outputMap = outputs.map(s => (s.id, s)).toMap

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
   *
   * @see linkSpec
   *
   * @param id The identifier.
   * @return A LinkSpecification instance.
   */
  def interlink(id: Identifier) = linkSpecs.find(id == _.id)

  /**
   * Select a transform given an Id.
   *
   * @since 2.6.1
   *
   * @param id The transform identifier.
   * @return A transform instance or None.
   */
  def transform(id: Identifier) = transforms.find(id == _.id)

  /**
   * Selects an output by id.
   */
  def output(id: Identifier) = outputMap(id)

  /**
   * Merges this configuration with another configuration.
   */
  def merge(config: LinkingConfig) = {
    LinkingConfig(
      prefixes = prefixes ++ config.prefixes,
      runtime = runtime,
      sources = sources ++ config.sources,
      linkSpecs = linkSpecs ++ config.linkSpecs,
      outputs = outputs ++ config.outputs,
      transforms = transforms ++ config.transforms
    )
  }
}

object LinkingConfig {

  def empty = LinkingConfig(Prefixes.empty, RuntimeConfig(), Nil, Nil, Nil)

  /**
   * XML serialization format.
   * Reference links are currently not serialized and need to be serialize separably.
   */
  implicit object LinkingConfigFormat extends XmlFormat[LinkingConfig] {

    private val schemaLocation = "org/silkframework/LinkSpecificationLanguage.xsd"

    /**
     * Deserializes a LinkingConfig from XML.
     */
    def read(node: Node)(implicit prefixes: Prefixes, resources: ResourceManager): LinkingConfig = {
      // Validate against XSD Schema
      ValidatingXMLReader.validate(node, schemaLocation)

      implicit val prefixes = Prefixes.fromXML((node \ "Prefixes").head)
      val sources = (node \ "DataSources" \ "DataSource").map(fromXml[Dataset]).toSet
      val blocking = (node \ "Blocking").headOption match {
        case Some(blockingNode) => Blocking.fromXML(blockingNode)
        case None => Blocking()
      }
      val linkSpecifications = (node \ "Interlinks" \ "Interlink").map(p => fromXml[LinkSpecification](p))
      val transforms = (node \ "Transforms" \ "Transform").map(p => TransformSpecification.fromXML(p, resources))

      implicit val globalThreshold = None
      val outputs = (node \ "Outputs" \ "Output").map(fromXml[Dataset])

      LinkingConfig(prefixes, RuntimeConfig(blocking = blocking), sources, linkSpecifications, outputs, transforms)
    }


    /**
     * Serializes a LinkingConfig to XML.
     */
    def write(value: LinkingConfig)(implicit prefixes: Prefixes): Node = {
      <Silk>
        {value.prefixes.toXML}
        <DataSources>
          {value.sources.map(toXml[Dataset])}
        </DataSources>
        <Interlinks>
          {value.linkSpecs.map(spec => Serialization.toXml(spec))}
        </Interlinks>
        <Outputs>
          {value.outputs.map(toXml[Dataset])}
        </Outputs>
      </Silk>

      // TODO: add support for serializing the transforms.
    }
  }
}
