package org.silkframework.workspace.activity.vocabulary

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.rule.vocab.{DatatypePropertyType, GenericInfo, Vocabulary, VocabularyProperty}
import org.silkframework.rule.{DirectMapping, MappingRules, MappingTarget, ObjectMapping, RootMappingRule, TransformRule}
import org.silkframework.workspace.activity.transform.VocabularyCacheValue

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class GlobalVocabularySynonymCacheTest extends FlatSpec with MustMatchers {
  behavior of "Global vocabulary synonym cache"

  it should "extract synonyms from existing mapping rules" in {
    val synonymMap: mutable.HashMap[String, ArrayBuffer[String]] = mutable.HashMap()
    val transformRule: TransformRule = RootMappingRule(MappingRules(
      propertyRules = Seq(
        directMapping("cryptic", uri("1")),
        directMapping("pathA", uri("attrA")),
        objectMapping("objectA", "", MappingRules(
          directMapping(s"first/<${uri("objectAValue", caseNr = Some(0))}>", uri("1")))
        ),
        objectMapping("", uri("obj"), MappingRules(
          directMapping(s"first\\<${uri("objectBValue", caseNr = Some(1))}>", uri("2")),
          objectMapping(s"""first[propA = "specific"]/<${uri("tooSpecificObjectPath", caseNr = Some(2))}>""", uri("obj")),
          objectMapping(s"""\\first/<${uri("normalObjectPath", caseNr = Some(2))}>""", uri("obj"))
        )),
        objectMapping("objectC", uri("obj"), MappingRules(
          directMapping(s"first/<${uri("objectCValue", caseNr = Some(2))}>", uri("3")))
        )
      )
    ))
    val vocabularyCacheValue = new VocabularyCacheValue(Seq(
      vocab(uri("1"), uri("2"), uri("3"), uri("obj")),
      vocab(uri("A"), uri("B"), uri("attrA"))
    ), None)
    GlobalVocabularySynonymCache.extractPropertySynonyms(transformRule, synonymMap, vocabularyCacheValue)
    synonymMap.size mustBe 5
    synonymMap.get(uri("1")) mustBe Some(Seq("cryptic", "objectAValue"))
    synonymMap.get(uri("2")) mustBe Some(Seq("objectBValue"))
    synonymMap.get(uri("3")) mustBe Some(Seq("objectCValue"))
    synonymMap.get(uri("attrA")) mustBe Some(Seq("pathA"))
    synonymMap.get(uri("obj")) mustBe Some(Seq("normalObjectPath", "objectC"))
  }

  private def uri(labelLikePart: String, caseNr: Option[Int] = None): String = caseNr.getOrElse(math.abs(labelLikePart.hashCode % 3)) match {
    case 0 => s"https://domain.com/path/$labelLikePart"
    case 1 => s"http://domain.com/path/subPath#$labelLikePart"
    case 2 => s"urn:prop:$labelLikePart"
  }

  private var counter = 1
  private def id(): String = {
    counter += 1
    "id" + counter
  }

  private def vocab(properties: String*): Vocabulary = Vocabulary(GenericInfo(id()), Seq.empty, properties.map(p =>
    VocabularyProperty(GenericInfo(p), DatatypePropertyType, None, None)))

  private def directMapping(sourcePath: String, targetProperty: String): DirectMapping = {
    DirectMapping(id(), sourcePath = UntypedPath.parse(sourcePath), mappingTarget = MappingTarget(targetProperty))
  }

  private def objectMapping(sourcePath: String, targetProperty: String, rules: MappingRules = MappingRules()): ObjectMapping = {
    ObjectMapping(id(), sourcePath = UntypedPath.parse(sourcePath), target = Some(MappingTarget(targetProperty)), rules = rules)
  }
}
