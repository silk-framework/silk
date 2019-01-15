package org.silkframework.plugins.dataset.rdf.tasks

import org.apache.jena.query.QueryFactory
import org.silkframework.config.CustomTask
import org.silkframework.dataset.rdf.SparqlEndpointEntitySchema
import org.silkframework.entity.{AutoDetectValueType, EntitySchema, Path, TypedPath}
import org.silkframework.runtime.plugin.{MultilineStringParameter, Param, Plugin}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Uri

import scala.collection.JavaConverters._

@Plugin(
  id = "sparqlSelectOperator",
  label = "SPARQL Select Task",
  description = "A task that executes a SPARQL Select query on a SPARQL enabled data source and outputs the SPARQL result."
)
case class SparqlCopyCustomTask(
    @Param(label = "Construct query", value = "A SPARQL 1.1 construct query", example = "construct { ?s ?p ?o } where { ?s ?p ?o }")
      selectQuery: MultilineStringParameter,
    @Param(label = "Use temporary file", value = "When copying directly to the same SPARQL Endpoint or when copying large amounts of triples, set to True")
      tempFile: Boolean = false
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
  override def outputSchemaOpt: Option[EntitySchema] = Some(internalSchema)

  private def isValidSelect: Boolean = {
    val query = QueryFactory.create(selectQuery.str)
    var valid = query.getResultVars.containsAll(List("subject", "predicate", "object").asJavaCollection)
    valid = valid && query.getResultVars.size() == 3 || query.getResultVars.size() == 4 && query.getResultVars.contains("graph")
    valid && query.isSelectType
  }

  val internalSchema: EntitySchema = {
    val query = QueryFactory.create(selectQuery.str)
    if (query.isConstructType || isValidSelect) {
      val typedPaths = query.getResultVars.asScala map { v =>
        TypedPath(Path(v), AutoDetectValueType, isAttribute = false)
      }
      EntitySchema(
        typeUri = Uri(""),
        typedPaths = typedPaths.toIndexedSeq
      )
    }
    else{
      throw new ValidationException("Query is neither a valid CONSTRUCT query or a SELECT query for subject, predicate, object, (graph) !")
    }
  }
}
