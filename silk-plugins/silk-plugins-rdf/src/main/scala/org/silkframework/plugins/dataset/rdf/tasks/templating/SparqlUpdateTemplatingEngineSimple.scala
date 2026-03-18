package org.silkframework.plugins.dataset.rdf.tasks.templating

import org.apache.jena.graph.NodeFactory
import org.silkframework.rule.util.JenaSerializationUtil
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.runtime.templating.{CompiledTemplate, EvaluationConfig, TemplateEngine, TemplateVariableName, TemplateVariableValue}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Uri

import java.io.Writer
import scala.collection.mutable.ArrayBuffer
import scala.util.matching.Regex

/**
  * A simple SPARQL Update templating engine that supports plain literal and URI placeholders.
  */
@Plugin(
  id = SparqlSimpleTemplateEngine.id,
  label = "Simple SPARQL",
  description = "A simple SPARQL Update templating engine that supports plain literal and URI placeholders."
)
case class SparqlSimpleTemplateEngine() extends TemplateEngine {

  override def compile(templateString: String): SparqlSimpleCompiledTemplate = {
    new SparqlSimpleCompiledTemplate(templateString)
  }
}

object SparqlSimpleTemplateEngine {
  final val id = "simple"
}

/**
  * A compiled simple SPARQL Update template that can only render plain literals and URIs.
  * Example:
  *
  * DELETE DATA { ${<PROP_FROM_ENTITY_SCHEMA1>} rdf:label ${"PROP_FROM_ENTITY_SCHEMA2"} }
  */
class SparqlSimpleCompiledTemplate(val sparqlUpdateTemplate: String) extends CompiledTemplate {

  override lazy val variables: Option[Seq[TemplateVariableName]] = Some(
    properties.map(p => new TemplateVariableName(p, ""))
  )

  override def evaluate(values: Map[String, AnyRef], writer: Writer): Unit = {
    val stringValues = values.map { case (k, v) => k -> String.valueOf(v) }
    writer.write(render(stringValues))
  }

  override def evaluate(values: Seq[TemplateVariableValue], writer: Writer, evaluationConfig: EvaluationConfig): Unit = {
    evaluate(convertValues(values), writer)
  }

  private def render(placeholderAssignments: Map[String, String]): String = {
    def assignmentValue(prop: String): String = placeholderAssignments.get(prop) match {
      case Some(value) => value
      case None => throw new ValidationException(s"No value assignment for placeholder property $prop")
    }
    (sparqlUpdateTemplateParts map {
      case SparqlUpdateTemplatePlainLiteralPlaceholder(prop) =>
        val value = assignmentValue(prop)
        JenaSerializationUtil.serializeSingleNode(NodeFactory.createLiteral(value))
      case SparqlUpdateTemplateURIPlaceholder(prop) =>
        val value = assignmentValue(prop)
        validateUri(value)
        JenaSerializationUtil.serializeSingleNode(NodeFactory.createURI(value))
      case SparqlUpdateTemplateStaticPart(partialSparql) =>
        partialSparql
    }).mkString
  }

  private def validateUri(uri: String): Unit = {
    Uri(uri).toURI.failed.toOption foreach { failure =>
      throw new ValidationException(s"URI $uri used in SPARQL Update template is not a valid URI (relative or absolute)", failure)
    }
  }

  /** The template disassembled into its atomic parts */
  lazy val sparqlUpdateTemplateParts: Seq[SparqlUpdateTemplatePart] = {
    val uriPlaceholder = """\$\{<(\S+)>\}""".r
    val literalPlaceholder = """\$\{"(\S+)"\}""".r
    val uriMatches = uriPlaceholder.findAllMatchIn(sparqlUpdateTemplate)
    val literalMatches = literalPlaceholder.findAllMatchIn(sparqlUpdateTemplate)
    var currentIndex = 0
    var nextLiteralMatch: Option[Regex.Match] = None
    var nextUriMatch: Option[Regex.Match] = None
    val templateParts = ArrayBuffer[SparqlUpdateTemplatePart]()
    val templateStr = sparqlUpdateTemplate

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
    templateParts.toSeq
  }

  private val properties: Seq[String] = sparqlUpdateTemplateParts.
      filter(_.isInstanceOf[SparqlUpdateTemplatePlaceholder]).
      map(_.asInstanceOf[SparqlUpdateTemplatePlaceholder].prop).
      distinct
}

sealed trait SparqlUpdateTemplatePart

sealed trait SparqlUpdateTemplatePlaceholder extends SparqlUpdateTemplatePart {
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
