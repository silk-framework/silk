package org.silkframework.plugins.dataset.rdf.tasks.templating

import java.io.{StringReader, StringWriter}
import java.net.URI

import org.apache.jena.graph.NodeFactory
import org.apache.velocity.context.Context
import org.apache.velocity.exception.MethodInvocationException
import org.apache.velocity.runtime.RuntimeSingleton
import org.apache.velocity.{Template, VelocityContext}
import org.silkframework.rule.util.JenaSerializationUtil

import scala.util.Try

/**
  * Templating engine für SPARQL queries.
  * Based on the Apache Velocity engine.
  *
  * @see See [[https://velocity.apache.org/]] for more information.
  */
object SparqlVelocityTemplating {
  final val ROW_VAR_NAME = "row"
  final val INPUT_PROPERTIES_VAR_NAME = "inputProperties"
  final val OUTPUT_PROPERTIES_VAR_NAME = "outputProperties"

  final val templatingVariables = Seq(ROW_VAR_NAME, INPUT_PROPERTIES_VAR_NAME, OUTPUT_PROPERTIES_VAR_NAME)

  /** Creates a Velocity template based on the given template string. */
  def createTemplate(sparqlTemplate: String): Template = {
    val service = RuntimeSingleton.getRuntimeServices
    service.addProperty("runtime.strict_mode.enable", true) // This should fail if it cannot replace variables with input values.
    val reader = new StringReader(sparqlTemplate)
    val template = new Template()
    template.setRuntimeServices(service)
    template.setData(service.parse(reader, template))
    template.initDocument()
    template
  }

  /** Renders the template with the given context */
  def renderTemplate(template: Template, context: Context): String = {
    val writer = new StringWriter()
    template.merge(context, writer)
    writer.toString
  }

  def renderTemplate(template: Template, row: Row, taskProperties: TaskProperties): String = {
    try {
      val context = new VelocityContext()
      context.put(ROW_VAR_NAME, row)
      context.put(INPUT_PROPERTIES_VAR_NAME, InputProperties(taskProperties.inputTask))
      context.put(OUTPUT_PROPERTIES_VAR_NAME, OutputProperties(taskProperties.outputTask))
      renderTemplate(template, context)
    } catch {
      case ex: MethodInvocationException =>
        val adaptedMessage = prettifyExceptionMessage(Option(ex.getMessage).getOrElse(""))
        throw TemplateExecutionException("Template could not be rendered. Error detail: " + adaptedMessage, ex)
    }
  }

  private def prettifyExceptionMessage(errorMessage: String): String = {
    var replacement = errorMessage.
        replace("java.lang.String", "String").
        replace("<unknown template>", "").
        replace("threw exception org.silkframework.plugins.dataset.rdf.tasks.templating.TemplateExecutionException", "has failed with error message")
    for((className, varName) <- Seq(("Row", ROW_VAR_NAME), ("InputProperties", INPUT_PROPERTIES_VAR_NAME), ("OutputProperties", OUTPUT_PROPERTIES_VAR_NAME))) {
      replacement = replacement.replace(s"Object 'org.silkframework.plugins.dataset.rdf.sparql.$className'", varName).
          replace(s"in  class org.silkframework.plugins.dataset.rdf.tasks.templating.$className" , s"of $$$varName object")
    }
    replacement
  }
}

/** Row API used in SPARQL templates. Represents a single row where input paths are either exactly one value or empty.
  *
  * The Row object will be available in Velocity templates as 'row' variable.
  *
  * Examples:
  *
  * <pre>
  *   $row.asUri("urn:prop:uriProp") ## Renders the value of the input path as URI, e.g. <http://...>
  *   $row.asPlainLiteral("urn:prop:stringProp") ## Renders the value of the input paths as plain string, e.g. "Quotes \" are escaped"
  *   $row.asRawUnsafe("urn:prop:trustedValuesOnly") ## Puts the value as it is into the rendered template. This is UNSAFE and prone to injection attacks.
  *   #if ( $row.exists("urn:prop:valueMightNotExist") ) ## Checks if a value exists for the input path, i.e. values can always be optional.
  *     $row.asPlainLiteral("urn:prop:valueMightNotExist") ## If no value exists for the input path then this would throw an exception
  *   #end
  * </pre>
  *
  * @param inputValues The map of existing input values, i.e. values that were defined by input paths, but where no value was available are not set.
  */
case class Row(inputValues: Map[String, String]) extends TemplateValueAccessApi {
  override val templateVarName: String = SparqlVelocityTemplating.ROW_VAR_NAME
}

/** Similar to Row, but for the input task properties. */
case class InputProperties(inputValues: Map[String, String]) extends TemplateValueAccessApi {
  override val templateVarName: String = SparqlVelocityTemplating.INPUT_PROPERTIES_VAR_NAME
}

/** Similar to Row, but for the output task properties. */
case class OutputProperties(inputValues: Map[String, String]) extends TemplateValueAccessApi {
  override val templateVarName: String = SparqlVelocityTemplating.OUTPUT_PROPERTIES_VAR_NAME
}

/** API used in templates to access all kinds of input values. Represents a key value object where input values are either exactly one value or empty/not defined.
  *
  * See [[Row]] for examples.
  *
  */
trait TemplateValueAccessApi {
  def inputValues: Map[String, String]

  def templateVarName: String

  /** Returns the value for a specific input path as URI, i.e. <...> */
  def asUri(inputPath: String): String = {
    val value = objectValue(inputPath)
    if(Try(new URI(value)).isFailure) {
      throw TemplateExecutionException(s"Value for input path '$inputPath' is not a valid URI: '$value'")
    }
    val uriNode = NodeFactory.createURI(value)
    JenaSerializationUtil.serializeSingleNode(uriNode)
  }

  /** Checks if a value for the provided input path exists */
  def exists(inputPath: String): Boolean = {
    inputValues.contains(inputPath)
  }

  private def objectValue(inputPath: String): String = {
    inputValues.get(inputPath) match {
      case Some(value) =>
        value
      case None =>
        throw TemplateExecutionException(s"Input path '$inputPath' did not exist in $$$templateVarName.")
    }
  }

  /** Returns the value for a specific input path as SPARQL plain literal, i.e. "..." */
  def asPlainLiteral(inputPath: String): String = {
    val value = objectValue(inputPath)
    val uriNode = NodeFactory.createLiteral(value)
    JenaSerializationUtil.serializeSingleNode(uriNode)
  }

  /** Puts the value of the input path as raw string into the rendered template.
    * This can be UNSAFE and should never be used when the input data comes from untrusted sources. */
  def asRawUnsafe(inputPath: String): String = {
    objectValue(inputPath)
  }
}

case class TemplateExecutionException(message: String, cause: Throwable = null) extends RuntimeException(message, cause)