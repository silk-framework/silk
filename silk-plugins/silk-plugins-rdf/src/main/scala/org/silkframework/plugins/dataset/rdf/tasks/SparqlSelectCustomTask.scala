package org.silkframework.plugins.dataset.rdf.tasks

import org.apache.jena.query.QueryFactory
import org.silkframework.config.CustomTask
import org.silkframework.dataset.rdf.{SparqlEndpointDatasetParameter, SparqlEndpointEntitySchema}
import org.silkframework.entity._
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.runtime.plugin.MultilineStringParameter
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Uri

import scala.util.Try
import scala.collection.JavaConverters._

/**
  * Custom task that executes a SPARQL select query on the input data source and translates the SPARQL result into
  * an Entity table.
  */
@Plugin(
  id = "sparqlSelectOperator",
  label = "SPARQL Select Task",
  description = "A task that executes a SPARQL Select query on a SPARQL enabled data source and outputs the SPARQL result."
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
