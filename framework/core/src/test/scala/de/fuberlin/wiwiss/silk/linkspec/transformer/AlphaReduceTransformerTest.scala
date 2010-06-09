package de.fuberlin.wiwiss.silk.linkspec.transformer

import org.scalatest.matchers.{ShouldMatchers}
import de.fuberlin.wiwiss.silk.linkspec.Transformer
import org.scalatest.FlatSpec

class AlphaReduceTransformerTest extends FlatSpec with ShouldMatchers
{
    val transformer = Transformer("alphaReduce", Map())

    "AlphaReduceTransformer" should "return 'abc'" in
    {
        transformer.evaluate(List("a1b0c")) should equal ("abc")
    }

    "AlphaReduceTransformer" should "return 'def'" in
    {
        transformer.evaluate(List("-def-")) should equal ("def")
    }
}