package de.fuberlin.wiwiss.silk.hadoop.impl

import de.fuberlin.wiwiss.silk.entity.Entity
import java.io._
import org.apache.hadoop.io.Writable
import de.fuberlin.wiwiss.silk.util.SourceTargetPair

/**
 * Represents a pair of source and target entity.
 */
class EntityPair(var sourceEntity : Entity, var targetEntity : Entity) extends Writable {
  def this() = this(null, null)

  override def write(out : DataOutput) : Unit = {
    val bytesStream = new ByteArrayOutputStream()
    val objectStream = new ObjectOutputStream(bytesStream)

    objectStream.writeObject(sourceEntity)
    objectStream.writeObject(targetEntity)
    objectStream.flush()
    objectStream.close()

    val entityBytes = bytesStream.toByteArray
    out.writeInt(entityBytes.length)
    out.write(entityBytes)
  }

  override def readFields(in : DataInput) : Unit = {
    val entityBytes = new Array[Byte](in.readInt())
    in.readFully(entityBytes)

    val bytesStream = new ByteArrayInputStream(entityBytes)
    val objectStream = new ObjectInputStream(bytesStream)

    sourceEntity = objectStream.readObject().asInstanceOf[Entity]
    targetEntity = objectStream.readObject().asInstanceOf[Entity]

    objectStream.close()
  }
}

object EntityPair {
  implicit def toPair(pair : EntityPair) = SourceTargetPair(pair.sourceEntity, pair.targetEntity)
}
