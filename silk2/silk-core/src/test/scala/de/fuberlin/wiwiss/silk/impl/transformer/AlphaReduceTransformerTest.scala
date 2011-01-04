package de.fuberlin.wiwiss.silk.linkspec.transformer

import org.scalatest.matchers.{ShouldMatchers}
import org.scalatest.FlatSpec
import de.fuberlin.wiwiss.silk.linkspec.input.Transformer
import de.fuberlin.wiwiss.silk.impl.DefaultImplementations

class AlphaReduceTransformerTest extends FlatSpec with ShouldMatchers
{
    DefaultImplementations.register()

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