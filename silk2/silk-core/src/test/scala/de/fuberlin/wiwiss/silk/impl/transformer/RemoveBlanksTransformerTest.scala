package de.fuberlin.wiwiss.silk.impl.transformer

import org.scalatest.matchers.{ShouldMatchers}
import org.scalatest.FlatSpec
import de.fuberlin.wiwiss.silk.impl.DefaultImplementations

class RemoveBlanksTransformerTest extends FlatSpec with ShouldMatchers
{
  DefaultImplementations.register()

  val transformer = new RemoveBlanksTransformer()

  "RemoveBlanksTransformer" should "return 'abc'" in
  {
    transformer.evaluate("a b c") should equal ("abc")
  }
}