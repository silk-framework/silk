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

package de.fuberlin.wiwiss.silk.datasource

import de.fuberlin.wiwiss.silk.entity.EntityDescription
import xml.Node
import de.fuberlin.wiwiss.silk.util.{Identifier, ValidatingXMLReader}

/**
 * A source of entities.
 */
case class Source(id: Identifier, dataSource: DataSource) {
  /**
   * Retrieves entities from this source which satisfy a specific entity description.
   *
   * @param entityDesc The entity description
   * @param entities The URIs of the entities to be retrieved. If empty, all entities will be retrieved.
   *
   * @return A Traversable over the entities. The evaluation of the Traversable may be non-strict.
   */
  def retrieve(entityDesc: EntityDescription, entities: Seq[String] = Seq.empty) = dataSource.retrieve(entityDesc, entities)

  def toXML: Node = dataSource match {
    case DataSource(dataSourceType, params) => {
      <DataSource id={id} type={dataSourceType}>
        {params.map {
        case (name, value) => <Param name={name} value={value}/>
      }}
      </DataSource>
    }
  }
}

object Source {
  private val schemaLocation = "de/fuberlin/wiwiss/silk/LinkSpecificationLanguage.xsd"

  def load = {
    new ValidatingXMLReader(node => fromXML(node), schemaLocation)
  }

  def fromXML(node: Node): Source = {
    new Source(node \ "@id" text, DataSource(node \ "@type" text, readParams(node)))
  }

  private def readParams(element: Node): Map[String, String] = {
    element \ "Param" map (p => (p \ "@name" text, p \ "@value" text)) toMap
  }
}