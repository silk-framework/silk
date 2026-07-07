package org.silkframework.runtime.templating

case class InMemoryTemplateVariablesReader(override val all: TemplateVariables, override val scopes: Set[Seq[String]]) extends TemplateVariablesReader
