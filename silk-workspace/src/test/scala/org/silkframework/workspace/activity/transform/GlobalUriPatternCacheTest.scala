package org.silkframework.workspace.activity.transform

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.rule._

import scala.collection.mutable

class GlobalUriPatternCacheTest extends FlatSpec with MustMatchers {
  it should "extract URI patterns from existing mapping rules" in {
    val rootUriPattern = "https://root.org/{id}"
    def uriPattern(suffix: String): String = s"urn:someUriPattern:suffix$suffix"
    def typeUri(suffix: String): String = s"urn:type:suffix$suffix"
    val uriPatternMap: mutable.HashMap[String, mutable.HashSet[String]] = mutable.HashMap()
    val transformRule: TransformRule = RootMappingRule(MappingRules(
      propertyRules = Seq(
        objectMapping(sourcePath = "objectA", targetProperty = "", objectMappingRules = Seq(
          objectMapping(targetClasses = typeUri("noPattern")),
          objectMapping(uriPattern = uriPattern("noTargetClass")),
          objectMapping(),
          objectMapping(
            targetClasses = s"${typeUri("multiA")},${typeUri("multiB")}",
            uriPattern = uriPattern("multiClasses")
          ),
        )),
        objectMapping(targetProperty = "urn:targetProperty:1", objectMappingRules = Seq(
          objectMapping(
            targetClasses = "urn:rootTypeA",
            uriPattern = uriPattern("aPattern")
          ),
        )),
      ),
      typeRules = Seq(TypeMapping(id(), "urn:rootTypeA"), TypeMapping(id(), "urn:rootTypeB")),
      uriRule = Some(PatternUriMapping(id(), rootUriPattern))
    ))
    GlobalUriPatternCache.extractUriPatterns(transformRule, uriPatternMap)
    uriPatternMap.size mustBe 4
    uriPatternMap("urn:rootTypeA") mustBe Set(rootUriPattern, uriPattern("aPattern"))
    uriPatternMap("urn:rootTypeB") mustBe Set(rootUriPattern)
    uriPatternMap(typeUri("multiA")) mustBe Set(uriPattern("multiClasses"))
    uriPatternMap(typeUri("multiB")) mustBe Set(uriPattern("multiClasses"))
  }

  private var counter = 1
  private def id(): String = {
    counter += 1
    "id" + counter
  }

  private def objectMapping(sourcePath: String = "",
                            targetProperty: String = "urn:targetProp:" + id(),
                            targetClasses: String = "",
                            objectMappingRules: Seq[ObjectMapping] = Seq.empty,
                            uriPattern: String = ""): ObjectMapping = {
    val uriPatternRule = Some(uriPattern).toSeq.filter(_.nonEmpty).map(p => PatternUriMapping(id(), p))
    val typeRules = Some(targetClasses).toSeq.filter(_.nonEmpty).flatMap(types => types.split(",").map(t => TypeMapping(id(), t)))
    val rules = objectMappingRules ++ uriPatternRule ++ typeRules
    ObjectMapping(id(), sourcePath = UntypedPath.parse(sourcePath), target = Some(MappingTarget(targetProperty)),
      rules = rules)
  }
}
