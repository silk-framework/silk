package org.silkframework.rule

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Uri

class TransformRuleTest extends FlatSpec with MustMatchers {
  behavior of "Transform Rule"

  it should "validate illegal target URIs" in {
    val illegalUri = "http://fsdfds "
    val emptyUri = ""
    val relativeUri = "relativePath"
    intercept[ValidationException] {
      checkTargetUri(illegalUri)
    }
    checkTargetUri(emptyUri)
    checkTargetUri(relativeUri)
  }

  private def checkTargetUri(targetPropertyUri: String) = {
    // Nest it, so we know it works recursively
    val objectMapping = ObjectMapping(rules = MappingRules(propertyRules = Seq(
      DirectMapping(mappingTarget = MappingTarget(Uri(targetPropertyUri)))
    )))
    val rootMappingRule = RootMappingRule("id", MappingRules(propertyRules = Seq(objectMapping)))
    rootMappingRule.validate()
  }
}
