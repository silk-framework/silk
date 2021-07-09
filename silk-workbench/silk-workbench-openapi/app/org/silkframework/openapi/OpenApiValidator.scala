package org.silkframework.openapi

import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.core.models.ParseOptions

import java.util
import scala.jdk.CollectionConverters.asScalaBufferConverter

object OpenApiValidator {

  def validate(url: String): ValidationResult = {
    val parser = new OpenAPIV3Parser()
    val parseOptions = new ParseOptions()
    val result = parser.readLocation(url, new util.ArrayList(), parseOptions)
    ValidationResult(result.getMessages.asScala)
  }

}
