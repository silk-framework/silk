package de.fuberlin.wiwiss.silk.instance

import java.io.{DataInputStream, DataOutputStream}

class Partition(val instances : Array[Instance], val indices : Array[Index])
{
  require(instances.size == indices.size, "instances.size == indices.size")

  def size = instances.size

  def serialize(stream : DataOutputStream)
  {
    stream.writeInt(instances.size)
    for(i <- 0 until instances.size)
    {
      instances(i).serialize(stream)
      indices(i).serialize(stream)
    }
  }
}

object Partition
{
  def apply(instances : Array[Instance], indices : Array[Index]) = new Partition(instances, indices)

  def apply(instances : Array[Instance], indices : Array[Index], count : Int) =
  {
    if(count < instances.size)
    {
      val instanceArray = new Array[Instance](count)
      Array.copy(instances, 0, instanceArray, 0, count)

      val indicesArray = new Array[Index](count)
      Array.copy(indices, 0, indicesArray, 0, count)

      new Partition(instanceArray, indicesArray)
    }
    else
    {
      new Partition(instances, indices)
    }
  }

  def deserialize(stream : DataInputStream, spec : InstanceSpecification) =
  {
    val partitionSize = stream.readInt()
    val instances = new Array[Instance](partitionSize)
    val indices = new Array[Index](partitionSize)

    for(i <- 0 until partitionSize)
    {
      instances(i) = Instance.deserialize(stream, spec)
      indices(i) = Index.deserialize(stream)
    }

    apply(instances, indices)
  }
}