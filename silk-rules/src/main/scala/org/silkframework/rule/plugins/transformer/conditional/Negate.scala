package org.silkframework.rule.plugins.transformer.conditional

import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.annotations.{Plugin, TransformExample, TransformExamples}

@Plugin(
  id = "negate",
  label = "negate binary (NOT)",
  categories = Array("Conditional"),
  description = "Accepts one input, which is either 'true', '1' or 'false', '0' and negates it."
)
@TransformExamples(Array(
  new TransformExample(
    input1 = Array("0", "1", "false", "true", "False", "True"),
    output = Array("1", "0", "true", "false", "true", "false")
  ),
  new TransformExample(
    input1 = Array("falsee", "true"),
    throwsException = "java.lang.IllegalArgumentException"
  ),
  new TransformExample(
    input1 = Array(),
    throwsException = "java.lang.IllegalArgumentException"
  )
))
case class Negate() extends Transformer {

  def negate(value: String): String ={
    value.trim.toLowerCase match{
      case "0" => "1"
      case "1" => "0"
      case "true" => "false"
      case "false" => "true"
      case _ => throw new IllegalArgumentException("Unrecognized value for a boolean: " + value + " , only '0', '1', 'true' or 'false' are allowed")
    }
  }

  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    require(values.size == 1, "The negate transformation accepts only one input")
    val input = values.head
    if(input.isEmpty) {
      throw new IllegalArgumentException("No values for input in the negate transformer.")
    }
    input.map(negate)
  }
}
