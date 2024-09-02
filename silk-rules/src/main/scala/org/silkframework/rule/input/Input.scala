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

import org.silkframework.entity.paths.UntypedPath
import org.silkframework.entity.Entity
import org.silkframework.rule.{Operator, TaskContext}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat, XmlSerialization}

import scala.xml.Node

/**
 * An input that retrieves a set of values.
 */
trait Input extends Operator {

  /**
   * Retrieves the values of this input for a given entity.
   *
   * @param entity The entity from which the values should be read.
   * @return The values.
   */
  def apply(entity: Entity): Value

  override def withContext(taskContext: TaskContext): Input = this
}

object Input {

  /**
   * XML serialization format.
   */
  implicit object InputFormat extends XmlFormat[Input] {

    import XmlSerialization._

    def read(node: Node)(implicit readContext: ReadContext): Input = {
      node match {
        case node @ <Input/> => fromXml[PathInput](node)
        case node @ <TransformInput>{_*}</TransformInput> => fromXml[TransformInput](node)
      }
    }

    def write(value: Input)(implicit writeContext: WriteContext[Node]): Node = {
      value match {
        case path: PathInput => toXml(path)
        case transform: TransformInput => toXml(transform)
      }
    }
  }

  def rewriteSourcePaths(input: Input, rewriteFn: UntypedPath => UntypedPath): Input = {
    input match {
      case TransformInput(id, transformer, inputs) =>
        TransformInput(id, transformer, inputs.map(rewriteSourcePaths(_, rewriteFn)))
      case PathInput(id, path) =>
        PathInput(id, rewriteFn(path.asUntypedPath))
    }
  }
}