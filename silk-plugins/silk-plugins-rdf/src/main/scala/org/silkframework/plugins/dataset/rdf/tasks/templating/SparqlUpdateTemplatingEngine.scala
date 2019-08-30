package org.silkframework.plugins.dataset.rdf.tasks.templating

import org.silkframework.entity.EntitySchema

/**
  * Trait that every SPARQL Update templating engine must implement.
  */
trait SparqlUpdateTemplatingEngine {
  /**
    * Renders the template based on the variable assignments.
    */
  def generate(placeholderAssignments: Map[String, String], taskProperties: TaskProperties): String

  /** Validates the template */
  def validate(): Unit

  /** The input entity schema that is expected by the template. */
  def inputSchema: EntitySchema
}

/** Makes properties of the input and output task of a SPARQL Update operator execution available. */
case class TaskProperties(inputTask: Map[String, String], outputTask: Map[String, String])