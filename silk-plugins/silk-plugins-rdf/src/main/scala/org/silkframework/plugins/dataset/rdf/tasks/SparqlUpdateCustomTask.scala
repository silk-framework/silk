package org.silkframework.plugins.dataset.rdf.tasks

import org.silkframework.config.CustomTask
import org.silkframework.entity._
import org.silkframework.execution.local.SparqlUpdateEntitySchema
import org.silkframework.plugins.dataset.rdf.tasks.templating._
import org.silkframework.runtime.plugin.{MultilineStringParameter, Param, Plugin}

@Plugin(
  id = "sparqlUpdateOperator",
  label = "SPARQL Update Task",
  description =
"""A task that outputs SPARQL Update queries for every entity from the input based on a SPARQL Update template.
The output of this operator should be connected to the SPARQL datasets to which the results should be written."""
)
case class SparqlUpdateCustomTask(@Param(label = "SPARQL update query", value = SparqlUpdateCustomTask.sparqlUpdateTemplateDescription,
                                         example = "DELETE DATA { ${<PROP_FROM_ENTITY_SCHEMA1>} rdf:label ${\"PROP_FROM_ENTITY_SCHEMA2\"} }")
                                  sparqlUpdateTemplate: MultilineStringParameter,
                                  @Param(label = "Batch size", value = "How many entities should be handled in a single update request.")
                                  batchSize: Int = SparqlUpdateCustomTask.defaultBatchSize,
                                  @Param("The templating mode. 'Simple' only allows simple URI and literal insertions, whereas 'Velocity Engine' supports complex templating." +
                                      " See 'Sparql Update Template' parameter description for examples and http://velocity.apache.org for details on the Velocity templates.")
                                  templatingMode: SparqlUpdateTemplatingMode = SparqlUpdateTemplatingMode.simple) extends CustomTask {
  assert(batchSize >= 1, "Batch size must be greater zero!")

  val templatingEngine: SparqlUpdateTemplatingEngine = templatingMode match {
    case SparqlUpdateTemplatingMode.simple => SparqlUpdateTemplatingEngineSimple(sparqlUpdateTemplate.str, batchSize)
    case SparqlUpdateTemplatingMode.velocity => SparqlTemplatingEngineVelocity(sparqlUpdateTemplate.str, batchSize)
  }

  templatingEngine.validate()

  /**
    * Generates The SPARQL Update query based on the placeholder assignments.
    * @param placeholderAssignments For each placeholder in the query template
    * @return
    */
  def generate(placeholderAssignments: Map[String, String], taskProperties: TaskProperties): String = {
    templatingEngine.generate(placeholderAssignments, taskProperties)
  }

  override def inputSchemataOpt: Option[Seq[EntitySchema]] = {
    Some(Seq(expectedInputSchema))
  }

  def expectedInputSchema: EntitySchema = templatingEngine.inputSchema

  override def outputSchemaOpt: Option[EntitySchema] = Some(SparqlUpdateEntitySchema.schema)

  def isStaticTemplate: Boolean = templatingEngine.isStaticTemplate
}

object SparqlUpdateCustomTask {
  final val sparqlUpdateTemplateDescription =
    """
This operator takes a SPARQL Update Query Template that depending on the templating mode (Simple/Velocity Engine) supports
a set of templating features, e.g. filling in input values via placeholders in the template.

Example for the 'Simple' mode:

  DELETE DATA { ${<PROP_FROM_ENTITY_SCHEMA1>} rdf:label ${"PROP_FROM_ENTITY_SCHEMA2"} }
  INSERT DATA { ${<PROP_FROM_ENTITY_SCHEMA1>} rdf:label ${"PROP_FROM_ENTITY_SCHEMA3"} }
  
  This will insert the URI serialization of the property value PROP_FROM_ENTITY_SCHEMA1 for the ${<PROP_FROM_ENTITY_SCHEMA1>} expression.
  And it will insert a plain literal serialization for the property values PROP_FROM_ENTITY_SCHEMA2/3 for the template literal expressions.

  It is be possible to write something like ${"PROP"}^^<http://someDatatype> or ${"PROP"}@en.

Example for the 'Velocity Engine' mode:

  DELETE DATA { $row.uri("PROP_FROM_ENTITY_SCHEMA1") rdf:label $row.plainLiteral("PROP_FROM_ENTITY_SCHEMA2") }
  #if ( $row.exists("PROP_FROM_ENTITY_SCHEMA1") )
    INSERT DATA { $row.uri("PROP_FROM_ENTITY_SCHEMA1") rdf:label $row.plainLiteral("PROP_FROM_ENTITY_SCHEMA3") }
  #end

  Input values are accessible via various methods of the 'row' variable:

  - uri(inputPath: String): Renders an input value as URI. Throws exception if the value is no valid URI.
  - plainLiteral(inputPath: String): Renders an input value as plain literal, i.e. escapes problematic characters etc.
  - rawUnsafe(inputPath: String): Renders an input value as is, i.e. no escaping is done. This should only be used – better never – if the input values can be trusted.
  - exists(inputPath: String): Returns true if a value for the input path exists, else false.

  The methods uri, plainLiteral and rawUnsafe throw an exception if no input value is available for the given input path.

  In addition to input values, properties of the input and output tasks can be accessed via the inputProperties and outputProperties objects
  in the same way as the row object, e.g.

    $inputProperties.uri("graph")

  For more information about the Velocity Engine visit http://velocity.apache.org.
    """

  final val defaultBatchSize = 1
}