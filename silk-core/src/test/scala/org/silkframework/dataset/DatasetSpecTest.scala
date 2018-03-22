package org.silkframework.dataset

import org.scalatest.{FlatSpec, ShouldMatchers}
import org.silkframework.entity.StringValueType

class DatasetSpecTest extends FlatSpec with ShouldMatchers {

  behavior of "DatasetSpec"

  it should "generate URI properties" in {
    val dataset = MockDataset()
    var entities = Map[String, Seq[Seq[String]]]()
    dataset.writeEntityFn = (uri: String, values: Seq[Seq[String]]) => entities += ((uri, values))
    val datasetSpec = DatasetSpec(dataset, uriProperty = Some("urn:schema:URI"))

    val sink = datasetSpec.entitySink
    sink.openTable("someType", Seq(TypedProperty("existingProperty", StringValueType, isBackwardProperty = false)))
    sink.writeEntity("entityUri", Seq(Seq("someValue")))
    sink.closeTable()
    sink.close()

    entities("entityUri") shouldBe Seq(Seq("entityUri"), Seq("someValue"))
  }

}
