package org.silkframework.rule.plugins.transformer.sparql

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.annotations.Plugin

import scala.util.matching.Regex

@Plugin(
  id = "escape_multiline_literal",
  categories = Array("SPARQL"),
  label = "Escape SPARQL multiline literal",
  description = "Escapes a value so it can be safely used inside a SPARQL triple-quoted string literal " +
    "(`\"\"\"...\"\"\"` or `'''...'''`). Escapes backslashes and breaks any run of three or more " +
    "consecutive single or double quotes. Individual quotes and newlines are preserved. " +
    "The returned value does not include enclosing quotation marks."
)
@TransformExamples(Array(
  new TransformExample(
    input1 = Array("simple\nvalue"),
    output = Array("simple\nvalue")
  ),
  new TransformExample(
    input1 = Array("with \"quote\""),
    output = Array("with \"quote\"")
  ),
  new TransformExample(
    input1 = Array("back\\slash"),
    output = Array("back\\\\slash")
  ),
  new TransformExample(
    input1 = Array("triple \"\"\" quotes"),
    output = Array("triple \\\"\\\"\\\" quotes")
  ),
  new TransformExample(
    input1 = Array("triple ''' quotes"),
    output = Array("triple \\'\\'\\' quotes")
  )
))
case class EscapeMultilineLiteralTransformer() extends SimpleTransformer {

  override def evaluate(value: String): String = {
    val withBackslashes = value.replace("\\", "\\\\")
    val noTripleDq = EscapeMultilineLiteralTransformer.dqRun3.replaceAllIn(withBackslashes,
      m => Regex.quoteReplacement("\\\"" * m.matched.length))
    val noTripleSq = EscapeMultilineLiteralTransformer.sqRun3.replaceAllIn(noTripleDq,
      m => Regex.quoteReplacement("\\'" * m.matched.length))
    noTripleSq
  }
}

object EscapeMultilineLiteralTransformer {
  private val dqRun3: Regex = "\"{3,}".r
  private val sqRun3: Regex = "'{3,}".r
}