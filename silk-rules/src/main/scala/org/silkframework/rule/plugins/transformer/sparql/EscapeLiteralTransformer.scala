package org.silkframework.rule.plugins.transformer.sparql

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.annotations.Plugin

@Plugin(
  id = "escape_literal",
  categories = Array("SPARQL"),
  label = "Escape SPARQL plain literal",
  description = "Escapes a value so it can be safely used inside a SPARQL short-form string literal. " +
    "Escapes backslashes, quotes, newlines, carriage returns and tabs. " +
    "The returned value does not include enclosing quotation marks."
)
@TransformExamples(Array(
  new TransformExample(
    input1 = Array("simple value"),
    output = Array("simple value")
  ),
  new TransformExample(
    input1 = Array("with \"quotes\""),
    output = Array("with \\\"quotes\\\"")
  ),
  new TransformExample(
    input1 = Array("back\\slash"),
    output = Array("back\\\\slash")
  ),
  new TransformExample(
    input1 = Array("line1\nline2"),
    output = Array("line1\\nline2")
  )
))
case class EscapeLiteralTransformer() extends SimpleTransformer {

  override def evaluate(value: String): String = {
    val sb = new StringBuilder(value.length)
    var i = 0
    while (i < value.length) {
      value.charAt(i) match {
        case '\\' => sb.append("\\\\")
        case '"'  => sb.append("\\\"")
        case '\'' => sb.append("\\'")
        case '\n' => sb.append("\\n")
        case '\r' => sb.append("\\r")
        case '\t' => sb.append("\\t")
        case c    => sb.append(c)
      }
      i += 1
    }
    sb.toString
  }
}