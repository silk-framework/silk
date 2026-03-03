package org.silkframework.plugins.dataset.rdf.tasks.templating

import org.silkframework.entity.EntitySchema
import org.silkframework.runtime.templating.CompiledTemplate

/**
  * Extension of CompiledTemplate with SPARQL Update specific capabilities.
  */
trait SparqlCompiledTemplate extends CompiledTemplate {

  /** Renders the template based on the variable assignments. */
  def generate(placeholderAssignments: Map[String, String], taskProperties: TaskProperties): String

  /** Validates the template, including batch validation if batchSize > 1. */
  def validate(batchSize: Int): Unit

  /** The input entity schema that is expected by the template. */
  def inputSchema: EntitySchema

  /** True if the given template is static, i.e. contains no placeholder variables. */
  def isStaticTemplate: Boolean
}

/** Makes properties of the input and output task of a SPARQL Update operator execution available. */
case class TaskProperties(inputTask: Map[String, String], outputTask: Map[String, String])
