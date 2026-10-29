package org.silkframework.runtime.plugin.types

import org.silkframework.runtime.templating.TemplateVariablesReader

import scala.collection.immutable.ArraySeq
import scala.language.implicitConversions

sealed trait CodeParameter {
  def str: String

  override def toString: String = str

  def lines: Seq[String] = ArraySeq.unsafeWrapArray(str.split("[\\r\\n]+"))
}

object CodeParameter {
  implicit def codeParameter2str(codeParameter: CodeParameter): String = codeParameter.str
}

case class Jinja2CodeParameter(var str: String) extends CodeParameter

object Jinja2CodeParameter {
  implicit def str2parameter(str: String): Jinja2CodeParameter = Jinja2CodeParameter(str)
}

case class JsonCodeParameter(var str: String) extends CodeParameter

/**
 * A SPARQL code parameter that might contain Jinja template variables.
 *
 * @param str The SPARQL query
 * @param variables That variables that are available at creation time
 */
case class SparqlCodeParameter(var str: String, val variables: Option[TemplateVariablesReader] = None) extends CodeParameter

object SparqlCodeParameter {
  implicit def str2parameter(str: String): SparqlCodeParameter = SparqlCodeParameter(str)
}

case class SqlCodeParameter(var str: String) extends CodeParameter

object SqlCodeParameter {
  implicit def str2parameter(str: String): SqlCodeParameter = SqlCodeParameter(str)
}

case class XmlCodeParameter(var str: String) extends CodeParameter

object XmlCodeParameter {
  implicit def str2parameter(str: String): XmlCodeParameter = XmlCodeParameter(str)
}

case class YamlCodeParameter(var str: String) extends CodeParameter

case class PythonCodeParameter(var str: String) extends CodeParameter

case class TurtleCodeParameter(var str: String) extends CodeParameter

case class HtmlCodeParameter(var str: String) extends CodeParameter

object HtmlCodeParameter {
  implicit def str2parameter(str: String): HtmlCodeParameter = HtmlCodeParameter(str)
}
