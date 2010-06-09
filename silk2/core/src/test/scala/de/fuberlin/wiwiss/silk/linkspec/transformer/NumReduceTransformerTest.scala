package de.fuberlin.wiwiss.silk.linkspec.transformer

import org.scalatest.matchers.{ShouldMatchers}
import org.scalatest.FlatSpec
import de.fuberlin.wiwiss.silk.linkspec.Transformer

class NumReduceTransformerTest extends FlatSpec with ShouldMatchers
{
    val transformer = Transformer("numReduce", Map())

    "NumReduceTransformer" should "return '10'" in
    {
        transformer.evaluate(List("a1b0c")) should equal ("10")
    }
}