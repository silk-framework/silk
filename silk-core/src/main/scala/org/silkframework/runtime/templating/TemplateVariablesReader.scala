package org.silkframework.runtime.templating

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.validation.NotFoundException

/**
  * Allows to read a set of template variables.
  */
trait TemplateVariablesReader {

  /**
    * The available variable scopes. Each scope is represented as a sequence of strings forming a prefix path,
    * e.g., Seq("project") or Seq("project", "metaData").
    */
  def scopes: Set[Seq[String]]

  /**
    * Retrieves all template variables.
    */
  def all: TemplateVariables

  /**
    * Retrieves a template variable by name.
    *
    * @throws NotFoundException If no variable with the given name has been found.
    */
  def get(name: String): TemplateVariable = {
    all.map.get(name) match {
      case Some(v) =>
        v
      case None =>
        throw new NotFoundException(s"No variable '$name' has been found.")
    }
  }

}