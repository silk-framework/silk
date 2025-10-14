package org.silkframework.rule.plugins.transformer.selection

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.annotations.Plugin

/**
  * This transformer takes 3 inputs.
  * The first input should have exactly one value that should be passed out again untouched.
  * The second input has at least two Regex values - two in order to make sense.
  * The third input should have exactly one value which is checked against the regexes.
  *
  * The result of the transformer is a sequence with the same length of number of regexes.
  * For the output value (of the first input) is set to each position in this sequence where
  * the related regex also matched.
  *
  * Example:
  *
  * For inputs
  * {{{
  *   Seq(
  *     Seq("output"),
  *     Seq("a", "b", "c"),
  *     Seq("catch")
  *   )
  * }}}
  *
  * the transformer will return
  * {{{
  *   Seq("output", "", "output")
  * }}}
  */
@Plugin(
  id = "regexSelect",
  categories = Array("Selection"),
  label = "Regex selection",
  description = """This transformer takes 3 inputs: one output value, multiple regex patterns, and a value to check against those patterns. It returns the output value at positions where regex patterns match the input value.""",
  documentationFile = "RegexSelectTransformer.md"
)
@TransformExamples(Array(
  new TransformExample(
    description = "sets the correct outputs based on the regexes",
    parameters = Array("oneOnly", "false"),
    input1 = Array("output"),
    input2 = Array("a", "b", "c"),
    input3 = Array("catch"),
    output = Array("output", "", "output")
  ),
  new TransformExample(
    description = "return only first match position if oneOnly = true",
    parameters = Array("oneOnly", "true"),
    input1 = Array("output"),
    input2 = Array("a", "b", "c"),
    input3 = Array("catch"),
    output = Array("output", "", "")
  ),
))
case class RegexSelectTransformer(oneOnly: Boolean = false) extends Transformer {
  override def apply(inputs: Seq[Seq[String]]): Seq[String] = {
    require(inputs.size == 3, "The ")
    require(inputs.head.nonEmpty, "The first input needs to have at least one value!")
    require(inputs(2).nonEmpty, "The 3. input needs to have at least one value!")
    val outputValue = inputs.head.head
    val valueToCheck = inputs(2).head
    var oneMatched = false
    def tryMore: Boolean = !oneOnly || !oneMatched
    val outputs = inputs(1) map { regex =>
      if(tryMore && regex.r.findFirstMatchIn(valueToCheck).isDefined) {
        oneMatched = true
        outputValue
      } else {
        ""
      }
    }
    outputs
  }
}
