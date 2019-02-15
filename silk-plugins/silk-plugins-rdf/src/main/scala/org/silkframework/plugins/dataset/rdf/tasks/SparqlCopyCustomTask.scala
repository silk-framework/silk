package org.silkframework.plugins.dataset.rdf.tasks

import org.apache.jena.query.QueryFactory
import org.silkframework.config.CustomTask
import org.silkframework.dataset.rdf.SparqlEndpointEntitySchema
import org.silkframework.entity.EntitySchema
import org.silkframework.execution.local.QuadEntityTable
import org.silkframework.runtime.plugin.{MultilineStringParameter, Param, Plugin}
import org.silkframework.runtime.validation.ValidationException

import scala.util.{Failure, Success, Try}

@Plugin(
  id = "sparqlCopyOperator",
  label = "SPARQL Copy Task",
  description = "A task that executes a SPARQL Construct query on a SPARQL enabled data source and outputs the SPARQL result."
)
case class SparqlCopyCustomTask(
    @Param(label = "Construct query", value = "A SPARQL 1.1 construct query", example = "construct { ?s ?p ?o } where { ?s ?p ?o }")
      query: MultilineStringParameter,
    @Param(label = "Use temporary file", value = "When copying directly to the same SPARQL Endpoint or when copying large amounts of triples, set to True by default")
      tempFile: Boolean = true
  ) extends CustomTask {

  private def validateQuery(): Unit = {
    val parsedQuery = Try(QueryFactory.create(query.str)) match {
      case Success(q) => q
      case Failure(ex) => throw new ValidationException("The given query could not be parsed. Details:\n" + ex.getMessage, ex)
    }
    if(!parsedQuery.isConstructType) {
      throw new ValidationException("Entered query is not a valid SPARQL CONSTRUCT query!")
    }
  }
  validateQuery()

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
