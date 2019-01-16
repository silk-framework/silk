package org.silkframework.plugins.dataset.rdf.formatters

import org.silkframework.dataset.rdf._

class NTriplesQuadFormatter() extends QuadFormatter {

  private def replaceQuotes(value: String) = value.replaceAll("(^|[^\\\\])\"", "\\\\\"")

  private def format(quad: Quad, asQuad: Boolean = true): String = {
    val sb = new StringBuilder()
    quad.subject match{
      case Left(r) => sb.append("<").append(r.value).append("> ")
      case Right(b) => sb.append("_:").append(b.value).append(" ")
    }
    // predicate
    sb.append("<").append(quad.predicate.value).append("> ")
    // object
    quad.objectVal match{
      case Resource(value) => sb.append("<").append(value).append("> ")
      case BlankNode(value) => sb.append("_:").append(value).append(" ")
      case LanguageLiteral(value, lang) =>
        sb.append("\"").append(replaceQuotes(value))
        if(lang != null && lang.nonEmpty) sb.append("\"@").append(lang).append(" ")
        else sb.append("\" ")
      case DataTypeLiteral(value, typ) =>
        sb.append("\"").append(replaceQuotes(value))
        if(typ != null && typ.nonEmpty) sb.append("\"^^<").append(typ).append("> ")
        else sb.append("\" ")
      case PlainLiteral(value) =>
        sb.append("\"").append(replaceQuotes(value)).append("\" ")
    }
    // graph
    if(asQuad && quad.context.nonEmpty){
      sb.append("<").append(quad.context.get.value).append("> ")
    }
    // line end
    sb.append(". ")

    sb.toString()
  }

  override def formatQuad(quad: Quad): String = format(quad)

  override def formatAsTriple(triple: Quad): String = format(triple, asQuad = false)
}
