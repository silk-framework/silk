package org.silkframework.dataset

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.config.Prefixes
import org.silkframework.entity.StringValueType
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.{EmptyResourceManager, ResourceManager}
import org.silkframework.runtime.validation.ValidationException

class DatasetSpecTest extends FlatSpec with Matchers {

  behavior of "DatasetSpec"

  implicit val prefixes: Prefixes = Prefixes.empty
  implicit val emptyResourceManager: ResourceManager = EmptyResourceManager()

  it should "generate URI properties" in {
    val dataset = MockDataset()
    var entities = Map[String, Seq[Seq[String]]]()
    dataset.writeEntityFn = (uri: String, values: Seq[Seq[String]]) => entities += ((uri, values))
    val datasetSpec = DatasetSpec(dataset, uriProperty = Some("urn:schema:URI"))
    implicit val userContext: UserContext = UserContext.Empty
    implicit val prefixes: Prefixes = Prefixes.empty
    val sink = datasetSpec.entitySink
    sink.openTable("someType", Seq(TypedProperty("existingProperty", StringValueType, isBackwardProperty = false)))
    sink.writeEntity("entityUri", Seq(Seq("someValue")))
    sink.closeTable()
    sink.close()

    entities("entityUri") shouldBe Seq(Seq("entityUri"), Seq("someValue"))
  }

  it should "support retrieving and updating properties" in {
    val dataset = DatasetSpec(MockDataset(name = "initial name"))

    // Check if we can retrieve the current plugin parameters
    dataset.properties should contain ("name" -> "initial name")

    // Check if we can update the plugin parameters
    val updatedDataset = dataset.withProperties(Map("name" -> "updated name"))
    updatedDataset.properties should not contain ("name" -> "initial name")
    updatedDataset.properties should contain ("name" -> "updated name")
  }

  it should "throw an error if a parameter should be updated that does not exist" in {
    intercept[ValidationException] {
      DatasetSpec(MockDataset()).withProperties(Map("invalidParameter" -> "value"))
    }
  }

}
