package org.silkframework.rule.plugins.transformer.conditional

import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.annotations.{Plugin, TransformExample, TransformExamples}

@Plugin(
  id = "containsAllOf",
  label = "Contains all of",
  categories = Array("Conditional"),
  description = "Accepts two inputs. If the first input contains all of the second input values it returns 'true', else 'false' is returned."
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
    output = Array("false")
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
    throwsException = "java.lang.IllegalArgumentException"
  ),
  new TransformExample(
    input1 = Array("A"),
    input2 = Array("A"),
    input3 = Array("A"),
    throwsException = "java.lang.IllegalArgumentException"
  ),
  new TransformExample(
    input1 = Array("A"),
    throwsException = "java.lang.IllegalArgumentException"
  )
))
case class ContainsAllOf() extends Transformer {
  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    require(values.size == 2, "The containsAllOf transformation accepts two inputs")
    val input1 = values.head
    val input2 = values(1)
    if(input2.isEmpty) {
      throw new IllegalArgumentException("No values for second input found. There must at least be one value to check if " +
          "it is contained in the first input!")
    }
    val boolResult: Boolean = input2.forall(input1.contains)
    Seq(boolResult.toString)
  }
}
