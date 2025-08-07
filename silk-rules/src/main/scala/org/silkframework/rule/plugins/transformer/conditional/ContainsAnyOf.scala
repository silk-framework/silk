package org.silkframework.rule.plugins.transformer.conditional

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.annotations.Plugin

@Plugin(
  id = "containsAnyOf",
  label = "Contains any of",
  categories = Array("Conditional"),
  description = "Accepts two inputs. If the first input contains any of the second input values it returns 'true', else 'false' is returned."
)
@TransformExamples(Array(
  new TransformExample(
    input1 = Array("A", "B", "C"),
    input2 = Array("A", "B"),
    output = Array("true")
  ),
  new TransformExample(
    input1 = Array("A", "B", "C"),
    input2 = Array("A", "D"),
    output = Array("true")
  ),
  new TransformExample(
    input1 = Array("A", "B", "C"),
    input2 = Array("D"),
    output = Array("false")
  ),
  new TransformExample(
    input1 = Array("A", "B", "C"),
    input2 = Array("A", "B", "C"),
    output = Array("true")
  ),
  new TransformExample(
    input1 = Array("A", "B", "C"),
    input2 = Array(),
    throwsException = classOf[java.lang.IllegalArgumentException]
  ),
  new TransformExample(
    input1 = Array("A"),
    input2 = Array("A"),
    input3 = Array("A"),
    throwsException = classOf[java.lang.IllegalArgumentException]
  ),
  new TransformExample(
    input1 = Array("A"),
    throwsException = classOf[java.lang.IllegalArgumentException]
  )
))
case class ContainsAnyOf() extends Transformer {
  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    require(values.size == 2, "The containsAnyOf transformation accepts exactly two inputs")
    val input1 = values.head
    val input2 = values(1)
    if(input2.isEmpty) {
      throw new IllegalArgumentException("No values for second input found. There must at least be one value to check if " +
          "it is contained in the first input!")
    }
    val boolResult: Boolean = input2.exists(input1.contains)
    Seq(boolResult.toString)
  }
}
