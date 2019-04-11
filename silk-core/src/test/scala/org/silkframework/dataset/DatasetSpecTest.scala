package org.silkframework.dataset

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.entity.StringValueType
import org.silkframework.runtime.activity.UserContext

class DatasetSpecTest extends FlatSpec with Matchers {

  behavior of "DatasetSpec"

  it should "generate URI properties" in {
    val dataset = MockDataset()
    var entities = Map[String, Seq[Seq[String]]]()
    dataset.writeEntityFn = (uri: String, values: Seq[Seq[String]]) => entities += ((uri, values))
    val datasetSpec = DatasetSpec(dataset, uriProperty = Some("urn:schema:URI"))
    implicit val userContext: UserContext = UserContext.Empty
    val sink = datasetSpec.entitySink
    sink.openTable("someType", Seq(TypedProperty("existingProperty", StringValueType, isBackwardProperty = false)))
    sink.writeEntity("entityUri", Seq(Seq("someValue")))
    sink.closeTable()
    sink.close()

    entities("entityUri") shouldBe Seq(Seq("entityUri"), Seq("someValue"))
  }

}
