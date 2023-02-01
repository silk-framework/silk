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

import org.silkframework.entity.Entity
import org.silkframework.rule.Operator
import org.silkframework.runtime.plugin.PluginBackwardCompatibility
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat, XmlSerialization}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Identifier

import scala.xml.Node

/**
  * A TransformInput applies a transform to input values.
  *
  * @param id The id of this transform input
  * @param transformer The transformer used to transform values.
  * @param inputs The children operators from which input values are read.
 */
case class TransformInput(id: Identifier = Operator.generateId, transformer: Transformer, inputs: Seq[Input] = Seq.empty) extends Input {

  def apply(entity: Entity): Seq[String] = {
    val values = for (input <- inputs) yield input(entity)

    transformer(values)
  }

  override def children = inputs

  override def withChildren(newChildren: Seq[Operator]) = {
    copy(inputs = newChildren.map(_.asInstanceOf[Input]))
  }

  override def toString = transformer match {
    case Transformer(name, params, _) => "Transformer(type=" + name + ", params=" + params + ", inputs=" + inputs + ")"
  }
}

object TransformInput {

  /**
   * XML serialization format.
   */
  implicit object TransformInputFormat extends XmlFormat[TransformInput] {

    import XmlSerialization._

    def read(node: Node)(implicit readContext: ReadContext): TransformInput = {
      val id = Operator.readId(node)
      val inputs = node.child.filter(n => n.label == "Input" || n.label == "TransformInput").map(fromXml[Input])

      try {
        val transformerPluginId = (node \ "@function").text
        val transformer = Transformer(PluginBackwardCompatibility.transformerIdMapping.getOrElse(transformerPluginId, transformerPluginId), Operator.readParams(node))
        TransformInput(id, transformer, inputs.toList)
      } catch {
        case ex: Exception => throw new ValidationException(ex.getMessage, id, "Transformation")
      }
    }

    def write(value: TransformInput)(implicit writeContext: WriteContext[Node]): Node = {
      val plugin = value.transformer.pluginSpec
      val params = value.transformer.parameters

      <TransformInput id={value.id} function={plugin.id}>
        { value.inputs.map(toXml[Input]) }
        {XmlSerialization.serializeParameters(params)}
      </TransformInput>
    }
  }
}