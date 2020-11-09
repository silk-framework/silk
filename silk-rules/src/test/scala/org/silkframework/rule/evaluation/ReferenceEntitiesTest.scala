package org.silkframework.rule.evaluation

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.entity.{Entity, EntitySchema, Link}
import org.silkframework.util.{Uri, XmlSerializationHelperTrait}

class ReferenceEntitiesTest extends FlatSpec with MustMatchers with XmlSerializationHelperTrait {
  behavior of "Reference entities"

  it should "serialize and deserialize correctly" in {
    val sourceSchema = EntitySchema(Uri("type1"), typedPaths = IndexedSeq(UntypedPath.parse("/a/b").asStringTypedPath))
    val targetSchema = EntitySchema(Uri("type1"), typedPaths = IndexedSeq("/a/b", "c").map(UntypedPath.parse(_).asStringTypedPath))
    val targetDefaultValues = IndexedSeq(Seq("value"), Seq())
    val referenceEntities = ReferenceEntities(
      sourceEntities = Map(
        "sourceEntity" -> Entity(Uri("sourceEntity"), IndexedSeq(Seq("value")), sourceSchema)
      ),
      targetEntities = Map(
        "tentity1" -> Entity(Uri("tentity1"), IndexedSeq(Seq("value <>"), Seq("üöä &nbsp;")), targetSchema),
        "tentity2" -> Entity(Uri("tentity2"), targetDefaultValues, targetSchema),
        "tentity3" -> Entity(Uri("tentity3"), targetDefaultValues, targetSchema),
        "tentity4" -> Entity(Uri("tentity4"), targetDefaultValues, targetSchema),
        "tentity5" -> Entity(Uri("tentity5"), targetDefaultValues, targetSchema)
      ),
      positiveLinks = Set(
        Link("sourceEntity", "tentity1")
      ),
      negativeLinks = Set(
        Link("sourceEntity", "tentity2"),
        Link("sourceEntity", "tentity4"),
        Link("sourceEntity", "tentity5")
      ),
      unlabeledLinks = Set(
        Link("sourceEntity", "tentity3")
      )
    )
    testRoundTripSerializationStreaming(referenceEntities)
    testRoundTripSerializationStreaming(ReferenceEntities())
  }
}
