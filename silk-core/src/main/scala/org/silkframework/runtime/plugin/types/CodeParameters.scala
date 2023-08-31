package org.silkframework.runtime.plugin.types

import scala.language.implicitConversions

sealed trait CodeParameter {
  def str: String

  override def toString: String = str

  def lines: Seq[String] = str.split("[\\r\\n]+")
}

object CodeParameter {
  implicit def codeParameter2str(codeParameter: CodeParameter): String = codeParameter.str
}

case class Jinja2CodeParameter(var str: String) extends CodeParameter

case class JsonCodeParameter(var str: String) extends CodeParameter

case class SparqlCodeParameter(var str: String) extends CodeParameter

case class SqlCodeParameter(var str: String) extends CodeParameter

case class XmlCodeParameter(var str: String) extends CodeParameter

case class YamlCodeParameter(var str: String) extends CodeParameter