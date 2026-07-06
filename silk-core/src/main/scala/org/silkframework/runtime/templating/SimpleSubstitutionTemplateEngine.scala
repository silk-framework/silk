package org.silkframework.runtime.templating

import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.runtime.templating.exceptions.UnboundVariablesException

import java.io.Writer

@Plugin(
  id = SimpleSubstitutionTemplateEngine.id,
  label = "Simple substitution",
  description = "Internal engine that substitutes plain '{{scope.name}}' variable references. Does not support any other expressions, filters or control structures. Mainly intended for tests; not offered for selection in the UI."
)
case class SimpleSubstitutionTemplateEngine() extends TemplateEngine {

  override def compile(templateString: String): CompiledTemplate = {
    new SimpleSubstitutionTemplate(templateString)
  }
}

object SimpleSubstitutionTemplateEngine {
  final val id = "simpleSubstitution"
}

/**
  * A compiled template consisting of literal parts and plain variable references.
  */
class SimpleSubstitutionTemplate(templateString: String) extends CompiledTemplate {

  import SimpleSubstitutionTemplate._

  /** The template disassembled into literal parts and variable references. */
  private val parts: Seq[TemplatePart] = {
    val builder = Seq.newBuilder[TemplatePart]
    var currentIndex = 0
    for (m <- variablePattern.findAllMatchIn(templateString)) {
      if (m.start > currentIndex) {
        builder += LiteralPart(templateString.substring(currentIndex, m.start))
      }
      builder += VariablePart(TemplateVariableName.parse(m.group(1)))
      currentIndex = m.end
    }
    if (currentIndex < templateString.length) {
      builder += LiteralPart(templateString.substring(currentIndex))
    }
    builder.result()
  }

  override val variables: Option[Seq[TemplateVariableName]] = {
    Some(parts.collect { case VariablePart(name) => name }.distinct)
  }

  override def evaluate(values: Seq[TemplateVariableValue], writer: Writer, evaluationConfig: EvaluationConfig): Unit = {
    val valueMap = values.map(value => value.scopedName -> value).toMap
    val missingVars = variables.get.filterNot(variable => valueMap.contains(variable.scopedName))
    if (missingVars.nonEmpty && !evaluationConfig.ignoreUnboundVariables) {
      throw new UnboundVariablesException(missingVars)
    }
    for (part <- parts) {
      part match {
        case LiteralPart(literal) =>
          writer.write(literal)
        case VariablePart(name) =>
          valueMap.get(name.scopedName) match {
            case Some(value) => writer.write(value.values.mkString(""))
            case None => writer.write(name.scopedName) // Unbound variables evaluate to their name (see EvaluationConfig)
          }
      }
    }
  }

  override def evaluate(values: Map[String, AnyRef], writer: Writer): Unit = {
    for (part <- parts) {
      part match {
        case LiteralPart(literal) =>
          writer.write(literal)
        case VariablePart(name) =>
          writer.write(resolveValue(name, values))
      }
    }
  }

  /** Resolves a variable reference within (possibly nested) value maps. */
  private def resolveValue(name: TemplateVariableName, values: Map[String, AnyRef]): String = {
    def unbound() = throw new UnboundVariablesException(Seq(name))
    val path = name.scope :+ name.name
    var current: AnyRef = values.getOrElse(path.head, unbound())
    for (segment <- path.tail) {
      current = current match {
        case map: java.util.Map[_, _] =>
          Option(map.asInstanceOf[java.util.Map[String, AnyRef]].get(segment)).getOrElse(unbound())
        case map: Map[_, _] =>
          map.asInstanceOf[Map[String, AnyRef]].getOrElse(segment, unbound())
        case _ =>
          unbound()
      }
    }
    current.toString // Strings stay as-is; IterableTemplateValues concatenates its values
  }
}

object SimpleSubstitutionTemplate {

  private val variablePattern = """\{\{\s*([^{}\s]+)\s*\}\}""".r

  private sealed trait TemplatePart
  private case class LiteralPart(literal: String) extends TemplatePart
  private case class VariablePart(name: TemplateVariableName) extends TemplatePart
}
