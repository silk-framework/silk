package org.silkframework.rule.plugins.transformer.normalize

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import java.net.URLEncoder

import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}

@Plugin(
  id = "urlEncode",
  categories = Array("Normalize"),
  label = "Encode URL",
  description = "URL encodes the string."
)
@TransformExamples(Array(
  new TransformExample(
    input1 = Array("ab"),
    output = Array("ab")
  ),
  new TransformExample(
    input1 = Array("a&b"),
    output = Array("a%26b")
  ),
  new TransformExample(
    input1 = Array("http://example.org/some/path"),
    output = Array("http%3A%2F%2Fexample.org%2Fsome%2Fpath")
  )
))
case class UrlEncodeTransformer(
  @Param(value = "The character encoding.", advanced = true)
  encoding: String = "UTF-8") extends SimpleTransformer {

  override def evaluate(value: String): String = {
    URLEncoder.encode(value, encoding)
  }
}
