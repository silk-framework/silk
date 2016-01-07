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

package org.silkframework.rule.input

import org.silkframework.entity.{Entity, Path}
import org.silkframework.config.Prefixes
import org.silkframework.rule.Operator
import org.silkframework.runtime.resource.{ResourceManager, ResourceLoader}
import org.silkframework.runtime.serialization.{XmlFormat, ValidationException}
import scala.xml.Node
import org.silkframework.util.{Identifier, DPair}

/**
 * A PathInput retrieves values from a data item by a given RDF path and optionally applies a transform to them.
 */
case class PathInput(id: Identifier = Operator.generateId, path: Path) extends Input {

  @volatile private var cachedPathIndex = -1

  /**
    * Returns an empty sequence as a path input does not have any children.
    */
  override def children = Seq.empty

  /**
    * As a path input does not have any children, an [IllegalArgumentException] will be thrown if the provided children sequence is nonempty.
    */
  override def withChildren(newChildren: Seq[Operator]) = {
    if(newChildren.isEmpty)
      this
    else
      throw new IllegalArgumentException("PathInput cannot have any children")
  }

  /**
   * Retrieves the values of this input for a given entity.
   *
   * @param entities The pair of entities.
   * @return The values.
   */
  override def apply(entities: DPair[Entity]): Seq[String] = {
    if (entities.source.desc.variable == path.variable)
      eval(entities.source)
    else if (entities.target.desc.variable == path.variable)
      eval(entities.target)
    else
      Seq.empty
  }

  private def eval(entity: Entity) = {
    if(path.operators.isEmpty)
      Seq(entity.uri)
    else {
      var index = cachedPathIndex
      if(index == -1 || index >= entity.desc.paths.size || entity.desc.paths(index) != path) {
        index = entity.desc.pathIndex(path)
        cachedPathIndex = index
      }
      entity.evaluate(index)
    }
  }
}

object PathInput {

  /**
   * XML serialization format.
   */
  implicit object PathInputFormat extends XmlFormat[PathInput] {

    def read(node: Node)(implicit prefixes: Prefixes, resources: ResourceManager): PathInput = {
      val id = Operator.readId(node)
      try {
        val pathStr = (node \ "@path").text
        val path = Path.parse(pathStr)
        PathInput(id, path)
      } catch {
        case ex: Exception => throw new ValidationException(ex.getMessage, id, "Path")
      }
    }

    def write(value: PathInput)(implicit prefixes: Prefixes): Node = {
      <Input id={value.id} path={value.path.serialize}/>
    }
  }
}
