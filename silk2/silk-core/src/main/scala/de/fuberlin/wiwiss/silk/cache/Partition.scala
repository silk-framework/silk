/* 
 * Copyright 2009-2011 Freie Universität Berlin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.cache

import java.io.{DataInputStream, DataOutputStream}
import de.fuberlin.wiwiss.silk.entity.{EntityDescription, Entity}

class Partition(val entities: Array[Entity], val indices: Array[BitsetIndex]) {
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
  def apply(entities: Array[Entity], indices: Array[BitsetIndex]) = new Partition(entities, indices)

  def apply(entities: Array[Entity], indices: Array[BitsetIndex], count: Int) = {
    if (count < entities.size) {
      val entityArray = new Array[Entity](count)
      Array.copy(entities, 0, entityArray, 0, count)

      val indicesArray = new Array[BitsetIndex](count)
      Array.copy(indices, 0, indicesArray, 0, count)

      new Partition(entityArray, indicesArray)
    } else {
      new Partition(entities, indices)
    }
  }

  def deserialize(stream: DataInputStream, desc: EntityDescription) = {
    val partitionSize = stream.readInt()
    val entities = new Array[Entity](partitionSize)
    val indices = new Array[BitsetIndex](partitionSize)

    for (i <- 0 until partitionSize) {
      entities(i) = Entity.deserialize(stream, desc)
      indices(i) = BitsetIndex.deserialize(stream)
    }

    apply(entities, indices)
  }
}