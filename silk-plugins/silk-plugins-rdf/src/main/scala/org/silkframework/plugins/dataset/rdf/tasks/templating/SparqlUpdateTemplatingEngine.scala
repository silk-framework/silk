package org.silkframework.plugins.dataset.rdf.tasks.templating

import org.silkframework.entity.EntitySchema

/**
  * Trait that every SPARQL Update templating engine must implement.
  */
trait SparqlUpdateTemplatingEngine {
  /**
    * Renders the template based on the variable assignments.
    */
  def generate(placeholderAssignments: Map[String, String]): String

  /** Validates the template */
  def validate(): Unit

  /** The input entity schema that is expected by the template. */
  def inputSchema: EntitySchema
}
