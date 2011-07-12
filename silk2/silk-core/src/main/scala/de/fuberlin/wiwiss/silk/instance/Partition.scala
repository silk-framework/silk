package de.fuberlin.wiwiss.silk.instance

import java.io.{DataInputStream, DataOutputStream}

class Partition(val instances : Array[Instance])
{
//  val index : Map[Int, Int] =
//  {
//    for(i <- 0 until instances.size; instances(i).in)
//  }

  //lazy val index = instances.

  def size = instances.size

  def serialize(stream : DataOutputStream)
  {
    stream.writeInt(instances.size)
    for(i <- 0 until instances.size)
    {
      instances(i).serialize(stream)
    }
  }
}

object Partition
{
  def apply(instances : Array[Instance]) = new Partition(instances)

  def apply(instances : Array[Instance], count : Int) =
  {
    if(count < instances.size)
    {
      val instanceArray = new Array[Instance](count)
      Array.copy(instances, 0, instanceArray, 0, count)
      new Partition(instanceArray)
    }
    else
    {
      new Partition(instances)
    }
  }

  def deserialize(stream : DataInputStream, spec : InstanceSpecification) =
  {
    val partitionSize = stream.readInt()
    val instances = new Array[Instance](partitionSize)

    for(i <- 0 until partitionSize)
    {
      instances(i) = Instance.deserialize(stream, spec)
    }

    apply(instances)
  }
}