package org.silkframework.rule.evaluation

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.entity.{Entity, EntitySchema, Link}
import org.silkframework.util.{Uri, XmlSerializationHelperTrait}

class ReferenceEntitiesTest extends FlatSpec with MustMatchers with XmlSerializationHelperTrait {
  behavior of "Reference entities"

  it should "serialize and deserialize correctly" in {
    val entitySchema = EntitySchema(Uri("type1"), typedPaths = IndexedSeq(UntypedPath.parse("/a/b").asStringTypedPath))
    val referenceEntities = ReferenceEntities(
      sourceEntities = Map(
        "sourceEntity" -> Entity(Uri("sourceEntity"), IndexedSeq(Seq("value")), entitySchema)
      ),
      targetEntities = Map(
        "tentity1" -> Entity(Uri("tentity1"), IndexedSeq(Seq("value")), entitySchema),
        "tentity2" -> Entity(Uri("tentity2"), IndexedSeq(Seq("value")), entitySchema),
        "tentity3" -> Entity(Uri("tentity3"), IndexedSeq(Seq("value")), entitySchema),
        "tentity4" -> Entity(Uri("tentity4"), IndexedSeq(Seq("value")), entitySchema),
        "tentity5" -> Entity(Uri("tentity5"), IndexedSeq(Seq("value")), entitySchema)
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
  }
}
