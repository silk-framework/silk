package org.silkframework.dataset

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.config.Prefixes
import org.silkframework.entity.ValueType
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.validation.ValidationException

class DatasetSpecTest extends FlatSpec with Matchers {

  behavior of "DatasetSpec"

  implicit val prefixes: Prefixes = Prefixes.empty
  implicit val pluginContext: PluginContext = PluginContext.empty

  it should "generate URI properties" in {
    val dataset = MockDataset()
    var entities = Map[String, Seq[Seq[String]]]()
    dataset.writeEntityFn = (uri: String, values: Seq[Seq[String]]) => entities += ((uri, values))
    val datasetSpec = DatasetSpec(dataset, uriAttribute = Some("urn:schema:URI"))
    implicit val userContext: UserContext = UserContext.Empty
    implicit val prefixes: Prefixes = Prefixes.empty
    val sink = datasetSpec.entitySink
    sink.openTable("someType", Seq(TypedProperty("existingProperty", ValueType.STRING, isBackwardProperty = false)), singleEntity = false)
    sink.writeEntity("entityUri", IndexedSeq(Seq("someValue")))
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
