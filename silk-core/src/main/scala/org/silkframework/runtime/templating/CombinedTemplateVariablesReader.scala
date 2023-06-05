package org.silkframework.runtime.templating

case class CombinedTemplateVariablesReader(readers: Seq[TemplateVariablesReader]) extends TemplateVariablesReader {
  require(readers.nonEmpty, "Need to provide at least one reader")

  /**
    * The available variable scopes.
    */
  override def scopes: Set[String] = {
    readers.flatMap(_.scopes).toSet
  }

  /**
    * Retrieves all template variables.
    */
  override def all: TemplateVariables = {
    readers.map(_.all).reduce(_ merge _)
  }
}
