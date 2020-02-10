package org.silkframework.rule.plugins.transformer.normalize

import java.net.{URI, URLEncoder}

import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.annotations.{Plugin, TransformExample, TransformExamples}

/**
  * Fixes URIs if necessary
  */
@Plugin(
  id = "uriFix",
  categories = Array("Normalize"),
  label = "URI Fixer",
  description = "Generates valid absolute URIs from the given values. Already valid absolute URIs are left untouched."
)
@TransformExamples(Array(
  new TransformExample(
    input1 = Array("ab"),
    output = Array("urn:url-encoded-value:ab")
  ),
  new TransformExample(
    input1 = Array("a&b"),
    output = Array("urn:url-encoded-value:a%26b")
  ),
  new TransformExample(
    input1 = Array("http://example.org/some/path"),
    output = Array("http://example.org/some/path")
  ),
  new TransformExample(
    input1 = Array("urn:valid:uri"),
    output = Array("urn:valid:uri")
  ),
  new TransformExample(
    input1 = Array("http://www.broken domain.com/broken weird path äöü/nice/path/andNowSomeFragment#fragmentäöü"),
    output = Array("http://www.broken%20domain.com/broken%20weird%20path%20%C3%A4%C3%B6%C3%BC/nice/path/andNowSomeFragment#fragment%C3%A4%C3%B6%C3%BC")
  ),
  new TransformExample(
    input1 = Array("http://domain/##path#"),
    output = Array("http://domain/%23%23path#")
  )
))
case class UriFixTransformer(uriPrefix: String = "urn:url-encoded-value:") extends SimpleTransformer {
  override def evaluate(value: String): String = {
    value.indexOf(':') match {
      case -1 =>
        // Cannot be an absolute URI, generate encoded value URI
        uriPrefix + URLEncoder.encode(value, "UTF-8")
      case colonIdx: Int =>
        val (scheme, rest) = value.splitAt(colonIdx)
        val uri = rest.drop(1).lastIndexOf('#') match {
          case -1 =>
            new URI(scheme, rest.drop(1), null)
          case fragmentIdx: Int =>
            val (path, fragment) = rest.drop(1).splitAt(fragmentIdx)
            new URI(scheme, path, fragment.drop(1))
        }
        uri.toASCIIString
    }
  }
}
