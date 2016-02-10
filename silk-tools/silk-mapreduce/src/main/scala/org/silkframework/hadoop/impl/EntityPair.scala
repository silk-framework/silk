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

package org.silkframework.hadoop.impl

import java.io._

import org.apache.hadoop.io.Writable
import org.silkframework.entity.Entity
import org.silkframework.util.DPair

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
  implicit def toPair(pair : EntityPair) = DPair(pair.sourceEntity, pair.targetEntity)
}
