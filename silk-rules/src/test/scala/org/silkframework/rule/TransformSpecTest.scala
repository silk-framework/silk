package org.silkframework.rule

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.entity.ValueType
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.rule.TransformOutputView.RulesAtTargetPath
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

  it should "keep the rules below an inlined object rule visible in the view" in {
    val spec = TransformSpec(DatasetSelection("input"), RootMappingRule(MappingRules(propertyRules = Seq(
      ObjectMapping(id = "details", sourcePath = UntypedPath("details"), target = None, rules = MappingRules(propertyRules = Seq(
        ObjectMapping(id = "addr", sourcePath = UntypedPath.empty, target = Some(MappingTarget("urn:p:addr")), rules = MappingRules(propertyRules = Seq(
          ObjectMapping(id = "geo", sourcePath = UntypedPath.empty, target = Some(MappingTarget("urn:p:geo")), rules = MappingRules(propertyRules = Seq(
            DirectMapping("lat", UntypedPath("lat"), MappingTarget("urn:p:lat")))))))))))
    ))))
    val mergedRoot = spec.outputView.mergedRuleSchemata.head

    // The generators nested below the folded 'details' rule stay addressable
    spec.outputView.rulesAtTargetPath(mergedRoot.transformRule, UntypedPath.parse("<urn:p:addr>/<urn:p:geo>")) match {
      case RulesAtTargetPath.Rules(schemata) => schemata.map(_.transformRule.id.toString) mustBe Seq("geo")
      case other => fail(s"Expected the geo rule at the nested path, but got: $other")
    }
    // The inlined object rule reads its prefixed source path as a resource reference, not as text
    mergedRoot.inputSchema.typedPaths must contain(TypedPath(UntypedPath("details"), ValueType.URI, isAttribute = false))
  }

  it should "resolve a type that is declared inside an object rule without a target" in {
    val spec = TransformSpec(DatasetSelection("input"), RootMappingRule(MappingRules(propertyRules = Seq(
      ObjectMapping(id = "extra", sourcePath = UntypedPath.empty, target = None, rules = MappingRules(
        typeRules = Seq(TypeMapping(id = "extraType", typeUri = Uri("urn:t:Person"))),
        propertyRules = Seq(DirectMapping("name", UntypedPath("name"), MappingTarget("urn:p:name")))))
    ))))

    // The folded rule's type belongs to the merged entities, like its properties
    spec.outputView.ruleSchemataForTargetTypeOption(Uri("urn:t:Person")) mustBe defined
  }

  it should "fold an object rule whose target property is empty into its parent" in {
    val spec = TransformSpec(DatasetSelection("input"), RootMappingRule(MappingRules(propertyRules = Seq(
      DirectMapping("id", UntypedPath("ID"), MappingTarget("urn:p:id")),
      ObjectMapping(id = "extra", sourcePath = UntypedPath.empty, target = Some(MappingTarget("")), rules = MappingRules(propertyRules = Seq(
        DirectMapping("extraId", UntypedPath("ID"), MappingTarget("urn:p:extraId")))))
    ))))

    // An empty target property writes into the parent's entities, like a missing target (see collectSchemata)
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
