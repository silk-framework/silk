package de.fuberlin.wiwiss.silk.impl.transformer

import org.scalatest.matchers.{ShouldMatchers}
import org.scalatest.FlatSpec
import de.fuberlin.wiwiss.silk.linkspec.input.Transformer
import de.fuberlin.wiwiss.silk.impl.DefaultImplementations

class RemoveBlanksTransformerTest extends FlatSpec with ShouldMatchers
{
    DefaultImplementations.register()

    val transformer = Transformer("removeBlanks", Map())

    "RemoveBlanksTransformer" should "return 'abc'" in
    {
        transformer.evaluate(List("a b c")) should equal ("abc")
    }
}