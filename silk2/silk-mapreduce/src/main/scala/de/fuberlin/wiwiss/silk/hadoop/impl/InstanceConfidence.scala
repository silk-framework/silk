package de.fuberlin.wiwiss.silk.hadoop.impl

import org.apache.hadoop.io.Writable
import java.io.{DataInput, DataOutput}

class InstanceConfidence(var similarity : Double, var targetUri : String) extends Writable
{
  def this() = this(0.0, null)

  override def write(out : DataOutput)
  {
    out.writeDouble(similarity)
    out.writeUTF(targetUri)
  }

  override def readFields(in : DataInput)
  {
    similarity = in.readDouble()
    targetUri = in.readUTF()
  }

  override def toString = targetUri + " (" + similarity + ")"
}