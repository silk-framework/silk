package org.silkframework.plugins.dataset.json

import org.silkframework.runtime.validation.ValidationException

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
    * Parses templates of the form "prefix{{entities}}suffix"
    */
  def parse(templateStr: String): JsonTemplate = {
    if(!templateStr.contains("{{entities}}")) {
      throw new ValidationException("Template must contain {{entities}}.")
    }
    val parts = templateStr.split("""\{\{entities\}\}""")
    parts.length match {
      case 0 => JsonTemplate("", "")
      case 1 => JsonTemplate(parts(0), "")
      case 2 => JsonTemplate(parts(0), parts(1))
      case _ => throw new ValidationException("Template must contain {{entities}} exactly once.")
    }
  }

}