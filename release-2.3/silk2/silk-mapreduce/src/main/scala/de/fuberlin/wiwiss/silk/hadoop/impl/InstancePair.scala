package de.fuberlin.wiwiss.silk.hadoop.impl

import de.fuberlin.wiwiss.silk.instance.Instance
import java.io._
import org.apache.hadoop.io.Writable
import de.fuberlin.wiwiss.silk.util.SourceTargetPair

/**
 * Represents a pair of source and target instance.
 */
class InstancePair(var sourceInstance : Instance, var targetInstance : Instance) extends Writable
{
  def this() = this(null, null)

  override def write(out : DataOutput) : Unit =
  {
    val bytesStream = new ByteArrayOutputStream()
    val objectStream = new ObjectOutputStream(bytesStream)

    objectStream.writeObject(sourceInstance)
    objectStream.writeObject(targetInstance)
    objectStream.flush()
    objectStream.close()

    val instanceBytes = bytesStream.toByteArray
    out.writeInt(instanceBytes.length)
    out.write(instanceBytes)
  }

  override def readFields(in : DataInput) : Unit =
  {
    val instanceBytes = new Array[Byte](in.readInt())
    in.readFully(instanceBytes)

    val bytesStream = new ByteArrayInputStream(instanceBytes)
    val objectStream = new ObjectInputStream(bytesStream)

    sourceInstance = objectStream.readObject().asInstanceOf[Instance]
    targetInstance = objectStream.readObject().asInstanceOf[Instance]

    objectStream.close()
  }
}

object InstancePair
{
  implicit def toPair(pair : InstancePair) = SourceTargetPair(pair.sourceInstance, pair.targetInstance)
}
