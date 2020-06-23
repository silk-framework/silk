package org.silkframework.rule

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.entity.EntitySchema
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.plugins.dataset.csv.CsvDataset
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.{InMemoryResourceManager, WritableResource}
import org.silkframework.util.Uri

class TransformedDataSourceTest extends FlatSpec with MustMatchers {
  behavior of "Transformed Data Source"

  implicit val userContext: UserContext = UserContext.Empty

  val inMemoryResource: WritableResource = {
    val manager = InMemoryResourceManager()
    val resource = manager.get("temp.csv")
    resource.writeString("ID\n1\n2\n3")
    resource
  }

  it should "retrieve by URI on the transformed entities" in {
    val csvDataset = CsvDataset(inMemoryResource)
    val entitySchema = EntitySchema(Uri(""), typedPaths = IndexedSeq(UntypedPath("ID")).map(_.asStringTypedPath))
    val entities = csvDataset.source.retrieve(entitySchema).entities
    val entityUris = entities.map(_.uri.toString).toSet
    val mappingRule = RootMappingRule(MappingRules(PatternUriMapping(pattern = "http://entity/{ID}"),
      DirectMapping(sourcePath = UntypedPath("ID"), mappingTarget = MappingTarget("ID"))))
    val transformedDataSource = new TransformedDataSource(csvDataset.source, entitySchema, mappingRule)
    val transformedEntities = transformedDataSource.retrieve(entitySchema).entities
    val transformedUris = transformedEntities.map(_.uri.toString).toSet
    entityUris must have size 3
    transformedUris must have size 3
    entityUris.intersect(transformedUris) must have size 0
    // Source entities can be retrieved from source via source entity URIs
    csvDataset.source.retrieveByUri(entitySchema, entities = Seq(Uri(entityUris.head))).entities must have size 1
    // Transformed entities cannot be retrieved via source entity URIs
    transformedDataSource.retrieveByUri(entitySchema, entities = Seq(Uri(entityUris.head))).entities must have size 0
    // Transformed entities can be retrieved via transformed entity URIs
    transformedDataSource.retrieveByUri(entitySchema, entities = Seq(Uri(transformedUris.head))).entities must have size 1
  }
}
