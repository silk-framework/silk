package controllers.workspace

import org.silkframework.dataset.rdf._
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.serialization.json.JsonFormat
import play.api.libs.json._

import scala.collection.immutable.SortedMap

/**
  * Containing all (De-)Serializer for Json objects needed for SPARQL 1.1 protocol SELECT and ASK queries.
  * (see: https://www.w3.org/TR/sparql11-results-json/)
  */
object SparqlResultSerializer extends JsonFormat[SparqlResults] {

  override def read(value: JsValue)(implicit readContext: ReadContext): SparqlResults = {
    val resultSet = value.as[JsObject]
    if(resultSet.keys.contains("boolean")){
      new SparqlAskResult(resultSet("boolean").as[JsBoolean].value)
    }
    else{
      val results = resultSet("results").as[JsObject]
      val bindings = SparqlResultBindings.read(results("bindings"))
      val sortedMaps = bindings.bindings.map(b => SortedMap(b.map.map(y => y._1 -> SparqlVariableBinding.toRedNode(y._2)).toSeq:_*))
      SparqlResults(sortedMaps)
    }
  }

  override def write(value: SparqlResults)(implicit writeContext: WriteContext[JsValue]): JsValue = value match{
    case ask: SparqlAskResult => JsObject(Map(
      "head" -> SparqlResultHeader.write(SparqlResultHeader(IndexedSeq.empty, IndexedSeq.empty)),
      "boolean" -> JsBoolean(ask.askResult)
    ))
    case select: SparqlResults => JsObject(Map(
      "head" -> SparqlResultHeader.write(SparqlResultHeader(select.variables.toIndexedSeq, IndexedSeq.empty)),
      "results" -> JsObject(Map(
        "bindings" -> SparqlResultBindings.write(SparqlResultBindings(select.bindings.toIndexedSeq.map(e =>
          SparqlQuerySolution(e.map(y => y._1 -> SparqlVariableBinding.fromRdfNode(y._2))))))))
    ))
  }
}

case class SparqlResultHeader(vars:IndexedSeq[String], link: IndexedSeq[String] = IndexedSeq.empty)
object SparqlResultHeader extends JsonFormat[SparqlResultHeader] {
  override def read(value: JsValue)(implicit readContext: ReadContext): SparqlResultHeader = {
    val fields = value.as[JsObject].fieldSet.toMap
    SparqlResultHeader(
      fields("vars").as[JsArray].value.map(_.toString()),
      fields("link").as[JsArray].value.map(_.toString())
    )
  }

  override def write(value: SparqlResultHeader)(implicit writeContext: WriteContext[JsValue]): JsValue = JsObject(Map(
    "vars" -> JsArray(value.vars.map(x => JsString(x))),
    "link" -> JsArray(value.link.map(x => JsString(x)))
  ))
}

case class SparqlResultBindings(bindings: IndexedSeq[SparqlQuerySolution])
object SparqlResultBindings extends JsonFormat[SparqlResultBindings] {
  override def read(value: JsValue)(implicit readContext: ReadContext): SparqlResultBindings =
    SparqlResultBindings(value.as[JsArray].value.map(e => SparqlQuerySolution.read(e)))

  override def write(value: SparqlResultBindings)(implicit writeContext: WriteContext[JsValue]): JsValue =
    JsArray(value.bindings.map(e => SparqlQuerySolution.write(e)))
}

case class SparqlQuerySolution(map: Map[String, SparqlVariableBinding])
object SparqlQuerySolution  extends JsonFormat[SparqlQuerySolution] {

  override def read(value: JsValue)(implicit readContext: ReadContext): SparqlQuerySolution =
    SparqlQuerySolution(value.as[JsObject].value.map(e => e._1 -> SparqlVariableBinding.read(e._2)).toMap)

  override def write(value: SparqlQuerySolution)(implicit writeContext: WriteContext[JsValue]): JsValue =
    JsObject(value.map.map(e => e._1 -> SparqlVariableBinding.write(e._2)))
}

case class SparqlVariableBinding(typ: String, value: String, datatype: String = "", xmlLang: String = "")
object SparqlVariableBinding extends JsonFormat[SparqlVariableBinding] {
  override def read(value: JsValue)(implicit readContext: ReadContext): SparqlVariableBinding = {
    val map = value.as[JsObject].value
    SparqlVariableBinding(
      map("type").toString(),
      map("value").toString(),
      map.get("xml:Lang").map(_.toString()).getOrElse(""),
      map.get("datatype").map(_.toString()).getOrElse("")
    )
  }

  override def write(value: SparqlVariableBinding)(implicit writeContext: WriteContext[JsValue]): JsValue = JsObject(Seq(
    Some(("type", JsString(value.typ))),
    Some(("value", JsString(value.value))),
    if(value.xmlLang.trim.nonEmpty) Some(("xml:Lang", JsString(value.xmlLang))) else None,
    if(value.datatype.trim.nonEmpty) Some(("datatype", JsString(value.datatype))) else None
  ).flatten)

  def fromRdfNode(node: RdfNode): SparqlVariableBinding ={
    node match{
      case res: Resource => SparqlVariableBinding("uri", res.value)
      case blank: BlankNode => SparqlVariableBinding("bnode", blank.value)
      case typed: DataTypeLiteral => SparqlVariableBinding("literal", typed.value, typed.dataType)
      case lang: LanguageLiteral => SparqlVariableBinding("literal", lang.value, "", lang.language)
      case plain: PlainLiteral => SparqlVariableBinding("literal", plain.value)
    }
  }

  def toRedNode(b: SparqlVariableBinding): RdfNode = b match{
    case SparqlVariableBinding(typ: String, value: String, _, lang: String) if lang.nonEmpty => LanguageLiteral(value, lang)
    case SparqlVariableBinding(typ: String, value: String, dataType: String, _) if dataType.nonEmpty => DataTypeLiteral(value, dataType)
    case SparqlVariableBinding(typ: String, value: String, _: String, _) if typ == "literal" => PlainLiteral(value)
    case SparqlVariableBinding(typ: String, value: String, _: String, _) if typ == "bnode" => BlankNode(value)
    case SparqlVariableBinding(typ: String, value: String, _: String, _) if typ == "uri" => Resource(value)
  }
}
