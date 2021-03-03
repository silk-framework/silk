package org.silkframework.plugins.dataset.json

import org.silkframework.runtime.validation.ValidationException

import scala.util.matching.Regex

/**
  * Simple template to specify an envelope around generated JSON objects.
  */
case class JsonTemplate(prefix: String, suffix: String)

object JsonTemplate {

  /**
    * Default template for writing objects into a JSON array.
    */
  val default: JsonTemplate = JsonTemplate("[", "]")

  /**
    * The placeholder variable for the writen entities.
    */
  val placeholder: String = "{{entities}}"

  /**
    * Parses templates of the form "prefix[[placeholder]]suffix"
    */
  def parse(templateStr: String): JsonTemplate = {
    if(!templateStr.contains(placeholder)) {
      throw new ValidationException(s"Template must contain $placeholder.")
    }
    val parts = templateStr.split(Regex.quote(placeholder))
    parts.length match {
      case 0 => JsonTemplate("", "")
      case 1 => JsonTemplate(parts(0), "")
      case 2 => JsonTemplate(parts(0), parts(1))
      case _ => throw new ValidationException(s"Template must contain $placeholder exactly once.")
    }
  }

}