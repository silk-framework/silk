package org.silkframework.rule.plugins.transformer.conditional

import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.annotations.{Plugin, TransformExample, TransformExamples}

@Plugin(
  id = "ifContains",
  label = "if contains",
  categories = Array("Conditional"),
  description = "Accepts two or three inputs. If the first input contains the given value, the second input is forwarded. Otherwise, the third input is forwarded (if present)."
)
@TransformExamples(Array(
  new TransformExample(
    parameters = Array("search", "match"),
    input1 = Array("matching string"),
    input2 = Array("this is a match"),
    output = Array("this is a match")
  ),
  new TransformExample(
    parameters = Array("search", "match"),
    input1 = Array("different string"),
    input2 = Array("this is a match"),
    output = Array()
  ),
  new TransformExample(
    parameters = Array("search", "match"),
    input1 = Array("different string"),
    input2 = Array("this is a match"),
    input3 = Array("this is no match"),
    output = Array("this is no match")
  )
))
case class IfContains(search: String) extends Transformer {
  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    require(values.size >= 2, "The ifContains transformation accepts two or three inputs")
    if(values.head.exists(_.contains(search)))
      values(1)
    else
      if(values.size >= 3) values(2) else Seq.empty
  }
}