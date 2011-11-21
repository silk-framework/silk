/* 
 * Copyright 2009-2011 Freie Universität Berlin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.config

import de.fuberlin.wiwiss.silk.output.Output
import de.fuberlin.wiwiss.silk.datasource.Source
import xml.Node
import de.fuberlin.wiwiss.silk.util.{Identifier, ValidatingXMLReader}

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
                         sources: Traversable[Source],
                         linkSpecs: Traversable[LinkSpecification],
                         outputs: Traversable[Output] = Traversable.empty) {

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
      outputs = outputs ++ config.outputs
    )
  }

  def toXML: Node = {
    <Silk>
      {prefixes.toXML}
      <DataSources>
        {sources.map(_.toXML)}
      </DataSources>
      <Interlinks>
        {linkSpecs.map(_.toXML(prefixes))}
      </Interlinks>
    </Silk>
  }
}

object LinkingConfig {
  private val schemaLocation = "de/fuberlin/wiwiss/silk/LinkSpecificationLanguage.xsd"

  def empty = LinkingConfig(Prefixes.empty, RuntimeConfig(), Nil, Nil, Nil)

  def load = {
    new ValidatingXMLReader(fromXML, schemaLocation)
  }

  def fromXML(node: Node) = {
    implicit val prefixes = Prefixes.fromXML(node \ "Prefixes" head)
    val sources = (node \ "DataSources" \ "DataSource").map(Source.fromXML)
    val blocking = (node \ "Blocking").headOption match {
      case Some(blockingNode) => Blocking.fromXML(blockingNode)
      case None => Blocking()
    }
    val linkSpecifications = (node \ "Interlinks" \ "Interlink").map(p => LinkSpecification.fromXML(p))

    implicit val globalThreshold = None
    val outputs = (node \ "Outputs" \ "Output").map(Output.fromXML)

    LinkingConfig(prefixes, RuntimeConfig(blocking), sources, linkSpecifications, outputs)
  }
}
