package org.silkframework.entity

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.entity.Entity.EntitySerializer.{deserialize, serialize}
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.util.Uri

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, DataInputStream, DataOutputStream}


class EntityTest extends AnyFlatSpec with Matchers {

  val schema = EntitySchema(typeUri = Uri(""), typedPaths = IndexedSeq(UntypedPath("path1").asStringTypedPath, UntypedPath("path2").asStringTypedPath), filter = Restriction.empty)

  val entity1 = Entity("http://silk-framework.com/example", IndexedSeq(Seq("value1", "value2"), Seq("value3")), schema)

  val entity2 = Entity("http://silk-framework.com/example", IndexedSeq(Seq("dÃ©clarÃ©s", "v2"), Seq("v3")), schema)

  "Entity" should "be serializable" in {
    serialized(entity1) should be (entity1)
  }

  "Entity" should "be serializable if it contains non-ASCII characters" in {
    serialized(entity2) should be (entity2)
  }

  "Entity" should "be serializable if it contains big literals" in {
    val largeString: String = "text text " * 7000
    val largeEntity = entity2.copy(uri = largeString, values = IndexedSeq(Seq(largeString), Seq(largeString)))
    largeString.length shouldBe 70000
    serialized(largeEntity) should be (largeEntity)
  }

  def serialized(entity: Entity): Entity = {
    // Serialize entity
    val outputStream = new ByteArrayOutputStream()
    serialize(entity, new DataOutputStream(outputStream))
    // Deserialize entity
    val inputStream = new ByteArrayInputStream(outputStream.toByteArray)
    deserialize(new DataInputStream(inputStream), entity.schema)
  }

  /* COMPLEX ENTITIES */

  val subSchema1 = EntitySchema(typeUri = Uri("sub1"), typedPaths = IndexedSeq(UntypedPath("path3").asStringTypedPath, UntypedPath("path4").asStringTypedPath), filter = Restriction.empty, UntypedPath("sub1"))
  val subSchema2 = EntitySchema(typeUri = Uri("sub2"), typedPaths = IndexedSeq(UntypedPath("path5").asStringTypedPath, UntypedPath("path6").asStringTypedPath), filter = Restriction.empty, UntypedPath("sub2"))

  val complexSchema = new MultiEntitySchema( schema, IndexedSeq(subSchema1, subSchema2))

  val se1 = Entity("http://silk-framework.com/example/subE1", IndexedSeq(Seq("value3", "value4"), Seq("value11")), subSchema1)
  val se2 = Entity("http://silk-framework.com/example/subE2", IndexedSeq(Seq("value5", "value6"), Seq()), subSchema2)

  "Multiple entities" should "serialize/deserialize correctly" in {
    val outputStream = new ByteArrayOutputStream()
    val dataOutputStream = new DataOutputStream(outputStream)
    serialize(se1, dataOutputStream)
    serialize(se2, dataOutputStream)
    // Deserialize entity
    val inputStream = new ByteArrayInputStream(outputStream.toByteArray)
    val dataInputStream = new DataInputStream(inputStream)
    val re1 = deserialize(dataInputStream, subSchema1)
    val re2 = deserialize(dataInputStream, subSchema2)
    re1 shouldBe se1
    re2 shouldBe se2
  }
}
