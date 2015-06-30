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

package de.fuberlin.wiwiss.silk.linkagerule.input

import de.fuberlin.wiwiss.silk.entity.{Entity, Path}
import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.linkagerule.Operator
import scala.xml.Node
import de.fuberlin.wiwiss.silk.util.{ValidationException, Identifier, DPair}

/**
 * A PathInput retrieves values from a data item by a given RDF path and optionally applies a transform to them.
 */
case class PathInput(id: Identifier = Operator.generateId, path: Path) extends Input {
  @volatile private var cachedPathIndex = -1

  /**
   * Retrieves the values of this input for a given entity.
   *
   * @param entities The pair of entities.
   * @return The values.
   */
  override def apply(entities: DPair[Entity]): Set[String] = {
    if (entities.source.desc.variable == path.variable)
      eval(entities.source)
    else if (entities.target.desc.variable == path.variable)
      eval(entities.target)
    else
      Set.empty
  }

  private def eval(entity: Entity) = {
    if(path.operators.isEmpty)
      Set(entity.uri)
    else {
      var index = cachedPathIndex
      if(index == -1 || entity.desc.paths(index) != path) {
        index = entity.desc.pathIndex(path)
        cachedPathIndex = index
      }
      entity.evaluate(index)
    }
  }

  override def toXML(implicit prefixes: Prefixes) = <Input id={id} path={path.serialize}/>
}

object PathInput {
  def fromXML(node: Node)(implicit prefixes: Prefixes) = {
    val id = Operator.readId(node)

    println("NODE: " + (node \ "@path").text)

    try {
      val pathStr = (node \ "@path").text
      val path = Path.parse(pathStr)
      PathInput(id, path)
    } catch {
      case ex: Exception => throw new ValidationException(ex.getMessage, id, "Path")
    }
  }
}
