package org.silkframework.dataset


import org.silkframework.config.Prefixes
import org.silkframework.entity.ValueType
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{ParameterValues, PluginContext, TestPluginContext}
import org.silkframework.runtime.validation.ValidationException
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DatasetSpecTest extends AnyFlatSpec with Matchers {

  behavior of "DatasetSpec"

  implicit val prefixes: Prefixes = Prefixes.empty
  implicit val pluginContext: PluginContext = TestPluginContext()

  it should "generate URI properties" in {
    val dataset = MockDataset()
    var entities = Map[String, Seq[Seq[String]]]()
    dataset.writeEntityFn = (uri: String, values: Seq[Seq[String]]) => entities += ((uri, values))
    val datasetSpec = DatasetSpec(dataset, uriAttribute = Some("urn:schema:URI"))
    implicit val userContext: UserContext = UserContext.Empty
    implicit val prefixes: Prefixes = Prefixes.empty
    val sink = datasetSpec.entitySink
    sink.openTable("", Seq(TypedProperty("existingProperty", ValueType.STRING, isBackwardProperty = false)), singleEntity = false)
    sink.writeEntity("entityUri", IndexedSeq(Seq("someValue")))
    sink.closeTable()
    sink.close()

    entities("entityUri") shouldBe Seq(Seq("entityUri"), Seq("someValue"))
  }

  it should "add a type property for RDF datasets, if it is not already there" in {
    val dataset = MockDataset()
    dataset.characteristicsVal = DatasetCharacteristics(typedEntities = true)
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

    entities("entityUri") shouldBe Seq(Seq("entityUri"), Seq("someType"), Seq("someValue"))
  }

  it should "support retrieving and updating properties" in {
    val dataset = DatasetSpec(MockDataset(name = "initial name"))

    // Check if we can retrieve the current plugin parameters
    dataset.parameters.toStringMap should contain ("name" -> "initial name")

    // Check if we can update the plugin parameters
    val updatedDataset = dataset.withParameters(ParameterValues.fromStringMap(Map("name" -> "updated name")))
    updatedDataset.parameters.toStringMap should not contain ("name" -> "initial name")
    updatedDataset.parameters.toStringMap should contain ("name" -> "updated name")
  }

  it should "throw an error if a parameter should be updated that does not exist" in {
    intercept[ValidationException] {
      DatasetSpec(MockDataset()).withParameters(ParameterValues.fromStringMap(Map("invalidParameter" -> "value")))
    }
  }

}
