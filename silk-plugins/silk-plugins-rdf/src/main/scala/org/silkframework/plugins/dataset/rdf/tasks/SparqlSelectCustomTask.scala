package org.silkframework.plugins.dataset.rdf.tasks

import org.apache.jena.query.QueryFactory
import org.silkframework.config.{CustomTask, FixedNumberOfInputs, FixedSchemaPort, InputPorts, Port}
import org.silkframework.dataset.rdf.SparqlEndpointDatasetParameter
import org.silkframework.entity._
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.execution.typed.SparqlEndpointEntitySchema
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.runtime.plugin.types.SparqlCodeParameter
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Uri

import scala.jdk.CollectionConverters.ListHasAsScala
import scala.util.Try

/**
  * Custom task that executes a SPARQL select query on the input data source and translates the SPARQL result into
  * an Entity table.
  */
@Plugin(
  id = "sparqlSelectOperator",
  label = "SPARQL Select query",
  description =
    "A task that executes a SPARQL Select query on a SPARQL enabled data source and outputs the SPARQL result." +
    " If the SPARQL source is defined on a specific graph, a FROM clause will be added to the query at execution time," +
    " except when there already exists a GRAPH or FROM clause in the query. FROM NAMED clauses are not injected.",
  documentationFile = "SparqlSelectCustomTask.md"
)
case class SparqlSelectCustomTask(
  @Param(label = "Select query", value = "A SPARQL 1.1 select query", example = "select * where { ?s ?p ?o }")
  selectQuery: SparqlCodeParameter,
  @Param(label = "Result limit", value = "If set to a positive integer, the number of results is limited")
  limit: String = "",
  @Param(
    label = "Optional SPARQL dataset",
    value = "An optional SPARQL dataset that can be used for example data, so e.g. the transformation editor shows mapping examples.",
    autoCompletionProvider = classOf[SparqlEndpointDatasetAutoCompletionProvider],
    autoCompleteValueWithLabels = true, allowOnlyAutoCompletedValues = true)
  optionalInputDataset: SparqlEndpointDatasetParameter = SparqlEndpointDatasetParameter(""),
  @Param(
    label = "SPARQL query timeout (ms)",
    value = "SPARQL query timeout (select/update) in milliseconds. A value of zero means that there is no timeout set explicitly." +
      " If a value greater zero is specified this overwrites possible default timeouts."
  )
  sparqlTimeout: Int = 0
) extends CustomTask {
  val intLimit: Option[Int] = {
    // Only allow positive ints
    Try(limit.toInt).filter(_ > 0).toOption
  }

  override def inputPorts: InputPorts = {
    FixedNumberOfInputs(Seq(FixedSchemaPort(SparqlEndpointEntitySchema.schema)))
  }

  override def outputPort: Option[Port] = {
    Some(FixedSchemaPort(outputSchema))
  }

  val outputSchema: EntitySchema = {
    val query = QueryFactory.create(selectQuery.str)
    if (!query.isSelectType) {
      throw new ValidationException("Query is not a SELECT query!")
    }
    val typedPaths = query.getResultVars.asScala map { v =>
      TypedPath(UntypedPath(v), ValueType.STRING, isAttribute = false)
    }
    EntitySchema(
      typeUri = Uri(""),
      typedPaths = typedPaths.toIndexedSeq
    )
  }
}
