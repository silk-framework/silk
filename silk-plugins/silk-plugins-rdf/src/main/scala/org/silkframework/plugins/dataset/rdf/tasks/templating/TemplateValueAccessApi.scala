package org.silkframework.plugins.dataset.rdf.tasks.templating

import java.net.URI

import org.apache.jena.graph.NodeFactory
import org.silkframework.rule.util.JenaSerializationUtil
import org.silkframework.runtime.templating.exceptions.TemplateEvaluationException

import scala.util.Try


/**
 * API used in templates to access all kinds of input values. Represents a key value object where input values are either exactly one value or empty/not defined.
  *
  * See [[Row]] for examples.
  *
  */
trait TemplateValueAccessApi {
  def inputValues: Map[String, String]

  def templateVarName: String

  /** Returns the value for a specific input path as URI, i.e. <...> */
  def uri(inputPath: String): String = {
    val value = objectValue(inputPath)
    if(Try(new URI(value)).isFailure) {
      throw new TemplateEvaluationException(s"Value for input path '$inputPath' is not a valid URI: '$value'")
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
        throw new TemplateEvaluationException(s"Input path '$inputPath' did not exist in $$$templateVarName.")
    }
  }

  /** Returns the value for a specific input path as SPARQL plain literal, i.e. "..." */
  def plainLiteral(inputPath: String): String = {
    val value = objectValue(inputPath)
    val uriNode = NodeFactory.createLiteral(value)
    JenaSerializationUtil.serializeSingleNode(uriNode)
  }

  /** Puts the value of the input path as raw string into the rendered template.
    * This can be UNSAFE and should never be used when the input data comes from untrusted sources. */
  def rawUnsafe(inputPath: String): String = {
    objectValue(inputPath)
  }
}
