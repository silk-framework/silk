package org.silkframework.workspace.changes

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config.MetaData
import org.silkframework.entity.ValueType
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.rule.input.{PathInput, TransformInput}
import org.silkframework.rule.plugins.transformer.combine.ConcatTransformer
import org.silkframework.rule.plugins.transformer.normalize.LowerCaseTransformer
import org.silkframework.rule.plugins.transformer.replace.RegexReplaceTransformer
import org.silkframework.rule.plugins.transformer.value.ConstantTransformer
import org.silkframework.rule.{ComplexMapping, DirectMapping, MappingRules, MappingTarget, NodePosition, ObjectMapping, PatternUriMapping, RuleLayout, TransformRule, TypeMapping}

/** What a mapping rule update lists as details: the editor fields that differ, an operator tree as a formula. */
class MappingRuleDiffTest extends AnyFlatSpec with Matchers {

  behavior of "mapping rule update details"

  private val name = DirectMapping(id = "name", sourcePath = UntypedPath("name"), mappingTarget = MappingTarget("http://example.org/name"))

  private def path(name: String): PathInput = PathInput(id = name, path = UntypedPath(name))

  private val lower = ComplexMapping(id = "name", operator = TransformInput("lower", LowerCaseTransformer(), IndexedSeq(path("name"))), target = name.target)

  private def details(before: TransformRule, after: TransformRule): Seq[String] = {
    UpdateMapping("transform", before, after).details.map(_.describe)
  }

  it should "list the changed fields of a value mapping" in {
    details(name, name) shouldBe empty
    details(name, name.copy(sourcePath = UntypedPath("fullName"))) shouldBe Seq("Value path 'name' → 'fullName'")
    val retargeted = name.copy(mappingTarget = MappingTarget("http://example.org/label", ValueType.INTEGER, isAttribute = true))
    details(name, retargeted) shouldBe Seq("Target property 'http://example.org/name' → 'http://example.org/label'",
      "Data type 'String' → 'Integer'", "Single value 'false' → 'true'")
    val renamed = name.copy(id = "fullName", metaData = MetaData(Some("Full name"), Some("The full name")))
    details(name, renamed) shouldBe Seq("Label '' → 'Full name'", "Description '' → 'The full name'", "Id 'name' → 'fullName'")
    UpdateMapping("transform", name, renamed).describe shouldBe "Updated mapping rule 'Full name' in transform 'transform': " +
      "Label '' → 'Full name', Description '' → 'The full name', Id 'name' → 'fullName'"
  }

  it should "show an operator tree as a formula" in {
    details(name, lower) shouldBe Seq("Value formula 'name' → 'lowerCase(name)'")
    // Non-default parameters in brackets, constants quoted and escaped
    val full = TransformInput("concat", ConcatTransformer(glue = " "), IndexedSeq(path("first"),
      TransformInput("dash", ConstantTransformer("\"-\""), IndexedSeq.empty),
      TransformInput("clean", RegexReplaceTransformer(regex = "\\s+", replace = " "), IndexedSeq(path("last")))))
    details(lower, lower.copy(operator = full)) shouldBe
      Seq("""Value formula 'lowerCase(name)' → 'concat[glue=" "](first, "\"-\"", regexReplace[regex="\s+", replace=" "](last))'""")
    // A moved operator is an editor-only change; a long formula is cut
    details(lower, lower.copy(layout = RuleLayout(Map("lower" -> NodePosition(10, 10))))) shouldBe Seq("Editor layout changed")
    val long = ComplexMapping(id = "name", operator = path("x" * 250), target = name.target)
    details(lower, long).head should endWith("→ '" + "x" * 200 + "…'")
  }

  it should "describe an added rule by its formula and the nested rules it brings" in {
    AddMapping("transform", "root", lower).describe shouldBe
      "Added value mapping 'name' (lowerCase(name) → http://example.org/name) under 'root' in transform 'transform'"
    AddMapping("transform", "root", name).details shouldBe empty
    val address = ObjectMapping(id = "address", sourcePath = UntypedPath("address"), rules = MappingRules(propertyRules = Seq(name)))
    val outer = ObjectMapping(id = "outer", sourcePath = UntypedPath("outer"), rules = MappingRules(propertyRules = Seq(address)))
    AddMapping("transform", "root", outer).describe shouldBe "Added object mapping 'outer' (outer → http://www.w3.org/2002/07/owl#sameAs) " +
      "under 'root' in transform 'transform': Nested rule 'address' added, Nested rule 'name' added"
  }

  it should "list the changed fields and the nested rules of an object mapping" in {
    val address = ObjectMapping(id = "address", sourcePath = UntypedPath("address"), rules = MappingRules(propertyRules = Seq(name)))
    val moved = address.copy(sourcePath = UntypedPath("home"), rules = MappingRules(uriRule = Some(PatternUriMapping("uri", "urn:{id}")),
      propertyRules = Seq(name.copy(sourcePath = UntypedPath("fullName")))))
    details(address, moved) shouldBe
      Seq("Value path 'address' → 'home'", "Nested rule 'uri' added", "Nested rule 'name' / Value path 'name' → 'fullName'")
    details(moved, address) shouldBe
      Seq("Value path 'home' → 'address'", "Nested rule 'name' / Value path 'fullName' → 'name'", "Nested rule 'uri' removed")
    // A nested container counts by its own fields; a change deeper down is listed for the rule it happened in
    val outer = ObjectMapping(id = "outer", rules = MappingRules(propertyRules = Seq(address)))
    details(outer, outer.copy(rules = MappingRules(propertyRules = Seq(moved)))) shouldBe
      Seq("Nested rule 'address' / Value path 'address' → 'home'", "Nested rule 'uri' added", "Nested rule 'name' / Value path 'name' → 'fullName'")
    // A change without a visible field, such as a new operator id, is named only
    val retagged = address.copy(rules = MappingRules(propertyRules = Seq(name.copy(inputId = Some("other")))))
    details(address, retagged) shouldBe Seq("Nested rule 'name' changed")
  }

  it should "list the type URI, the URI pattern and a changed kind" in {
    details(TypeMapping("type", "http://example.org/Person"), TypeMapping("type", "http://example.org/Human")) shouldBe
      Seq("Type 'http://example.org/Person' → 'http://example.org/Human'")
    details(PatternUriMapping("uri", "urn:{id}"), PatternUriMapping("uri", "urn:{name}")) shouldBe Seq("URI pattern 'urn:{id}' → 'urn:{name}'")
    details(name, TypeMapping("name")) shouldBe Seq("Kind 'value mapping' → 'type mapping'")
  }
}
