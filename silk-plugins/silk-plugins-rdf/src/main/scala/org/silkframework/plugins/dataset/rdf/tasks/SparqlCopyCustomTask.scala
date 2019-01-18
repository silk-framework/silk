package org.silkframework.plugins.dataset.rdf.tasks

import org.silkframework.config.CustomTask
import org.silkframework.dataset.rdf.SparqlEndpointEntitySchema
import org.silkframework.entity.EntitySchema
import org.silkframework.execution.local.QuadEntityTable
import org.silkframework.runtime.plugin.{MultilineStringParameter, Param, Plugin}

@Plugin(
  id = "sparqlCopyOperator",
  label = "SPARQL Copy Task",
  description = "A task that executes a SPARQL Construct query on a SPARQL enabled data source and outputs the SPARQL result."
)
case class SparqlCopyCustomTask(
    @Param(label = "Construct query", value = "A SPARQL 1.1 construct query", example = "construct { ?s ?p ?o } where { ?s ?p ?o }")
      selectQuery: MultilineStringParameter,
    @Param(label = "Use temporary file", value = "When copying directly to the same SPARQL Endpoint or when copying large amounts of triples, set to True by default")
      tempFile: Boolean = true
  ) extends CustomTask {

  /**
    * The schemata of the input data for this task.
    * A separate entity schema is returned for each input.
    * Or None is returned, which means that this task can handle any number of inputs and any kind
    * of entity schema.
    * A result of Some(Seq()) on the other hand means that this task has no inputs at all.
    */
  override def inputSchemataOpt: Option[Seq[EntitySchema]] = Some(Seq(SparqlEndpointEntitySchema.schema))

  /**
    * The schema of the output data.
    * Returns None, if the schema is unknown or if no output is written by this task.
    */
  override def outputSchemaOpt: Option[EntitySchema] = Some(QuadEntityTable.schema)
}
