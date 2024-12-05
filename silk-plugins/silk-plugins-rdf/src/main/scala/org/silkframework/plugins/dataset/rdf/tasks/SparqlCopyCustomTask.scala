package org.silkframework.plugins.dataset.rdf.tasks

import org.apache.jena.query.QueryFactory
import org.silkframework.config._
import org.silkframework.execution.typed.{QuadEntitySchema, SparqlEndpointEntitySchema}
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.runtime.plugin.types.SparqlCodeParameter
import org.silkframework.runtime.validation.ValidationException

import scala.util.{Failure, Success, Try}

@Plugin(
  id = "sparqlCopyOperator",
  label = "SPARQL Construct query",
  description = "A task that executes a SPARQL Construct query on a SPARQL enabled data source and outputs the SPARQL result. " +
    "If the result should be written to the same RDF store it is read from, the SPARQL Update operator is preferable."
)
case class SparqlCopyCustomTask(
    @Param(label = "Construct query", value = "A SPARQL 1.1 construct query", example = "construct { ?s ?p ?o } where { ?s ?p ?o }")
      query: SparqlCodeParameter,
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

  override def inputPorts: InputPorts = {
    FixedNumberOfInputs(Seq(FixedSchemaPort(SparqlEndpointEntitySchema.schema)))
  }

  override def outputPort: Option[Port] = {
    Some(FixedSchemaPort(QuadEntitySchema.schema))
  }
}
