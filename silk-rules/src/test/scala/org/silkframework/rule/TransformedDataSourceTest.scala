package org.silkframework.rule


import org.silkframework.config.{PlainTask, Prefixes, Task}
import org.silkframework.dataset.{DataSource, Dataset, DatasetSpec}
import org.silkframework.execution.{EntityHolder, ExecutorRegistry}
import org.silkframework.execution.local.GenericEntityTable
import org.silkframework.entity.EntitySchema
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.plugins.dataset.csv.CsvDataset
import org.silkframework.runtime.activity.{TestPluginContextTrait, UserContext}
import org.silkframework.runtime.iterator.CloseableIterator
import org.silkframework.runtime.plugin.PluginContext
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
    val transformedDataSource = new TransformedDataSource(csvSource, entitySchema, mappingRule, transformTask(mappingRule))
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
    val entities = twoRuleSource.retrieve(addressSchema).entities.toSeq
    // Values, not entity counts: picking a single rule leaves the other rule's property empty everywhere
    entities.flatMap(_.values.head) mustBe Seq("1", "2", "3")
    entities.flatMap(_.values(1)) mustBe Seq("1", "2", "3")
  }

  it should "bound the result by the limit without under-delivering" in {
    // Two rules over three rows, so six entities are available in total. Four is the first limit that spans both rules.
    for(limit <- Seq(1, 4, 6, 7)) {
      withClue(s"limit $limit: ") {
        twoRuleSource.retrieve(addressSchema, Some(limit)).entities.toSeq must have size math.min(limit, 6)
      }
    }
  }

  it should "deliver the full limit even if one rule yields fewer entities than the others" in {
    // The first rule yields a single entity, the second yields three: four are available in total
    def source = new TransformedDataSource(new RecordingSource(csvSource, truncateFirstTo = Some(1)),
      rootInputSchema, twoAddressRules, transformTask(twoAddressRules))
    source.retrieve(addressSchema).entities.toSeq must have size 4 // The fixture really is asymmetric
    // Dividing the limit between the rules would starve the request of the entities the first rule cannot supply
    source.retrieve(addressSchema, Some(4)).entities.toSeq must have size 4
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

  it should "deliver the values of an object rule without a target at the path of its parent" in {
    val source = transformedSource(RootMappingRule(MappingRules(propertyRules = Seq(
      DirectMapping("id", UntypedPath("ID"), MappingTarget("urn:p:id")),
      ObjectMapping(id = "extra", sourcePath = UntypedPath.empty, target = None, rules = MappingRules(propertyRules = Seq(
        DirectMapping("extraId", UntypedPath("ID"), MappingTarget("urn:p:extraId")))))
    ))))
    val schema = EntitySchema(Uri(""), IndexedSeq(UntypedPath.parse("<urn:p:id>").asStringTypedPath,
      UntypedPath.parse("<urn:p:extraId>").asStringTypedPath))

    // The no-target rule writes into the root entities: one entity per source record, with both values
    val entities = source.retrieve(schema).entities.toSeq
    entities must have size 3
    entities.map(_.values.head) mustBe Seq(Seq("1"), Seq("2"), Seq("3"))
    entities.map(_.values(1)) mustBe Seq(Seq("1"), Seq("2"), Seq("3"))
  }

  it should "merge an object rule without a target into its parent at a nested path" in {
    val source = transformedSource(RootMappingRule(MappingRules(propertyRules = Seq(
      ObjectMapping(id = "address", sourcePath = UntypedPath.empty, target = Some(MappingTarget("urn:p:address")),
        rules = MappingRules(propertyRules = Seq(
          DirectMapping("zip", UntypedPath("ID"), MappingTarget("urn:p:zip")),
          ObjectMapping(id = "extra", sourcePath = UntypedPath.empty, target = None, rules = MappingRules(propertyRules = Seq(
            DirectMapping("extraZip", UntypedPath("ID"), MappingTarget("urn:p:extraZip")))))
        )))
    ))))
    val schema = EntitySchema(Uri(""), IndexedSeq(UntypedPath.parse("<urn:p:zip>").asStringTypedPath,
      UntypedPath.parse("<urn:p:extraZip>").asStringTypedPath), subPath = UntypedPath.parse("<urn:p:address>"))

    val entities = source.retrieve(schema).entities.toSeq
    entities must have size 3
    entities.map(_.values.head) mustBe Seq(Seq("1"), Seq("2"), Seq("3"))
    entities.map(_.values(1)) mustBe Seq(Seq("1"), Seq("2"), Seq("3"))
  }

  it should "prepend the source path of an object rule without a target to its inlined rules" in {
    val source = transformedSource(RootMappingRule(MappingRules(propertyRules = Seq(
      DirectMapping("id", UntypedPath("ID"), MappingTarget("urn:p:id")),
      ObjectMapping(id = "extra", sourcePath = UntypedPath("ID"), target = None, rules = MappingRules(propertyRules = Seq(
        DirectMapping("nested", UntypedPath.empty, MappingTarget("urn:p:nested")))))
    ))))
    val schema = EntitySchema(Uri(""), IndexedSeq(UntypedPath.parse("<urn:p:nested>").asStringTypedPath))

    // The child rule reads relative to the no-target rule's source path 'ID'
    val entities = source.retrieve(schema).entities.toSeq
    entities must have size 3
    entities.map(_.values.head) mustBe Seq(Seq("1"), Seq("2"), Seq("3"))
  }

  it should "deliver no entities for a sub path with a filter instead of failing" in {
    val schema = EntitySchema(Uri(""), IndexedSeq(UntypedPath.parse("<urn:p:billingZip>").asStringTypedPath),
      subPath = UntypedPath.parse("""<urn:p:address>[<urn:p:type> = "home"]"""))
    // Datasets accept filtered paths, so a transform input yields nothing rather than an error
    twoRuleSource.retrieve(schema).entities.toSeq mustBe empty
  }

  it should "fail if no rule generates the requested sub path" in {
    val source = transformedSource(RootMappingRule(MappingRules(propertyRules = Seq(
      DirectMapping("id", UntypedPath("ID"), MappingTarget("urn:p:id"))
    ))))
    val exception = the[BadUserInputException] thrownBy source.retrieve(addressSchema)
    exception.getMessage must include("<urn:p:address>")
  }

  it should "resolve a sub path that spans multiple nested rules" in {
    val source = transformedSource(twoLevelRule)
    val schema = EntitySchema(Uri(""), IndexedSeq(UntypedPath.parse("<urn:p:lat>").asStringTypedPath),
      subPath = UntypedPath.parse("<urn:p:address>/<urn:p:geo>"))
    source.retrieve(schema).entities.toSeq.flatMap(_.values.head) mustBe Seq("1", "2", "3")
  }

  it should "resolve a sub path relative to the rule the source was built on" in {
    // Mimics a source that was built for a selected type, i.e. on a nested rule rather than on the root rule
    val spec = TransformSpec(DatasetSelection("input"), twoLevelRule)
    val addressSchemata = spec.ruleSchemata.find(_.transformRule.id.toString == "address").get
    addressSchemata.outputSchema.subPath mustBe UntypedPath.parse("<urn:p:address>")
    val source = new TransformedDataSource(csvSource, addressSchemata.inputSchema, addressSchemata.transformRule,
      PlainTask("transform", spec))
    // Relative to the address rule 'geo' is a single hop, so the rule's own path has to be prepended to find it
    val schema = EntitySchema(Uri(""), IndexedSeq(UntypedPath.parse("<urn:p:lat>").asStringTypedPath),
      subPath = UntypedPath.parse("<urn:p:geo>"))
    source.retrieve(schema).entities.toSeq.flatMap(_.values.head) mustBe Seq("1", "2", "3")
  }

  it should "not deliver the entities of a sibling rule that shares the requested target path" in {
    // Billing and shipping both generate <urn:p:address>/<urn:p:geo>
    def addressGeoRule(id: String): ObjectMapping = {
      ObjectMapping(id = id, sourcePath = UntypedPath.empty, target = Some(MappingTarget("urn:p:address")),
        rules = MappingRules(propertyRules = Seq(
          ObjectMapping(id = id + "Geo", sourcePath = UntypedPath.empty, target = Some(MappingTarget("urn:p:geo")),
            rules = MappingRules(propertyRules = Seq(
              DirectMapping(id + "Lat", UntypedPath("ID"), MappingTarget("urn:p:" + id + "Lat"))))))))
    }
    val spec = TransformSpec(DatasetSelection("input"), RootMappingRule(MappingRules(propertyRules = Seq(
      addressGeoRule("billing"), addressGeoRule("shipping")))))
    val billingSchemata = spec.ruleSchemata.find(_.transformRule.id.toString == "billing").get
    val source = new TransformedDataSource(csvSource, billingSchemata.inputSchema, billingSchemata.transformRule,
      PlainTask("transform", spec))

    val schema = EntitySchema(Uri(""), IndexedSeq(UntypedPath.parse("<urn:p:billingLat>").asStringTypedPath),
      subPath = UntypedPath.parse("<urn:p:geo>"))
    // Only the billing tree belongs to this source, so the shipping geo entities are not delivered
    val entities = source.retrieve(schema).entities.toSeq
    entities must have size 3
    entities.flatMap(_.values.head) mustBe Seq("1", "2", "3")
  }

  it should "close the opened rule sources if the limit stops the iteration early" in {
    val recording = new RecordingSource(csvSource)
    val source = new TransformedDataSource(recording, rootInputSchema, twoAddressRules, transformTask(twoAddressRules))
    // A limit of four exhausts the first rule and stops inside the second, so both sources have been opened
    val entities = source.retrieve(addressSchema, Some(4)).entities
    entities.toSeq must have size 4
    entities.close()
    recording.retrieveCount mustBe 2
    recording.closeCount mustBe 2
  }

  /** The schema a downstream task requests for a nested rule reading `urn:p:address`. */
  private val addressSchema = EntitySchema(
    typeUri = Uri(""),
    typedPaths = IndexedSeq(UntypedPath.parse("<urn:p:billingZip>").asStringTypedPath,
      UntypedPath.parse("<urn:p:shippingZip>").asStringTypedPath),
    subPath = UntypedPath.parse("<urn:p:address>")
  )

  private val rootInputSchema = EntitySchema(Uri(""), typedPaths = IndexedSeq(UntypedPath("ID").asStringTypedPath))

  private def addressRule(id: String, zipProperty: String): ObjectMapping = {
    ObjectMapping(
      id = id,
      sourcePath = UntypedPath.empty,
      target = Some(MappingTarget("urn:p:address")),
      rules = MappingRules(propertyRules = Seq(DirectMapping(id + "Zip", UntypedPath("ID"), MappingTarget(zipProperty))))
    )
  }

  /** Two object rules with the same target property, e.g. a billing and a shipping address. */
  private def twoAddressRules: RootMappingRule = {
    RootMappingRule(MappingRules(propertyRules = Seq(
      addressRule("billing", "urn:p:billingZip"), addressRule("shipping", "urn:p:shippingZip")
    )))
  }

  /** A root rule with an object rule that itself holds a nested object rule. */
  private def twoLevelRule: RootMappingRule = {
    RootMappingRule(MappingRules(propertyRules = Seq(
      ObjectMapping(id = "address", sourcePath = UntypedPath.empty, target = Some(MappingTarget("urn:p:address")),
        rules = MappingRules(propertyRules = Seq(
          DirectMapping("zip", UntypedPath("ID"), MappingTarget("urn:p:zip")),
          ObjectMapping(id = "geo", sourcePath = UntypedPath.empty, target = Some(MappingTarget("urn:p:geo")),
            rules = MappingRules(propertyRules = Seq(DirectMapping("lat", UntypedPath("ID"), MappingTarget("urn:p:lat")))))
        )))
    )))
  }

  private def csvSource: DataSource = ExecutorRegistry.access(CsvDataset(inMemoryResource)).source

  private def transformTask(mappingRule: RootMappingRule): Task[TransformSpec] = {
    PlainTask("transform", TransformSpec(DatasetSelection("input"), mappingRule))
  }

  private def twoRuleSource: TransformedDataSource = transformedSource(twoAddressRules)

  private def transformedSource(mappingRule: RootMappingRule): TransformedDataSource = {
    // Built on the merged root schemata, like TransformTaskUtils.asDataSource builds it
    val task = transformTask(mappingRule)
    val rootSchemata = task.data.outputView.ruleSchemataForTargetTypeOrPrimary(Uri(""))
    new TransformedDataSource(csvSource, rootSchemata.inputSchema, rootSchemata.transformRule, task)
  }

  /** Counts retrievals and closes, so that leaked rule sources become visible. */
  private class RecordingSource(delegate: DataSource, truncateFirstTo: Option[Int] = None) extends DataSource {
    var retrieveCount = 0
    var closeCount = 0

    override def underlyingTask: Task[DatasetSpec[Dataset]] = delegate.underlyingTask

    override def retrieveTypes(limit: Option[Int])
                              (implicit userContext: UserContext, prefixes: Prefixes): Iterable[(String, Double)] = {
      delegate.retrieveTypes(limit)
    }

    override def retrievePaths(typeUri: Uri, depth: Int, limit: Option[Int])
                              (implicit userContext: UserContext, prefixes: Prefixes) = {
      delegate.retrievePaths(typeUri, depth, limit)
    }

    override def retrieve(entitySchema: EntitySchema, limit: Option[Int])(implicit context: PluginContext): EntityHolder = {
      retrieveCount += 1
      val retrieved = delegate.retrieve(entitySchema, limit)(context).entities
      val entities = if(retrieveCount == 1) truncateFirstTo.map(retrieved.take).getOrElse(retrieved) else retrieved
      GenericEntityTable(CloseableIterator(entities, () => { closeCount += 1; entities.close() }), entitySchema, underlyingTask)
    }

    override def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri])(implicit context: PluginContext): EntityHolder = {
      delegate.retrieveByUri(entitySchema, entities)(context)
    }
  }
}
