package controllers.sparqlapi

import org.silkframework.dataset.rdf._
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}

import scala.collection.immutable.SortedMap
import scala.xml.Node

object SparqlResultXmlSerializers extends XmlFormat[SparqlResults] {

  override def read(node: Node)(implicit readContext: ReadContext): SparqlResults = {
    (node \ "boolean").headOption match {
      case Some(ask) => new SparqlAskResult(ask.text.trim.toBoolean)
      case None =>
        val bindings = SparqlResultXmlBindings((node \ "results").toIndexedSeq.map(b => SparqlXmlQuerySolution.read(b)))
        val sortedMaps = bindings.bindings.map(b => SortedMap(b.map.map(y => y._1 -> SparqlXmlVariableBinding.toRdfNode(y._2)).toSeq: _*))
        SparqlResults(sortedMaps)
    }
  }

  override def write(value: SparqlResults)(implicit writeContext: WriteContext[Node]): Node = {
    <sparql xmlns="http://www.w3.org/2005/sparql-results#">
      {
      value match{
        case ask: SparqlAskResult =>
          SparqlResultXmlHeader.write(SparqlResultXmlHeader(IndexedSeq.empty, IndexedSeq.empty))
        case select: SparqlResults =>
          SparqlResultXmlHeader.write(SparqlResultXmlHeader(select.variables.toIndexedSeq, IndexedSeq.empty))
      }}
      {
      value match{
        case ask: SparqlAskResult => <boolean>{ask.askResult}</boolean>
        case select: SparqlResults =>
          SparqlResultXmlBindings.write(SparqlResultXmlBindings(select.bindings.toIndexedSeq.map(e =>
            SparqlXmlQuerySolution(e.map(y => y._1 -> SparqlXmlVariableBinding.fromRdfNode(y._2))))))
      }}
    </sparql>
  }

  case class SparqlResultXmlHeader(vars:IndexedSeq[String], link: IndexedSeq[String] = IndexedSeq.empty)
  object SparqlResultXmlHeader extends XmlFormat[SparqlResultXmlHeader] {
    override def read(node: Node)(implicit readContext: ReadContext): SparqlResultXmlHeader = {
      val vars = (node \ "variable").toIndexedSeq.map(v => (v \ "@name").text.trim)
      val link = (node \ "link").toIndexedSeq.map(v => (v \ "@href").text.trim)
      SparqlResultXmlHeader(vars, link)
    }

    override def write(value: SparqlResultXmlHeader)(implicit writeContext: WriteContext[Node]): Node = {
      <head>
        {
        for (variable <- value.vars) yield {
          <variable name={variable}/>
        }}{
        for (link <- value.link) yield {
            <link href={link}/>
        }}
      </head>
    }
  }

  case class SparqlResultXmlBindings(bindings: IndexedSeq[SparqlXmlQuerySolution])
  object SparqlResultXmlBindings extends XmlFormat[SparqlResultXmlBindings] {
    override def read(node: Node)(implicit readContext: ReadContext): SparqlResultXmlBindings ={
      SparqlResultXmlBindings((node \ "results").toIndexedSeq.map(r => SparqlXmlQuerySolution.read(r)))
    }

    override def write(value: SparqlResultXmlBindings)(implicit writeContext: WriteContext[Node]): Node ={
      <results>
        {
        for (result <- value.bindings) yield {
          SparqlXmlQuerySolution.write(result)
        }}
      </results>
    }
  }

  case class SparqlXmlQuerySolution(map: Map[String, SparqlXmlVariableBinding])
  object SparqlXmlQuerySolution  extends XmlFormat[SparqlXmlQuerySolution] {

    override def read(node: Node)(implicit readContext: ReadContext): SparqlXmlQuerySolution ={
      val bindings = node \ "binding"
      SparqlXmlQuerySolution(bindings.map(b => (b \ "@name").text.trim -> SparqlXmlVariableBinding.read(b)).toMap)
    }

    override def write(value: SparqlXmlQuerySolution)(implicit writeContext: WriteContext[Node]): Node = {
      <result>
        {
      for (binding <- value.map) yield {
        <binding name={binding._1}>{SparqlXmlVariableBinding.write(binding._2)}</binding>
      }}
      </result>
    }
  }

  case class SparqlXmlVariableBinding(typ: String, value: String, datatype: String = "", xmlLang: String = "")
  object SparqlXmlVariableBinding extends XmlFormat[SparqlXmlVariableBinding] {
    override def read(node: Node)(implicit readContext: ReadContext): SparqlXmlVariableBinding = {
      val typ = node.label.trim
      val value = node.text.trim
      val lang = (node \ "@xml:lang").headOption.map(_.text.trim).getOrElse("")
      val data = (node \ "@datatype").headOption.map(_.text.trim).getOrElse("")
      SparqlXmlVariableBinding(typ, value, lang, data)
    }

    override def write(value: SparqlXmlVariableBinding)(implicit writeContext: WriteContext[Node]): Node = value match{
      case SparqlXmlVariableBinding("uri", uri, _, _) => <uri>{uri}</uri>
      case SparqlXmlVariableBinding("bnode", label, _, _) => <bnode>{label}</bnode>
      case SparqlXmlVariableBinding("literal", label, datatype, _) if datatype.nonEmpty => <literal datatype={datatype}>{label}</literal>
      case SparqlXmlVariableBinding("literal", label, _, lang) if lang.nonEmpty => <literal xml:lang={lang}>{label}</literal>
      case SparqlXmlVariableBinding("literal", label, _, _) => <literal>{label}</literal>
    }

    def fromRdfNode(node: RdfNode): SparqlXmlVariableBinding ={
      node match{
        case res: Resource => SparqlXmlVariableBinding("uri", res.value)
        case blank: BlankNode => SparqlXmlVariableBinding("bnode", blank.value)
        case typed: DataTypeLiteral => SparqlXmlVariableBinding("literal", typed.value, typed.dataType)
        case lang: LanguageLiteral => SparqlXmlVariableBinding("literal", lang.value, "", lang.language)
        case plain: PlainLiteral => SparqlXmlVariableBinding("literal", plain.value)
      }
    }

    def toRdfNode(b: SparqlXmlVariableBinding): RdfNode = b match{
      case SparqlXmlVariableBinding(typ: String, value: String, _, lang: String) if lang.nonEmpty => LanguageLiteral(value, lang)
      case SparqlXmlVariableBinding(typ: String, value: String, dataType: String, _) if dataType.nonEmpty => DataTypeLiteral(value, dataType)
      case SparqlXmlVariableBinding(typ: String, value: String, _: String, _) if typ == "literal" => PlainLiteral(value)
      case SparqlXmlVariableBinding(typ: String, value: String, _: String, _) if typ == "bnode" => BlankNode(value)
      case SparqlXmlVariableBinding(typ: String, value: String, _: String, _) if typ == "uri" => Resource(value)
    }
  }
}
