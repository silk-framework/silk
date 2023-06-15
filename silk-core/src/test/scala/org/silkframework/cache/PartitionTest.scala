package org.silkframework.cache

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, DataInputStream, DataOutputStream}
import org.silkframework.entity.{Entity, EntitySchema, Restriction}
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.util.Uri
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers


class PartitionTest extends AnyFlatSpec with Matchers {

  private val schema =
    EntitySchema(
      typeUri = Uri("urn:Type"),
      typedPaths = IndexedSeq(UntypedPath("path1").asStringTypedPath, UntypedPath("path2").asStringTypedPath),
      filter = Restriction.empty
    )

  "Partition" should "be serializable" in {
    val entity = Entity("urn:uri", IndexedSeq(Seq("value1"), Seq()), schema)
    val partition = Partition(Array(entity, entity), Array(BitsetIndex.build(Set(1)), BitsetIndex.build(Set(2))))

    serialized(partition) should be (partition)
  }

  def serialized(partition: Partition): Partition = {
    // Serialize entity
    val outputStream = new ByteArrayOutputStream()
    partition.serialize(new DataOutputStream(outputStream))
    // Deserialize entity
    val inputStream = new DataInputStream(new ByteArrayInputStream(outputStream.toByteArray))
    Partition.deserialize(inputStream, schema)
  }
}
