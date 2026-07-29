package org.silkframework.rule

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.util.Uri

class TransformSpecTest extends AnyFlatSpec with Matchers {

  behavior of "TransformSpec"

  it should "fold the rules of an object rule without a target into its generator's schemata" in {
    val spec = TransformSpec(DatasetSelection("input"), RootMappingRule(MappingRules(propertyRules = Seq(
      DirectMapping("id", UntypedPath("ID"), MappingTarget("urn:p:id")),
      ObjectMapping(id = "extra", sourcePath = UntypedPath.empty, target = None, rules = MappingRules(propertyRules = Seq(
        DirectMapping("extraId", UntypedPath("ID"), MappingTarget("urn:p:extraId")))))
    ))))

    // The output view offers the no-target rule's property on its parent and holds no separate schemata for it
    spec.outputView.mergedRuleSchemata must have size 1
    spec.outputView.outputSchemaForTargetType(Uri("")).typedPaths.map(_.normalizedSerialization) must contain("<urn:p:extraId>")
  }

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
    spec.outputView.ruleSchemataForTargetTypeOption(Uri("")) mustBe None
    spec.outputView.outputSchemaForTargetType(Uri("")) mustBe spec.primaryRuleSchemata.outputSchema
  }
}
