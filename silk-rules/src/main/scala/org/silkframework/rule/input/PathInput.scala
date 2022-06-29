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

import org.silkframework.entity._
import org.silkframework.entity.paths.{Path, TypedPath, UntypedPath}
import org.silkframework.rule.Operator
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Identifier

import scala.xml.Node

/**
 * A PathInput retrieves values from a data item by a given RDF path and optionally applies a transform to them.
 */
case class PathInput(id: Identifier = Operator.generateId, path: Path) extends Input {

  @volatile private var cachedPathIndex = -1

  /**
    * Returns an empty sequence as a path input does not have any children.
    */
  override def children: Seq[Operator] = Seq.empty

  /**
    * As a path input does not have any children, an [IllegalArgumentException] will be thrown if the provided children sequence is nonempty.
    */
  override def withChildren(newChildren: Seq[Operator]): PathInput = {
    if(newChildren.isEmpty)
      this
    else
      throw new IllegalArgumentException("PathInput cannot have any children")
  }

  /**
   * Retrieves the values of this input for a given entity.
   *
   * @param entity The entity.
   * @return The values.
   */
  override def apply(entity: Entity): Seq[String] = {
    eval(entity)
  }

  private def eval(entity: Entity): Seq[String] = {
    if(path.operators.isEmpty) {
      Array(entity.uri.toString)
    } else {
      var index = cachedPathIndex
      if(index < 0 || index >= entity.schema.typedPaths.size || entity.schema.typedPaths(index).normalizedSerialization != path.normalizedSerialization) {
        index = path match {
          case u: UntypedPath => entity.schema.indexOfPath(u)
          case t: TypedPath => entity.schema.indexOfTypedPath(t)
        }
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

    def read(node: Node)(implicit readContext: ReadContext): PathInput = {
      val id = Operator.readId(node)
      try {
        val pathStr = (node \ "@path").text
        val path = UntypedPath.parse(pathStr)(readContext.prefixes)
        PathInput(id, path)
      } catch {
        case ex: Exception => throw new ValidationException(ex.getMessage, id, "Path")
      }
    }

    def write(value: PathInput)(implicit writeContext: WriteContext[Node]): Node = {
      <Input id={value.id} path={value.path.normalizedSerialization}/>
    }
  }
}
