package org.silkframework.plugins.dataset.rdf.tasks

import org.apache.jena.query.QueryFactory
import org.silkframework.config._
import org.silkframework.dataset.rdf.SparqlEndpointDatasetParameter
import org.silkframework.entity._
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.execution.typed.SparqlEndpointEntitySchema
import org.silkframework.plugins.dataset.rdf.tasks.templating.SparqlTemplate
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.runtime.plugin.types.SparqlCodeParameter
import org.silkframework.runtime.templating.TemplateEngineAutocompletionProvider
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
  description = "A task that executes a SPARQL Select query and outputs the SPARQL result.",
  documentationFile = "SparqlSelectCustomTask.md",
  iconFile = "sparql-select-query.svg"
)
case class SparqlSelectCustomTask(
  @Param(
    label = "Select query",
    value = "A SPARQL 1.1 select query. The query supports Jinja templating. " +
      "Parameters of the connected input and output tasks can be accessed via 'input.config.<param>' and 'output.config.<param>'. " +
      "Project and global template variables are available as 'project.<key>' and 'global.<key>'. " +
      "Example: SELECT * WHERE { GRAPH <{{ input.config.graph }}> { ?s ?p ?o } }",
    example = "select * where { ?s ?p ?o }")
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
    label = "Use default RDF dataset",
    value = "If enabled, the SELECT query is submitted directly to the configured default RDF dataset." +
      " If the query template references input entities, one query is generated per input entity."
  )
  useDefaultDataset: Boolean = false,
  @Param(
    value = "The templating mode for the template engine.",
    autoCompletionProvider = classOf[TemplateEngineAutocompletionProvider],
    autoCompleteValueWithLabels = true
  )
  templatingMode: String = "jinja",
  @Param(
    label = "Default scope",
    value = "Variables from this scope can be accessed without the scope prefix in Jinja. " +
      "For example, with default scope 'input.entity', a template may reference '{{ property }}' instead of '{{ input.entity.property }}'. " +
      "Leave empty to disable."
  )
  defaultScope: String = "input.entity",
  @Param(
    label = "SPARQL query timeout (ms)",
    value = "SPARQL query timeout (select/update) in milliseconds. A value of zero means that there is no timeout set explicitly." +
      " If a value greater zero is specified this overwrites possible default timeouts.",
    advanced = true
  )
  sparqlTimeout: Int = 0,
) extends CustomTask {
  val intLimit: Option[Int] = {
    // Only allow positive ints
    Try(limit.toInt).filter(_ > 0).toOption
  }

  private val defaultScopePath: Seq[String] = defaultScope.split('.').map(_.trim).filter(_.nonEmpty).toSeq

  val queryTemplate: SparqlTemplate = SparqlTemplate.create(templatingMode, selectQuery.str, defaultScopePath)

  def isStaticTemplate: Boolean = queryTemplate.isStaticTemplate

  def expectedInputSchema: EntitySchema = queryTemplate.inputSchema

  override def inputPorts: InputPorts = {
    if (useDefaultDataset) {
      if (isStaticTemplate) {
        InputPorts.NoInputPorts
      } else {
        FixedNumberOfInputs(Seq(FixedSchemaPort(expectedInputSchema)))
      }
    } else {
      FixedNumberOfInputs(Seq(FixedSchemaPort(SparqlEndpointEntitySchema.schema)))
    }
  }

  override def outputPort: Option[Port] = {
    Some(FixedSchemaPort(outputSchema))
  }

  val outputSchema: EntitySchema = {
    val query = QueryFactory.create(queryTemplate.generateWithDefaults())
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
