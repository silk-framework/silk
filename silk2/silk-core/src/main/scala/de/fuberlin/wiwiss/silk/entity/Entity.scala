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

package de.fuberlin.wiwiss.silk.entity

import xml.Node
import java.io.{DataInputStream, DataOutputStream}

/**
 * A single entity.
 */
class Entity(val uri: String, val values: IndexedSeq[Set[String]], val desc: EntityDescription) {
  def evaluate(path: Path): Set[String] = evaluate(desc.pathIndex(path))

  def evaluate(pathIndex: Int): Set[String] = values(pathIndex)

  override def toString = uri + "\n{\n  " + values.mkString("\n  ") + "\n}"

  def toXML = {
    <Entity uri={uri}> {
      for (valueSet <- values) yield {
        <Val> {
          for (value <- valueSet) yield {
            <e>{value}</e>
          }
        }
        </Val>
      }
    }
    </Entity>
  }

  def serialize(stream: DataOutputStream) {
    stream.writeUTF(uri)
    for (valueSet <- values) {
      stream.writeInt(valueSet.size)
      for (value <- valueSet) {
        stream.writeUTF(value)
      }
    }
  }
}

object Entity {
  def fromXML(node: Node, desc: EntityDescription) = {
    new Entity(
      uri = (node \ "@uri").text.trim,
      values = {
        for (valNode <- node \ "Val") yield {
          { for (e <- valNode \ "e") yield e.text }.toSet
        }
      }.toIndexedSeq,
      desc = desc
    )
  }

  def deserialize(stream: DataInputStream, desc: EntityDescription) = {
    //Read URI
    val uri = stream.readUTF()

    //Read Values
    def readValue = Traversable.fill(stream.readInt)(stream.readUTF).toSet
    val values = IndexedSeq.fill(desc.paths.size)(readValue)

    new Entity(uri, values, desc)
  }
}
