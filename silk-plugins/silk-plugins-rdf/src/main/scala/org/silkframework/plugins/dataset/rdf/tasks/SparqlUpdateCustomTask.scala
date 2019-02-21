package org.silkframework.plugins.dataset.rdf.tasks

import org.apache.jena.graph.NodeFactory
import org.apache.jena.update.UpdateFactory
import org.silkframework.config.CustomTask
import org.silkframework.entity._
import org.silkframework.execution.local.SparqlUpdateEntitySchema
import org.silkframework.plugins.dataset.rdf.RdfFormatUtil
import org.silkframework.runtime.plugin.{MultilineStringParameter, Param, Plugin}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Uri

import scala.collection.mutable.ArrayBuffer
import scala.util.Try
import scala.util.matching.Regex

@Plugin(
  id = "sparqlUpdateOperator",
  label = "SPARQL Update Task",
  description =
"""A task that outputs SPARQL Update queries for every entity from the input based on a SPARQL Update template.
The output of this operator should be connected to the SPARQL datasets to which the results should be written."""
)
case class SparqlUpdateCustomTask(@Param(label = "SPARQL update query", value = SparqlUpdateCustomTask.sparqlUpdateTemplateDescription,
                                         example = "DELETE DATA { ${<PROP_FROM_ENTITY_SCHEMA1>} rdf:label ${\"PROP_FROM_ENTITY_SCHEMA2\"} }")
                                  sparqlUpdateTemplate: MultilineStringParameter,
                                  @Param(label = "Batch size", value = "How many entities should be handled in a single update request.")
                                  batchSize: Int = SparqlUpdateCustomTask.defaultBatchSize) extends CustomTask {
  assert(batchSize >= 1, "Batch size must be greater zero!")

  /** The template disassembled into its atomic parts */
  val sparqlUpdateTemplateParts: Seq[SparqlUpdateTemplatePart] = {
    val uriPlaceholder = """\$\{<(\S+)>\}""".r
    val literalPlaceholder = """\$\{"(\S+)"\}""".r
    val uriMatches = uriPlaceholder.findAllMatchIn(sparqlUpdateTemplate.str)
    val literalMatches = literalPlaceholder.findAllMatchIn(sparqlUpdateTemplate.str)
    var currentIndex = 0
    var nextLiteralMatch: Option[Regex.Match] = None
    var nextUriMatch: Option[Regex.Match] = None
    val templateParts = ArrayBuffer[SparqlUpdateTemplatePart]()
    val templateStr = sparqlUpdateTemplate.str

    def handleStaticPartBeforeMatch(m: Regex.Match, placeHolderLabel: String): Unit = {
      if (m.start < currentIndex) {
        throw new ValidationException(s"Invalid SPARQL Update template. $placeHolderLabel placeholder at illegal position. Placeholder: " + m.group(0))
      } else if (m.end > currentIndex) {
        templateParts.append(SparqlUpdateTemplateStaticPart(templateStr.substring(currentIndex, m.start)))
      }
    }

    def handleUriMatch(uriMatch: Regex.Match): Int = {
      handleStaticPartBeforeMatch(uriMatch, "URI")
      templateParts.append(SparqlUpdateTemplateURIPlaceholder(uriMatch.group(1)))
      nextUriMatch = None
      uriMatch.end
    }

    def handleLiteralMatch(literalMatch: Regex.Match): Int = {
      handleStaticPartBeforeMatch(literalMatch, "Literal")
      templateParts.append(SparqlUpdateTemplatePlainLiteralPlaceholder(literalMatch.group(1)))
      nextLiteralMatch = None
      literalMatch.end
    }

    while(currentIndex < templateStr.length) {
      if(nextLiteralMatch.isEmpty && literalMatches.hasNext) {
        nextLiteralMatch = Some(literalMatches.next())
      }
      if(nextUriMatch.isEmpty && uriMatches.hasNext) {
        nextUriMatch = Some(uriMatches.next())
      }
      currentIndex = (nextUriMatch, nextLiteralMatch) match {
        case (None, None) =>
          templateParts.append(SparqlUpdateTemplateStaticPart(templateStr.substring(currentIndex)))
          templateStr.length
        case (Some(uriMatch), None) =>
          handleUriMatch(uriMatch)
        case (None, Some(literalMatch)) =>
          handleLiteralMatch(literalMatch)
        case (Some(uriMatch), Some(literalMatch)) =>
          if(uriMatch.start < literalMatch.start) {
            handleUriMatch(uriMatch)
          } else {
            handleLiteralMatch(literalMatch)
          }
      }
    }
    templateParts
  }

  validateSparql()

  /** Validate the generated SPARQL of the template and check for batch execution characteristics */
  private def validateSparql(): Unit = {
    val sparql = (sparqlUpdateTemplateParts map {
      case SparqlUpdateTemplatePlainLiteralPlaceholder(prop) =>
        validateUri(prop)
        "\"placeholder value\""
      case SparqlUpdateTemplateURIPlaceholder(prop) =>
        validateUri(prop)
        "<urn:placeholder:uri>"
      case SparqlUpdateTemplateStaticPart(partialSparql) =>
        partialSparql
    }).mkString
    Try(UpdateFactory.create(sparql)).failed.toOption foreach { parseError =>
      throw new ValidationException("The SPARQL Update template does not generate valid SPARQL Update queries. Error message: " +
          parseError.getMessage + ", example query: " + sparql)
    }
    if(batchSize > 1) {
      val batchSparql = sparql + "\n" + sparql
      Try(UpdateFactory.create(batchSparql)).failed.toOption foreach { parseError =>
        throw new ValidationException("The SPARQL Update template cannot be batched processed. There is probably a ';' missing at the end. Error message: " +
            parseError.getMessage + ", example batch query: " + batchSparql)
      }
    }
  }

  /**
    * Generates The SPARQL Update query based on the placeholder assignments.
    * @param placeholderAssignments For each placeholder in the query template
    * @return
    */
  def generate(placeholderAssignments: Map[String, String]): String = {
    def assignmentValue(prop: String): String = placeholderAssignments.get(prop) match {
      case Some(value) => value
      case None => throw new ValidationException(s"No value assignment for placeholder property $prop")
    }
    (sparqlUpdateTemplateParts map {
      case SparqlUpdateTemplatePlainLiteralPlaceholder(prop) =>
        val value = assignmentValue(prop)
        RdfFormatUtil.serializeSingleNode(NodeFactory.createLiteral(value))
      case SparqlUpdateTemplateURIPlaceholder(prop) =>
        val value = assignmentValue(prop)
        validateUri(value)
        RdfFormatUtil.serializeSingleNode(NodeFactory.createURI(value))
      case SparqlUpdateTemplateStaticPart(partialSparql) =>
        partialSparql
    }).mkString
  }

  private def validateUri(uri: String): Unit = {
    Uri(uri).toURI.failed.toOption foreach { failure =>
      throw new ValidationException(s"URI $uri used in SPARQL Update template is not a valid URI (relative or absolute)", failure)
    }
  }

  override def inputSchemataOpt: Option[Seq[EntitySchema]] = {
    Some(Seq(expectedInputSchema))
  }

  def expectedInputSchema: EntitySchema = {
    val properties = sparqlUpdateTemplateParts.
        filter(_.isInstanceOf[SparqlUpdateTemplatePlaceholder]).
        map(_.asInstanceOf[SparqlUpdateTemplatePlaceholder].prop).
        distinct
    EntitySchema("", properties.map(p => Path(p).asUntypedValueType).toIndexedSeq)
  }

  override def outputSchemaOpt: Option[EntitySchema] = Some(SparqlUpdateEntitySchema.schema)
}

sealed trait SparqlUpdateTemplatePart

trait SparqlUpdateTemplatePlaceholder extends SparqlUpdateTemplatePart {
  def prop: String
}

/** A placeholder for a URI node
  * @param prop The property from which URI values are inserted for the placeholder.
  **/
case class SparqlUpdateTemplateURIPlaceholder(prop: String) extends SparqlUpdateTemplatePlaceholder

/** A placeholder for a URI node
  * @param prop The property from which literal values are inserted for the placeholder.
  **/
case class SparqlUpdateTemplatePlainLiteralPlaceholder(prop: String) extends SparqlUpdateTemplatePlaceholder

/** Static SPARQL update query part */
case class SparqlUpdateTemplateStaticPart(queryPart: String) extends SparqlUpdateTemplatePart

object SparqlUpdateCustomTask {
  final val sparqlUpdateTemplateDescription =
    """
This operator takes a SPARQL Update Query Template, with placeholders for either URI or plain literal nodes.
Example:
  DELETE DATA { ${<PROP_FROM_ENTITY_SCHEMA1>} rdf:label ${"PROP_FROM_ENTITY_SCHEMA2"} }
  INSERT DATA { ${<PROP_FROM_ENTITY_SCHEMA1>} rdf:label ${"PROP_FROM_ENTITY_SCHEMA3"} }
  
This will insert the URI serialization of the property value PROP_FROM_ENTITY_SCHEMA1 for the ${<PROP_FROM_ENTITY_SCHEMA1>} expression.
And it will insert a plain literal serialization for the property values PROP_FROM_ENTITY_SCHEMA2/3 for the template literal expressions.

It is be possible to write something like ${"PROP"}^^<http://someDatatype> or ${"PROP"}@en.
    """

  final val defaultBatchSize = 10
}