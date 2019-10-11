package controllers.sparqlapi

import org.silkframework.dataset.rdf._
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.serialization.json.JsonFormat
import play.api.libs.json._

import scala.collection.immutable.SortedMap

/**
  * Containing all (De-)Serializer for Json objects needed for SPARQL 1.1 protocol SELECT and ASK queries.
  * (see: https://www.w3.org/TR/sparql11-results-json/)
  */
object SparqlResultJsonSerializers extends JsonFormat[SparqlResults] {

  override def read(value: JsValue)(implicit readContext: ReadContext): SparqlResults = {
    val resultSet = value.as[JsObject]
    if(resultSet.keys.contains("boolean")){
      new SparqlAskResult(resultSet("boolean").as[JsBoolean].value)
    }
    else{
      val results = resultSet("results").as[JsObject]
      val bindings = SparqlResultJsonBindings.read(results("bindings"))
      val sortedMaps = bindings.bindings.map(b => SortedMap(b.map.map(y => y._1 -> SparqlJsonVariableBinding.toRdfNode(y._2)).toSeq:_*))
      SparqlResults(sortedMaps)
    }
  }

  override def write(value: SparqlResults)(implicit writeContext: WriteContext[JsValue]): JsValue = value match{
    case ask: SparqlAskResult => JsObject(Map(
      "head" -> SparqlResultJsonHeader.write(SparqlResultJsonHeader(IndexedSeq.empty, IndexedSeq.empty)),
      "boolean" -> JsBoolean(ask.askResult)
    ))
    case select: SparqlResults => JsObject(Map(
      "head" -> SparqlResultJsonHeader.write(SparqlResultJsonHeader(select.variables.toIndexedSeq, IndexedSeq.empty)),
      "results" -> JsObject(Map(
        "bindings" -> SparqlResultJsonBindings.write(SparqlResultJsonBindings(select.bindings.toIndexedSeq.map(e =>
          SparqlJsonQuerySolution(e.map(y => y._1 -> SparqlJsonVariableBinding.fromRdfNode(y._2))))))))
    ))
  }

  case class SparqlResultJsonHeader(vars:IndexedSeq[String], link: IndexedSeq[String] = IndexedSeq.empty)
  object SparqlResultJsonHeader extends JsonFormat[SparqlResultJsonHeader] {
    override def read(value: JsValue)(implicit readContext: ReadContext): SparqlResultJsonHeader = {
      val fields = value.as[JsObject].fieldSet.toMap
      SparqlResultJsonHeader(
        fields("vars").as[JsArray].value.map(_.toString()),
        fields("link").as[JsArray].value.map(_.toString())
      )
    }

    override def write(value: SparqlResultJsonHeader)(implicit writeContext: WriteContext[JsValue]): JsValue = JsObject(Map(
      "vars" -> JsArray(value.vars.map(x => JsString(x))),
      "link" -> JsArray(value.link.map(x => JsString(x)))
    ))
  }

  case class SparqlResultJsonBindings(bindings: IndexedSeq[SparqlJsonQuerySolution])
  object SparqlResultJsonBindings extends JsonFormat[SparqlResultJsonBindings] {
    override def read(value: JsValue)(implicit readContext: ReadContext): SparqlResultJsonBindings =
      SparqlResultJsonBindings(value.as[JsArray].value.map(e => SparqlJsonQuerySolution.read(e)))

    override def write(value: SparqlResultJsonBindings)(implicit writeContext: WriteContext[JsValue]): JsValue =
      JsArray(value.bindings.map(e => SparqlJsonQuerySolution.write(e)))
  }

  case class SparqlJsonQuerySolution(map: Map[String, SparqlJsonVariableBinding])
  object SparqlJsonQuerySolution  extends JsonFormat[SparqlJsonQuerySolution] {

    override def read(value: JsValue)(implicit readContext: ReadContext): SparqlJsonQuerySolution =
      SparqlJsonQuerySolution(value.as[JsObject].value.map(e => e._1 -> SparqlJsonVariableBinding.read(e._2)).toMap)

    override def write(value: SparqlJsonQuerySolution)(implicit writeContext: WriteContext[JsValue]): JsValue =
      JsObject(value.map.map(e => e._1 -> SparqlJsonVariableBinding.write(e._2)))
  }

  case class SparqlJsonVariableBinding(typ: String, value: String, datatype: String = "", xmlLang: String = "")
  object SparqlJsonVariableBinding extends JsonFormat[SparqlJsonVariableBinding] {
    override def read(value: JsValue)(implicit readContext: ReadContext): SparqlJsonVariableBinding = {
      val map = value.as[JsObject].value
      SparqlJsonVariableBinding(
        map("type").toString(),
        map("value").toString(),
        map.get("xml:Lang").map(_.toString()).getOrElse(""),
        map.get("datatype").map(_.toString()).getOrElse("")
      )
    }

    override def write(value: SparqlJsonVariableBinding)(implicit writeContext: WriteContext[JsValue]): JsValue = JsObject(Seq(
      Some(("type", JsString(value.typ))),
      Some(("value", JsString(value.value))),
      if(value.xmlLang.trim.nonEmpty) Some(("xml:Lang", JsString(value.xmlLang))) else None,
      if(value.datatype.trim.nonEmpty) Some(("datatype", JsString(value.datatype))) else None
    ).flatten)

    def fromRdfNode(node: RdfNode): SparqlJsonVariableBinding ={
      node match{
        case res: Resource => SparqlJsonVariableBinding("uri", res.value)
        case blank: BlankNode => SparqlJsonVariableBinding("bnode", blank.value)
        case typed: DataTypeLiteral => SparqlJsonVariableBinding("literal", typed.value, typed.dataType)
        case lang: LanguageLiteral => SparqlJsonVariableBinding("literal", lang.value, "", lang.language)
        case plain: PlainLiteral => SparqlJsonVariableBinding("literal", plain.value)
      }
    }

    def toRdfNode(b: SparqlJsonVariableBinding): RdfNode = b match{
      case SparqlJsonVariableBinding(typ: String, value: String, _, lang: String) if lang.nonEmpty => LanguageLiteral(value, lang)
      case SparqlJsonVariableBinding(typ: String, value: String, dataType: String, _) if dataType.nonEmpty => DataTypeLiteral(value, dataType)
      case SparqlJsonVariableBinding(typ: String, value: String, _: String, _) if typ == "literal" => PlainLiteral(value)
      case SparqlJsonVariableBinding(typ: String, value: String, _: String, _) if typ == "bnode" => BlankNode(value)
      case SparqlJsonVariableBinding(typ: String, value: String, _: String, _) if typ == "uri" => Resource(value)
    }
  }
}
