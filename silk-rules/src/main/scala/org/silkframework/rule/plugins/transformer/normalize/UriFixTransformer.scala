package org.silkframework.rule.plugins.transformer.normalize

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.annotations.Plugin

import java.net.{URI, URISyntaxException, URLEncoder}

/**
  * Fixes URIs if necessary
  */
@Plugin(
  id = "uriFix",
  categories = Array("Normalize"),
  label = "Fix URI",
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
    input1 = Array("http://example.org/path?query=some+stuff#hashtag"),
    output = Array("http://example.org/path?query=some+stuff#hashtag")
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
    output = Array("http://domain/#%23path%23")
  ),
  new TransformExample(
    input1 = Array("http : invalid URI"),
    output = Array("urn:url-encoded-value:http+%3A+invalid+URI")
  ),
  new TransformExample(
    input1 = Array("http://domain.com/[squareBrackets]"),
    output = Array("urn:url-encoded-value:http%3A%2F%2Fdomain.com%2F%5BsquareBrackets%5D")
  )
))
case class UriFixTransformer(uriPrefix: String = "urn:url-encoded-value:") extends SimpleTransformer {

  // Allowed scheme according to RFC 2396:
  // scheme = alpha *( alpha | digit | "+" | "-" | "." )
  private val schemeRegex = "\\w[\\w\\d+.-]*"

  // Regex to match URI-like strings that can be fixed
  private val lenientUriRegex = s"($schemeRegex):([^#]+)(#.*)?".r

  override def evaluate(value: String): String = {
    try {
      value match {
        case lenientUriRegex(scheme, schemeSpecificPart, fragment) =>
          if (fragment != null) {
            new URI(scheme, schemeSpecificPart, fragment.substring(1)).toASCIIString
          } else {
            new URI(scheme, schemeSpecificPart, null).toASCIIString
          }
        case _ =>
          generateUri(value)
      }
    } catch {
      case _: URISyntaxException =>
        generateUri(value)
    }
  }

  /**
    * Generates a valid URI from an arbitrary string.
    */
  @inline
  private def generateUri(value: String): String = {
    uriPrefix + URLEncoder.encode(value, "UTF-8")
  }
}
