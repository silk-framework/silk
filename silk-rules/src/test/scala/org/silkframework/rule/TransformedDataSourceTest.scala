package org.silkframework.rule


import org.silkframework.config.{PlainTask, Prefixes}
import org.silkframework.execution.ExecutorRegistry
import org.silkframework.entity.EntitySchema
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.plugins.dataset.csv.CsvDataset
import org.silkframework.runtime.activity.{TestPluginContextTrait, UserContext}
import org.silkframework.runtime.resource.{InMemoryResourceManager, WritableResource}
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.util.Uri
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class TransformedDataSourceTest extends AnyFlatSpec with Matchers with TestPluginContextTrait {
  behavior of "Transformed Data Source"

  val inMemoryResource: WritableResource = {
    val manager = InMemoryResourceManager()
    val resource = manager.get("temp.csv")
    resource.writeString("ID\n1\n2\n3")
    resource
  }

  it should "retrieve by URI on the transformed entities" in {
    val csvDataset = CsvDataset(inMemoryResource)
    val csvSource = ExecutorRegistry.access(csvDataset).source
    val entitySchema = EntitySchema(Uri(""), typedPaths = IndexedSeq(UntypedPath("ID")).map(_.asStringTypedPath))
    val entities = csvSource.retrieve(entitySchema).entities
    val entityUris = entities.map(_.uri.toString).toSet
    val mappingRule = RootMappingRule(MappingRules(PatternUriMapping(pattern = "http://entity/{ID}"),
      DirectMapping(sourcePath = UntypedPath("ID"), mappingTarget = MappingTarget("ID"))))
    val transformedDataSource = new TransformedDataSource(csvSource, entitySchema, mappingRule, PlainTask("dataset", null))
    val transformedEntities = transformedDataSource.retrieve(entitySchema).entities
    val transformedUris = transformedEntities.map(_.uri.toString).toSet
    entityUris must have size 3
    transformedUris must have size 3
    entityUris.intersect(transformedUris) must have size 0
    // Source entities can be retrieved from source via source entity URIs
    csvSource.retrieveByUri(entitySchema, entities = Seq(Uri(entityUris.head))).entities must have size 1
    // Transformed entities cannot be retrieved via source entity URIs
    transformedDataSource.retrieveByUri(entitySchema, entities = Seq(Uri(entityUris.head))).entities must have size 0
    // Transformed entities can be retrieved via transformed entity URIs
    transformedDataSource.retrieveByUri(entitySchema, entities = Seq(Uri(transformedUris.head))).entities must have size 1
  }

  it should "deliver the values of every rule that generates the requested sub path" in {
    // Two object rules with the same target property, e.g. a billing and a shipping address
    val source = transformedSource(RootMappingRule(MappingRules(propertyRules = Seq(
      addressRule("billing", "urn:p:billingZip"), addressRule("shipping", "urn:p:shippingZip")
    ))))

    val entities = source.retrieve(addressSchema).entities.toSeq
    // Values, not entity counts: picking a single rule leaves the other rule's property empty everywhere
    entities.flatMap(_.values.head) mustBe Seq("1", "2", "3")
    entities.flatMap(_.values(1)) mustBe Seq("1", "2", "3")
  }

  it should "share the limit between the rules instead of letting the first exhaust it" in {
    val source = transformedSource(RootMappingRule(MappingRules(propertyRules = Seq(
      addressRule("billing", "urn:p:billingZip"), addressRule("shipping", "urn:p:shippingZip")
    ))))

    val entities = source.retrieve(addressSchema, Some(2)).entities.toSeq
    entities must have size 2 // The limit bounds the whole result
    entities.flatMap(_.values.head) mustBe Seq("1") // and neither rule is starved by the other
    entities.flatMap(_.values(1)) mustBe Seq("1")
  }

  it should "deliver no entities for a path that is generated as a value" in {
    // A downstream object mapping may read a path the upstream generates as a literal: it holds no entities
    val source = transformedSource(RootMappingRule(MappingRules(propertyRules = Seq(
      DirectMapping("address", UntypedPath("ID"), MappingTarget("urn:p:address"))
    ))))
    source.retrieve(addressSchema).entities.toSeq mustBe empty
  }

  it should "deliver entities without values for an object rule that has no properties of its own" in {
    val source = transformedSource(RootMappingRule(MappingRules(propertyRules = Seq(
      ObjectMapping(id = "address", sourcePath = UntypedPath.empty,
        target = Some(MappingTarget("urn:p:address")), rules = MappingRules())
    ))))

    val entities = source.retrieve(addressSchema).entities.toSeq
    entities must have size 3 // One per CSV row
    entities.flatMap(_.values.flatten) mustBe empty
  }

  it should "fail if no rule generates the requested sub path" in {
    val source = transformedSource(RootMappingRule(MappingRules(propertyRules = Seq(
      DirectMapping("id", UntypedPath("ID"), MappingTarget("urn:p:id"))
    ))))
    val exception = the[BadUserInputException] thrownBy source.retrieve(addressSchema)
    exception.getMessage must include("<urn:p:address>")
  }

  /** The schema a downstream task requests for a nested rule reading `urn:p:address`. */
  private val addressSchema = EntitySchema(
    typeUri = Uri(""),
    typedPaths = IndexedSeq(UntypedPath.parse("<urn:p:billingZip>").asStringTypedPath,
      UntypedPath.parse("<urn:p:shippingZip>").asStringTypedPath),
    subPath = UntypedPath.parse("<urn:p:address>")
  )

  private def addressRule(id: String, zipProperty: String): ObjectMapping = {
    ObjectMapping(
      id = id,
      sourcePath = UntypedPath.empty,
      target = Some(MappingTarget("urn:p:address")),
      rules = MappingRules(propertyRules = Seq(DirectMapping(id + "Zip", UntypedPath("ID"), MappingTarget(zipProperty))))
    )
  }

  private def transformedSource(mappingRule: RootMappingRule): TransformedDataSource = {
    val csvSource = ExecutorRegistry.access(CsvDataset(inMemoryResource)).source
    val inputSchema = EntitySchema(Uri(""), typedPaths = IndexedSeq(UntypedPath("ID").asStringTypedPath))
    new TransformedDataSource(csvSource, inputSchema, mappingRule,
      PlainTask("transform", TransformSpec(DatasetSelection("input"), mappingRule)))
  }
}
