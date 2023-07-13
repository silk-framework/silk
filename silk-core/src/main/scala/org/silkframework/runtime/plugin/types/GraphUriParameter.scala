package org.silkframework.runtime.plugin.types

import scala.language.implicitConversions

case class GraphUriParameter(uri: String) {
  override def toString: String = uri
}

object GraphUriParameter {
  implicit def string2GraphUriParameter(uri: String): GraphUriParameter = GraphUriParameter(uri)
  implicit def graphUriParameter2string(gup: GraphUriParameter): String = gup.uri
}