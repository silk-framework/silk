package org.silkframework.rule.plugins.transformer.normalize

import java.net.{URI, URISyntaxException, URLEncoder}

import org.silkframework.rule.input.{SimpleTransformer}
import org.silkframework.runtime.plugin.{Param, Plugin, TransformExample, TransformExamples}

@Plugin(
  id = "urlEncode",
  categories = Array("Normalize"),
  label = "URL Encode",
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
    parameters = Array("onlyIfNeeded", "false"),
    input1 = Array("http://example.org/some/path"),
    output = Array("http%3A%2F%2Fexample.org%2Fsome%2Fpath")
  ),
  new TransformExample(
    parameters = Array("onlyIfNeeded", "true"),
    input1 = Array("http://example.org/some/path"),
    output = Array("http://example.org/some/path")
  ),
  new TransformExample(
    parameters = Array("onlyIfNeeded", "true"),
    input1 = Array("a%26b"),
    output = Array("a%26b")
  )
))
case class UrlEncodeTransformer(
  @Param("The character encoding.")
  encoding: String = "UTF-8",
  @Param("If true, only encodes values that are no valid URIs yet.")
  onlyIfNeeded: Boolean = false) extends SimpleTransformer {

  override def evaluate(value: String) = {
    if(onlyIfNeeded && isValid(value)) {
      value
    } else {
      URLEncoder.encode(value, encoding)
    }
  }

  private def isValid(value: String): Boolean = {
    try {
      new URI(value)
      true
    } catch {
      case _: URISyntaxException =>
        false
    }
  }
}
