/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.silkframework.cache

import org.silkframework.entity.Entity.EntitySerializer

import java.io._
import org.silkframework.entity.{Entity, EntitySchema}

class Partition(val entities: Array[Entity], val indices: Array[BitsetIndex]) {
  require(entities.length == indices.length, "entities.size == indices.size")

  def size: Int = entities.length

  def serialize(stream: DataOutput): Unit = {
    stream.writeInt(entities.length)
    for (i <- entities.indices) {
      EntitySerializer.serialize(entities(i), stream)
      indices(i).serialize(stream)
    }
  }

  override def equals(obj: Any): Boolean = obj match {
    case p: Partition =>
      entities.sameElements(p.entities) && indices.sameElements(p.indices)
    case _ =>
      false
  }
}

object Partition {
  def apply(entities: Array[Entity], indices: Array[BitsetIndex]): Partition = new Partition(entities, indices)

  def apply(entities: Array[Entity], indices: Array[BitsetIndex], count: Int): Partition = {
    if (count < entities.length) {
      val entityArray = new Array[Entity](count)
      Array.copy(entities, 0, entityArray, 0, count)

      val indicesArray = new Array[BitsetIndex](count)
      Array.copy(indices, 0, indicesArray, 0, count)

      new Partition(entityArray, indicesArray)
    } else {
      new Partition(entities, indices)
    }
  }

  def deserialize(stream: DataInput, desc: EntitySchema): Partition = {
    val partitionSize = stream.readInt()
    val entities = new Array[Entity](partitionSize)
    val indices = new Array[BitsetIndex](partitionSize)

    for (i <- 0 until partitionSize) {
      entities(i) = EntitySerializer.deserialize(stream, desc)
      indices(i) = BitsetIndex.deserialize(stream)
    }

    apply(entities, indices)
  }
}