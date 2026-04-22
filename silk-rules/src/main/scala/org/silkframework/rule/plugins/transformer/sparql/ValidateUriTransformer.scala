package org.silkframework.rule.plugins.transformer.sparql

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.runtime.validation.ValidationException

import java.net.URI
import scala.util.Try

@Plugin(
  id = "validate_uri",
  categories = Array("Validation", "SPARQL"),
  label = "Validate URI",
  description = "Validates that the input is a valid absolute IRI and returns it unchanged. " +
    "Throws a validation error if the input is not a valid IRI. "
)
@TransformExamples(Array(
  new TransformExample(
    input1 = Array("http://example.org/entity1"),
    output = Array("http://example.org/entity1")
  ),
  new TransformExample(
    input1 = Array("urn:example:1"),
    output = Array("urn:example:1")
  ),
  new TransformExample(
    input1 = Array("not a uri"),
    throwsException = classOf[ValidationException]
  ),
  new TransformExample(
    input1 = Array(""),
    throwsException = classOf[ValidationException]
  )
))
case class ValidateUriTransformer() extends SimpleTransformer {

  override def evaluate(value: String): String = {
    Try(new URI(value)) match {
      case scala.util.Success(uri) if uri.isAbsolute => value
      case _ =>
        throw new ValidationException(s"Value is not a valid absolute IRI: '$value'")
    }
  }
}