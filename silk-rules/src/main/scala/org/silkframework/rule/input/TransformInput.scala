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
import org.silkframework.execution.ExecutionException
import org.silkframework.rule.Operator
import org.silkframework.runtime.plugin.PluginBackwardCompatibility
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat, XmlSerialization}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Identifier

import scala.util.control.NonFatal
import scala.xml.Node

/**
  * A TransformInput applies a transform to input values.
  *
  * @param id The id of this transform input
  * @param transformer The transformer used to transform values.
  * @param inputs The children operators from which input values are read.
 */
case class TransformInput(id: Identifier = Operator.generateId, transformer: Transformer, inputs: IndexedSeq[Input] = IndexedSeq.empty) extends Input {

  def apply(entity: Entity): Value = {
    val inputValues = new Array[Seq[String]](inputs.length)
    var errors = Seq[OperatorEvaluationError]()

    // Evaluate input operators
    for(i <- inputs.indices) {
      val result = inputs(i)(entity)
      inputValues(i) = result.values
      for(error <- result.errors) {
        errors +:= error
      }
    }

    // Evaluate transform
    try {
      Value(transformer(inputValues), errors)
    } catch {
      case ex: ExecutionException if ex.abortExecution =>
        throw ex
      case NonFatal(ex) =>
        errors +:= OperatorEvaluationError(id, ex)
        Value(Seq.empty, errors)
    }
  }

  override def children: Seq[Input] = inputs

  override def withChildren(newChildren: Seq[Operator]): TransformInput = {
    copy(inputs = newChildren.map(_.asInstanceOf[Input]).toIndexedSeq)
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
        TransformInput(id, transformer, inputs.toIndexedSeq)
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