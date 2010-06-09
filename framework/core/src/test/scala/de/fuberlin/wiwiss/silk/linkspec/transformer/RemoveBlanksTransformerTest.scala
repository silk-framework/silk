package de.fuberlin.wiwiss.silk.linkspec.transformer

import org.scalatest.matchers.{ShouldMatchers}
import org.scalatest.FlatSpec
import de.fuberlin.wiwiss.silk.linkspec.Transformer

class RemoveBlanksTransformerTest extends FlatSpec with ShouldMatchers
{
    val transformer = Transformer("removeBlanks", Map())

    "RemoveBlanksTransformer" should "return 'abc'" in
    {
        transformer.evaluate(List("a b c")) should equal ("abc")
    }
}