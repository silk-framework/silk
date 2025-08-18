package org.silkframework.rule.plugins.transformer.normalize

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.annotations.Plugin

import java.net.{MalformedURLException, URI, URISyntaxException, URL, URLDecoder, URLEncoder}

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
    description = "Non-absolute URIs are prefixed with the configured URI prefix.",
    input1 = Array("ab"),
    output = Array("urn:url-encoded-value:ab")
  ),
  new TransformExample(
    description = "URI reserved characters are encoded.",
    input1 = Array("a&b"),
    output = Array("urn:url-encoded-value:a%26b")
  ),
  new TransformExample(
    description = "Valid absolute URIs are forwarded unchanged.",
    input1 = Array("http://example.org/some/path"),
    output = Array("http://example.org/some/path")
  ),
  new TransformExample(
    description = "Query parameters and fragments are left unchanged.",
    input1 = Array("http://example.org/path?query=some+stuff#hashtag"),
    output = Array("http://example.org/path?query=some+stuff#hashtag")
  ),
  new TransformExample(
    description = "Valid URNs are forwarded unchanged.",
    input1 = Array("urn:valid:uri"),
    output = Array("urn:valid:uri")
  ),
  new TransformExample(
    description = "Special characters are encoded.",
    input1 = Array("http://www.broken domain.com/broken weird path äöü/nice/path/andNowSomeFragment#fragmentäöü"),
    output = Array("http://www.broken%20domain.com/broken%20weird%20path%20%C3%A4%C3%B6%C3%BC/nice/path/andNowSomeFragment#fragment%C3%A4%C3%B6%C3%BC")
  ),
  new TransformExample(
    description = "Hash signs are only encoded if they don't denote a fragment.",
    input1 = Array("http://domain/##path#"),
    output = Array("http://domain/#%23path%23")
  ),
  new TransformExample(
    description = "Invalid URIs are fully encoded.",
    input1 = Array("http : invalid URI"),
    output = Array("urn:url-encoded-value:http+%3A+invalid+URI")
  ),
  new TransformExample(
    description = "Leading and trailing spaces are removed.",
    input1 = Array("  http://domain.com/[squareBrackets] "),
    output = Array("http://domain.com/%5BsquareBrackets%5D")
  ),
  new TransformExample(
    input1 = Array("100%"),
    output = Array("urn:url-encoded-value:100%25")
  ),
))
case class UriFixTransformer(uriPrefix: String = "urn:url-encoded-value:") extends SimpleTransformer {

  // Allowed scheme according to RFC 2396:
  // scheme = alpha *( alpha | digit | "+" | "-" | "." )
  private val schemeRegex = "\\w[\\w\\d+.-]*"

  // Regex to match URI-like strings that can be fixed
  private val lenientUriRegex = s"($schemeRegex):([^#]+)(#.*)?".r

  override def evaluate(value: String): String = {
    val trimmedValue = value.trim
    try {
      trimmedValue match {
        case lenientUriRegex(scheme, schemeSpecificPart, fragment) =>
          if (fragment != null) {
            new URI(scheme, schemeSpecificPart, fragment.substring(1)).toASCIIString
          } else {
            new URI(scheme, schemeSpecificPart, null).toASCIIString
          }
        case _ =>
          convertToValidUri(trimmedValue)
      }
    } catch {
      case _: URISyntaxException =>
        convertToValidUri(trimmedValue)
    }
  }

  private def convertToValidUri(possiblyInvalidUri: String): String = {
    try {
      // Convert the String and decode the URL into the URL class
      val url = new URL(URLDecoder.decode(possiblyInvalidUri, "UTF-8"))
      val uri = new URI(url.getProtocol, url.getUserInfo, url.getHost, url.getPort, url.getPath, url.getQuery, url.getRef)
      uri.toString
    } catch {
      case _: URISyntaxException | _: MalformedURLException | _: IllegalArgumentException =>
        generateUri(possiblyInvalidUri)
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
