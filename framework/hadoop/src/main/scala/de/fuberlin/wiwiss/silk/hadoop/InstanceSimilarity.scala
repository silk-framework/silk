package de.fuberlin.wiwiss.silk.hadoop

import org.apache.hadoop.io.Writable
import java.io.{DataInput, DataOutput}

class InstanceSimilarity(var similarity : Double, var targetUri : String) extends Writable
{
    def this() = this(0.0, null)

    override def write(out : DataOutput) : Unit =
    {
        out.writeDouble(similarity)
        out.writeUTF(targetUri)
    }

    override def readFields(in : DataInput) : Unit =
    {
        similarity = in.readDouble()
        targetUri = in.readUTF()
    }

    override def toString = targetUri + " (" + similarity + ")"
}