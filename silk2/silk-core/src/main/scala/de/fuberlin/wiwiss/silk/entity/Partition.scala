package de.fuberlin.wiwiss.silk.entity

import java.io.{DataInputStream, DataOutputStream}

class Partition(val entities: Array[Entity], val indices: Array[Index]) {
  require(entities.size == indices.size, "entities.size == indices.size")

  def size = entities.size

  def serialize(stream: DataOutputStream) {
    stream.writeInt(entities.size)
    for (i <- 0 until entities.size) {
      entities(i).serialize(stream)
      indices(i).serialize(stream)
    }
  }
}

object Partition {
  def apply(entities: Array[Entity], indices: Array[Index]) = new Partition(entities, indices)

  def apply(entities: Array[Entity], indices: Array[Index], count: Int) = {
    if (count < entities.size) {
      val entityArray = new Array[Entity](count)
      Array.copy(entities, 0, entityArray, 0, count)

      val indicesArray = new Array[Index](count)
      Array.copy(indices, 0, indicesArray, 0, count)

      new Partition(entityArray, indicesArray)
    } else {
      new Partition(entities, indices)
    }
  }

  def deserialize(stream: DataInputStream, desc: EntityDescription) = {
    val partitionSize = stream.readInt()
    val entities = new Array[Entity](partitionSize)
    val indices = new Array[Index](partitionSize)

    for (i <- 0 until partitionSize) {
      entities(i) = Entity.deserialize(stream, desc)
      indices(i) = Index.deserialize(stream)
    }

    apply(entities, indices)
  }
}