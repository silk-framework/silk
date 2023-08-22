package org.silkframework.plugins.dataset.rdf.tasks

import org.apache.jena.query.QueryFactory
import org.silkframework.config.CustomTask
import org.silkframework.dataset.rdf.{SparqlEndpointDatasetParameter, SparqlEndpointEntitySchema}
import org.silkframework.entity._
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.runtime.plugin.types.MultilineStringParameter
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
  // TODO: Remove
  pluginIcon = "data:image/svg+xml;base64,PHN2ZyBpZD0iaWNvbiIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIiB3aWR0aD0iMzIiIGhlaWdodD0iMzIiIHZpZXdCb3g9IjAgMCAzMiAzMiI+CiAgPGRlZnM+CiAgICA8c3R5bGU+CiAgICAgIC5jbHMtMSB7CiAgICAgICAgZmlsbDogI2ZmZjsKICAgICAgfQoKICAgICAgLmNscy0yIHsKICAgICAgICBmaWxsOiBub25lOwogICAgICB9CiAgICA8L3N0eWxlPgogIDwvZGVmcz4KICA8cGF0aCBkPSJNMjgsMzBIMjBWMjRhMi4wMDIsMi4wMDIsMCwwLDEsMi0yaDRWMThIMjBWMTZoNmEyLjAwMjMsMi4wMDIzLDAsMCwxLDIsMnY0YTIuMDAyMywyLjAwMjMsMCwwLDEtMiwySDIydjRoNloiLz4KICA8cmVjdCB4PSI0LjQ3OTEiIHk9IjE1LjUwMDEiIHdpZHRoPSIyMi4wNDE4IiBoZWlnaHQ9IjEuOTk5OCIgdHJhbnNmb3JtPSJ0cmFuc2xhdGUoLTcuMTI3NCAxNS43OTI5KSByb3RhdGUoLTQ1KSIvPgogIDxnPgogICAgPHBvbHlnb24gY2xhc3M9ImNscy0xIiBwb2ludHM9IjQuNSAxNS41IDQuNSAxNC41IDcuNSAxNC41IDcuNSAzLjUgNC41IDMuNSA0LjUgMi41IDguNSAyLjUgOC41IDE0LjUgMTEuNSAxNC41IDExLjUgMTUuNSA0LjUgMTUuNSIvPgogICAgPHBhdGggZD0iTTgsM1YxNUg4VjNNOSwySDRWNEg3VjE0SDR2Mmg4VjE0SDlWMloiLz4KICA8L2c+CiAgPHJlY3QgaWQ9Il9UcmFuc3BhcmVudF9SZWN0YW5nbGVfIiBkYXRhLW5hbWU9IiZsdDtUcmFuc3BhcmVudCBSZWN0YW5nbGUmZ3Q7IiBjbGFzcz0iY2xzLTIiIHdpZHRoPSIzMiIgaGVpZ2h0PSIzMiIvPgo8L3N2Zz4K",
  description = "A task that executes a SPARQL Select query on a SPARQL enabled data source and outputs the SPARQL result. If the SPARQL source is defined on a specific graph, " +
  "a FROM clause will be added to the query at execution time, except when there already exists a GRAPH or FROM clause in the query. FROM NAMED clauses are not injected."
)
case class SparqlSelectCustomTask(@Param(label = "Select query", value = "A SPARQL 1.1 select query", example = "select * where { ?s ?p ?o }")
                                  selectQuery: MultilineStringParameter,
                                  @Param(label = "Result limit", value = "If set to a positive integer, the number of results is limited")
                                  limit: String = "",
                                 @Param(label = "Optional SPARQL dataset",
                                   value = "An optional SPARQL dataset that can be used for example data, so e.g. the transformation editor shows mapping examples.",
                                   autoCompletionProvider = classOf[SparqlEndpointDatasetAutoCompletionProvider],
                                   autoCompleteValueWithLabels = true, allowOnlyAutoCompletedValues = true)
                                  optionalInputDataset: SparqlEndpointDatasetParameter = SparqlEndpointDatasetParameter(""),
                                  @Param(
                                    label = "SPARQL query timeout (ms)",
                                    value = "SPARQL query timeout (select/update) in milliseconds. A value of zero means that there is no timeout set explicitly." +
                                        " If a value greater zero is specified this overwrites possible default timeouts.")
                                  sparqlTimeout: Int = 0
                                 ) extends CustomTask {
  val intLimit: Option[Int] = {
    // Only allow positive ints
    Try(limit.toInt).filter(_ > 0).toOption
  }

  override def inputSchemataOpt: Option[Seq[EntitySchema]] = Some(Seq(SparqlEndpointEntitySchema.schema))

  override def outputSchemaOpt: Option[EntitySchema] = Some(outputSchema)

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
