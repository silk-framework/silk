package org.silkframework.serialization.json

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.config.Prefixes
import org.silkframework.entity.{Path, StringValueType}
import org.silkframework.rule.expressions.ExpressionGenerator
import org.silkframework.rule.input.PathInput
import org.silkframework.rule.{ComplexMapping, DirectMapping, MappingTarget, TransformRule}
import org.silkframework.runtime.serialization.ReadContext
import org.silkframework.util.Uri
import play.api.libs.json.Json

class TransformRuleJsonFormatTest extends FlatSpec with Matchers {

  private implicit val prefixes = Prefixes.default
  private implicit val readContext = ReadContext(prefixes = prefixes)
  private val generator = new ExpressionGenerator

  behavior of "TransformRuleJsonFormat"

  it should "parse direct mappings" in {
    val mappingJson =
      """
      | {
      |   "mappingType": "DirectMapping",
      |   "name": "simpleMapping",
      |   "source": "rdfs:label",
      |   "mappingTarget": {
      |     "property": "rdfs:label",
      |     "valueType": "StringValueType"
      |   }
      | }
      """.stripMargin


    parse(mappingJson) shouldBe
      DirectMapping(
        name = "simpleMapping",
        sourcePath = Path.parse("rdfs:label"),
        mappingTarget =
          MappingTarget(
            propertyUri = Uri.parse("rdfs:label", prefixes),
            valueType = StringValueType
          )
      )
  }

  it should "parse complex mappings" in {
    val mappingJson =
      """
        | {
        |   "name": "complexMapping",
        |   "mappingType": "ComplexMapping",
        |   "sourceExpression": "rdfs:label",
        |   "mappingTarget": {
        |     "property": "rdfs:label",
        |     "valueType": "StringValueType"
        |   }
        | }
      """.stripMargin


    parse(mappingJson) shouldBe
      ComplexMapping(
        name = "complexMapping",
        operator = generator.path("rdfs:label"),
        target = Some(
          MappingTarget(
            propertyUri = Uri.parse("rdfs:label", prefixes),
            valueType = StringValueType
          )
        )
      )
  }

  private def parse(json: String): TransformRule = {
    TransformRuleJsonFormat.read(Json.parse(json))
  }

}
