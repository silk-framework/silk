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

package de.fuberlin.wiwiss.silk.entity.rdf

import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.entity.{EntitySchema, Path}
import de.fuberlin.wiwiss.silk.runtime.resource.{ResourceManager, ResourceLoader}
import de.fuberlin.wiwiss.silk.runtime.serialization.XmlFormat

import scala.xml.Node

case class SparqlEntitySchema(variable: String = "a", restrictions: SparqlRestriction = SparqlRestriction.empty, paths: IndexedSeq[Path]) {
  require(paths.forall(_.operators.nonEmpty), "Entity description must not contain an empty path")

  /**
   * Retrieves the index of a given path.
   */
  def pathIndex(path: Path) = {
    var index = 0
    while(path != paths(index)) {
      index += 1
      if(index >= paths.size)
        throw new NoSuchElementException(s"Path $path not found on entity. Available paths: ${paths.mkString(", ")}.")
    }
    index
  }
}

object SparqlEntitySchema {

  /**
   * Creates an empty entity description.
   */
  def empty = SparqlEntitySchema("a", SparqlRestriction.empty, IndexedSeq.empty)

  def fromSchema(entitySchema: EntitySchema) = {
    val sparqlRestriction = new SparqlRestrictionBuilder("a")(Prefixes.empty).apply(entitySchema.filter)
    val typeRestriction = SparqlRestriction.fromSparql("a", s"?a a <${entitySchema.typ}>")
    SparqlEntitySchema("a", sparqlRestriction merge typeRestriction, entitySchema.paths)
  }

  /**
   * XML serialization format.
   */
  implicit object EntityDescriptionFormat extends XmlFormat[SparqlEntitySchema] {
    /**
     * Deserialize an EntityDescription from XML.
     */
    def read(node: Node)(implicit prefixes: Prefixes, resources: ResourceManager) = {
      val variable = (node \ "Variable").text.trim
      new SparqlEntitySchema(
        variable = variable,
        restrictions = SparqlRestriction.fromSparql(variable, (node \ "Restrictions").text),
        paths = for (pathNode <- (node \ "Paths" \ "Path").toIndexedSeq) yield Path.parse(pathNode.text.trim)
      )
    }

    /**
     * Serialize an EntityDescription to XML.
     */
    def write(desc: SparqlEntitySchema)(implicit prefixes: Prefixes): Node =
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
