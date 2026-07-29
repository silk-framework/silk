package org.silkframework.rule

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.util.Uri

class TransformSpecTest extends AnyFlatSpec with Matchers {

  behavior of "TransformSpec"

  it should "resolve the empty type to the primary rule even if a type rule has an empty URI" in {
    // A type rule with an empty URI is creatable via the task APIs
    val spec = TransformSpec(DatasetSelection("input"), RootMappingRule(MappingRules(propertyRules = Seq(
      DirectMapping("name", UntypedPath("name"), MappingTarget("urn:p:name")),
      ObjectMapping(id = "nested", sourcePath = UntypedPath.empty, target = Some(MappingTarget("urn:p:nested")),
        rules = MappingRules(
          typeRules = Seq(TypeMapping(id = "emptyType", typeUri = Uri(""))),
          propertyRules = Seq(DirectMapping("city", UntypedPath("city"), MappingTarget("urn:p:city")))
        ))
    ))))

    // The empty URI means that no type is selected, so it addresses the root rule, as in asDataSource
    spec.ruleSchemataForTargetTypeOption(Uri("")) mustBe None
    spec.outputSchemaForTargetType(Uri("")) mustBe spec.primaryRuleSchemata.outputSchema
  }
}
