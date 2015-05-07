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

package de.fuberlin.wiwiss.silk.entity

import de.fuberlin.wiwiss.silk.runtime.serialization.XmlFormat
import scala.xml.Node
import de.fuberlin.wiwiss.silk.config.Prefixes

case class EntityDescription(variable: String, restrictions: SparqlRestriction, paths: IndexedSeq[Path]) {
  require(paths.forall(!_.operators.isEmpty), "Entity description must not contain an empty path")

  /**
   * Retrieves the index of a given path.
   */
  def pathIndex(path: Path) = {
    var index = 0
    while(path != paths(index)) {
      index += 1
      if(index >= paths.size)
        throw new NoSuchElementException("Path " + path + " not found on entity.")
    }
    index
  }

  /**
   * Merges two entity descriptions.
   * The variable as well as the restrictions of both entity descriptions must be the same.
   *
   * @param other The entity description that should be merged with this entity description
   * @return The merged entity description
   */
  def merge(other: EntityDescription) = {
    require(variable == other.variable)
    require(restrictions == other.restrictions)

    copy(paths = (paths ++ other.paths).distinct)
  }
}

object EntityDescription {

  /**
   * Creates an empty entity description.
   */
  def empty = EntityDescription("a", SparqlRestriction.empty, IndexedSeq.empty)

  /**
   * XML serialization format.
   */
  implicit object EntityDescriptionFormat extends XmlFormat[EntityDescription] {
    /**
     * Deserialize a value from XML.
     */
    def read(node: Node) = {
      val variable = (node \ "Variable").text.trim
      new EntityDescription(
        variable = variable,
        restrictions = SparqlRestriction.fromSparql(variable, (node \ "Restrictions").text),
        paths = for (pathNode <- (node \ "Paths" \ "Path").toIndexedSeq) yield Path.parse(pathNode.text.trim)
      )
    }

    /**
     * Serialize a value to XML.
     */
    def write(desc: EntityDescription): Node =
      <EntityDescription>
        <Variable>{desc.variable}</Variable>
        <Restrictions>{desc.restrictions.toSparql}</Restrictions>
        <Paths> {
          for (path <- desc.paths) yield {
            <Path>{path.serialize(Prefixes.empty)}</Path>
          }
          }
        </Paths>
      </EntityDescription>
  }
}
