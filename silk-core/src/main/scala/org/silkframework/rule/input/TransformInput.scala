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

import org.silkframework.config.Prefixes
import org.silkframework.entity.Entity
import org.silkframework.rule.Operator
import org.silkframework.rule.similarity.SimilarityOperator
import org.silkframework.runtime.resource.{ResourceManager, ResourceLoader}
import org.silkframework.runtime.serialization.{Serialization, ValidationException, XmlFormat}
import org.silkframework.util.{DPair, Identifier}

import scala.xml.Node

/**
  * A TransformInput applies a transform to input values.
  *
  * @param id The id of this transform input
  * @param transformer The transformer used to transform values.
  * @param inputs The children operators from which input values are read.
 */
case class TransformInput(id: Identifier = Operator.generateId, transformer: Transformer, inputs: Seq[Input] = Seq.empty) extends Input {

  def apply(entities: DPair[Entity]): Seq[String] = {
    val values = for (input <- inputs) yield input(entities)

    transformer(values)
  }

  override def children = inputs

  override def withChildren(newChildren: Seq[Operator]) = {
    copy(inputs = newChildren.map(_.asInstanceOf[Input]))
  }

  override def toString = transformer match {
    case Transformer(name, params) => "Transformer(type=" + name + ", params=" + params + ", inputs=" + inputs + ")"
  }
}

object TransformInput {

  /**
   * XML serialization format.
   */
  implicit object TransformInputFormat extends XmlFormat[TransformInput] {

    import Serialization._

    def read(node: Node)(implicit prefixes: Prefixes, resources: ResourceManager): TransformInput = {
      val id = Operator.readId(node)
      val inputs = node.child.filter(n => n.label == "Input" || n.label == "TransformInput").map(fromXml[Input])

      try {
        val transformer = Transformer((node \ "@function").text, Operator.readParams(node), resources)
        TransformInput(id, transformer, inputs.toList)
      } catch {
        case ex: Exception => throw new ValidationException(ex.getMessage, id, "Tranformation")
      }
    }

    def write(value: TransformInput)(implicit prefixes: Prefixes): Node = {
      value.transformer match {
        case Transformer(plugin, params) =>
          <TransformInput id={value.id} function={plugin.id}>
            { value.inputs.map(toXml[Input]) }
            { params.map { case (name, v) => <Param name={name} value={v}/>  } }
          </TransformInput>
      }
    }
  }
}
