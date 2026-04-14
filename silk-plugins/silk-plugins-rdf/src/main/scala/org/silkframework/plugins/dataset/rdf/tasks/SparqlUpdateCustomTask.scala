package org.silkframework.plugins.dataset.rdf.tasks

import org.silkframework.config._
import org.silkframework.entity._
import org.silkframework.execution.typed.SparqlUpdateEntitySchema
import org.silkframework.plugins.dataset.rdf.tasks.templating._
import org.silkframework.plugins.dataset.rdf.datasets.SparqlDataset
import org.silkframework.runtime.plugin.annotations.{Param, Plugin, PluginReference}

import org.silkframework.runtime.plugin.types.SparqlCodeParameter

@Plugin(
  id = SparqlUpdateCustomTask.pluginId,
  label = "SPARQL Update query",
  description =
    "A task that outputs SPARQL Update queries for every entity from the input based on a SPARQL Update template." +
      " The output of this operator should be connected to the SPARQL datasets to which the results should be written.",
  documentationFile = "SparqlUpdateCustomTask.md",
  iconFile = "sparql-update-query.svg",
  relatedPlugins = Array(
    new PluginReference(
      id = SparqlDataset.pluginId,
      description = "A SPARQL endpoint dataset in the workflow receives the update statements this plugin generates and executes them against the remote store."
    ),
    new PluginReference(
      id = SparqlSelectCustomTask.pluginId,
      description = "This plugin turns entity input into SPARQL Update statements that modify a store. The SPARQL Select query plugin reads from the same kind of store by executing a SELECT query and outputting the results as an entity table."
    )
  )
)
case class SparqlUpdateCustomTask(
  @Param(
    label = "SPARQL update query",
    value = "The SPARQL UPDATE template for constructing SPARQL UPDATE queries for every entity from the input." +
      " The possible values for the template engine are `Simple` and `Velocity Engine`." +
      " See the general documentation of this plugin for further details on the features of each template engine.",
    example = "DELETE DATA { ${<PROP_FROM_ENTITY_SCHEMA1>} rdf:label ${\"PROP_FROM_ENTITY_SCHEMA2\"} }"
  )
  sparqlUpdateTemplate: SparqlCodeParameter,
  @Param(label = "Batch size", value = "How many entities should be handled in a single update request.")
  batchSize: Int = SparqlUpdateCustomTask.defaultBatchSize,
  @Param(
    "The templating mode for the template engine. The possible values are `Simple` and `Velocity Engine`." +
      " See the general documentation of this plugin for further details on the features of each template engine.",
  )
  templatingMode: SparqlUpdateTemplatingMode = SparqlUpdateTemplatingMode.simple
) extends CustomTask {
  assert(batchSize >= 1, "Batch size must be greater zero!")

  val templatingEngine: SparqlUpdateTemplatingEngine = templatingMode match {
    case SparqlUpdateTemplatingMode.simple => SparqlUpdateTemplatingEngineSimple(sparqlUpdateTemplate.str, batchSize)
    case SparqlUpdateTemplatingMode.velocity => SparqlTemplatingEngineVelocity(sparqlUpdateTemplate.str, batchSize)
  }

  templatingEngine.validate()

  def isStaticTemplate: Boolean = templatingEngine.isStaticTemplate

  def expectedInputSchema: EntitySchema = templatingEngine.inputSchema

  /**
    * Generates The SPARQL Update query based on the placeholder assignments.
    * @param placeholderAssignments For each placeholder in the query template
    * @return
    */
  def generate(placeholderAssignments: Map[String, String], taskProperties: TaskProperties): String = {
    templatingEngine.generate(placeholderAssignments, taskProperties)
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
  final val pluginId = "sparqlUpdateOperator"
}