package org.silkframework.runtime.templating

/**
  * Allows to read a set of template variables.
  */
trait TemplateVariablesReader {

  /**
    * The available variable scopes.
    */
  def scopes: Set[String]

  /**
    * Retrieves all template variables.
    */
  def all: TemplateVariables

  /**
    * Retrieves a template variable by name.
    */
  def get(name: String): TemplateVariable = {
    all.map(name)
  }

}