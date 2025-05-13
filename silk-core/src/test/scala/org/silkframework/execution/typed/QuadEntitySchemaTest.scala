package org.silkframework.execution.typed

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.dataset.rdf._
import org.silkframework.runtime.plugin.PluginContext

class QuadEntitySchemaTest extends AnyFlatSpec with Matchers {

  behavior of "QuadEntitySchema"

  implicit val pluginContext: PluginContext = PluginContext.empty

  it should "serialize a Quad to an Entity and back" in {
    val quad = Quad(
      subject = Resource("http://example.org/subject"),
      predicate = Resource("http://example.org/predicate"),
      objectVal = PlainLiteral("objectValue"),
      context = Some(Resource("http://example.org/graph"))
    )

    // Serialize and deserialize
    val entity = QuadEntitySchema.toEntity(quad)
    val deserializedQuad = QuadEntitySchema.fromEntity(entity)

    // Validate Quad
    deserializedQuad.subject shouldBe quad.subject
    deserializedQuad.predicate shouldBe quad.predicate
    deserializedQuad.objectVal shouldBe quad.objectVal
    deserializedQuad.context shouldBe quad.context
  }

  it should "handle Quads with different object types" in {
    val quads = Seq(
      Quad(Resource("http://example.org/subject"), Resource("http://example.org/predicate"), Resource("http://example.org/object"), None),
      Quad(Resource("http://example.org/subject"), Resource("http://example.org/predicate"), BlankNode("blankNode"), None),
      Quad(Resource("http://example.org/subject"), Resource("http://example.org/predicate"), LanguageLiteral("objectValue", "en"), None),
      Quad(Resource("http://example.org/subject"), Resource("http://example.org/predicate"), DataTypeLiteral("objectValue", "http://example.org/datatype"), None)
    )

    quads.foreach { quad =>
      val entity = QuadEntitySchema.toEntity(quad)
      val deserializedQuad = QuadEntitySchema.fromEntity(entity)
      deserializedQuad shouldBe quad
    }
  }
}