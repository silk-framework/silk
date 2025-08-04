package org.silkframework.runtime.templating

import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import org.silkframework.runtime.validation.BadUserInputException

import scala.xml.{Node, PCData}

/**
  * A single template variable.
  */
case class TemplateVariable(override val name: String,
                            value: String,
                            template: Option[String] = None,
                            description: Option[String] = None,
                            isSensitive: Boolean = false,
                            override val scope: String) extends TemplateVariableValue(name, scope, values = Seq(value)) {

  validate()

  private def validate(): Unit = {
    if(!isAllowedChar(name.head, firstChar = true) || !name.tail.forall(isAllowedChar(_, firstChar = false))) {
      throw new BadUserInputException(s"Invalid variable name '$name'. " +
        "Variable names may only consist of uppercase and lowercase letters (A-Z , a-z), digits (0-9), and the underscore character ( _ ). " +
        "In addition, the first character of a variable name cannot be a digit")
    }
  }

  private def isAllowedChar(c: Char, firstChar: Boolean): Boolean = {
    (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (!firstChar && c >= '0' && c <= '9') || (c == '_')
  }

}

object TemplateVariable {

  /**
    * XML serialization format.
    */
  implicit object TemplateVariableFormat extends XmlFormat[TemplateVariable] {

    val tagName = "Variable"

    override def tagNames: Set[String] = Set(tagName)

    override def read(value: Node)(implicit readContext: ReadContext): TemplateVariable = {
      TemplateVariable(
        name = (value \ "@name").text,
        value =(value \ "Value").text,
        template = Option((value \ "Template").text).filter(_.trim.nonEmpty),
        description = Option((value \ "Description").text).filter(_.trim.nonEmpty),
        isSensitive = (value \ "@isSensitive").text.toBoolean,
        scope = (value \ "@scope").text,
      )
    }

    override def write(value: TemplateVariable)(implicit writeContext: WriteContext[Node]): Node = {
      <Variable name={value.name}
                isSensitive={value.isSensitive.toString}
                scope={value.scope}>
        <Value xml:space="preserve">{PCData(value.value)}</Value>
        { value.template.toSeq.map(template => <Template xml:space="preserve">{PCData(template)}</Template>) }
        <Description xml:space="preserve">{value.description.getOrElse("")}</Description>
      </Variable>
    }
  }

}
