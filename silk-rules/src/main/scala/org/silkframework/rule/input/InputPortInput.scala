package org.silkframework.rule.input

import org.silkframework.entity.Entity
import org.silkframework.rule.{Operator, TaskContext}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Identifier

import scala.xml.Node

/**
 * An input port placeholder inside a rule block definition.
 * The runtime value is provided by the surrounding rule block invocation.
 */
case class InputPortInput(id: Identifier = Operator.generateId,
                          portId: Identifier) extends Input {

  override def children: Seq[Operator] = Seq.empty

  override def withId(newId: Identifier): Operator = copy(id = newId)

  override def withChildren(newChildren: Seq[Operator]): InputPortInput = {
    if(newChildren.isEmpty) {
      this
    } else {
      throw new IllegalArgumentException("InputPortInput cannot have any children")
    }
  }

  override def execution(taskContext: TaskContext): InputExecution = {
    new InputPortExecution(this, None)
  }
}

/**
 * Runtime executor for an [[InputPortInput]].
 * If no binding exists for the port, it evaluates to an empty value.
 */
final class InputPortExecution(override val operator: InputPortInput,
                               val bindingExecution: Option[InputExecution]) extends InputExecution {

  override def apply(entity: Entity): Value = {
    bindingExecution.map(_(entity)).getOrElse(Value(Seq.empty))
  }
}

object InputPortInput {

  implicit object InputPortInputFormat extends XmlFormat[InputPortInput] {
    override def tagNames: Set[String] = Set("InputPortInput")

    override def read(node: Node)(implicit readContext: ReadContext): InputPortInput = {
      val id = Operator.readId(node)
      val portId = (node \ "@portId").headOption.map(_.text).filter(_.nonEmpty).map(Identifier.apply).getOrElse {
        throw new ValidationException("InputPortInput requires a non-empty 'portId' attribute.", id, "InputPortInput")
      }
      InputPortInput(id, portId)
    }

    override def write(value: InputPortInput)(implicit writeContext: WriteContext[Node]): Node = {
      <InputPortInput id={value.id.toString} portId={value.portId.toString}/>
    }
  }
}
