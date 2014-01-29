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

package de.fuberlin.wiwiss.silk.datasource

import xml.Node
import de.fuberlin.wiwiss.silk.util.{Identifier, ValidatingXMLReader}
import de.fuberlin.wiwiss.silk.entity.{Path, SparqlRestriction, EntityDescription}
import de.fuberlin.wiwiss.silk.runtime.resource.ResourceLoader

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
  def retrieve(entityDesc: EntityDescription, entities: Seq[String] = Seq.empty) = {
    dataSource.retrieve(entityDesc, entities)
  }

  /**
   * Retrieves the most frequent paths in this source.
   * Implementations are only required to work on a best effort basis i.e. it does not necessarily return all paths in the source.
   * The default implementation returns an empty traversable.
   *
   * @param restriction Only retrieve path on entities which satisfy the given restriction. If not given, all paths are retrieved.
   * @param depth Only retrieve paths up to a certain length. If not given, only paths of length 1 are returned.
   * @param limit Restricts the number of paths to be retrieved. If not given, all found paths are returned.
   *
   * @return A Traversable of the found paths and their frequency.
   */
  def retrievePaths(restriction: SparqlRestriction = SparqlRestriction.empty, depth: Int = 1, limit: Option[Int] = None) = {
    dataSource.retrievePaths(restriction, depth, limit)
  }

  /**
   * Retrieve the most frequent types in the source.
   * Implementations are only required to work on a best effort basis i.e. it does not necessarily return any types.
   * The default implementation returns an empty traversable.
   *
   * @param limit Restricts the number of types to be retrieved. If not given, all found types are returned.
   *
   */
  def retrieveTypes(limit: Option[Int] = None): Traversable[(String, Double)] = {
    dataSource.retrieveTypes(limit)
  }

  def toXML: Node = dataSource match {
    case DataSource(dataSourceType, params) =>
      <DataSource id={id} type={dataSourceType}>
        {params.map {
        case (name, value) => <Param name={name} value={value}/>
      }}
      </DataSource>
  }
}

object Source {
  private val schemaLocation = "de/fuberlin/wiwiss/silk/LinkSpecificationLanguage.xsd"

  def load(resourceLoader: ResourceLoader) = {
    new ValidatingXMLReader(node => fromXML(node, resourceLoader), schemaLocation)
  }

  def fromXML(node: Node, resourceLoader: ResourceLoader): Source = {
    new Source(
      id = node \ "@id" text,
      dataSource = DataSource(node \ "@type" text, readParams(node), resourceLoader)
    )
  }

  private def readParams(element: Node): Map[String, String] = {
    element \ "Param" map (p => (p \ "@name" text, p \ "@value" text)) toMap
  }
}