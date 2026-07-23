package org.silkframework.rule

import org.scalatest.OptionValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.entity.ValueType
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.util.Identifier

class MappingRuleTreePruningTest extends AnyFlatSpec with Matchers with OptionValues {

  behavior of "MappingRuleTreePruning"

  // A small hierarchical mapping:
  //   root  (uri: rootUri, type: rootType=Person)
  //     ├─ name      (value -> rdfs:label)
  //     └─ address   (object, uri: addrUri, type: addrType=Address)
  //          └─ street (value -> ex:street)
  private val street = DirectMapping(id = "street", sourcePath = UntypedPath("street"),
    mappingTarget = MappingTarget("http://ex/street"))
  private val addrUri = PatternUriMapping(id = "addrUri", pattern = "http://ex/addr/{street}")
  private val addrType = TypeMapping(id = "addrType", typeUri = "http://ex/Address")
  private val address = ObjectMapping(
    id = "address",
    sourcePath = UntypedPath("address"),
    target = Some(MappingTarget("http://ex/address", ValueType.URI)),
    rules = MappingRules(uriRule = Some(addrUri), typeRules = Seq(addrType), propertyRules = Seq(street))
  )
  private val name = DirectMapping(id = "name", sourcePath = UntypedPath("name"),
    mappingTarget = MappingTarget("http://www.w3.org/2000/01/rdf-schema#label"))
  private val rootUri = PatternUriMapping(id = "rootUri", pattern = "http://ex/{id}")
  private val rootType = TypeMapping(id = "rootType", typeUri = "http://ex/Person")
  private val root = RootMappingRule(MappingRules(
    uriRule = Some(rootUri), typeRules = Seq(rootType), propertyRules = Seq(name, address)))

  private def ids(ruleIds: String*): Set[Identifier] = ruleIds.map(Identifier(_)).toSet

  it should "keep a selected leaf value rule, dropping the (unselected) ancestor's type and siblings" in {
    val pruned = MappingRuleTreePruning.pruneRoot(root, ids("name")).value
    pruned.rules.uriRule.map(_.id.toString) shouldBe Some("rootUri") // subject IRI retained
    pruned.rules.typeRules shouldBe empty                            // root not selected -> no rdf:type
    pruned.rules.propertyRules.map(_.id.toString) shouldBe Seq("name")
  }

  it should "keep the whole subtree (type + label + children) when an object node is selected" in {
    val pruned = MappingRuleTreePruning.pruneRoot(root, ids("address")).value
    pruned.rules.propertyRules.map(_.id.toString) shouldBe Seq("address")
    val prunedAddress = pruned.rules.propertyRules.head.asInstanceOf[ObjectMapping]
    prunedAddress.rules.uriRule.map(_.id.toString) shouldBe Some("addrUri")
    prunedAddress.rules.typeRules.map(_.id.toString) shouldBe Seq("addrType")
    prunedAddress.rules.propertyRules.map(_.id.toString) shouldBe Seq("street")
  }

  it should "keep a deep leaf as a chain of shells, retaining only the URI rules along the path" in {
    val pruned = MappingRuleTreePruning.pruneRoot(root, ids("street")).value
    pruned.rules.uriRule.map(_.id.toString) shouldBe Some("rootUri")
    pruned.rules.typeRules shouldBe empty
    val prunedAddress = pruned.rules.propertyRules.head.asInstanceOf[ObjectMapping]
    prunedAddress.rules.uriRule.map(_.id.toString) shouldBe Some("addrUri")
    prunedAddress.rules.typeRules shouldBe empty                     // address not selected -> no rdf:type
    prunedAddress.rules.propertyRules.map(_.id.toString) shouldBe Seq("street")
  }

  it should "emit a selected type rule even when the containing object is not selected" in {
    val pruned = MappingRuleTreePruning.pruneRoot(root, ids("addrType")).value
    val prunedAddress = pruned.rules.propertyRules.head.asInstanceOf[ObjectMapping]
    prunedAddress.rules.typeRules.map(_.id.toString) shouldBe Seq("addrType")
    prunedAddress.rules.uriRule.map(_.id.toString) shouldBe Some("addrUri")
    prunedAddress.rules.propertyRules shouldBe empty
  }

  it should "return the whole tree when the root is selected" in {
    val pruned = MappingRuleTreePruning.pruneRoot(root, ids("root")).value
    pruned shouldBe root
  }

  it should "keep multiple selected leaves under different parents at different depths" in {
    val pruned = MappingRuleTreePruning.pruneRoot(root, ids("name", "street")).value
    pruned.rules.propertyRules.map(_.id.toString) should contain theSameElementsAs Seq("name", "address")
    val prunedAddress = pruned.rules.propertyRules.collectFirst { case o: ObjectMapping => o }.value
    prunedAddress.rules.propertyRules.map(_.id.toString) shouldBe Seq("street")
  }

  it should "return None when nothing produces output" in {
    MappingRuleTreePruning.pruneRoot(root, ids("doesNotExist")) shouldBe None
    MappingRuleTreePruning.pruneRoot(root, Set.empty[Identifier]) shouldBe None
  }
}
