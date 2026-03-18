package org.silkframework.plugins.dataset.rdf.tasks

import org.silkframework.config._
import org.silkframework.entity._
import org.silkframework.execution.typed.SparqlUpdateEntitySchema
import org.silkframework.plugins.dataset.rdf.tasks.templating._
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.runtime.plugin.types.SparqlCodeParameter
import org.silkframework.runtime.templating.{TemplateEngineAutocompletionProvider, TemplateEngines}

@Plugin(
  id = "sparqlUpdateOperator",
  label = "SPARQL Update query",
  description =
    "A task that outputs SPARQL Update queries for every entity from the input based on a SPARQL Update template." +
      " The output of this operator should be connected to the SPARQL datasets to which the results should be written.",
  documentationFile = "SparqlUpdateCustomTask.md",
  iconFile = "sparql-update-query.svg"
)
case class SparqlUpdateCustomTask(
  @Param(
    label = "SPARQL update query",
    value = "The SPARQL UPDATE template for constructing SPARQL UPDATE queries for every entity from the input." +
      " The possible values for the template engine are `Simple`, `Velocity Engine` and `Jinja`." +
      " See the general documentation of this plugin for further details on the features of each template engine.",
    example = "DELETE DATA { ${<PROP_FROM_ENTITY_SCHEMA1>} rdf:label ${\"PROP_FROM_ENTITY_SCHEMA2\"} }"
  )
  sparqlUpdateTemplate: SparqlCodeParameter,
  @Param(label = "Batch size", value = "How many entities should be handled in a single update request.")
  batchSize: Int = SparqlUpdateCustomTask.defaultBatchSize,
  @Param(
    value = "The templating mode for the template engine. See the general documentation of this plugin for further details on the features of each template engine.",
    autoCompletionProvider = classOf[TemplateEngineAutocompletionProvider]
  )
  templatingMode: String = SparqlSimpleTemplateEngine.id
) extends CustomTask {
  assert(batchSize >= 1, "Batch size must be greater zero!")

  val compiledTemplate: SparqlTemplate = {
    val templateEngine = TemplateEngines.create(templatingMode)
    new SparqlTemplate(templateEngine.compile(sparqlUpdateTemplate.str))
  }

  compiledTemplate.validate(batchSize)

  def isStaticTemplate: Boolean = compiledTemplate.isStaticTemplate

  def expectedInputSchema: EntitySchema = compiledTemplate.inputSchema

  /**
    * Generates The SPARQL Update query based on the placeholder assignments.
    * @param placeholderAssignments For each placeholder in the query template
    * @return
    */
  def generate(placeholderAssignments: Map[String, String], taskProperties: TaskProperties): String = {
    compiledTemplate.generate(placeholderAssignments, taskProperties)
  }

  override def inputPorts: InputPorts = {
    if(isStaticTemplate) {
      InputPorts.NoInputPorts
    } else {
      FixedNumberOfInputs(Seq(FixedSchemaPort(expectedInputSchema)))
    }
  }

  override def outputPort: Option[Port] = Some(FixedSchemaPort(SparqlUpdateEntitySchema.schema))
}

object SparqlUpdateCustomTask {
  final val defaultBatchSize = 1
}
