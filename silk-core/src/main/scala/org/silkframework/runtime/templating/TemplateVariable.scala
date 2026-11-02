package org.silkframework.runtime.templating

import org.silkframework.runtime.plugin.StringParameterType.PasswordParameterType
import org.silkframework.runtime.plugin.types.PasswordParameter
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import org.silkframework.runtime.validation.BadUserInputException

import java.util.logging.{Level, Logger}
import scala.util.control.NonFatal
import scala.xml.{Node, PCData}

/**
  * A single template variable.
  *
  * @param name        The local name of the variable.
  * @param value       The value of the variable.
  * @param template    Optional template expression to compute the value dynamically.
  * @param description Optional description for documentation.
  * @param isSensitive True if the variable value should not be exposed to users.
  * @param scope       The scope of the variable. May be empty.
  *                    For example, a variable with name "label" and scope "project.metaData"
  *                    is addressed as "project.metaData.label".
  */
case class TemplateVariable(override val name: String,
                            value: String,
                            template: Option[String] = None,
                            description: Option[String] = None,
                            isSensitive: Boolean = false,
                            override val scope: VariableScope = VariableScope.empty) extends TemplateVariableValue(name, scope, values = Seq(value)) {

  validate()

  /**
    * True if this sensitive variable holds its value or template in the encrypted form it is stored in.
    * After reading, this is only the case for one that could not be decrypted, see [[TemplateVariable.fromStorage]].
    */
  def hasEncryptedValue: Boolean = {
    isSensitive && (value +: template.toSeq).exists(_.startsWith(PasswordParameterType.PREAMBLE))
  }

  /** This variable as it is stored: the value and template of a sensitive variable are encrypted like a password parameter. */
  def forStorage: TemplateVariable = {
    if (isSensitive) copy(value = TemplateVariable.encrypted(value), template = template.map(TemplateVariable.encrypted)) else this
  }

  private def validate(): Unit = {
    // The emptiness check must come first, else name.head throws an index exception instead of the message below
    if(name.isEmpty || !isAllowedChar(name.head, firstChar = true) || !name.tail.forall(isAllowedChar(_, firstChar = false))) {
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

  private val log: Logger = Logger.getLogger(getClass.getName)

  /**
    * Reads a variable as it is stored, see [[TemplateVariable.forStorage]].
    * The value and template of a sensitive variable are decrypted; plain ones, stored before they were encrypted, are read as
    * they are. One that cannot be decrypted, e.g., after the key has been changed, is kept encrypted, so that it is not lost.
    */
  def fromStorage(variable: TemplateVariable): TemplateVariable = {
    if (variable.hasEncryptedValue) {
      variable.copy(value = decrypted(variable, variable.value), template = variable.template.map(decrypted(variable, _)))
    } else {
      variable
    }
  }

  private def encrypted(str: String): String = {
    if (str.startsWith(PasswordParameterType.PREAMBLE)) str else PasswordParameter.encrypt(str).toString
  }

  private def decrypted(variable: TemplateVariable, str: String): String = {
    if (str.startsWith(PasswordParameterType.PREAMBLE)) {
      try {
        PasswordParameter.decrypt(str.stripPrefix(PasswordParameterType.PREAMBLE))
      } catch {
        case NonFatal(ex) =>
          log.log(Level.WARNING, s"The sensitive variable '${variable.scopedName}' cannot be decrypted with the key configured at " +
            s"'${PasswordParameterType.CONFIG_KEY}'. It is kept encrypted until it is entered again.", ex)
          str
      }
    } else {
      str
    }
  }

  /**
    * XML serialization format.
    */
  implicit object TemplateVariableFormat extends XmlFormat[TemplateVariable] {

    val tagName = "Variable"

    override def tagNames: Set[String] = Set(tagName)

    override def read(value: Node)(implicit readContext: ReadContext): TemplateVariable = fromStorage {
      TemplateVariable(
        name = (value \ "@name").text,
        value =(value \ "Value").text,
        template = Option((value \ "Template").text).filter(_.trim.nonEmpty),
        description = Option((value \ "Description").text).filter(_.trim.nonEmpty),
        isSensitive = (value \ "@isSensitive").text.toBoolean,
        scope = VariableScope.parse((value \ "@scope").text),
      )
    }

    override def write(value: TemplateVariable)(implicit writeContext: WriteContext[Node]): Node = {
      val stored = value.forStorage
      <Variable name={value.name}
                isSensitive={value.isSensitive.toString}
                scope={value.scope.toString}>
        <Value xml:space="preserve">{PCData(stored.value)}</Value>
        { stored.template.toSeq.map(template => <Template xml:space="preserve">{PCData(template)}</Template>) }
        <Description xml:space="preserve">{value.description.getOrElse("")}</Description>
      </Variable>
    }
  }

}
