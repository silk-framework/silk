package de.fuberlin.wiwiss.silk.entity

import java.io.{DataInputStream, ByteArrayInputStream, DataOutputStream, ByteArrayOutputStream}

import de.fuberlin.wiwiss.silk.entity.rdf.SparqlEntitySchema
import org.junit.runner.RunWith
import org.scalatest.{Matchers, FlatSpec}
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class EntityTest extends FlatSpec with Matchers {

  val schema = SparqlEntitySchema(paths = IndexedSeq(Path("path1"), Path("path2")))

  val entity1 = new Entity("http://silk-framework.com/example", IndexedSeq(Set("value1", "value2"), Set("value3")), schema)

  val entity2 = new Entity("http://silk-framework.com/example", IndexedSeq(Set("dÃ©clarÃ©s", "v2"), Set("v3")), schema)

  "Entity" should "be serializable" in {
    serialized(entity1) should be (entity1)
  }

  "Entity" should "be serializable if it contains non-ASCII characters" in {
    serialized(entity2) should be (entity2)
  }

  def serialized(entity: Entity): Entity = {
    // Serialize entity
    val outputStream = new ByteArrayOutputStream()
    entity.serialize(new DataOutputStream(outputStream))
    // Deserialize entity
    val inputStream = new ByteArrayInputStream(outputStream.toByteArray)
    Entity.deserialize(new DataInputStream(inputStream), entity.desc)
  }
}